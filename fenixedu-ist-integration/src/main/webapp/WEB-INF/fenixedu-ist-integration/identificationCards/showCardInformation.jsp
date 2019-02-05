<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<spring:url value="/identification-card" var="baseUrl"/>

<h2><bean:message key="title.person.welcome"/> - <bean:message key="label.identification.card"  bundle="APPLICATION_RESOURCES"/></h2>

<c:if test="${not empty currentState}">
	<p><strong>Estado atual</strong></p>

	<c:out value="${currentState}" />
</c:if>

<c:if test="${not empty requests}">
	<p><strong>Log</strong></p>

	<div class="alert well" >
		<c:forEach var="request" items="${requests}">
			<c:set var="format" value="dd-MM-yyyy HH:mm" />
			<c:out value="${request.createdAt.toString(format)}" />
		</c:forEach>
	</div>
</c:if>

<form action="${baseUrl}/request-card" method="post">
	${csrf.field()}
	<button id="bpi-form-submit-button" class="btn btn-primary" type="submit">
		<spring:message code="authorize.personal.data.access.submit"/>
	</button>
</form>