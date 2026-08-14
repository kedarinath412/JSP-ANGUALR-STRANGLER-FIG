# Local POC Automation

Use the three shell commands in this directory. They hide the long Docker and WebSphere `wsadmin` commands:

```bash
# Run once, or whenever the database settings/credentials change
DB_PASSWORD='your-local-postgres-password' ./local-poc/configure-websphere.sh

# Confirm WebSphere can connect to PostgreSQL
./local-poc/test-datasource.sh

# Run after mvn clean package to install or update the EAR
./local-poc/deploy-application.sh
```

Do not commit or paste the database password. The first wrapper passes `DB_PASSWORD` into the running WebSphere container for that command only. The script never prints it or writes it to a repository file; WebSphere saves it in the J2C authentication alias.

## What each command does

### `configure-websphere.sh`

1. Checks that `DB_PASSWORD` was supplied.
2. Copies `configure-websphere-postgresql.py` into the WebSphere container.
3. Reads the generated WebSphere administrator password inside the container without displaying it.
4. Runs WebSphere `wsadmin`.
5. The Jython script creates or updates:
   - J2C authentication alias `LegacyPocPostgresAuth`;
   - PostgreSQL JDBC provider;
   - DataSource `LegacyPocDS` with JNDI name `jdbc/LegacyPocDS`;
   - host, port, and database custom properties.
6. Saves the WebSphere configuration.

Prerequisites: the PostgreSQL JDBC JAR must already exist at `/work/lib/postgresql.jar`, and the WebSphere and PostgreSQL containers must share the local POC Docker network.

### `test-datasource.sh`

1. Copies the small connection-test Jython script into WebSphere.
2. Locates `LegacyPocDS` at the configured cell/node/server scope.
3. Calls WebSphere's built-in `testConnection` operation.

A successful wrapper run ends with:

```text
SUCCESS: WebSphere connected to jdbc/LegacyPocDS.
```

The local user-defined provider uses `GenericDataStoreHelper`. In this setup, `wsadmin` may print `WASX7388E` because WebSphere produced a warning even though the physical connection succeeded. The wrapper therefore checks the new server-log entries for WebSphere's authoritative `DSRA8030I: Successfully connected to DataSource` confirmation and returns a failing shell status if that confirmation is absent.

### `deploy-application.sh`

1. Verifies that `legacy-poc-ear/target/legacy-poc-ear.ear` exists.
2. Copies the EAR and deployment Jython script into WebSphere.
3. Installs the application if it is new.
4. If it is already installed, stops it and updates it with the new EAR.
5. Saves the WebSphere configuration and starts the application.

The application is then available at `http://localhost:9080/legacy-poc/`.

## Why the `.py` files still exist

The `.py` files are Jython, not general-purpose Python scripts. IBM WebSphere Traditional exposes its administrative objects—`AdminConfig`, `AdminTask`, `AdminControl`, and `AdminApp`—only inside `wsadmin`. The shell wrappers provide the commands a developer normally runs, while the Jython files contain the server-specific automation.

Each Jython script has been divided into named functions and includes comments explaining the WebSphere operation. They contain no hard-coded passwords and are safe to commit.

To use a different container name:

```bash
WEBSPHERE_CONTAINER=my-websphere ./local-poc/test-datasource.sh
```
