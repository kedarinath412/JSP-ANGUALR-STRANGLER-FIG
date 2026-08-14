# Local POC Automation

These Jython scripts are intended for WebSphere Traditional `wsadmin`:

- `configure-websphere-postgresql.py` creates or updates the PostgreSQL J2C alias, pooled JDBC provider, DataSource, and custom properties. Supply the database password through the process environment as `DB_PASSWORD`.
- `test-websphere-datasource.py` runs WebSphere's DataSource connection test.
- `deploy-application.py` installs and starts `/work/app/legacy-poc-ear.ear`.

The scripts assume the local Docker names, cell, node, server, JNDI name, and paths documented in the root README. They contain no passwords and are safe to commit.
