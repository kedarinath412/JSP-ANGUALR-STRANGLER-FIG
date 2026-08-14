# Internal Working Guide

This guide explains what happens inside the application from build and WebSphere startup through authentication, JSP rendering, Angular loading, REST calls, transactions, database access, error handling, and logout. It describes the code as it exists in this repository.

## Phase terminology

The repository uses the word "phase" in two related ways:

- Modernization Phase 1: traditional Spring MVC and JSP application.
- Modernization Phase 2: Angular and REST added to the same WAR while JSP remains operational.
- Modernization Phase 3: shared session security for JSP, Angular, and REST.
- Implementation phases 1–9: the smaller construction steps recorded under `docs/implementation/`.

The running application now contains all three modernization phases.

### Implementation-phase coverage

| Implementation phase | Internal behavior explained here |
|---|---|
| 1 — Maven foundation | Reactor and artifact construction in section 1 |
| 2 — Core layers | Service, transactions, DAO, JDBC, and PostgreSQL in section 8 |
| 3 — MVC and JSP | Context hierarchy, routing, binding, validation, and JSP rendering in sections 3–5 |
| 4 — WebSphere and EAR | Archive discovery, context root, descriptors, and JNDI binding in sections 1–2 |
| 5 — Testing/build verification | Maven lifecycle and independently testable boundaries in sections 1 and 14 |
| 6 — Deployment documentation | Runtime contracts and production verification points in sections 2 and 13 |
| 7 — Local runtime | Container topology and automation in section 2 |
| 8 — Angular and REST | Static packaging, Angular startup, REST, and JSON errors in sections 6–7 |
| 9 — Shared security | Login, session sharing, roles, CSRF, and logout in section 9 |

## Runtime architecture

```text
Browser
   |
   | HTTP requests carrying one JSESSIONID cookie
   v
WebSphere Traditional
   |
   v
legacy-poc-ear.ear
   |
   v
legacy-poc-web.war  (context root /legacy-poc)
   |
   +-- Spring Security filter chain
   |
   +-- DispatcherServlet
          |
          +-- JSP controllers ------> JSP views
          |
          +-- REST controllers -----> JSON
          |
          +-- /app handlers --------> Angular static files
                   
JSP controller -----+
REST controller ----+--> EmployeeService --> EmployeeDao --> JdbcTemplate
                                                            |
                                                            v
                                              WebSphere JNDI DataSource
                                                            |
                                                            v
                                                       PostgreSQL
```

JSP and Angular are not separate server applications. They are two presentation paths inside the same WAR and therefore share the same context root, Spring Security filter chain, WebSphere session, service layer, DAO, DataSource, and database.

## 1. What Maven builds

The root Maven reactor builds modules in this order:

```text
legacy-modernization-poc (parent POM)
             |
             +--> legacy-poc-web (WAR)
             |
             +--> legacy-poc-ear (EAR, depends on the WAR)
```

During the WAR build Maven:

1. Installs the project-pinned Node and npm versions under `legacy-poc-web/target`.
2. Runs `npm ci` from the Angular workspace.
3. Compiles the Java classes and runs the Java tests.
4. Runs the Angular unit tests.
5. Produces the Angular browser bundle under `legacy-poc-web/target/angular/browser`.
6. Copies the Angular browser files into `/app` inside the WAR.
7. Packages Java classes, Spring XML, JSP files, descriptors, and application libraries into `legacy-poc-web.war`.
8. Places that WAR inside `legacy-poc-ear.ear`.

The deployable structure is:

```text
legacy-poc-ear.ear
├── META-INF/application.xml
└── legacy-poc-web.war
    ├── app/
    │   ├── index.html
    │   ├── main-<hash>.js
    │   └── styles-<hash>.css
    ├── resources/
    └── WEB-INF/
        ├── web.xml
        ├── ibm-web-bnd.xml
        ├── spring/dispatcher-servlet.xml
        ├── views/*.jsp
        ├── classes/
        │   ├── com/example/legacypoc/**/*.class
        │   └── spring/application-context.xml
        └── lib/
            ├── Spring Framework
            ├── Spring Security
            └── other application libraries
```

The Servlet API is not placed in `WEB-INF/lib` because WebSphere provides it. The PostgreSQL JDBC driver is also server-managed for this POC rather than embedded in the application.

