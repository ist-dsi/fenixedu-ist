<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<% final String contextPath = request.getContextPath(); %>

<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
<script src="${pageContext.request.contextPath}/javaScript/jquery/jquery-ui.js"></script>

<div class="page-header">
	<h1>
		<spring:message code="title.client.management" text="Client Management"/>
	</h1>
</div>

<h2>
    <spring:message code="title.client.management.search" text="Client Search"/>
</h2>

<form class="form-horizontal" method="GET" enctype="multipart/form-data" action="<%= contextPath %>/client-management/search">
    ${csrf.field()}
    <div class="col-sm-8">
        <spring:message var="searchPlaceholder" scope="request" code="label.search.client.by.vat.or.name"/>
        <input type="text" id="searchTerm" name="searchTerm" required="required" class="form-control"
            placeholder="<%= request.getAttribute("searchPlaceholder") %>"/>
        <input type="hidden" id="client" name="client" value="">
    </div>
    <div class="col-sm-3">
        <button id="submitRequest" class="btn btn-primary">
            <spring:message code="label.search" text="Search" />
        </button>
    </div>
</form>

<br/>
<br/>

<div id="clientInfo" class="alert well well-sm" style="margin-top: 0; font-weight: initial; display: none;">
    <table class="table">
        <tr>
            <th>
                <spring:message code="label.client.accountId" text="accountId"/>
            </th>
            <td id="accountId">
            </td>
        </tr>
        <tr>
            <th>
                <spring:message code="label.client.clientId" text="clientId"/>
            </th>
            <td id="clientId">
            </td>
        </tr>
        <tr>
            <th>
                <spring:message code="label.client.companyName" text="companyName"/>
            </th>
            <td id="companyName">
            </td>
        </tr>
        <tr>
            <th>
                <spring:message code="label.client.country" text="country"/>
            </th>
            <td id="country">
            </td>
        </tr>
        <tr>
            <th>
                <spring:message code="label.client.street" text="street"/>
            </th>
            <td id="street">
            </td>
        </tr>
        <tr>
            <th>
                <spring:message code="label.client.city" text="city"/>
            </th>
            <td id="city">
            </td>
        </tr>
        <tr>
            <th>
                <spring:message code="label.client.region" text="region"/>
            </th>
            <td id="region">
            </td>
        </tr>
        <tr>
            <th>
                <spring:message code="label.client.postalCode" text="postalCode"/>
            </th>
            <td id="postalCode">
            </td>
        </tr>
        <tr>
            <th>
                <spring:message code="label.client.vatNumber" text="vatNumber"/>
            </th>
            <td id="vatNumber">
            </td>
        </tr>
        <tr>
            <th>
                <spring:message code="label.client.fiscalCountry" text="fiscalCountry"/>
            </th>
            <td id="fiscalCountry">
            </td>
        </tr>
        <tr>
            <th>
                <spring:message code="label.client.nationality" text="nationality"/>
            </th>
            <td id="nationality">
            </td>
        </tr>
        <tr>
            <th>
                <spring:message code="label.client.billingIndicator" text="billingIndicator"/>
            </th>
            <td id="billingIndicator">
            </td>
        </tr>
    </table>
</div>

<h2>
    <spring:message code="title.client.management.upload" text="Import Clients From Filet"/>
</h2>

<div class="alert well well-sm" style="margin-top: 0; font-weight: initial">
    <p><spring:message code="label.client.management.upload.instructions" text="Upload Instructions"/></p>
</div>

<div id="errors" style="display: none; margin-bottom: 25px;" class="alert-warning"></div>

<div id="message" style="display: none; margin-bottom: 25px;" class="alert-warning"></div>

<form class="form-horizontal" method="POST" enctype="multipart/form-data">
    ${csrf.field()}
    <div class="form-group">
        <label class="control-label col-sm-2" for="type">
            <spring:message code="label.client.file.to.upload" text="Client File To Upload" />
        </label>
        <div class="col-sm-10">
            <input name="file" type="file" class="form-control" id="file"/>
        </div>
    </div>
    <div class="form-group">
        <div class="col-sm-10 col-sm-offset-2">
            <button id="submitRequest" class="btn btn-primary">
                <spring:message code="label.upload" text="Upload" />
            </button>
        </div>
    </div>
 </form>

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
        if (${not empty errors}) {
            document.getElementById("errors").style.display = 'block';
            $('#errors').html('${errors}');
        }
        if (${not empty message}) {
            document.getElementById("message").style.display = 'block';
            $('#message').html('${message}');
        }
<%
        final String client = (String) request.getAttribute("client");
        if (client != null && !client.isEmpty()) {
%>
            var client = JSON.parse('<%= client %>');
        	document.getElementById("clientInfo").style.display = 'block';
        	$('#accountId').html(client.accountId);
            $('#clientId').html(client.clientId);
            $('#companyName').html(client.companyName);
            $('#country').html(client.country);
            $('#street').html(client.street);
            $('#city').html(client.city);
            $('#region').html(client.region);
            $('#postalCode').html(client.postalCode);
            $('#vatNumber').html(client.vatNumber);
            $('#fiscalCountry').html(client.fiscalCountry);
            $('#nationality').html(client.nationality);
            $('#billingIndicator').html(client.billingIndicator);
<%
        }
%>
    });
</script>
