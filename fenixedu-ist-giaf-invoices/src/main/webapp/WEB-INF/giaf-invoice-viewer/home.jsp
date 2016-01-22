<%@page import="org.fenixedu.academic.domain.accounting.AccountingTransactionDetail"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.stream.Collectors"%>
<%@page import="org.fenixedu.academic.domain.Person"%>
<%@page import="org.fenixedu.bennu.core.domain.User"%>
<%@page import="org.fenixedu.bennu.core.security.Authenticate"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<% final String contextPath = request.getContextPath(); %>
<script src='<%= contextPath + "/bennu-portal/js/angular.min.js" %>'></script>
<script src='<%= contextPath + "/bennu-scheduler-ui/js/libs/moment/moment.min.js" %>'></script>
<script src='<%= contextPath + "/webjars/jquery-ui/1.11.1/jquery-ui.js" %>'></script>
<script src='<%= contextPath + "/webjars/angular-ui-bootstrap/0.9.0/ui-bootstrap-tpls.min.js" %>'></script>

<div class="page-header">
	<h1>
		<spring:message code="title.giaf.invoice.viewer" text="Invoices"/>
	</h1>
</div>

<h3 id="NoResults" style="display: none;"><spring:message code="label.giaf.invoice.none" text="No available results." /></h3>

<table id="invoiceTable" class="table tdmiddle" style="display: none;">
	<thead>
		<tr>
			<th><spring:message code="label.giaf.invoice.academic.year" text="Academic Year"/></th>
			<th><spring:message code="label.giaf.invoice.cycle" text="Cycle"/></th>
			<th><spring:message code="label.giaf.invoice.payment.date" text="Payment Date"/></th>
			<th><spring:message code="label.giaf.invoice.payment.document.number" text="Payment Document Number"/></th>
			<th><spring:message code="label.giaf.invoice.financial.classification" text="Financial Classification"/></th>
			<th><spring:message code="label.giaf.invoice.description" text="Description"/></th>
			<th><spring:message code="label.giaf.invoice.value" text="Value"/></th>
			<th><spring:message code="label.giaf.invoice.document" text="Invoice"/></th>
		</tr>
	</thead>
	<tbody id="invoiceList">
	</tbody>
</table>

<script type="text/javascript">
	var details = ${details};
	var contextPath = '<%= contextPath %>';
	$(document).ready(function() {
		if (details.length == 0) {
			document.getElementById("NoResults").style.display = 'block';
		} else {
			document.getElementById("invoiceTable").style.display = 'block';
		}
        $(details).each(function(i, d) {
        	if (d.paymentDate > '2015') {
            	row = $('<tr/>').appendTo($('#invoiceList'))
                	.append($('<td/>').text(d.reference))
                	.append($('<td/>').text(d.observation))
                	.append($('<td/>').text(d.paymentDate))
                	.append($('<td/>').text(d.paymentMethod + ' ' + d.documentNumber))
            		.append($('<td/>').text(d.article))
                	.append($('<td/>').text(d.description))
                	.append($('<td/>').text(d.unitPrice))
                	;
            	if (d.invoiceNumber) {
            		filePath = contextPath + '/giaf-invoice-downloader/' + d.id;
            		row.append($('<td/>').html('<a href="' + filePath + '">' + d.invoiceNumber + '</a>'));
            	} else {
            		row.append($('<td/>'));
            	}
        	}
        });
	});
</script>

