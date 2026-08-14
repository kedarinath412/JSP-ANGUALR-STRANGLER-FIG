<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Legacy Modernization POC</title>
    <c:url value="/resources/css/application.css" var="cssUrl"/>
    <link rel="stylesheet" href="${cssUrl}">
</head>
<body>
<header class="app-header">
    <div class="container">
        <h1>Legacy Modernization POC</h1>
        <p>Spring MVC + JSP + WebSphere + PostgreSQL</p>
    </div>
</header>
<main class="container">
    <section class="panel hero">
        <h2>Legacy Modernization POC</h2>
        <p>Compare the Phase 1 server-rendered screen with the Phase 2 Angular screen. Both use the same service and persistence layers.</p>
        <c:url value="/employees" var="employeesUrl"/>
        <a class="button" href="${employeesUrl}">Employee Management</a>
        <c:url value="/app/" var="angularUrl"/>
        <a class="button secondary" href="${angularUrl}">Modern Angular UI</a>
    </section>
</main>
</body>
</html>