## 2. What WebSphere does during deployment and startup

### Local runtime topology

The verified local POC runs WebSphere and PostgreSQL in separate Docker containers connected to `legacy-poc-network`:

```text
Host browser
    |
    | localhost:9080
    v
legacy-poc-websphere
    |
    | Docker DNS: legacy-poc-postgres:5432
    v
legacy-poc-postgres
    |
    +-- database: legacy_poc
    +-- schema owner: legacy_poc

Host psql, when needed, uses localhost:15432.
```

The containers separate application-server configuration from database configuration. WebSphere does not connect through PostgreSQL's host-mapped port; it uses the PostgreSQL container name and internal port on the shared Docker network.

The local setup sequence is:

1. PostgreSQL starts and the three `db/` scripts create/sample/drop the schema as requested.
2. The PostgreSQL JDBC JAR is made available to WebSphere at `/work/lib/postgresql.jar`.
3. `configure-websphere.sh` passes the database password into the container process without printing it.
4. Its WebSphere Jython script creates or updates the J2C alias, JDBC provider, and DataSource.
5. `test-datasource.sh` asks WebSphere to open a real pooled connection and checks its server log for the success message.
6. `deploy-application.sh` installs or updates the EAR, maps it to the server, saves configuration, and starts the application.

The shell scripts are the developer-facing interface. The `.py` files are Jython executed inside WebSphere `wsadmin`, where IBM administrative objects such as `AdminConfig`, `AdminTask`, `AdminControl`, and `AdminApp` exist. They are not application runtime code and are not loaded by the WAR.

### EAR discovery

WebSphere opens the EAR and reads `META-INF/application.xml`. It discovers `legacy-poc-web.war` as a web module and assigns it the context root `/legacy-poc`.

Consequently, application-relative `/employees` becomes the external URL `/legacy-poc/employees`.

### Resource binding

The WAR declares the logical resource reference `jdbc/LegacyPocDS` in `web.xml`. `WEB-INF/ibm-web-bnd.xml` maps that reference to the WebSphere JNDI name `jdbc/LegacyPocDS`.

```text
Application resource reference
    jdbc/LegacyPocDS
            |
            | ibm-web-bnd.xml or install-time mapping
            v
WebSphere JNDI DataSource
    jdbc/LegacyPocDS
            |
            v
WebSphere connection pool + PostgreSQL JDBC provider
```

The same mapping can be confirmed or overridden during EAR installation. The DataSource must be visible at the scope where the application runs.

### `web.xml` initialization order

The web module uses a traditional deployment descriptor. Its important registrations are:

1. `CharacterEncodingFilter` applies UTF-8 to every request and response.
2. `DelegatingFilterProxy` applies the Spring bean named `springSecurityFilterChain` to every URL.
3. `ContextLoaderListener` creates the root Spring application context.
4. `DispatcherServlet` creates its child MVC context at startup because `load-on-startup` is `1`.
5. The DispatcherServlet mapping `/` makes Spring MVC the front controller for application requests.
6. The resource reference declares the container-managed DataSource contract.

Filters execute before the DispatcherServlet. A denied request therefore never reaches an MVC controller.

## 3. The two Spring application contexts

Spring creates a parent/child context hierarchy:

```text
Root ApplicationContext
created by ContextLoaderListener
│
├── SecurityConfiguration
├── DataSource (JNDI lookup)
├── JdbcTemplate
├── DataSourceTransactionManager
├── JdbcEmployeeDao
└── DefaultEmployeeService
          ^
          | parent beans are visible to the child
          |
DispatcherServlet child ApplicationContext
├── EmployeeController
├── EmployeeRestController
├── AuthenticationController
├── exception advice
├── annotated MVC infrastructure
├── static resource handlers
└── JSP view resolver
```

The root context scans only DAO, service, and security packages. The DispatcherServlet context scans the controller package. This prevents controllers from being registered twice.

The child context can inject beans from its parent, which is why both MVC and REST controllers can receive the same `EmployeeService`. The service has no dependency on JSP, Angular, HTTP requests, controller models, or JSON.

### Root context creation

`application-context.xml` performs these operations:

1. Looks up `java:comp/env/jdbc/LegacyPocDS` through Spring JNDI support.
2. Exposes the result as the `dataSource` bean.
3. Constructs one `JdbcTemplate` using that DataSource.
4. Constructs `DataSourceTransactionManager` using the same DataSource.
5. Enables `@Transactional` interception.
6. discovers DAO, service, and security components.

