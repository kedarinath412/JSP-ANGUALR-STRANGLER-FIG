<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Sign in</title>
    <c:url value="/resources/css/application.css" var="cssUrl"/>
    <link rel="stylesheet" href="${cssUrl}">
</head>
<body>
<header class="app-header"><div class="container"><h1>Legacy Modernization POC</h1><p>Shared JSP + Angular session</p></div></header>
<main class="container narrow">
    <section class="panel">
        <h2>Sign in</h2>
        <p>One WebSphere session authorizes both the legacy JSP and modern Angular paths.</p>
        <c:if test="${param.error != null}"><div class="alert error">Invalid username or password.</div></c:if>
        <c:if test="${param.logout != null}"><div class="alert success">You have been signed out.</div></c:if>
        <c:url value="/login" var="loginUrl"/>
        <form method="post" action="${loginUrl}" class="employee-form">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="field"><label for="username">Username</label><input id="username" name="username" autocomplete="username" required autofocus></div>
            <div class="field"><label for="password">Password</label><input id="password" type="password" name="password" autocomplete="current-password" required></div>
            <button class="button" type="submit">Sign in</button>
        </form>
        <p class="help-text">Local demo: <code>employee-admin / admin-demo</code> or <code>employee-viewer / viewer-demo</code>.</p>
    </section>
</main>
</body>
</html>
