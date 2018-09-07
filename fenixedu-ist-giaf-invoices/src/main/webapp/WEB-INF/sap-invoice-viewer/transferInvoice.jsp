<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<% final String contextPath = request.getContextPath(); %>

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
            <input name="uvat" type="text" class="form-control" id="uvat" required="required" value=""/>
        </div>
    </div>
    <div class="form-group">
        <label class="control-label col-sm-2" for="type">
            <spring:message code="label.invoice.transfer.value" text="Value to Transfer" />
        </label>
        <div class="col-sm-10">
            <input name="valueToTransfer" type="text" class="form-control" id="valueToTransfer" required="required" value=""/>
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