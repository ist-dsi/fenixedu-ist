<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<spring:url value="/identification-card" var="baseUrl"/>

<h2><bean:message key="title.person.welcome"/> - <bean:message key="label.identification.card"  bundle="APPLICATION_RESOURCES"/></h2>


<form action="${baseUrl}/request-card" method="post">
	${csrf.field()}
	<button id="bpi-form-submit-button" class="btn btn-primary" type="submit">
		<spring:message code="authorize.personal.data.access.submit"/>
	</button>
</form>