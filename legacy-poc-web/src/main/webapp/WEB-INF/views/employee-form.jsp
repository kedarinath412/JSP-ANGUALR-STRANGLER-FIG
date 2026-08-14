<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title><c:out value="${formTitle}"/></title>
    <c:url value="/resources/css/application.css" var="cssUrl"/>
    <link rel="stylesheet" href="${cssUrl}">
</head>
<body>
<header class="app-header">
    <div class="container"><h1>Legacy Modernization POC</h1><p>Spring MVC + JSP + WebSphere + PostgreSQL</p><p>Signed in as <strong><sec:authentication property="principal.username"/></strong></p></div>
</header>
<main class="container narrow">
    <section class="panel">
        <h2><c:out value="${formTitle}"/></h2>
        <c:url value="${formAction}" var="submitUrl"/>
        <form:form method="post" action="${submitUrl}" modelAttribute="employee" cssClass="employee-form">
            <form:hidden path="employeeId"/>
            <div class="field">
                <form:label path="firstName">First Name <span class="required">*</span></form:label>
                <form:input path="firstName" maxlength="100"/>
                <form:errors path="firstName" cssClass="field-error"/>
            </div>
            <div class="field">
                <form:label path="lastName">Last Name <span class="required">*</span></form:label>
                <form:input path="lastName" maxlength="100"/>
                <form:errors path="lastName" cssClass="field-error"/>
            </div>
            <div class="field">
                <form:label path="email">Email <span class="required">*</span></form:label>
                <form:input path="email" type="email" maxlength="200"/>
                <form:errors path="email" cssClass="field-error"/>
            </div>
            <div class="field">
                <form:label path="department">Department</form:label>
                <form:input path="department" maxlength="100"/>
                <form:errors path="department" cssClass="field-error"/>
            </div>
            <div class="form-actions">
                <button class="button" type="submit">Save Employee</button>
                <c:url value="/employees" var="cancelUrl"/><a class="button secondary" href="${cancelUrl}">Cancel</a>
            </div>
        </form:form>
    </section>
</main>
</body>
</html>
