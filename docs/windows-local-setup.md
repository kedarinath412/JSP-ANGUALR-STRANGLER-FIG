# Windows Local Setup Guide

This runbook explains how to build and run the complete POC on a Windows laptop using WSL2, Docker Desktop, WebSphere Traditional, and PostgreSQL.

The recommended approach is to run Git, Java, Maven, and the repository scripts inside an Ubuntu WSL2 distribution while Docker Desktop supplies the Docker engine. This keeps the existing Bash automation portable and avoids maintaining a second set of PowerShell deployment scripts.

## 1. What must be installed

Required:

- supported 64-bit Windows 10 or Windows 11;
- hardware virtualization enabled in BIOS/UEFI;
- WSL2 with an Ubuntu distribution;
- Docker Desktop configured for its WSL2 backend and Linux containers;
- Git inside Ubuntu;
- a Java 8 JDK inside Ubuntu;
- Maven 3.8 or later inside Ubuntu;
- internet access for GitHub, Maven Central, npm, Docker Hub, and `icr.io`.

Optional:

- IntelliJ IDEA installed on Windows.

You do not need to install these directly on Windows:

- WebSphere;
- PostgreSQL;
- Oracle;
- Node.js or npm;
- Angular CLI;
- Tomcat or another embedded server.

Maven installs the project-pinned Node 24.15.0 distribution under `target` and uses it to test and compile Angular.

## 2. Practical hardware guidance

For a comfortable local POC:

- 16 GB RAM is recommended;
- reserve approximately 25–40 GB of free disk space;
- an Intel or AMD x64 laptop is strongly preferred.

The WebSphere image used by this POC is a Linux `amd64` image and is much larger than the PostgreSQL image. Windows on ARM would require emulation and should be treated as an unverified environment for this POC.

