# Phase 7 — Local WebSphere and PostgreSQL Runtime

## Delivered

- Official WebSphere Traditional 9.0.5.28 container running under `linux/amd64` emulation with IBM Java 8.
- Native ARM64 PostgreSQL 17 Alpine container with persistent storage and generated local credentials.
- PostgreSQL JDBC 42.7.11 installed as a WebSphere-managed driver.
- Secured J2C alias, user-defined pooled JDBC provider, and `jdbc/LegacyPocDS` configured through idempotent `wsadmin` scripts.
- EAR installed and started at `/legacy-poc`.

## Verification

- WebSphere reports `server1 open for e-business`.
- PostgreSQL reports ready and contains the three sample employees.
- WebSphere identifies PostgreSQL 17.10/JDBC 42.7.11 and logs a successful DataSource connection.
- Home and employee list return HTTP 200.
- Create, update, duplicate-email, validation, and delete acceptance tests passed through the deployed UI endpoints.
- The disposable CRUD test row was deleted; the database returned to the three sample rows.
- The unused Oracle container, 12.1 GB image, and incomplete Oracle data volume were removed after PostgreSQL verification.

Status: Complete.
