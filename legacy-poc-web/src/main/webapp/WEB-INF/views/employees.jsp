<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Employee Management</title>
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
    <div class="page-heading">
        <div><h2>Employee Management</h2><p>Employees loaded from PostgreSQL through the WebSphere DataSource.</p></div>
        <div>
            <c:url value="/app/" var="angularUrl"/>
            <a class="button secondary" href="${angularUrl}">Modern Angular UI</a>
            <c:url value="/employees/new" var="newEmployeeUrl"/>
            <a class="button" href="${newEmployeeUrl}">Add Employee</a>
        </div>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="alert success" role="status"><c:out value="${successMessage}"/></div>
    </c:if>

    <div class="panel table-wrapper">
        <table>
            <thead><tr><th>ID</th><th>First Name</th><th>Last Name</th><th>Email</th><th>Department</th><th>Created</th><th>Actions</th></tr></thead>
            <tbody>
            <c:choose>
                <c:when test="${empty employees}">
                    <tr><td colspan="7" class="empty-state">No employees found.</td></tr>
                </c:when>
                <c:otherwise>
                    <c:forEach items="${employees}" var="employee">
                        <tr>
                            <td><c:out value="${employee.employeeId}"/></td>
                            <td><c:out value="${employee.firstName}"/></td>
                            <td><c:out value="${employee.lastName}"/></td>
                            <td><c:out value="${employee.email}"/></td>
                            <td><c:out value="${employee.department}"/></td>
                            <td><fmt:formatDate value="${employee.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                            <td class="actions">
                                <c:url value="/employees/${employee.employeeId}/edit" var="editUrl"/>
                                <c:url value="/employees/${employee.employeeId}/delete" var="deleteUrl"/>
                                <a class="button secondary small" href="${editUrl}">Edit</a>
                                <form method="post" action="${deleteUrl}" class="inline-form">
                                    <button class="button danger small" type="submit">Delete</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
            </tbody>
        </table>
    </div>
    <c:url value="/" var="homeUrl"/><p><a href="${homeUrl}">Back to home</a></p>
</main>
</body>
</html>
