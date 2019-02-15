<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<spring:url value="/identification-card" var="baseUrl"/>

<h2><bean:message key="title.person.welcome"/> - <bean:message key="label.identification.card"  bundle="APPLICATION_RESOURCES"/></h2>

<c:if test="${not empty currentState}">
	<p><strong>Estado atual</strong></p>

	<c:out value="${currentState}" />
</c:if>

<br />
<br />

<p><strong>Available actions</strong></p>

<form action="${baseUrl}/request-card" method="post">
	${csrf.field()}
    <label for="action">Acção</label>
    <select name="action" id="action">
        <c:forEach var="action" items="${availableActions}">
            <option value="${action.action}">${action.label}</option>
        </c:forEach>
    </select>
	<button class="btn btn-primary" type="submit">
		<spring:message code="authorize.personal.data.access.submit"/>
	</button>
</form>

<br />
<br />

<p><strong>Test actions</strong></p>

<form action="${baseUrl}/request-card-test" method="post">
    ${csrf.field()}
    <label for="action">Acção</label>
    <select name="action" id="action">
        <option value="NOVO">NOVO</option>
        <option value="REMI">REMI</option>
        <option value="RENU">RENU</option>
        <option value="ATUA">ATUA</option>
        <option value="CANC">CANC</option>
    </select>
    <button class="btn btn-primary" type="submit">
        <spring:message code="authorize.personal.data.access.submit"/>
    </button>
</form>