<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>

<spring:url value="/identification-card" var="baseUrl"/>

<h2><bean:message key="title.person.welcome"/> - <bean:message key="label.identification.card"  bundle="APPLICATION_RESOURCES"/></h2>

<p><strong>Available actions</strong></p>

<form action="${baseUrl}/request-card" method="post">
	${csrf.field()}
    <label for="action">Acção</label>
    <select name="action" id="action">
        <c:forEach var="action" items="${availableActions}">
            <option value="${action.name}">${action.localizedName}</option>
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

<br />
<br />

<p><strong>Histórico de Cartões</strong></p>

<table class="table tdmiddle">
    <thead>
    <tr>
        <th><spring:message code="label.santander.entry.new.request.creation.date" text="Request Date"/></th>
        <th>Estado</th>
        <th>Expira em</th>
        <th></th>
    </tr>
    </thead>
    <tbody>
    <c:forEach var="card" items="${cardHistory}">
        <tr>
            <td><joda:format value="${card.lastUpdate}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
            <td><c:out value="${card.state}"/></td>
            <td><joda:format value="${card.expiryDate}" pattern="yyyy-MM-dd"/></td>
        </tr>
    </c:forEach>
    </tbody>
</table>
