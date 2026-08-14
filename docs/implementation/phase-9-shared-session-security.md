# Phase 9 — Shared JSP and Angular Session Security

## Goal

Prove that a user can authenticate once and use the same server-side session for the legacy JSP application and the Angular/REST modernization path.

## Delivered

- Spring Security 5.8 filter chain registered through `DelegatingFilterProxy`.
- Custom JSP login page and POST logout.
- WebSphere `HttpSession` persistence through the `JSESSIONID` cookie.
- One `SecurityContext` shared by `/employees/**`, `/app/**`, and `/api/**`.
- Local admin and viewer identities with server-enforced role authorization.
- JSON 401 and 403 responses for REST clients; login redirects for browser UI routes.
- `GET /api/session` returns username, authorities, and the current CSRF header/token.
- Angular sends the CSRF header on POST, PUT, DELETE, and logout.
- JSP unsafe forms contain CSRF tokens; Spring form tags integrate automatically.
- Admin-only edit controls hidden in JSP and Angular, with authorization still enforced at the server.
- Session invalidation and cookie deletion on logout.

## Request architecture

```text
Login form -> Spring Security -> WebSphere HttpSession -> JSESSIONID
                                      |
                         +------------+------------+
                         |                         |
                    JSP request              Angular REST request
                         |                         |
                         +---- SecurityContext ----+
```

Angular does not own a second login or token store. Since all requests are same-origin, the browser sends the WebSphere session cookie automatically. `/api/session` supplies presentation-safe identity data and the synchronizer CSRF token; it never returns the session ID.

## Local identities

- `employee-admin / admin-demo`: `ROLE_EMPLOYEE_ADMIN`, `ROLE_EMPLOYEE_VIEWER`.
- `employee-viewer / viewer-demo`: `ROLE_EMPLOYEE_VIEWER`.

These accounts deliberately use an in-memory `UserDetailsService` to isolate and validate session sharing. Replace that bean with the approved enterprise identity source for production.

## Verification

- Java 8 `mvn clean package`: success.
- 28 Java tests and 3 Angular tests passed.
- Unauthenticated JSP and Angular: 302 to `/login`.
- Unauthenticated REST: JSON 401.
- Successful login changed the session ID.
- The same cookie jar accessed JSP, Angular, and `/api/session` as `employee-admin`.
- Admin write without CSRF: 403; with CSRF: request reached normal 400 domain validation.
- Viewer JSP and REST reads: 200; viewer REST write: JSON 403; JSP edit controls absent.
- Logout: 302 to `/login?logout`; the previous session then received JSON 401.

## Production follow-up

- Select WebSphere registry/LDAP, OIDC, or SAML according to enterprise standards.
- Do not use local demo identities outside this POC.
- Require HTTPS and secure cookie settings.
- Define session timeout, concurrent-session, audit, account lifecycle, and password/identity-provider policies.
- For clusters, configure session affinity or replication and validate failover.

Status: Complete.
