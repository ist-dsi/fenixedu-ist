<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST Delegates.

    FenixEdu IST Delegates is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST Delegates is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.

--%>
<!DOCTYPE html>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<script>
	function onchangeExecYear(){
		$("#degreeType").val("");
		$("#degree").val("");
	}
	
	function onchangeDegreeType(){
		$("#degree").val("");
	}
	
	function onchangeDegree(){
	}
	
	
</script>

<div class="page-header">
  <h1><spring:message code="title.delegates.app"/><small><spring:message code="title.delegates.search"/></small></h1>
</div>

<spring:url var="formActionUrl" value="${action}"/>
<spring:url var="terminatePositionUrl" value="${terminate}"/>
<spring:url var="assignPositionUrl" value="${assign}"/>
<b><spring:message code="label.delegates.select.degree"/></b><p>
<form:form modelAttribute="searchBean" role="form" method="post" action="${formActionUrl}" enctype="multipart/form-data" class="form-horizontal">
	<div class="form-group">
		<label for="degreeType" class="col-md-1 control-label"><spring:message code="label.delegates.select.degree.type"/></label>
		<div class="col-md-11">
			<form:select path="degreeType" items="${searchBean.degreeTypes}" itemLabel="localizedName" onchange="onchangeDegreeType(); submit();" class="form-control"/>
		</div>
	</div>
	<div class="form-group">
		<label for="degree" class="col-md-1 control-label"><spring:message code="label.delegates.select.degree.sel"/></label>
		<div class="col-md-11">
			<form:select path="degree" items="${searchBean.degrees}" itemLabel="nameI18N" itemValue="externalId" onchange="onchangeDegree(); submit();" class="form-control"/>
		</div>
	</div>
</form:form>
<style>
td {
    padding: 0.5em;
}
</style>

<br>

<c:if test="${empty delegates}">
<spring:message code="label.no.delegates"/>
</c:if>
<c:if test="${not empty delegates}">
<b><spring:message code="label.delegates.of.selected.course"/></b><p>
<table class="table table-bordered">
	<th><spring:message code="label.delegates.type"/></th>
	<th><spring:message code="label.delegates.username"/></th>
	<th><spring:message code="delegates.messaging.table.name"/></th>
	<th><spring:message code="label.delegates.email"/></th>
	<th><spring:message code="label.delegates.interval"/></th>
	<th><spring:message code="label.delegates.operations"/></th>
<c:forEach var="delegate" items="${delegates}">
	<tr>
	<td>
		${delegate.delegateTitle}
	</td>
	<td>${delegate.username}</td>
	<td>${delegate.name}</td>
	<td>${delegate.email}</td>
	<td>${delegate.interval}</td>
	<spring:url var="delegateOID" value=""/>
	<c:if test="${not empty delegate.delegateOID}">
		<c:set var="delegateOID" value="&delegateOID=${delegate.delegateOID}"/>
	</c:if>
	<td>
		<a href="${assignPositionUrl}/${delegate.degree.externalId}?curricularYear=${delegate.curricularYear.externalId}&cycleType=${delegate.cycleType}${delegateOID}" ><spring:message code="label.delegates.attribute.position"/></a>
	<c:if test="${not empty delegate.delegateOID}">
		 ,<a href="${terminatePositionUrl}/${delegate.degree.externalId}/${delegate.delegateOID}" ><spring:message code="label.delegates.terminate.position"/></a>
	</c:if>
	</td>
	</tr>
</c:forEach>
</table>
</c:if>
</div>