Current platform requirements should be checked against the official [Docker Desktop Windows installation documentation](https://docs.docker.com/desktop/setup/install/windows-install/) and [Docker WSL2 documentation](https://docs.docker.com/desktop/features/wsl/).

## 3. Install WSL2

Open an administrator PowerShell window:

```powershell
wsl --install -d Ubuntu
```

Restart Windows if requested. Then update and inspect WSL:

```powershell
wsl --update
wsl --status
```

Open Ubuntu from the Start menu and complete its initial username and password setup.

## 4. Install and configure Docker Desktop

Install Docker Desktop for Windows. Configure it to:

1. use the WSL2 engine;
2. run Linux containers;
3. enable integration with the Ubuntu WSL distribution.

Docker recommends not installing a second Docker Engine directly inside the WSL distribution when Docker Desktop integration is being used.

From the Ubuntu terminal, verify connectivity to Docker Desktop:

```bash
docker version
docker run --rm hello-world
```

Both commands must succeed before continuing.

## 5. Install build tools inside Ubuntu

From the Ubuntu terminal:

```bash
sudo apt update
sudo apt install -y git maven curl unzip
```

Install a Linux Java 8 JDK distribution in Ubuntu, such as Amazon Corretto 8 or Eclipse Temurin 8. Java installed only on the Windows side is not automatically the Java runtime used by Maven inside WSL.

Verify the toolchain:

```bash
java -version
javac -version
mvn -version
git --version
```

For strict target compatibility, Java and Maven should report that Maven is running with Java 8.

## 6. Clone the project into WSL

Keep the working copy in the WSL Linux filesystem rather than under `/mnt/c`. Maven and npm file operations are generally faster there, and the shell scripts retain Linux-compatible behavior.

```bash
cd
git config --global core.autocrlf input
git clone https://github.com/kedarinath412/JSP-ANGUALR-STRANGLER-FIG.git
cd JSP-ANGUALR-STRANGLER-FIG
```

Check the scripts:

```bash
ls -l local-poc
```

If executable permissions were lost during checkout:

```bash
chmod +x local-poc/*.sh
```

## 7. Pull WebSphere and PostgreSQL

The locally verified environment uses PostgreSQL 17 Alpine and WebSphere Traditional 9.0.5.28.

```bash
docker pull postgres:17-alpine
docker pull icr.io/appcafe/websphere-traditional:9.0.5.28
```

IBM provides version-specific tags as well as the mutable `latest` tag. A version-specific tag is preferable for reproducing the verified environment. Confirm that your organization is authorized to use the WebSphere container under the applicable IBM license. See the official [WebSphere Application Server container-image documentation](https://www.ibm.com/docs/en/was/9.0.5?topic=container-websphere-application-server-images).

If the exact historical tag is unavailable in the registry, use an organization-approved WebSphere Traditional 9.0.5.x image and repeat the complete build, deployment, and acceptance test plan.

## 8. Create the Docker network and PostgreSQL volume

```bash
docker network create legacy-poc-network
docker volume create legacy-poc-postgres-data
```

If either command reports that the named object already exists, it can be reused.

The network lets WebSphere reach PostgreSQL by container name. The volume preserves PostgreSQL data when the PostgreSQL container is restarted or recreated with the same volume.

## 9. Start PostgreSQL

Choose a password used only for this local POC:

```bash
export LEGACY_POC_DB_PASSWORD='choose-a-local-password'
```

Do not commit or paste the real value into project files.

Start PostgreSQL:

```bash
docker run -d \
  --name legacy-poc-postgres \
  --network legacy-poc-network \
  -p 15432:5432 \
  -e POSTGRES_DB=legacy_poc \
  -e POSTGRES_USER=legacy_poc \
  -e POSTGRES_PASSWORD="${LEGACY_POC_DB_PASSWORD}" \
  -v legacy-poc-postgres-data:/var/lib/postgresql/data \
  postgres:17-alpine
```

Wait for PostgreSQL:

```bash
docker exec legacy-poc-postgres \
  pg_isready -U legacy_poc -d legacy_poc
```

Expected result:

```text
accepting connections
```

PostgreSQL listens on port `5432` inside the Docker network. Windows/WSL host tools connect through `localhost:15432`.

## 10. Start WebSphere

```bash
docker run -d \
  --name legacy-poc-websphere \
  --platform linux/amd64 \
  --network legacy-poc-network \
  -p 9080:9080 \
  -p 9043:9043 \
  -p 9443:9443 \
  icr.io/appcafe/websphere-traditional:9.0.5.28
```

Watch its startup log:

```bash
docker logs -f legacy-poc-websphere
```

Press `Ctrl+C` after the server reports that it is ready. This stops following the log; it does not stop the container.

Retrieve the generated local administrative password only when needed:

```bash
docker exec legacy-poc-websphere cat /tmp/PASSWORD
```

The administrative console is:

```text
URL:      https://localhost:9043/ibm/console
Username: wsadmin
Password: generated value from /tmp/PASSWORD
```

The container uses a locally generated certificate, so the browser may display a certificate warning.

## 11. Place the PostgreSQL JDBC driver in WebSphere

The application deliberately does not package the PostgreSQL driver. Download the verified driver through Maven:

```bash
mvn org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy \
  -Dartifact=org.postgresql:postgresql:42.7.11 \
  -DoutputDirectory=/tmp/legacy-poc-driver
```

Copy it into the WebSphere container:

```bash
docker exec legacy-poc-websphere mkdir -p /work/lib

docker cp \
  /tmp/legacy-poc-driver/postgresql-42.7.11.jar \
  legacy-poc-websphere:/work/lib/postgresql.jar
```

Restart WebSphere and wait for it to become ready again:

```bash
docker restart legacy-poc-websphere
docker logs -f legacy-poc-websphere
```

## 12. Create and seed the database schema

Run these commands from the repository root:

```bash
docker exec -i legacy-poc-postgres \
  psql -v ON_ERROR_STOP=1 -U legacy_poc -d legacy_poc \
  < db/01-create-employee-table.sql

docker exec -i legacy-poc-postgres \
  psql -v ON_ERROR_STOP=1 -U legacy_poc -d legacy_poc \
  < db/02-insert-sample-data.sql
```

Verify the rows:

```bash
docker exec legacy-poc-postgres \
  psql -U legacy_poc -d legacy_poc \
  -c 'SELECT * FROM EMPLOYEE ORDER BY EMPLOYEE_ID;'
```

The result should include John Smith, Sarah Williams, and David Miller.

## 13. Configure and test the WebSphere DataSource

Use the supplied shell automation:

```bash
DB_PASSWORD="${LEGACY_POC_DB_PASSWORD}" \
  ./local-poc/configure-websphere.sh
```

It creates or updates:

```text
J2C alias:       LegacyPocPostgresAuth
JDBC provider:   PostgreSQL user-defined provider
DataSource:      LegacyPocDS
JNDI name:       jdbc/LegacyPocDS
Database host:   legacy-poc-postgres
Database port:   5432
Database name:   legacy_poc
```

Test the connection through WebSphere:

```bash
./local-poc/test-datasource.sh
```

Expected result:

```text
SUCCESS: WebSphere connected to jdbc/LegacyPocDS.
```

The `.py` files invoked by these wrappers are WebSphere Jython scripts. They run inside `wsadmin`; no separate Python installation is required.

See [`../local-poc/README.md`](../local-poc/README.md) for the internal behavior of each wrapper.

## 14. Build the project

From the repository root:

```bash
mvn clean package
```

The first build downloads Maven dependencies, the pinned Node distribution, npm packages, and Angular build dependencies. It does not require a running WebSphere server or database for the automated tests.

Expected result:

```text
BUILD SUCCESS
```

Expected artifacts:

```text
legacy-poc-web/target/legacy-poc-web.war
legacy-poc-ear/target/legacy-poc-ear.ear
```

## 15. Deploy the EAR

```bash
./local-poc/deploy-application.sh
```

The wrapper copies the EAR into WebSphere, installs or updates the application, saves the WebSphere configuration, and starts the application.

## 16. Test URLs and identities

Open:

```text
Login/home:      http://localhost:9080/legacy-poc/
Legacy JSP:      http://localhost:9080/legacy-poc/employees
Angular UI:      http://localhost:9080/legacy-poc/app/
Admin console:   https://localhost:9043/ibm/console
PostgreSQL host: localhost:15432
```

Demo application identities:

| Role | Username | Password | Access |
|---|---|---|---|
| Admin | `employee-admin` | `admin-demo` | JSP and Angular read/write |
| Viewer | `employee-viewer` | `viewer-demo` | JSP and Angular read-only |

These identities are only for local architecture validation.

## 17. Open the WSL project in IntelliJ

Install IntelliJ IDEA on Windows and open the repository through its WSL path, for example:

```text
\\wsl$\Ubuntu\home\<your-user>\JSP-ANGUALR-STRANGLER-FIG
```

Configure:

- project SDK: Java 8;
- Maven runner: the WSL Maven installation using Java 8;
- Maven project: the root `pom.xml`.

IntelliJ Ultimate provides additional Spring, JSP, and application-server support. The command-line Maven build remains the source of truth.

## 18. Normal start and stop commands

After the initial setup, start both containers with:

```bash
docker start legacy-poc-postgres
docker start legacy-poc-websphere
```

Stop them without deleting data or configuration:

```bash
docker stop legacy-poc-websphere
docker stop legacy-poc-postgres
```

After changing application code:

```bash
mvn clean package
./local-poc/deploy-application.sh
```

## 19. Common Windows problems

### Docker is unavailable inside Ubuntu

- Confirm Docker Desktop is running.
- Confirm WSL2 engine mode is enabled.
- Enable Docker integration for the Ubuntu distribution.
- Do not install a competing Docker daemon inside Ubuntu.

### Shell scripts contain `^M` or fail with a bad interpreter

The files were converted to Windows CRLF endings. Clone from inside WSL with:

```bash
git config --global core.autocrlf input
```

Then restore a clean WSL checkout.

### Maven or npm operations are extremely slow

Keep the repository under the WSL home directory instead of `/mnt/c`.

### A port is already in use

Check:

```bash
docker ps --format 'table {{.Names}}\t{{.Ports}}'
```

The expected host ports are `9080`, `9043`, `9443`, and `15432`. Stop the conflicting process/container or deliberately choose a different host-side port and update the URLs used for testing.

### WebSphere cannot resolve PostgreSQL

Confirm both containers are attached to the same network:

```bash
docker network inspect legacy-poc-network
```

The WebSphere DataSource host must be `legacy-poc-postgres`, not `localhost`. Inside the WebSphere container, `localhost` means WebSphere itself.

### Downloads fail on a corporate laptop

Confirm that firewall, VPN, proxy, and certificate policies allow:

```text
github.com
repo.maven.apache.org
registry.npmjs.org
registry-1.docker.io
icr.io
```

Corporate Docker Desktop licensing and IBM WebSphere entitlement must also be confirmed with the organization.

## Related documentation

- [Root project README](../README.md)
- [Internal Working Guide](internal-working-guide.md)
- [Local POC automation](../local-poc/README.md)
- [Implementation progress](implementation/README.md)
