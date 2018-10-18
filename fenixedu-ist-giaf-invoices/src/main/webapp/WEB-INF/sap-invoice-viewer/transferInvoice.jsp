<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<% final String contextPath = request.getContextPath(); %>

<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
<script src="${pageContext.request.contextPath}/javaScript/jquery/jquery-ui.js"></script>

<div class="page-header">
    <h1>
        <spring:message code="title.sap.invoice.transfer" text="Transfer Invoice"/>
    </h1>
</div>

<div id="errors" style="display: none; margin-bottom: 25px;" class="alert-warning"></div>

<form class="form-horizontal" method="POST">
    ${csrf.field()}
    <div class="form-group">
        <label class="control-label col-sm-2" for="type">
            <spring:message code="label.event.description" text="Event" />
        </label>
        <div class="col-sm-10" id="eventDescription">
        </div>
    </div>
    <div class="form-group">
        <label class="control-label col-sm-2" for="type">
            <spring:message code="label.sapRequest.documentNumber" text="Document Number" />
        </label>
        <div class="col-sm-10" id="documentNumber">
        </div>
    </div>
    <div class="form-group">
        <label class="control-label col-sm-2" for="type">
            <spring:message code="label.sapRequest.value" text="Value" />
        </label>
        <div class="col-sm-10" id="value">
        </div>
    </div>
    <div class="form-group">
        <label class="control-label col-sm-2" for="type">
            <spring:message code="label.invoice.transfer.destination.ssn" text="Destination UVat Number" />
        </label>
        <div class="col-sm-10">
            <spring:message var="searchPlaceholder" scope="request" code="label.search.client.by.vat.or.name"/>
            <input type="text" id="searchTerm" name="searchTerm" required="required" class="form-control"
                placeholder="<%= request.getAttribute("searchPlaceholder") %>"/>
            <input type="hidden" id="client" name="client" value="">
        </div>
    </div>
    <div class="form-group">
        <label class="control-label col-sm-2" for="type">
            <spring:message code="label.invoice.transfer.value" text="Value to Transfer" />
        </label>
        <div class="col-sm-10">
            <input name="valueToTransfer" class="form-control" id="valueToTransfer" type="text" min="0.01" pattern="[0-9]+([\.][0-9]{0,2})?" placeholder="ex: xxxx.yy" required><span> €</span>
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
                <spring:message code="label.transfer" text="Transferir" />
            </button>
        </div>
    </div>
 </form>
 
 <script type="text/javascript">
    var event = ${event};
    var sapRequest = ${sapRequest};
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
    	$('#eventDescription').html(event.eventDescription);
    	$('#documentNumber').html(sapRequest.documentNumber);
    	$('#value').html(sapRequest.value);

    	if (${not empty error}) {
    		document.getElementById("errors").style.display = 'block';
    		$('#errors').html('<spring:message code="${error}" text="Error"/>');
    	}
    	if (${not empty exception}) {
    		document.getElementById("errors").style.display = 'block';
    		$('#errors').html('<spring:message code="${exception}" text="Error"/>');
        }
    });
 </script>
