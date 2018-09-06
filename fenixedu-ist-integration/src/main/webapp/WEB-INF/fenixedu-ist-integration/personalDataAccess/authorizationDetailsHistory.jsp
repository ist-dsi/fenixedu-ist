<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<div class="page-header">
    <h2>
        <spring:message code="authorize.personal.data.access.title" />
    </h2>
    <spring:url value="/authorize-personal-data-access" var="baseUrl"/>
    <a href="${baseUrl}">« <spring:message code="label.back" /></a>
</div>

<table class="table results">
    <thead>
        <th><spring:message code="label.date" /></th>
        <th><spring:message code="label.title" /></th>
        <th><spring:message code="label.text" /></th>
        <th><spring:message code="label.answer" /></th>
    </thead>
    <tbody>
    <c:if test="${not empty cardAuthorizationLogs}">
        <c:forEach var="log" items="${cardAuthorizationLogs}">
            <tr>
                <td>
                    <c:set var="format" value="dd-MM-yyyy HH:mm" />
                    <c:out value="${log.whenDateTime.toString(format)}" />
                </td>
                <td>
                    <c:out value="${log.title}" />
                </td>
                <td>
                    <c:out value="${log.description}" escapeXml="false" />
                </td>
                <td>
                    <c:out value="${log.answer}" />
                </td>
            </tr>
        </c:forEach>
    </c:if>
    </tbody>
</table>