The default JNDI lookup is performed during context initialization. A missing or incorrectly scoped DataSource normally prevents the root context—and therefore the application—from starting successfully. Database host, port, database name, username, and password remain in WebSphere configuration.

### DispatcherServlet context creation

`dispatcher-servlet.xml`:

1. Discovers annotated controllers and controller advice.
2. Enables annotated MVC request mapping, binding, validation, and HTTP message conversion.
3. Registers `/resources/**` for traditional static resources.
4. Registers `/app/**` for packaged Angular files.
5. Registers the `/app` redirect and `/app/` forward.
6. Configures `InternalResourceViewResolver` with `/WEB-INF/views/` and `.jsp`.

## 4. How a request is selected

Every application request first enters WebSphere under `/legacy-poc`. WebSphere removes the context root before matching paths inside the WAR.

For example:

```text
Browser URL                         Path seen by this web application
/legacy-poc/employees              /employees
/legacy-poc/app/                   /app/
/legacy-poc/api/employees          /api/employees
```

The processing sequence is:

```text
WebSphere
   |
CharacterEncodingFilter
   |
Spring Security filter chain
   |
DispatcherServlet
   |
HandlerMapping selects controller, view-controller, or resource handler
```

The DispatcherServlet does not decide that a path is "JSP" or "Angular" from the file extension. It consults the mappings registered by annotations and `dispatcher-servlet.xml`.

### Route ownership

| Internal path | Handler | Result |
|---|---|---|
| `/login` GET | `AuthenticationController` | Logical JSP view `login` |
| `/login` POST | Spring Security filter | Authentication; no MVC controller |
| `/` GET | `EmployeeController` | Logical JSP view `home` |
| `/employees...` | `EmployeeController` | JSP model, view, or redirect |
| `/api/session` GET | `AuthenticationController` with `@ResponseBody` | JSON session metadata |
| `/api/employees...` | `EmployeeRestController` | JSON or HTTP status |
| `/app` GET | MVC view-controller | Redirect to `/app/` |
| `/app/` GET | MVC view-controller | Forward to `/app/index.html` |
| `/app/**` | MVC resource handler | Angular HTML, JavaScript, and CSS |
| `/resources/**` | MVC resource handler | JSP-side CSS and static files |

Anything not explicitly allowed by the security rules is denied, and anything with no MVC/resource mapping produces a not-found response.

## 5. JSP request lifecycle

For `GET /legacy-poc/employees`:

```text
Browser sends GET /legacy-poc/employees + JSESSIONID
    |
    v
WebSphere resolves the session
    |
    v
Spring Security restores the SecurityContext
    |
    v
Authorization requires VIEWER or ADMIN
    |
    v
DispatcherServlet selects EmployeeController.listEmployees
    |
    v
EmployeeService.getEmployees
    |
    v
EmployeeDao.findAll -> JdbcTemplate -> DataSource -> PostgreSQL
    |
    v
List<Employee> added to the MVC Model as "employees"
    |
    v
Controller returns logical view name "employees"
    |
    v
ViewResolver expands it to /WEB-INF/views/employees.jsp
    |
    v
JSP + JSTL + Spring Security tags render HTML
```

Controllers return logical view names, never physical JSP paths. JSPs live under `WEB-INF`, so a browser cannot request them directly and bypass the controller.

JSTL and EL read the model. Spring form tags bind the `Employee` object and render validation messages. Spring Security tags display identity information and hide admin-only controls. Hiding controls improves the UI, while the server-side authorization rules remain the actual protection.

### JSP create sequence

1. An admin opens `GET /employees/new`.
2. The controller adds a new `Employee`, form title, and action to the model.
3. `employee-form.jsp` renders the form and CSRF token.
4. The browser posts to `/employees` with fields, `JSESSIONID`, and CSRF token.
5. The security filter validates the session, ADMIN role, and CSRF token.
6. Spring MVC binds request fields to `Employee` and runs Bean Validation.
7. With binding errors, the controller returns `employee-form`; no service call occurs.
8. With valid data, the controller calls `EmployeeService.createEmployee`.
9. On success, a flash message is stored and the controller returns `redirect:/employees`.
10. The browser performs a new GET, preventing duplicate form submission on refresh.

