<!DOCTYPE html> 
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<c:if test="${not empty requests}">
	<table class="tstyle1 thlight thright mtop025 table">
	    <thead>
	        <th><spring:message code="Identification" /></th>
	        <th><spring:message code="Name" /></th>
	        <th><spring:message code="Date" /></th>
	        <th><spring:message code="Request Successful" /></th>
	        <th><spring:message code="Error Code" /></th>
	        <th><spring:message code="Error Description" /></th>
	        <th><spring:message code="Error Line" /></th>
	    </thead>
	    <tbody>
	        <c:forEach var="request" items="${requests}">
		    	<tr>
	        		<td>
						<c:out value="${request.identificationNumber}" />
	        		</td>
	        		<td>
	        			<c:out value="${request.name}"/>
	        		</td>
	        		<td>
	        			<c:out value="${request.expiryDate.toString(\"MM/yy\")}"/>
	        		</td>
	        		<td>
	        			<c:choose>
	        				<c:when test="${request.registerSuccessful}">
								<spring:message code="Sim" />
	        				</c:when>
	        				<c:otherwise>
	        					<spring:message code="Não" />
	        				</c:otherwise>
	        			</c:choose>
	        		</td>
	        		<td>
	        			<c:choose>
	        				<c:when test="${request.registerSuccessful}">								
								<spring:message code="-" />
	        				</c:when>
	        				<c:otherwise>
								<c:out value="${request.errorCode}"/>
	        				</c:otherwise>
	        			</c:choose>
	        		</td>
	        		<td>
	        			<c:choose>
	        				<c:when test="${request.registerSuccessful}">
								<spring:message code="-" />
	        				</c:when>
	        				<c:otherwise>
	        					<c:out value="${request.errorDescription}"/>
	        				</c:otherwise>
	        			</c:choose>
	        		</td>
	        		<td>
	        			<c:choose>
	        				<c:when test="${request.registerSuccessful}">
	        					<spring:message code="-" />
	        				</c:when>
	        				<c:otherwise>
								<c:out value="${request.responseLine}"/>
	        				</c:otherwise>
	        			</c:choose>
	        		</td>
	        	</tr>
	        </c:forEach>
	    </tbody>
	</table>
</c:if>

 