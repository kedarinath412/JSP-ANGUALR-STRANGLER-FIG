# Phase 4 — WebSphere and EAR Packaging

## Delivered

- Servlet 3.1 `web.xml` with root/dispatcher contexts, UTF-8 filter, `/` mapping, and resource reference.
- `ibm-web-bnd.xml` mapping `jdbc/LegacyPocDS` to the server JNDI binding.
- Java EE 7 EAR descriptor and Maven EAR packaging at context root `/legacy-poc`.

## Verification

- EAR contains `META-INF/application.xml` and `legacy-poc-web.war`.
- WAR contains the Spring contexts, WebSphere binding, JSPs, classes, CSS, and required Spring libraries.
- PostgreSQL JDBC and container-provided Servlet/JSP/validation APIs are absent from `WEB-INF/lib`; the JDBC driver is managed by WebSphere.

Status: Complete; target-server descriptor/fix-pack compatibility remains an environment check.
