<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST Integration.

    FenixEdu IST Integration is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST Integration is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
    final String contextPath = request.getContextPath();
	final boolean allowCGDAccess = (Boolean) request.getAttribute("allowCGDAccess");
%>

<div class="page-header">
	<h1>
		<spring:message code="authorize.personal.data.access.title" />
	</h1>
</div>

<div id="txt">
	<div id="cgdBody">
		<h2>
			<spring:message code="authorize.personal.data.access.title.cgd" />
		</h2>
		<div class="infobox">
			<p>
				<spring:message code="authorize.personal.data.access.description.cgd" />
			</p>
		</div>
		<p>
			<spring:message code="label.answer"/>:
			<spring:message code='<%= allowCGDAccess ? "label.yes" : "label.no" %>' />
		</p>
		<p class="text-center">
			<form method="post">
				<input type="hidden" name="qs" value='/authorize-personal-data-access'/>					
				<input type="hidden" name="allowAccess" value='<%= Boolean.toString(!allowCGDAccess) %>'/>
				<button class='<%= "btn btn-lg " + (allowCGDAccess ? "btn-default" : "btn-primary") %>'>
					<spring:message code='<%= allowCGDAccess ? "label.revoke" : "label.grant" %>' />
				</button>
			</form>
		</p>
	</div>
</div>
