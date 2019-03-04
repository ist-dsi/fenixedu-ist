<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
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
        <tr>
            <td>
                ${sapRequestType}
            </td>
            <td>
                <span style="color: #ff8f0f; font-weight: bold;">
                    ${report.pending}
                </span>
            </td>
            <td>
                ${report.pendingValue}
                <spring:message code="label.euro" text="EUR"/>
            </td>
            <td>
                <span style="color: red; font-weight: bold;">
                   ${report.errorCount}
                </span>
            </td>
            <td>
                ${report.errorValue}
                <spring:message code="label.euro" text="EUR"/>
            </td>
            <td>
                ${report.sent}
            </td>
            <td>
                <span style="color: green;">
                    ${report.integrated}
                </span>
            </td>
        </tr>
    </table>

    <table class="table">
        <tr>
            <th>
                <spring:message code="label.sao.integration.error" text="Error"/>
            </th>
            <th>
                <spring:message code="label.event" text="Event"/>
            </th>
            <th>
                <spring:message code="label.sao.integration.sapRequest" text="SAP Request"/>
            </th>
        </tr>
        <c:forEach var="sapRequest" items="${errors}">
            <tr>
                <td>
                    <c:forEach var="errorMessage" items="${sapRequest.errorMessages}">
                        ${errorMessage}
                    </c:forEach>
                </td>
                <td>
                    <a href="<%= contextPath %>/accounting-management/${sapRequest.event.externalId}/details">
                        ${sapRequest.event.description}
                    </a>
                </td>
                <td>
                    <a href="<%= contextPath %>/sap-invoice-viewer/${sapRequest.event.externalId}">
                        ${sapRequest.documentNumber}
                    </a>
                </td>
            </tr>
        </c:forEach>
    </table>

</div>
