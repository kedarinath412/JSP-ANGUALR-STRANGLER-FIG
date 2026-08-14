# Legacy Modernization POC — Phases 1, 2, and 3

This repository is a runnable strangler-fig modernization POC built with Java 8, Spring Framework 5.3, Spring Security 5.8, Spring MVC, JSP, Angular 22, Spring JDBC, PostgreSQL, and IBM WebSphere Application Server Traditional 9.x. Maven builds one WAR containing both the existing JSP UI and the new Angular UI, then embeds it in an EAR.

Phase 1 established the traditional JSP application. Phase 2 adds Angular and JSON REST controllers while deliberately reusing the existing `EmployeeService`, DAO, transaction boundaries, JNDI DataSource, and database. The application still contains no Spring Boot, embedded server, JPA, Hibernate, Jakarta APIs, or hard-coded database credentials.

Phase 3 adds session-based identity and access management. A single Spring Security context is stored in WebSphere's `HttpSession`, so the same `JSESSIONID` authenticates legacy JSP navigation, Angular static resources, and Angular REST calls.

## Architecture

```text
Browser
   |
   v
IBM WebSphere Traditional 9.x
   |
   v
EAR
   |
   +-- WAR
        |
        +-- JSP <------ MVC Controller --+
        +-- Angular <-- REST Controller --+--> EmployeeService
                                                   |
                                                   v
                                             EmployeeDao
                                                   |
                                             JdbcTemplate
                                                   |
                                    WebSphere JNDI DataSource
                                                   |
                                              PostgreSQL
```

```text
Presentation
------------
JSP

Web Layer
---------
Spring MVC Controller

Business Layer
--------------
Spring Service

Persistence Layer
-----------------
Spring JdbcTemplate DAO

Infrastructure
--------------
WebSphere JNDI DataSource

Database
--------
PostgreSQL
```

Request lifecycle:

```text
GET /employees
       |
       v
EmployeeController
       |
       v
EmployeeService
       |
       v
EmployeeDao
       |
       v
JdbcTemplate
       |
       v
JNDI DataSource
       |
       v
PostgreSQL
       |
       v
List<Employee>
       |
       v
Controller Model
       |
       v
employees.jsp
       |
       v
HTML
```

Phase 2 request lifecycle:

```text
Browser -> /app/ -> Angular
                       |
                       v
                /api/employees
                       |
                       v
          EmployeeRestController
                       |
                       v
            existing EmployeeService
                       |
                       v
              existing EmployeeDao
                       |
                       v
             JdbcTemplate -> JNDI -> PostgreSQL
```

The two presentation paths coexist in the same WAR:

```text
Legacy:  /employees/** -> MVC Controller  -> existing EmployeeService
Modern:  /app/**       -> Angular -> /api/** -> REST Controller -> existing EmployeeService
```

The root Spring context owns the DataSource, `JdbcTemplate`, transaction manager, DAO, and service. The DispatcherServlet child context owns controllers and JSP configuration. The stable JNDI contract is:

```text
WebSphere JNDI:        jdbc/LegacyPocDS
Application lookup:   java:comp/env/jdbc/LegacyPocDS
```

## Project layout

```text
.
├── pom.xml
├── README.md
├── db/
│   ├── 01-create-employee-table.sql
│   ├── 02-insert-sample-data.sql
│   └── 03-drop-employee-table.sql
├── docs/implementation/
├── local-poc/
│   ├── configure-websphere.sh
│   ├── test-datasource.sh
│   ├── deploy-application.sh
│   ├── configure-websphere-postgresql.py
│   ├── test-websphere-datasource.py
│   └── deploy-application.py
├── legacy-poc-web/
│   ├── pom.xml
│   ├── src/main/frontend/       # Angular workspace
│   └── src/main + src/test      # Java, Spring, JSP, and tests
└── legacy-poc-ear/
    ├── pom.xml
    └── src/main/application/META-INF/application.xml
```

## Build and tests

Use Java 8 for the target-compatible build. Maven installs Node 24.15.0 locally under `target`, runs `npm ci`, runs Angular unit tests, produces the Angular bundle, and packages it into the WAR; no global Angular CLI is required.

