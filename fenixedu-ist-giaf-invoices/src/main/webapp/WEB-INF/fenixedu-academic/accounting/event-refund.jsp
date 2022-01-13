<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ page trimDirectiveWhitespaces="true" %>
<% final String contextPath = request.getContextPath(); %>
<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
<script src="${pageContext.request.contextPath}/javaScript/jquery/jquery-ui.js"></script>

<link rel="stylesheet" type="text/css" media="screen" href="<%= request.getContextPath() %>/CSS/accounting.css"/>

${portal.toolkit()}

<div class="container-fluid">
    <header>
        <h1>
            <jsp:include page="heading-event.jsp"/>
        </h1>
    </header>
    <jsp:include page="heading-person.jsp"/>

    <div class="row">
        <div class="col-md-4">
            <h2><spring:message code="label.org.fenixedu.academic.dto.accounting.CreateRefund" text="Create Refund"/></h2>
            <div class="overall-description">
                <dl>
                    <dt><spring:message code="accounting.event.details.payedDebtAmount" text="Total Payed Debt"/></dt>
                    <dd>${payedDebtAmount}</dd>
                </dl>
                <dl>
                    <dt><spring:message code="accounting.event.details.excess.payment" text="Excess Payment"/></dt>
                    <dd>${paidUnusedAmount}</dd>
                </dl>
            </div>
        </div>
    </div>

    <c:if test="${not empty param.error}">
        <div class="row">
            <div class="col-md-12">
                <section>
                    <ul class="nobullet list6">
                        <li><span class="error0"><c:out value="${param.error}"/></span></li>
                    </ul>
                </section>
            </div>
        </div>
    </c:if>

    <div class="row">
        <div class="col-md-12">
            <div class="form-group">
                <label class="control-label" for="type">
                    <spring:message code="label.refund.client.destination" text="Client To Refund" />
                </label>
                <div class="">
                    <spring:message var="searchPlaceholder" scope="request" code="label.search.client.by.vat.or.name"/>
                    <input type="text" id="searchTerm" name="searchTerm" required="required" class="form-control"
                        placeholder="<%= request.getAttribute("searchPlaceholder") %>"/>
                </div>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-md-12">
               <form method="post" class="form-horizontal" action="<%= request.getContextPath() %>/sap-invoice-viewer/${event.externalId}/refundEvent" style="display: inline;">
                    ${csrf.field()}
                    <input type="hidden" id="client1" name="client" value="">
                    <c:if test="${not (paidUnusedAmount > 0)}">
                        <div class="form-group">
                            <label class="control-label col-sm-1"><spring:message code="label.org.fenixedu.academic.dto.accounting.DepositAmountBean.amount"/></label>
                            <div class="col-sm-4">
                                <input name="amount" type="text" pattern="[0-9]+([\.][0-9]{0,2})?" required value="${payedDebtAmount}"><span> €</span>
                            </div>
                        </div>
                    </c:if>
                    <div class="form-group">
                        <label class="control-label col-sm-1"><spring:message code="label.org.fenixedu.academic.dto.accounting.CreateExemptionBean.justificationType"/></label>
                        <div class="col-sm-4">
                            <select class="form-control" name="justificationType" required>
                                <option value=""><spring:message code="label.org.fenixedu.academic.dto.accounting.CreateExemptionBean.justificationType.placeholder"/></option>
                                <c:forEach items="${eventExemptionJustificationTypes}" var="eventExemptionJustificationType">
                                    <option class="justificationTypeOption" value="${eventExemptionJustificationType}">${fr:message('resources.EnumerationResources', eventExemptionJustificationType.qualifiedName)}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="control-label col-sm-1"><spring:message code="label.org.fenixedu.academic.dto.accounting.bankAccountNumber"/></label>
                        <div class="col-sm-4">
                            <input name="bankAccountNumber" class="form-control" required></input>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="control-label col-sm-1"><spring:message code="label.org.fenixedu.academic.dto.accounting.CreateExemptionBean.reason"/></label>
                        <div class="col-sm-4">
                            <textarea name="reason" class="form-control" rows="4" required></textarea>
                        </div>
                    </div>
                    <c:if test="${not (paidUnusedAmount > 0)}">
                        <button type="submit" class="btn btn-primary"><spring:message code="label.create.refund"/></button>
                    </c:if>
                </form>
                <c:if test="${paidUnusedAmount > 0}">
                    <form method="post" action="<%= request.getContextPath() %>/sap-invoice-viewer/${event.externalId}/refundExcessPayment" style="display: inline;">
                        ${csrf.field()}
                        <input type="hidden" id="client2" name="client" value="">
                        <div class="form-group">
                            <label class="control-label col-sm-1"><spring:message code="label.org.fenixedu.academic.dto.accounting.bankAccountNumber"/></label>
                            <div class="col-sm-4">
                                <input name="bankAccountNumber" class="form-control" required></input>
                            </div>
                        </div>
                        <button type="submit" class="btn btn-primary"><spring:message code="label.create.excess.refund"/></button>
                    </form>
                </c:if>
        </div>
    </div>
</div>

  <script type="text/javascript">
    var contextPath = '<%= contextPath %>';

    $(function() {
        $('#searchTerm').autocomplete({
            focus: function(event, ui) {
                //  $( "#searchString" ).val( ui.item.label);
                return false;
            },
            minLength: 2,   
            contentType: "application/json; charset=UTF-8",
            search  : function(){$(this).addClass('ui-autocomplete-loading');},
            open    : function(){$(this).removeClass('ui-autocomplete-loading');},
            source : function(request,response){
                $.post(contextPath + "/client-management/availableClients", request,function(result) {
                    response($.map(result,function(item) {
                        return{
                            label: item.name,
                            value: item.id
                        }
                    }));
                });
            },
            
            select: function( event, ui ) {
                $( "#searchTerm" ).val( ui.item.label );
                $( "#client1" ).val( ui.item.value );
                $( "#client2" ).val( ui.item.value );
                $( "#client3" ).val( ui.item.value );
                return false;
            }
        });
    });

    $(document).ready(function() {
        if (${not empty error}) {
            document.getElementById("errors").style.display = 'block';
            $('#errors').html('<spring:message code="${error}" text="Error"/>');
        }
        if (${not empty exception}) {
            document.getElementById("errors").style.display = 'block';
            $('#errors').html('<spring:message code="${exception}" text="Error"/>');
        }
        if (window.location.href.endsWith("/transfer")) {
            document.getElementById('transferInvoiceForm').style.display = 'block';
        }
    });
 </script>
