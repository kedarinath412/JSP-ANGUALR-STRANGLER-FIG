# Phase 2 — Core Application Layers

## Delivered

- PostgreSQL create, sample-data, and drop scripts.
- Traditional `Employee` JavaBean with Bean Validation 1.1 annotations.
- Parameterized `JdbcTemplate` DAO with reusable row mapping and clean missing-row behavior.
- Transactional service with normalization, duplicate-email policy, and not-found handling.
- JNDI DataSource, JDBC transaction manager, and root-context configuration.

## Verification

- Core classes compile for Java 8.
- Eight service tests pass without PostgreSQL or WebSphere.
- Static audit found no direct JDBC connections or embedded connection credentials.

Status: Complete.