```bash
JAVA_HOME=/Users/kedar/Library/Java/JavaVirtualMachines/corretto-1.8.0_472/Contents/Home \
mvn clean package
```

The build does not require WebSphere or PostgreSQL. Unit tests mock those boundaries. The verified build runs 28 Java tests and 3 Angular tests, including shared-session security and initial employee-table rendering under Angular's zoneless change detection.

Expected artifacts:

```text
legacy-poc-web/target/legacy-poc-web.war
legacy-poc-ear/target/legacy-poc-ear.ear
```

The EAR is the primary deployment artifact.

## Angular packaging inside the EAR

Angular is not deployed as a separate application, WAR, or server process. Its source is compiled into static HTML, JavaScript, and CSS, which Maven places inside the existing Spring MVC WAR.

The packaging flow is:

```text
legacy-poc-web/src/main/frontend/       Angular source workspace
                    |
                    | frontend-maven-plugin
                    | npm ci, npm test, npm run build
                    v
legacy-poc-web/target/angular/browser/  Production browser files
                    |
                    | maven-war-plugin webResources
                    v
legacy-poc-web.war!/app/                Files inside the WAR
                    |
                    | maven-ear-plugin
                    v
legacy-poc-ear.ear!/legacy-poc-web.war  WAR inside the EAR
```

`angular.json` configures the production output directory as:

```text
legacy-poc-web/target/angular/browser/
```

Typical generated files are:

```text
index.html
main-<content-hash>.js
styles-<content-hash>.css
```

The WAR plugin configuration in `legacy-poc-web/pom.xml` copies that directory to target path `app`. The resulting archives therefore look like:

```text
legacy-poc-ear.ear
├── META-INF/application.xml
└── legacy-poc-web.war
    ├── app/
    │   ├── index.html
    │   ├── main-<content-hash>.js
    │   └── styles-<content-hash>.css
    └── WEB-INF/
        ├── web.xml
        ├── classes/
        ├── lib/
        ├── spring/
        └── views/
```

WebSphere deploys the WAR at context root `/legacy-poc`. Spring maps `/app/` to `/app/index.html` and serves `/app/**` as static resources. Consequently, the browser URLs are:

```text
Entry point:  http://localhost:9080/legacy-poc/app/
JavaScript:   http://localhost:9080/legacy-poc/app/main-<content-hash>.js
CSS:          http://localhost:9080/legacy-poc/app/styles-<content-hash>.css
REST API:     http://localhost:9080/legacy-poc/api/employees
```

The Angular application executes in the browser and calls the REST API in the same WAR. WebSphere only serves the compiled Angular files; it does not run Node or the Angular CLI at runtime.

## Shared JSP and Angular authentication session

Both presentation paths are same-origin and live in the same WAR:

```text
Browser cookie: JSESSIONID
          |
          v
WebSphere HttpSession
          |
          v
Spring SecurityContext
          |
          +--> JSP /employees/**
          +--> Angular /app/**
          +--> REST /api/**
```

There is no separate Angular login token and no browser-stored JWT. After form login, the browser automatically sends the same `JSESSIONID` cookie to all `/legacy-poc/**` requests. Angular calls `GET /api/session` to obtain the authenticated username, roles, and the CSRF header/token required for state-changing REST calls.

Local demo identities:

| Username | Password | Access |
|---|---|---|
| `employee-admin` | `admin-demo` | JSP/Angular read and write |
| `employee-viewer` | `viewer-demo` | JSP/Angular read only |

These in-memory identities are only for local architecture validation. They are not a production identity store. A production application should replace `UserDetailsService` with the organization's WebSphere registry/LDAP integration or an approved OIDC/SAML provider while retaining the shared server-side session and authorization design.

Route behavior:

```text
Unauthenticated /employees/** or /app/** -> 302 /login
Unauthenticated /api/**                  -> JSON 401
Viewer GET requests                      -> allowed
Viewer write requests                    -> JSON/HTTP 403
Admin write requests                     -> allowed with CSRF token
POST /logout                             -> session invalidated
```

