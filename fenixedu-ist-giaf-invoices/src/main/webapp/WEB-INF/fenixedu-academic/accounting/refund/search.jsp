<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page trimDirectiveWhitespaces="true" %>

${portal.toolkit()}

<link rel="stylesheet" type="text/css" media="screen" href="<%= request.getContextPath() %>/CSS/accounting.css"/>

<header>
    <h1><spring:message code="title.accounting.refund" text="Reembolsos"/></h1>
</header>

<body>
    <br/>
    <div class="container-fluid">
        <div class="row">
            <div class="col-md-12">
                <form method="get" action="<%= request.getContextPath() %>/accounting-refund/search" class="form-horizontal">
                    ${csrf.field()}
                    <div class="col-sm-3">
                        <input id="start" name="start" value="${start}" bennu-datetime required class="form-inline">
                    </div>
                    <div class="col-sm-3">
                        <span style="float: left; margin-right: 25px;">
                            <spring:message code="label.to" text="to"/>
                        </span>
                        <input id="end" name="end" value="${end}" bennu-datetime required class="form-inline">
                    </div>
                    <div class="col-sm-1">
                        <button class="btn btn-primary" type="submit">
                            <spring:message code="label.search" text="Search"/>
                        </button>
                    </div>
                    <div class="col-sm-5">
                    </div>
                </form>
            </div>
        </div>

        <br/>

        <div class="row">
            <div class="col-md-12">
                <table class="table">
                    <thead>
                        <tr>
                            <th>
                                <spring:message code="accounting.event.details.creation.date" text="Creation Date"/>
                            </th>
                            <th>
                                <spring:message code="label.event" text="Event"/>
                            </th>
                            <th>
                                <spring:message code="label.value" text="Value"/>
                            </th>
                            <th>
                                <spring:message code="label.state" text="State"/>
                            </th>
                            <th>
                                <spring:message code="label.document.number" text="Document Number"/>
                            </th>
                            <th>
                                <spring:message code="label.value" text="Value"/>
                            </th>
                            <th>
                                <spring:message code="label.client.to.refund" text="Client to Refund"/>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="refund" items="${refunds}">
                            <c:set var="srCount" value="${fn:length(refund.sapRequests)}"/>
                            <tr>
                                <td rowspan="${srCount}">
                                    ${refund.refund.whenOccured.toString('yyyy-MM-dd HH:mm:ss')}
                                </td>
                                <td rowspan="${srCount}">
                                    <a href="<%= request.getContextPath() %>/accounting-management/${refund.refund.event.externalId}/details">
                                        ${refund.refund.event.description}
                                    </a>
                                </td>
                                <td rowspan="${srCount}">
                                    ${refund.refund.amount}
                                </td>
                                <td rowspan="${srCount}">
                                        ${refund.bankAccountNumber}
                                </td>
                                <td rowspan="${srCount}">
                                    ${refund.refund.state}
                                </td>
                                <c:forEach var="sapRequest" items="${refund.sapRequests}" end="0">
                                    <td>
                                        ${sapRequest.documentNumber}
                                        &nbsp;
                                        ${sapRequest.documentData.workingDocumentNumber}
                                    </td>
                                    <td>
                                        ${sapRequest.value}
                                    </td>
                                    <td>
                                        ${sapRequest.clientId}
                                    </td>
                                </c:forEach>                                
                            </tr>
                            <c:forEach var="sapRequest" items="${refund.sapRequests}" begin="1">
                                <tr>
                                    <td>
                                        ${sapRequest.documentNumber}
                                        &nbsp;
                                        ${sapRequest.documentData.workingDocumentNumber}
                                    </td>
                                    <td>
                                        ${sapRequest.value}
                                    </td>
                                    <td>
                                        ${sapRequest.clientId}
                                    </td>
                                </tr>
                            </c:forEach>                                
                        </c:forEach>                        
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</body>