Duplicate-email business errors are converted into an `email` field error and the same form is rendered. Unexpected JSP-path exceptions are logged and mapped to a friendly `error.jsp`; internal details are not sent to the browser.

Update follows the same pattern with `POST /employees/{id}`. Delete uses `POST /employees/{id}/delete`, so a state-changing operation is not exposed as a GET.

## 6. Angular loading and routing

Angular is a compiled set of static browser files inside the WAR. There is no Node server at runtime.

For `GET /legacy-poc/app`:

1. Spring Security requires an authenticated viewer or admin.
2. MVC redirects `/app` to `/app/`.
3. The browser requests `/app/`.
4. MVC forwards internally to `/app/index.html`.
5. The resource handler reads `index.html` from the WAR.
6. Because Angular was built with `baseHref` equal to `./`, the trailing slash makes relative bundle URLs resolve under `/legacy-poc/app/`.
7. The browser requests the hashed JavaScript and CSS files from `/app/**`.
8. Spring Security checks the same session for those requests, and the MVC resource handler serves the files.
9. Angular bootstraps in the browser.

The `/app` to `/app/` redirect is therefore URL normalization for correct relative-asset resolution. It is not a second application or an authentication redirect.

The current Angular POC has one application screen and no Angular client-side router. If client-side routes are introduced later, a server fallback to `app/index.html` will be needed for those route URLs.

## 7. Angular session and REST lifecycle

When `AppComponent` starts:

```text
Angular AppComponent.ngOnInit
          |
          v
EmployeeService.getSession -> GET /api/session
          |
          +--> username and roles
          +--> CSRF header name and token
          |
          v
Component decides whether admin editing UI is visible
          |
          v
EmployeeService.getEmployees -> GET /api/employees
          |
          v
Render employee table
```

`EmployeeService` derives `/legacy-poc` from the current `/app` URL, so it does not hard-code a server name, port, or deployment context. Calls are same-origin. The browser therefore attaches the same WebSphere `JSESSIONID` cookie automatically; Angular does not store a second access token and does not need `withCredentials` for this deployment arrangement.

`GET /api/session` returns presentation-safe session metadata:

- username;
- granted roles;
- CSRF header name;
- CSRF token.

It deliberately does not return the session ID. The session ID remains in the HttpOnly cookie controlled by the browser and server.

For an Angular create operation:

1. Angular validates the reactive form.
2. `EmployeeService` sends JSON to `POST /api/employees`.
3. It adds the CSRF token using the header name obtained from `/api/session`.
4. The browser adds `JSESSIONID`.
5. Spring Security validates CSRF and requires the ADMIN role.
6. `EmployeeRestController` deserializes JSON into `Employee` and Bean Validation runs.
7. The controller calls the same `EmployeeService` used by JSP.
8. The REST controller returns `201 Created` after insert.
9. Angular performs another GET and refreshes its table.

Update uses `PUT /api/employees/{id}` and returns `204 No Content`. Delete uses `DELETE /api/employees/{id}` and also returns `204 No Content`.

`RestExceptionHandler` converts application errors to stable JSON responses:

| Condition | Status | Response behavior |
|---|---:|---|
| Invalid request body | 400 | Message plus field errors |
| Missing employee | 404 | Generic not-found message |
| Duplicate email | 409 | Email field error |
| Unexpected failure | 500 | Generic message; details logged server-side |

## 8. Service, transaction, DAO, and connection internals

Both presentation paths converge at the same Spring service:

```text
EmployeeController --------+
                            |
                            +--> proxied EmployeeService method
                            |
EmployeeRestController ----+
                                      |
                                      v
                                JdbcEmployeeDao
                                      |
                                      v
                                 JdbcTemplate
```

### Transaction boundary

`DefaultEmployeeService` is a Spring `@Service` with `@Transactional`. Spring wraps it with a transaction interceptor. When a controller calls a public service method:

1. The transaction interceptor runs before the actual method.
2. `DataSourceTransactionManager` obtains a connection from the JNDI DataSource.
3. Spring binds that connection to the current request thread for the transaction.
4. The service applies business rules and calls the DAO.
5. `JdbcTemplate` participates in the same thread-bound connection instead of opening an independent transaction.
6. If the method completes, Spring commits.
7. If a qualifying runtime exception escapes, Spring rolls back.
8. The logical connection is closed and returned to WebSphere's pool.

