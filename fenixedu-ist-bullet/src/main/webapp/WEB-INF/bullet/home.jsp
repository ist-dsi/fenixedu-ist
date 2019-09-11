<%@page import="org.fenixedu.academic.domain.ExecutionSemester"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html:xhtml/>

<div class="page-header">
    <h1>
        <span class="glyphicon glyphicon-transfer" aria-hidden="true"></span>
        <spring:message code="title.bullet" text="Bullet Integration"/>
    </h1>
</div>

<div>
    <a href="<%= request.getContextPath()%>/bullet/resetEventCounter" class="button">
        Reset Event Counter (BTT Id)
    </a>
</div>

<table class="table">
    <c:forEach var="semester" items="${semesters}">
        <tr>
            <th>
                ${semester.qualifiedName}
            </th>
            <th>
                <a href="<%= request.getContextPath()%>/bullet/${semester.externalId}/exportJson">
                    JSON
                </a>
            </th>
            <th>
                <a href="<%= request.getContextPath()%>/bullet/${semester.externalId}/exportXml">
                    XML
                </a>
            </th>
            <c:forEach var="type" items="${types}">
                <th>
                    <a href="<%= request.getContextPath()%>/bullet/${semester.externalId}/${type}/exportXls">
                        ${type}
                    </a>
                </th>
            </c:forEach>
        </tr>
    </c:forEach>
</table>
