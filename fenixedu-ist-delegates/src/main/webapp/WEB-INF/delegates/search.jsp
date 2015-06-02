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

<form:form modelAttribute="searchBean" role="form" method="post" action="${formActionUrl}" enctype="multipart/form-data" class="form-horizontal">
	<div class="form-group">
		<label for="executionYear" class="col-md-1 control-label"><spring:message code="label.delegates.select.execution.year"/></label>
		<div class="col-md-11">
			<form:select path="executionYear" items="${searchBean.executionYears}" itemLabel="name" itemValue="externalId" onchange="onchangeExecYear(); submit();" class="form-control"/>
		</div>
	</div>
	<div class="form-group">
		<label for="degreeType" class="col-md-1 control-label"><spring:message code="label.delegates.select.degree.type"/></label>
		<div class="col-md-11">
			<form:select path="degreeType" items="${searchBean.degreeTypes}" itemLabel="name.content" itemValue="externalId" onchange="onchangeDegreeType(); submit();" class="form-control"/>
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

<div class="row">
<c:set var="count" value="0" scope="page" />
<c:forEach var="delegate" items="${delegates}">

		<div class="col-md-1">
			<img src="${delegate.picture}" class="img-thumbnail"/>
		</div>
		<div class="col-md-3">
			<div>
				<b>${delegate.delegateTitle}</b>
			</div>
			<div>${delegate.name}</div>
			<div>${delegate.username}</div>
			<div>${delegate.email}</div>
		</div>
	<c:set var="count" value="${count + 1}" scope="page"/>
	<c:if test="${count % 3 == 0}">
		</div>
		<br>
		<div class="row">
	</c:if>
</c:forEach>
</div>