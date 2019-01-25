<!DOCTYPE html> 
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>

<h2 class="santanderTitle"><spring:message code="subtitle.santander.cards" text="Santander Cards"/></h2>

<br>
<br>

<div>
	<table class="table tdmiddle">
		<thead>
			<tr>
				<th><spring:message code="label.santander.entry.new.request.person.identification" text="Identification"/></th>
				<th><spring:message code="label.santander.entry.new.request.person.name" text="Name"/></th>
				<th><spring:message code="label.santander.entry.new.request.person..card.name" text="Name on Card"/></th>
				<th><spring:message code="label.santander.entry.new.request.person.card.expiry.date" text="Card Expiry Date"/></th>
				<th><spring:message code="label.santander.entry.new.request.creation.date" text="Last Updated"/></th>
				<th><spring:message code="label.santander.entry.new.request.error.flag" text="Request Successful"/></th>
				<th><spring:message code="label.santander.entry.new.request.error.message" text="Error Message"/></th>
				<th></th>
			</tr>
		</thead>
		<tbody>
			<c:forEach var="request" items="${requests}">
				<c:set var = "cardInfo" value = "${request.santanderCardInfo}"/>
				<tr>
					<td><c:out value="${cardInfo.identificationNumber}"/></td>                    
					<td><c:out value="${request.user.displayName}"/></td>
					<td><c:out value="${cardInfo.cardName}"/></td>
					<td><joda:format value="${cardInfo.expiryDate}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
					<td><joda:format value="${request.lastUpdate}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
					<td>
						<c:if test="${request.wasRegisterSuccessful()}">
							<span class="glyphicon glyphicon glyphicon-ok" aria-hidden="true" style="color: green;"></span>
						</c:if>
						<c:if test="${not request.wasRegisterSuccessful()}">
							<span class="glyphicon glyphicon glyphicon-remove" aria-hidden="true" style="color: red;"></span>
						</c:if>
					</td>
					<td>
                    	<c:if test="${request.wasRegisterSuccessful()}">
							<span class="glyphicon glyphicon glyphicon-ok" aria-hidden="true" style="color: green;"></span>
						</c:if>
						<c:if test="${not request.wasRegisterSuccessful()}">
							<c:out value="${request.errorDescriptionMessage}"/>
						</c:if>
					</td>
				</tr>
				

			</c:forEach>
		</tbody>
	</table>
</div>

 