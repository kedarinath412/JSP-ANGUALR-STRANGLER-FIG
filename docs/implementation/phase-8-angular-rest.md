# Phase 8 — Angular and REST Strangler Slice

## Goal

Add the first modern presentation slice inside the existing WAR while keeping the Phase 1 JSP application operational and reusing its business and persistence layers.

## Delivered

- Angular 22 standalone application under `legacy-poc-web/src/main/frontend`.
- Modern employee list and create, edit, and delete UI served at `/legacy-poc/app/`.
- Spring MVC JSON endpoints under `/api/employees/**`.
- REST-specific validation and safe JSON error handling for 400, 404, 409, and 500 responses.
- Existing `EmployeeService`, `EmployeeDao`, `JdbcTemplate`, transactions, JNDI DataSource, and PostgreSQL schema reused without duplication.
- Legacy home and employee JSP pages retained and linked to the Angular UI.
- Maven frontend lifecycle using project-local Node 24.15.0, locked `npm ci`, Angular unit tests, production bundling, and WAR resource packaging.
- Repeatable WebSphere update behavior in `local-poc/deploy-application.py`.

## Architecture

```text
JSP -> EmployeeController --------+
                                  |
                                  v
Angular -> EmployeeRestController -> existing EmployeeService -> existing EmployeeDao -> PostgreSQL
```

The REST controller contains HTTP mapping only. Email uniqueness, lookup behavior, logging, and transaction ownership remain in the existing service layer.

## Packaging

```text
src/main/frontend
    -> Angular production build
target/angular/browser
    -> Maven WAR web resource at /app
legacy-poc-web.war!/app
    -> Maven EAR module
legacy-poc-ear.ear!/legacy-poc-web.war
```

Angular is packaged as static browser assets in the existing WAR, not as a separate deployable module. WebSphere does not need Node at runtime. Maven uses project-local Node only while building the assets.

## Verification

- `mvn clean package` with Java 8: success.
- Java: 25 tests passed.
- Angular: 1 test passed.
- WAR contains `/app/index.html`, hashed JavaScript, and CSS assets.
- WAR and EAR were rebuilt and the installed WebSphere application was updated successfully.
- Live HTTP checks: JSP 200, Angular entry 200, Angular JavaScript 200, REST list/get 200, create 201, update/delete 204, validation 400, duplicate email 409, missing employee 404.
- The disposable REST verification employee was deleted after the test.

Status: Complete.