Read methods are marked `readOnly = true`; write methods use normal transactions. This is a local, single-DataSource transaction strategy. If production already uses JTA, the application should reuse that established transaction design.

### Business rules

The service, not the controllers, owns reusable rules:

- trims submitted text fields;
- rejects duplicate email addresses;
- verifies that an employee exists before update;
- treats zero-row update/delete results as not found;
- logs create, update, delete, and missing-employee operations.

The database unique constraint remains the final integrity protection. The service pre-check supplies a friendlier normal-path error, while the constraint protects against concurrent duplicates.

### DAO behavior

`JdbcEmployeeDao` owns SQL. It:

- uses `JdbcTemplate` rather than `DriverManager`;
- uses `?` placeholders for every user-supplied value;
- maps result rows through one reusable `RowMapper<Employee>`;
- returns `null` for an absent `findById`, which the service converts to `EmployeeNotFoundException`;
- logs Spring `DataAccessException` failures and rethrows them for transaction/error handling.

`JdbcTemplate` creates prepared statements, binds values, executes SQL, iterates result sets, translates JDBC exceptions into Spring exceptions, and closes JDBC resources. With a pooled WebSphere DataSource, closing the logical connection returns it to the pool rather than destroying the physical database connection.

## 9. Authentication, authorization, session, and CSRF internals

### One session for every presentation path

The shared identity works because JSP, Angular files, and REST endpoints are all under the same origin and WAR:

```text
Browser cookie: JSESSIONID=<opaque value>
                  |
                  v
WebSphere HttpSession
                  |
                  v
Spring SecurityContext
                  |
        +---------+---------+
        |                   |
   JSP requests       Angular REST requests
```

Spring Security stores the authenticated `SecurityContext` in the WebSphere-managed `HttpSession`. On each request, its filter chain loads the context before authorization and saves any changes afterward.

### Login sequence

1. An unauthenticated UI request is redirected to `/login`.
2. `GET /login` reaches `AuthenticationController`, which returns `login.jsp`.
3. The login JSP posts username, password, and CSRF token to `/login`.
4. `UsernamePasswordAuthenticationFilter` handles that POST; there is no controller POST method.
5. `UserDetailsService` loads the POC identity and `BCryptPasswordEncoder` verifies the password.
6. On success, Spring creates an authenticated `SecurityContext` and associates it with the WebSphere session.
7. Session-fixation protection changes the session ID after authentication.
8. WebSphere returns the updated `JSESSIONID` cookie.
9. The browser sends that cookie for later `/employees/**`, `/app/**`, and `/api/**` requests.
10. The saved request may be restored; otherwise the default success URL is `/`.

The current users are intentionally in memory for the POC. Replacing `UserDetailsService` with an enterprise-approved WebSphere registry, LDAP, or federated identity integration should not change controllers, services, DAO, JSP/Angular session sharing, or route structure.

### Authorization matrix

| Request | Viewer | Admin |
|---|---:|---:|
| GET `/`, `/employees/**`, `/app/**` | Allowed | Allowed |
| GET `/api/**` | Allowed | Allowed |
| POST `/employees/**` | Denied | Allowed with CSRF |
| POST/PUT/DELETE `/api/**` | Denied | Allowed with CSRF |

The current rule allows authenticated viewers to open JSP form GET URLs directly, although the list-page controls are hidden and all state-changing POST requests are denied. If form visibility itself must be restricted, add explicit ADMIN rules for `GET /employees/new` and `GET /employees/{id}/edit` before the broader `/employees/**` rule.

Unauthenticated UI requests receive a login redirect. Unauthenticated API GET requests receive JSON `401`. Authenticated API requests without sufficient role receive JSON `403`.

### CSRF sequence

The session cookie is sent automatically, so unsafe requests require protection against cross-site request forgery:

- Spring-generated JSP forms include a hidden CSRF field.
- Explicit JSP POST forms include the same token.
- Angular first reads the synchronizer token from `/api/session`.
- Angular adds it as a request header to POST, PUT, DELETE, and logout.
- Spring Security compares the submitted token with the token associated with the session before the controller runs.

A missing or invalid CSRF token causes rejection before MVC business logic. CSRF protection and role authorization are separate checks; an admin needs both a valid role and valid CSRF token for writes.

