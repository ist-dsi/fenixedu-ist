<!DOCTYPE html> 
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>


<spring:url var="searchPerson" value="/identification-card-log/search-person"/>
<form:form modelAttribute="usernameBean" method="POST" action="${searchPerson}" class="form-horizontal">
	${csrf.field()}
	
	<table>
          <tr>
              <td>Username</td>
              <td><form:input path="username" /></td>
          </tr>
    </table>

	<button id="bpi-form-submit-button" class="btn btn-primary" type="submit">
		<spring:message code="authorize.personal.data.access.submit"/>
	</button>
</form:form>