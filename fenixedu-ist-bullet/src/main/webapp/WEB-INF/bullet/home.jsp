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
    <a href="#" class="btn btn-default" onclick="resetCounter()">
        <spring:message code="reset.counter" text="Reset Event Counter (BTT Id)"/>
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

<script>
    function resetCounter() {
        if (confirm('<spring:message code="please.confirm" text="Please confirm the counter reset"/>')) {
            $.ajax({
                type: "GET",
                url: "bullet/resetEventCounter",
                success: function() {
                    location.reload();
                }
            });
        }
    }
</script>