### Logout sequence

1. JSP or Angular sends `POST /logout` with the CSRF token.
2. Spring Security's logout filter handles the request; no MVC logout controller is called.
3. Authentication is cleared.
4. The WebSphere `HttpSession` is invalidated.
5. Spring requests deletion of the `JSESSIONID` cookie.
6. The response redirects to `/login?logout`.
7. Reusing the previous session no longer authorizes JSP, Angular, or REST requests.

## 10. Error and logging boundaries

The application deliberately has separate presentation error contracts:

- JSP controller exceptions become a friendly JSP error page.
- REST controller exceptions become JSON with an HTTP status.
- Security failures become redirects for UI paths and JSON status responses for API paths.
- Underlying exceptions are logged with SLF4J; credentials and session IDs are not logged.

This separation prevents an Angular client from receiving HTML error pages for controller errors and prevents a JSP user from seeing raw JSON or stack traces.

## 11. Complete read and write comparisons

### Read through JSP

```text
GET /employees
 -> security session/role check
 -> EmployeeController
 -> EmployeeService read-only transaction
 -> JdbcEmployeeDao
 -> JdbcTemplate
 -> WebSphere pooled connection
 -> PostgreSQL
 -> List<Employee>
 -> MVC Model
 -> employees.jsp
 -> HTML
```

### Read through Angular

```text
GET /app/
 -> authenticated Angular static bundle
 -> GET /api/session using the same JSESSIONID
 -> GET /api/employees
 -> security session/role check
 -> EmployeeRestController
 -> the same EmployeeService read-only transaction
 -> the same DAO/DataSource/PostgreSQL
 -> JSON List<Employee>
 -> Angular table
```

### Write through either UI

```text
JSP form POST ------------------+
                                |
                                +-> session + role + CSRF
                                +-> validation
Angular JSON POST/PUT/DELETE ---+
                                |
                                v
                        EmployeeService transaction
                                |
                        business rules + DAO SQL
                                |
                        commit or rollback
                                |
                       redirect HTML or JSON status
```

## 12. Why this structure supports continued modernization

The stable center of the application is:

```text
EmployeeService -> EmployeeDao -> JdbcTemplate -> JNDI DataSource -> PostgreSQL
```

JSP and Angular are adapters around that center. A future UI or API authentication mechanism can be added without moving SQL into controllers or duplicating business rules. The current strangler path is already proving this boundary:

```text
Legacy JSP -> MVC Controller ----+
                                  +-> existing EmployeeService
Modern Angular -> REST Controller-+
```

If Angular is later deployed on a different origin, cookie behavior, CORS, CSRF, identity tokens, and logout must be redesigned deliberately. The current automatic shared session depends on Angular and REST remaining same-origin inside this WebSphere WAR.

## 13. Production items WebSphere owners must verify

- Application classloader policy when packaging Spring libraries in the WAR.
- Approved Java SDK, Spring versions, PostgreSQL JDBC provider, and WebSphere fix pack.
- JNDI DataSource scope and module resource-reference mapping.
- Connection-pool size, timeouts, validation, leak detection, and monitoring.
- Whether local JDBC transactions or the existing JTA strategy is required.
- HTTPS and `Secure`, `HttpOnly`, and appropriate `SameSite` cookie behavior.
- Session timeout, session affinity/replication, failover, and concurrent-session policy.
- Enterprise identity source, role mapping, account lifecycle, and audit requirements.
- CSRF, security headers, authorization rules, and error logging under organizational standards.

This guide explains the current POC mechanics; these production choices remain environment-specific.

## 14. How the automated tests avoid WebSphere and PostgreSQL

The Maven build does not connect to a live application server or database:

- service tests mock `EmployeeDao` and verify business decisions and calls;
- MVC tests invoke controllers through Spring's mock servlet infrastructure;
- REST tests verify mapping, validation, status codes, and JSON error contracts with a mocked service;
- security tests run the real Spring Security filter chain against mock HTTP sessions and requests;
- Angular tests mock HTTP and verify session-first loading, rendering, and CSRF headers.

This division tests application behavior while keeping WebSphere/JNDI/PostgreSQL integration as an explicit manual or local-container verification layer. `mvn clean package` can therefore run on a developer machine or CI worker without server credentials.
