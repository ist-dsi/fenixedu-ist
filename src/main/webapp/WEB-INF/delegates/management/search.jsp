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
  <h1>Delegados<small>Procurar Delegados</small></h1>
</div>

<spring:url var="formActionUrl" value="${action}"/>
<spring:url var="terminatePositionUrl" value="${terminate}"/>
<spring:url var="assignPositionUrl" value="${assign}"/>
<b>Selecione o curso pretendido</b><p>
<form:form modelAttribute="searchBean" role="form" method="post" action="${formActionUrl}" enctype="multipart/form-data" class="form-horizontal">
	<div class="form-group">
		<label for="degreeType" class="col-md-1 control-label">Degree Type</label>
		<div class="col-md-11">
			<form:select path="degreeType" items="${searchBean.degreeTypes}" itemLabel="localizedName" onchange="onchangeDegreeType(); submit();" class="form-control"/>
		</div>
	</div>
	<div class="form-group">
		<label for="degree" class="col-md-1 control-label">Degree</label>
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
	No Delegates Found
</c:if>
<c:if test="${not empty delegates}">
<b>Corpo de Delegados do curso selecionado</b><p>
<table class="table table-bordered">
	<th>Type of Delegate</th>
	<th>Student ID</th>
	<th>Name</th>
	<th>Email</th>
	<th>Interval</th>
	<th>Operations</th>
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
		<a href="${assignPositionUrl}/${delegate.degree.externalId}?curricularYear=${delegate.curricularYear.externalId}&cycleType=${delegate.cycleType}${delegateOID}" >Atribuir</a>
	<c:if test="${not empty delegate.delegateOID}">
		 ,<a href="${terminatePositionUrl}/${delegate.degree.externalId}/${delegate.delegateOID}" >Terminar Cargo</a>
	</c:if>
	</td>
	</tr>
</c:forEach>
</table>
</c:if>
</div>