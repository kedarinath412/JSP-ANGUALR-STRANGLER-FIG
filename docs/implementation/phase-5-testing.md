# Phase 5 — Testing and Build Verification

## Automated coverage

- Service delegation, missing employees, normalization, duplicate-email rules, update, and delete.
- MVC home/list/forms, validation, duplicate-email message, redirects, POST delete, and friendly 404 handling.

## Build evidence

```text
Command: mvn clean package
Result: BUILD SUCCESS
Tests: 18 run, 0 failures, 0 errors, 0 skipped
WAR: legacy-poc-web/target/legacy-poc-web.war
EAR: legacy-poc-ear/target/legacy-poc-ear.ear
```

The final verification used Maven 3.9.9 with Amazon Corretto 8 (`1.8.0_472`), matching the Java 8 deployment baseline.

Status: Complete.