Spring Security changes the session ID after successful login to protect against session fixation. CSRF protection remains enabled: Spring form tags/manual JSP hidden fields protect JSP POSTs, and Angular sends the token returned by `/api/session` in the configured request header.

For a WebSphere cluster, verify session affinity or replication, cookie domain/path, HTTPS-only secure cookies, session timeout, SameSite policy, and the organization's SSO/logout requirements. Sharing works directly here because JSP, Angular, and REST are under the same scheme, host, port, context root, WAR, and `HttpSession`.

## Local POC status

The following local environment was configured and verified on Apple Silicon:

| Component | Local value |
|---|---|
| WebSphere | Traditional 9.0.5.28, `linux/amd64` emulation |
| WebSphere Java | IBM Java 8.0.8.70 |
| PostgreSQL | 17 Alpine, native `linux/arm64` |
| PostgreSQL container | `legacy-poc-postgres` |
| PostgreSQL database/user | `legacy_poc` / `legacy_poc` |
| PostgreSQL host port | `15432` |
| Docker network | `legacy-poc-network` |
| JDBC driver | PostgreSQL JDBC 42.7.11, managed by WebSphere |
| J2C alias | `DefaultNode01/LegacyPocPostgresAuth` |
| DataSource | `jdbc/LegacyPocDS` |
| Application | `legacy-poc-ear` |
| Context root | `/legacy-poc` |

Generated WebSphere and PostgreSQL passwords are stored only in their Docker container configurations. They are not committed to this repository.

Current URLs:

```text
Application:       http://localhost:9080/legacy-poc/
Employee list:     http://localhost:9080/legacy-poc/employees
Angular UI:        http://localhost:9080/legacy-poc/app/
REST employees:    http://localhost:9080/legacy-poc/api/employees
Admin console:     https://localhost:9043/ibm/console
PostgreSQL:        localhost:15432
```

Use `wsadmin` user `wsadmin` for the admin console. To retrieve its generated local password directly in your terminal:

```bash
docker exec legacy-poc-websphere cat /tmp/PASSWORD
```

Do not paste or commit the password.

## PostgreSQL schema setup

For the configured local container:

```bash
docker exec -i legacy-poc-postgres \
  psql -v ON_ERROR_STOP=1 -U legacy_poc -d legacy_poc \
  < db/01-create-employee-table.sql

docker exec -i legacy-poc-postgres \
  psql -v ON_ERROR_STOP=1 -U legacy_poc -d legacy_poc \
  < db/02-insert-sample-data.sql
```

Verify:

```bash
docker exec legacy-poc-postgres \
  psql -U legacy_poc -d legacy_poc \
  -c 'SELECT * FROM EMPLOYEE ORDER BY EMPLOYEE_ID;'
```

The expected employees are John Smith, Sarah Williams, and David Miller. Cleanup is available through `db/03-drop-employee-table.sql`.

For a non-container PostgreSQL installation, connect as the POC schema owner with `psql`, execute the same scripts, and configure the environment-specific server, port, and database in WebSphere.

## WebSphere PostgreSQL configuration

The JDBC driver is server-managed and intentionally absent from the WAR/EAR.

### JDBC driver and provider

1. Install a Java 8-compatible PostgreSQL JDBC driver in an administrator-managed server directory.
2. Create a server-scoped **User-defined JDBC Provider**.
3. Use implementation class:

```text
org.postgresql.ds.PGConnectionPoolDataSource
```

4. Set the provider classpath to the server-side PostgreSQL JDBC JAR.
5. Use non-XA/one-phase operation for this single-database POC.

The local container uses `/work/lib/postgresql.jar`. Use the readable wrapper:

```bash
DB_PASSWORD='your-local-postgres-password' ./local-poc/configure-websphere.sh
```

It invokes the idempotent WebSphere Jython configuration internally. Then verify the connection:

```bash
./local-poc/test-datasource.sh
```

### Authentication alias

Create a secured J2C authentication alias:

```text
Alias:    LegacyPocPostgresAuth
User:     environment-specific PostgreSQL user
Password: stored only in WebSphere
```

Never store a real database password in Git.

### DataSource

Create a DataSource under the provider:

