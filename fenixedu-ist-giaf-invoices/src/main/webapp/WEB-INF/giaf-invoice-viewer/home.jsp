<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST GIAF Invoices.

    FenixEdu IST GIAF Invoices is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST GIAF Invoices is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST GIAF Invoices.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<% final String contextPath = request.getContextPath(); %>

<div class="page-header">
	<h1>
		<spring:message code="title.giaf.invoice.viewer" text="Invoices"/>
	</h1>
</div>

<h3 id="NoResults" style="display: none;"><spring:message code="label.giaf.invoice.none" text="No available results." /></h3>

<table id="invoiceTable" class="table tdmiddle" style="display: none;">
	<thead>
		<tr>
			<th><spring:message code="label.giaf.invoice.payment.date" text="Payment Date"/></th>
			<th><spring:message code="label.giaf.invoice.description" text="Description"/></th>
			<th><spring:message code="label.giaf.invoice.value" text="Value"/></th>
			<th><spring:message code="label.giaf.invoice.document" text="Invoice"/></th>
		</tr>
	</thead>
	<tbody id="invoiceList">
	</tbody>
</table>

<div id="detailsTable" style="display: none;">
<table class="table tdmiddle">
	<thead>
		<tr>
			<th><spring:message code="label.giaf.operation.date" text="Date"/></th>
			<th><spring:message code="label.giaf.operation.type" text="Type"/></th>
			<th><spring:message code="label.giaf.invoice.payment.document.number" text="Payment Document Number"/></th>
			<th><spring:message code="label.giaf.invoice.value" text="Value"/></th>
			<th><spring:message code="label.giaf.invoice.payment.date" text="Payment Date"/></th>
			<th><spring:message code="label.giaf.invoice.document" text="Invoice"/></th>
			<th><spring:message code="label.giaf.invoice.description" text="Description"/></th>
		</tr>
	</thead>
	<tbody id="detailsList">
	</tbody>
</table>
<a href="#" onclick="document.getElementById('detailsTable').style.display = 'none'; document.getElementById('showDetailsLink').style.display = 'block';">
	<spring:message code="label.giaf.invoice.view.details.hide" text="Hide Details"/>
</a>

<% if (request.getParameter("username") != null) { %>
	<form action="<%= contextPath + "/giaf-invoice-viewer" %>" method="post" style="float: right;">
		${csrf.field()}
		<input type="hidden" name="username" value="<%= request.getParameter("username") %>"/>

		<button value="submit">
			<spring:message code="label.giaf.invoice.syncEvents" text="Sync Events"/>
		</button>
	</form>
<% } %>

</div>

<a id="showDetailsLink" href="#" onclick="document.getElementById('detailsTable').style.display = 'block'; document.getElementById('showDetailsLink').style.display = 'none';">
	<spring:message code="label.giaf.invoice.view.details" text="View Details"/>
</a>

<% final String errors = (String) request.getAttribute("errors");
   if (errors != null) {
%><div style="color: red;"><pre><%= errors %></pre></div>
<% }%>
<script type="text/javascript">
	var events = ${events};
	var contextPath = '<%= contextPath %>';

	$(document).ready(function() {
		if (events.length == 0) {
			document.getElementById("NoResults").style.display = 'block';
		} else {
			document.getElementById("invoiceTable").style.display = 'block';
		}
        $(events).each(function(i, entry) {
			if (entry.type == "payment" || entry.type == "fine") {
				var link = contextPath + '/invoice-downloader/giaf/' + entry.eventId + '/' + entry.receiptNumber + '.pdf';
				row = $('<tr/>').appendTo($('#invoiceList'))
					.append($('<td/>').text(entry.paymentDate))
					.append($('<td/>').text(entry.eventDescription))
					.append($('<td/>').text(entry.value))
					.append($('<td/>').html('<a href="' + link + '">' + entry.receiptNumber + '</a>'))
					;
			}

			row = $('<tr/>').appendTo($('#detailsList'))
			.append($('<td/>').text(entry.date))
			.append($('<td/>').text(entry.type))
			.append($('<td/>').text(entry.invoiceNumber ? entry.invoiceNumber : ""))
			.append($('<td/>').text(entry.value))
			.append($('<td/>').text(entry.paymentDate))
			.append($('<td/>').text(entry.receiptNumber))
			.append($('<td/>').text(entry.eventDescription))
			;
        });
	});
</script>

