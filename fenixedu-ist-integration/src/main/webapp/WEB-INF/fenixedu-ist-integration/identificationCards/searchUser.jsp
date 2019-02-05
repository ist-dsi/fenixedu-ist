<!DOCTYPE html> 
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<h2 class="santanderTitle"><spring:message code="subtitle.santander.cards"/></h2>

<spring:url var="searchPerson" value="/identification-card-log/search-entries"/>
<form:form modelAttribute="santanderEntrySearchBean" method="POST" action="${searchPerson}" class="form-horizontal">
	${csrf.field()}
	
	<div class="tstyle5 thmiddle thright thlight">

		<div class="form-group">
			<div class="control-label col-sm-2"> <spring:message code="label.card.id.search.username"/> </div>
			<div class="col-sm-10"> <form:input class="form-control" path="username" /> </div>
		</div>

    	<div class="form-group">   
			<form:label path="executionYear" class="control-label col-sm-2"> <spring:message code="Ano Lectivo" /> </form:label>			
			<div class="col-sm-10">
				<form:select path="executionYear" class="form-control">
					<form:option value="Escolha um ano" />
					<form:options items="${executionYears}" itemValue="externalId" itemLabel="name"/>
				</form:select>
			</div>
		</div>
	</div>

	<p class="form-group">
		<p class="col-sm-offset-2 col-sm-10">
			<button class="btn btn-primary" type="submit">
				<spring:message code="authorize.personal.data.access.submit"/>
			</button>
		</p>
	</p>

</form:form>