```text
Name:       LegacyPocDS
JNDI name:  jdbc/LegacyPocDS
Helper:     com.ibm.websphere.rsadapter.GenericDataStoreHelper
Auth alias: LegacyPocPostgresAuth
```

Custom properties:

```text
serverName   = environment PostgreSQL host
portNumber   = environment PostgreSQL port
databaseName = legacy_poc
```

For containers on the local POC network, `serverName` is `legacy-poc-postgres` and `portNumber` is `5432`. Host tools connect through `localhost:15432` instead.

Restart WebSphere after changing the JDBC provider classpath or implementation. Run **Test connection**. A user-defined provider logs `DSRA0174W` because WebSphere uses `GenericDataStoreHelper`; `DSRA8030I: Successfully connected to DataSource` confirms success.

## EAR deployment

From the administrative console:

1. Open **Applications → New Application → New Enterprise Application**.
2. Select `legacy-poc-ear/target/legacy-poc-ear.ear`.
3. Map `legacy-poc-web.war` to `server1` or the intended cluster.
4. Verify context root `/legacy-poc`.
5. Verify resource reference `jdbc/LegacyPocDS` maps to `jdbc/LegacyPocDS`.
6. Save, synchronize if applicable, and start the application.

For repeatable local deployment after `mvn clean package`, run:

```bash
./local-poc/deploy-application.sh
```

The shell wrapper copies the EAR and invokes the required WebSphere Jython deployment internally. `WEB-INF/ibm-web-bnd.xml` contains the same resource mapping. See `local-poc/README.md` for a step-by-step explanation of all three commands.

## Transaction strategy

Service methods own transaction boundaries through Spring `@Transactional`. This single-resource POC uses `DataSourceTransactionManager` around the WebSphere-managed DataSource. Reads are marked read-only and writes use normal local transactions.

This POC demonstrates Spring-managed local JDBC transactions.

If the existing production WebSphere application already uses JTA or a
WebSphere-specific transaction manager, the new module should reuse the
existing transaction strategy rather than introducing a second one.

## Acceptance tests

1. Open `/legacy-poc/`; expect **Legacy Modernization POC**.
2. Open `/legacy-poc/employees`; expect the three PostgreSQL sample employees.
3. Create Mike Johnson with `mike.johnson@example.com`; expect redirect and database row.
4. Change department from Engineering to Architecture; verify UI and database.
5. Delete Mike; verify the row no longer exists.
6. Submit a duplicate email; expect a friendly field error, never a database stack trace.
7. Submit without first/last name; expect field validation errors and no insert.

All Phase 1 tests were executed successfully against the local WebSphere/PostgreSQL runtime. Phase 2 additionally verified Angular entry/static assets and REST list, get, create, update, delete, validation, duplicate-email, and not-found responses.

## Phase 2 implementation

Phase 1 continues unchanged:

```text
Browser -> WebSphere -> Spring MVC -> JSP -> EmployeeService -> EmployeeDao -> PostgreSQL
```

Phase 2 is now implemented here:

```text
Angular
   |
REST Controller
   |
existing EmployeeService
   |
existing EmployeeDao
   |
PostgreSQL
```

JSP routes continue under `/` and `/employees/**`. JSON endpoints use `/api/employees/**`, and WebSphere serves the Angular application under `/app/**`. Both controllers call the same Spring service; the Angular layer never accesses the DAO directly.

REST endpoints:

```text
GET    /api/employees
GET    /api/employees/{id}
POST   /api/employees
PUT    /api/employees/{id}
DELETE /api/employees/{id}
```

Expected JSON error statuses are `400` for validation, `404` for a missing employee, `409` for duplicate email, and `500` with a generic message for an unexpected server error. Internal exception details are logged but not returned to the browser.

## Production checks

- Confirm the WebSphere fix pack, supported Java SDK, and approved PostgreSQL JDBC version.
- Apply organizational classloader, JTA, authentication, TLS, pooling, backup, and monitoring standards.
- Use an environment-managed database and credentials rather than the local Docker setup.
- Confirm the DataSource scope is visible to every deployment target.
- Do not treat the local generated certificates or passwords as production configuration.

Phase records are maintained in `docs/implementation/`.
