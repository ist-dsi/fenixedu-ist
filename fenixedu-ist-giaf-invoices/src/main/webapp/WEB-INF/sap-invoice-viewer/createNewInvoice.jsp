<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<% final String contextPath = request.getContextPath(); %>
<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
<script src="${pageContext.request.contextPath}/javaScript/jquery/jquery-ui.js"></script>

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
        <spring:message code="label.sapRequest.create.new.invoice" text="Create New Invoice"/>
    </h3>

    <form class="form-horizontal" method="POST" action="<%= contextPath %>/sap-invoice-viewer/${event.externalId}/createNewInvoice">
        ${csrf.field()}

        <div class="form-group">
            <label class="control-label col-sm-2" for="type">
                <spring:message code="label.sapRequest.create.new.invoice.value" text="Value for Invoice" />
                <spring:message code="label.euro" text="EUR" />
            </label>
            <div class="col-sm-10">
                <input name="valueToTransfer" type="text" min="0.01" pattern="[0-9]+([\.][0-9]{0,2})?" placeholder="ex: xxxx.yy" required class="form-control" value="${sapRequest.valueAvailableForTransfer}">
            </div>
        </div>

        <div class="alert alert-warning">
            <h4><b><spring:message code="label.sapRequest.create.new.invoice.for.third.party" text="For External Entity" /></b></h4>

            <p style="white-space: pre-line;">
                <spring:message code="label.sapRequest.create.new.invoice.for.third.party.details" text="" />
            </p>
        </div>

        <div class="form-group">
            <label class="control-label col-sm-2" for="type">
                <spring:message code="label.invoice.transfer.destination.ssn" text="Destination UVat Number" />
            </label>
            <div class="col-sm-10">
                <spring:message var="searchPlaceholder" scope="request" code="label.search.client.by.vat.or.name"/>
                <input type="text" id="searchTerm" name="searchTerm" class="form-control"
                    placeholder="<%= request.getAttribute("searchPlaceholder") %>"/>
                <input type="hidden" id="client" name="client" value="">
            </div>
        </div>

        <div class="form-group">
            <label class="control-label col-sm-2" for="type">
                <spring:message code="label.invoice.transfer.pledgeNumber" text="Pledge Number" />
            </label>
            <div class="col-sm-10">
                <input name="pledgeNumber" type="text" class="form-control" id="pledgeNumber" value=""/>
            </div>
        </div>

        <div class="form-group">
            <div class="col-sm-10 col-sm-offset-2">
                <button id="submitRequest" class="btn btn-primary">
                    <spring:message code="label.create" text="Create" />
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
                $( "#client" ).val( ui.item.value );               
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
 