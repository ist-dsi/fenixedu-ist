<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html:xhtml />
<% final String contextPath = request.getContextPath(); %>

<link rel="stylesheet" type="text/css" media="screen" href="<%= request.getContextPath() %>/CSS/accounting.css"/>

<div class="page-header">
    <h1>
        <a href="<%= request.getContextPath() %>/accounting-management/${sapRequest.event.externalId}/details">
            ${sapRequest.event.description}
        </a>
    </h1>
</div>

<div class="page-body">
    <c:set var="event" scope="request" value="${sapRequest.event}"/>
    <c:set var="person" scope="request" value="${event.person}"/>
    <jsp:include page="../fenixedu-academic/accounting/heading-person.jsp"/>
    <h2>
        <span style="color: gray;"><spring:message code="label.document" text="Document"/></span>
        ${sapRequest.documentNumber}
        <c:if test="${sapRequest.requestType == 'ADVANCEMENT'}">
            <spring:message code="label.advancement" text="Advancement"/>
        </c:if>
        <c:if test="${sapRequest.requestType == 'CREDIT'}">
            <spring:message code="label.credit" text="Credit"/>
        </c:if>
        <c:if test="${sapRequest.requestType == 'DEBT'}">
            <spring:message code="label.debt" text="Debt"/>
        </c:if>
        <c:if test="${sapRequest.requestType == 'DEBT_CREDIT'}">
            <spring:message code="label.debtCredit" text="Debt Credit"/>
        </c:if>
        <c:if test="${sapRequest.requestType == 'INVOICE'}">
            <spring:message code="label.invoice" text="Invoice"/>
        </c:if>
        <c:if test="${sapRequest.requestType == 'INVOICE_INTEREST'}">
            <spring:message code="label.invoiceInterest" text="Interest Invoice"/>
        </c:if>
        <c:if test="${sapRequest.requestType == 'PAYMENT'}">
            <spring:message code="label.payment" text="Payment"/>
        </c:if>
        <c:if test="${sapRequest.requestType == 'PAYMENT_INTEREST'}">
            <spring:message code="label.paymentInterest" text="Interest Payment"/>
        </c:if>
        <c:if test="${sapRequest.requestType == 'REIMBURSEMENT'}">
            <spring:message code="label.refund" text="Refund"/>
        </c:if>
        <c:if test="${sapRequest.ignore}">
            <small>
            <span class="text-danger">(<spring:message code="label.ignored" text="Ignored"/>)</span>
            </small>
        </c:if>
    </h2>

    <div class="row">
        <div class="col-md-4">
            <div class="overall-description">
                <dl>
                    <dt><spring:message code="label.companyName" text="Client Name"/></dt>
                    <dd>${clientData.companyName}</dd>
                </dl>
                <dl>
                    <dt><spring:message code="label.vatNumber" text="Tax Identification Number"/></dt>
                    <dd>${clientData.vatNumber}</dd>
                </dl>
                <dl>
                    <dt><spring:message code="label.address" text="Address"/></dt>
                    <dd>
                        ${clientData.street}
                        <br/>
                        ${clientData.postalCode} - ${clientData.city}, ${clientData.region}
                        <br/>
                        ${clientData.country}
                    </dd>
                </dl>
                <dl>
                    <dt><spring:message code="label.clientId" text="Client ID"/> / <spring:message code="label.accountId" text="Account Type"/></dt>
                    <dd>${clientData.clientId} / ${clientData.accountId}</dd>
                </dl>
            </div>
        </div>
        <div class="col-md-4">
            <div class="overall-description">
                <dl>
                    <dt><spring:message code="label.event" text="Event"/></dt>
                    <dd>${sapRequest.event.description}</dd>
                </dl>
                <dl>
                    <dt><spring:message code="label.product" text="Product"/></dt>
                    <dd>${documentData.productCode} - ${documentData.productDescription}</dd>
                </dl>
                <dl>
                    <dt><spring:message code="label.value" text="Value"/></dt>
                    <dd>${sapRequest.value} ${documentData.currencyCode}</dd>
                </dl>
            </div>
        </div>

        <div class="col-md-2 col-md-push-1">
            <c:if test="${sapRequest.integrated}">
                <a class="btn btn-primary btn-block" href="<%= contextPath %>/invoice-downloader/sap/${sapRequest.externalId}/${sapRequest.documentNumber}.pdf">
                    <spring:message code="label.download" text="Download"/>
                </a>
            </c:if>
            <c:if test="${sapRequest.isAvailableForTransfer}">
                <c:if test="${isPaymentManager}">
                    <a class="btn btn-default btn-block" href="#" onclick="toggleBlock('transferInvoiceForm'); return false;">
                       <spring:message code="label.transfer" text="Transfer"/>
                    </a>
                </c:if>
            </c:if>
        </div>
    </div>

    <div class="row" id="transferInvoiceForm" style="display: none;">
        <div class="col-md-12">
            <jsp:include page="transferInvoice.jsp"/>
        </div>
    </div>

</div>

<script type="text/javascript">

    function toggleBlock(id) {
        if (getComputedStyle(document.getElementById(id), null).display === 'block') {
            document.getElementById(id).style.display = 'none';
        } else {
            document.getElementById(id).style.display = 'block';
        }
    }
</script>