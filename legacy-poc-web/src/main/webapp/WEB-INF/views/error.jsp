<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Application Error</title>
    <c:url value="/resources/css/application.css" var="cssUrl"/><link rel="stylesheet" href="${cssUrl}">
</head>
<body>
<header class="app-header"><div class="container"><h1>Legacy Modernization POC</h1><p>Spring MVC + JSP + WebSphere + PostgreSQL</p></div></header>
<main class="container narrow">
    <section class="panel error-panel">
        <h2><c:out value="${errorTitle}" default="Application Error"/></h2>
        <p><c:out value="${errorMessage}" default="The application could not complete your request."/></p>
        <c:url value="/employees" var="employeesUrl"/><a class="button" href="${employeesUrl}">Return to Employee Management</a>
    </section>
</main>
</body>
</html>
