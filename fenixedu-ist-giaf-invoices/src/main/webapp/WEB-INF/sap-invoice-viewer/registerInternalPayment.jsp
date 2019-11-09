<%@page import="org.joda.time.DateTime"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<% final String contextPath = request.getContextPath(); %>
<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
<script src="${pageContext.request.contextPath}/javaScript/jquery/jquery-ui.js"></script>

${portal.toolkit()}

<div class="page-header">
    <h1>
        <jsp:include page="../fenixedu-academic/accounting/heading-event.jsp"/>
    </h1>
</div>

<div class="page-body">
    <c:set var="person" scope="request" value="${event.person}"/>
    <jsp:include page="../fenixedu-academic/accounting/heading-person.jsp"/>

    <div id="errors" style="display: none; margin-bottom: 25px;" class="alert-info"></div>

    <h3>
        <spring:message code="title.register.internal.payment" text="Internal Payment"/>
    </h3>

    <form class="form-horizontal" method="POST" action="<%= contextPath %>/sap-invoice-viewer/${event.externalId}/registerInternalPayment">
        ${csrf.field()}

        <div class="form-group">
            <label class="control-label col-sm-1" for="type">
                <spring:message code="label.internal.unit" text="Internal Unit" />
            </label>
            <div class="col-sm-4">
                <input type="text" id="searchTerm" name="searchTerm" class="form-control"/>
                <input type="hidden" id="unit" name="unit" value="">
            </div>
        </div>

            <div class="form-group">
                <label class="control-label col-sm-1"><spring:message code="label.org.fenixedu.academic.dto.accounting.DepositAmountBean.whenRegistered"/></label>
                <div class="col-sm-4">
                    <input id="whenRegistered" name="whenRegistered" value="<%= new DateTime().toString("MM/dd/yyyy HH:mm:ss") %>" bennu-datetime required>
                </div>
            </div>

            <div class="form-group">
                <label class="control-label col-sm-1"><spring:message code="label.org.fenixedu.academic.dto.accounting.DepositAmountBean.amount"/></label>
                <div class="col-sm-4">
                    <input name="valueToTransfer" type="text" min="0.01" pattern="[0-9]+([\.][0-9]{0,2})?" placeholder="ex: xxxx.yy" required><span> <spring:message code="label.euro" text="EUR" /></span>
                </div>
            </div>
            <div class="form-group">
                <label class="control-label col-sm-1"><spring:message code="label.org.fenixedu.academic.dto.accounting.DepositAmountBean.reason"/></label>
                <div class="col-sm-4">
                    <textarea name="reason" class="form-control" rows="4" required></textarea>
                </div>
            </div>
            <div class="form-group">
                <div class="col-sm-offset-1 col-sm-4">
                    <button class="btn btn-primary" type="submit">
                        <spring:message code="label.submit"/>
                    </button>
                </div>
            </div>
    </form>
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
                $.post(contextPath + "/client-management/availableInternalUnits", request,function(result) {
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
                $( "#unit" ).val( ui.item.value );               
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
 
