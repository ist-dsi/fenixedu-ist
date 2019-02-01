<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page trimDirectiveWhitespaces="true" %>

${portal.toolkit()}

<link rel="stylesheet" type="text/css" media="screen" href="<%= request.getContextPath() %>/CSS/accounting.css"/>

<header>
    <h1><spring:message code="title.accounting.internalPayment" text="Internal Payments"/></h1>
</header>

<body>
    <br/>
    <div class="container-fluid">
        <div class="row">
            <div class="col-md-12">
                <form method="get" action="<%= request.getContextPath() %>/accounting-internalPayment/search" class="form-horizontal">
                    ${csrf.field()}
                    <div class="col-sm-3">
                        <input id="start" name="start" value="${start}" bennu-datetime requires-past required class="form-inline">
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
                                <spring:message code="label.whenSent" text="System Entry Date"/>
                            </th>
                            <th>
                                <spring:message code="label.event" text="Event"/>
                            </th>
                            <th>
                                <spring:message code="label.documentNumber" text="Document Number"/>
                            </th>
                            <th>
                                <spring:message code="label.value" text="Value"/>
                            </th>
                            <th>
                                <spring:message code="label.unit" text="Unit"/>
                            </th>
                            <th>
                                <spring:message code="label.comment" text="Comment"/>
                            </th>
                            <th>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="info" items="${internalPayments}">
                            <c:if test="${ not empty info.sapRequest.sapRootFromConsolidated }">
                                <c:set var="consolidated">consolidated</c:set>
                            </c:if>
                            <c:if test="${ empty info.sapRequest.sapRootFromConsolidated }">
                                <c:set var="consolidated"></c:set>
                            </c:if>
                            <tr class="${consolidated}">
                                <td>
                                    ${info.sapRequest.whenSent.toString('yyyy-MM-dd HH:mm:ss')}
                                </td>
                                <td>
                                    <a href="<%= request.getContextPath() %>/accounting-management/${info.sapRequest.event.externalId}/details">
                                        ${info.sapRequest.event.description}
                                    </a>
                                </td>
                                <td>
                                    ${info.sapRequest.documentNumber}
                                    &nbsp;
                                    ${info.invoiceSapRequest.documentNumber}
                                </td>
                                <td>
                                    ${info.sapRequest.value}
                                </td>
                                <td>
                                    ${info.accountingTransaction.transactionDetail.paymentReference}
                                </td>
                                <td>
                                    ${info.accountingTransaction.transactionDetail.comments}
                                </td>
                                <td>
                                    <c:if test="${ empty info.sapRequest.sapRootFromConsolidated }">
                                        <form method="post" action="<%= request.getContextPath() %>/accounting-internalPayment/${info.sapRequest.externalId}/consolidate" class="form-horizontal">
                                            ${csrf.field()}
                                            <button class="btn btn-primary" type="submit">
                                                <spring:message code="label.consolidate" text="Consolidate"/>
                                            </button>
                                        </form>                                        
                                    </c:if>
                                    <c:if test="${ not empty info.sapRequest.sapRootFromConsolidated }">
                                        <form method="post" action="<%= request.getContextPath() %>/accounting-internalPayment/${info.sapRequest.externalId}/revertConsolidation" class="form-horizontal">
                                            ${csrf.field()}
                                            <button class="btn btn-danger" type="submit">
                                                <spring:message code="label.consolidate.revert" text="Revert Consolidation"/>
                                            </button>
                                        </form>                                        
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>                        
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</body>

<style>
<!--
.consolidated {
    background-color: #bfffbf;
}
-->
</style>