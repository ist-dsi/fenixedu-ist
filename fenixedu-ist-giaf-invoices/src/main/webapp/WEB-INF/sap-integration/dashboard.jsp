<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<% final String contextPath = request.getContextPath(); %>

<div class="page-header">
    <h1>
        <spring:message code="title.sao.integration.dashboard" text="SAP Integration Dashboard"/>
    </h1>
</div>

<div class="page-body">

    <table class="table">
        <tr>
            <th>
                <spring:message code="label.sao.integration.request.type" text="Document Type"/>
            </th>
            <th>
                <spring:message code="label.sao.integration.request.pending" text="Pending"/>
            </th>
            <th>
                <spring:message code="label.sao.integration.request.pendingValue" text="Pending Value"/>
            </th>
            <th>
                <spring:message code="label.sao.integration.request.errors" text="Errors"/>
            </th>
            <th>
                <spring:message code="label.sao.integration.request.errors" text="Error Value"/>
            </th>
            <th>
                <spring:message code="label.sao.integration.request.sent" text="Sent"/>
            </th>
            <th>
                <spring:message code="label.sao.integration.request.integrated" text="Integrated"/>
            </th>
        </tr>
        <c:forEach var="entry" items="${dashboard.typeReport}">
            <tr>
                <td>
                    <a href="<%= contextPath %>/sap-integration-dashboard/${entry.key}">
                        ${entry.key}
                    </a>
                </td>
                <td>
                    <span style="color: #ff8f0f; font-weight: bold;">
                        ${entry.value.pending}
                    </span>
                </td>
                <td>
                    <span style="padding-right: 10px;">
                        ${entry.value.pendingValue}
                        <spring:message code="label.euro" text="EUR"/>
                    </span>
                </td>
                <td>
                    <span style="color: red; font-weight: bold;">
                        ${entry.value.errorCount}
                    </span>
                </td>
                <td>
                    <span style="padding-right: 10px;">
                        ${entry.value.errorValue}
                        <spring:message code="label.euro" text="EUR"/>
                    </span>
                </td>
                <td>
                    ${entry.value.sent}
                </td>
                <td>
                    <span style="color: green;">
                        ${entry.value.integrated}
                    </span>
                </td>
            </tr>
        </c:forEach>
    </table>

    <table class="table">
        <tr>
            <th>
                <spring:message code="label.sao.integration.request.type" text="Document Type"/>
            </th>
            <th>
                <spring:message code="label.sao.integration.error" text="Error"/>
            </th>
            <th>
                <spring:message code="label.sao.integration.errorCount" text="Error Count"/>
            </th>
        </tr>
        <c:forEach var="entry" items="${dashboard.typeReport}">
            <c:forEach var="entryError" items="${entry.value.errors}">
                <tr>
                    <td>
                        <a href="<%= contextPath %>/sap-integration-dashboard/${entry.key}">
                            ${entry.key}
                        </a>
                    </td>
                    <td>
                        ${entryError.key}
                    </td>
                    <td>
                        ${entryError.value}
                    </td>
            </c:forEach>
        </c:forEach>
    </table>

</div>
