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
			<th><spring:message code="label.giaf.invoice.payment.document.number" text="Payment Document Number"/></th>
			<th><spring:message code="label.giaf.invoice.payment.date" text="Payment Date"/></th>
			<th><spring:message code="label.giaf.invoice.description" text="Description"/></th>
			<th><spring:message code="label.giaf.invoice.value" text="Value"/></th>
			<th><spring:message code="label.giaf.invoice.document" text="Invoice"/></th>
		</tr>
	</thead>
	<tbody id="invoiceList">
	</tbody>
</table>

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
			if (entry.type == "payment") {
				var link = contextPath + '/giaf-invoice-downloader/' + entry.eventId + '/' + entry.receiptNumber + '.pdf';
				row = $('<tr/>').appendTo($('#invoiceList'))
					.append($('<td/>').text(entry.invoiceNumber ? entry.invoiceNumber : ""))
					.append($('<td/>').text(entry.paymentDate))
					.append($('<td/>').text(entry.eventDescription))
					.append($('<td/>').text(entry.value))
					.append($('<td/>').html('<a href="' + link + '">' + entry.receiptNumber + '</a>'))
					;
			} else if (entry.type == "fine") {
			}
        });
	});
</script>

