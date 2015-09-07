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
