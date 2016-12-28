<%@page contentType="text/html" %>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<spring:url var="viewUrl" value="/creditsPool"></spring:url>
<form:form role="form" modelAttribute="creditsPoolBean" method="GET" class="form-horizontal" action="${viewUrl}">
	${csrf.field()}
	<div class="form-group">
		<label for="executionYear" class="col-sm-2 control-label">${fr:message('resources.ApplicationResources', 'label.executionYear')}:</label>
		<div class="col-sm-10">
			<form:select path="executionYear" id="executionYear" class="form-control col-sm-11" required="required">
		    <form:options items="${executionYears}" itemLabel="name" itemValue="externalId"/>
		</form:select>
		</div>
	</div>
	<div class="form-group">
		<div class="col-sm-push-2 col-sm-10">
			<button type="submit" class="btn btn-default" id="search"><spring:message code="label.submit" /></button>
		</div>				
	</div>
</form:form>

<br/><br/>

<spring:url var="editUrl" value="/creditsPool/editCreditsPool"></spring:url>
<form:form role="form" modelAttribute="creditsPoolBean" method="POST" class="form-horizontal" action="${editUrl}">
	${csrf.field()}
	<c:set var="canEditCreditsPool" value="${creditsPoolBean.getCanEditCreditsPool()}"/>
	<h3>${fr:message('resources.TeacherCreditsSheetResources', 'label.departmentCreditsPool')} <c:out value="${creditsPoolBean.executionYear.year}"/></h3>
	<input type="hidden" name="executionYear" value="<c:out value='${creditsPoolBean.executionYear.externalId}'/>"/>
	<table class="table dataTable table-condensed">
			<thead><tr>
					<th>${fr:message('resources.TeacherCreditsSheetResources', 'label.department')}</th>
					<th>${fr:message('resources.TeacherCreditsSheetResources', 'label.departmentCreditsPool')}</th>
					<th>${fr:message('resources.TeacherCreditsSheetResources', 'label.changedDepartmentCreditsPool')}</th>
			</tr></thead>
			<tbody>		
				<c:forEach var="pool" items="${creditsPoolBean.creditsPoolByDepartments}" varStatus="status">
					<input type="hidden" name="creditsPoolByDepartments[${status.index}].department" value="<c:out value='${pool.department.externalId}'/>"/>
					<tr>
						<td><c:out value="${pool.department.name}"/></td>
						<c:choose>
							<c:when test="${canEditCreditsPool}">
							    <td><form:input path="creditsPoolByDepartments[${status.index}].originalCreditsPool" class="form-control" value="${pool.originalCreditsPool}" type="number" step="any" min="0"/></td>
								<td><form:input path="creditsPoolByDepartments[${status.index}].creditsPool" class="form-control" value="${pool.creditsPool}" type="number" step="any" min="0"/></td>
							</c:when>    
							<c:otherwise>
								<td><c:out value="${pool.originalCreditsPool}"/></td>
							    <td><c:out value="${pool.creditsPool}"/></td>
							</c:otherwise>
						</c:choose>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	<c:if test="${canEditCreditsPool}">
		<div class="form-group">
			<div class="col-sm-push-1 col-sm-11">
				<button type="submit" class="btn btn-default" ><spring:message code="label.submit" /></button>
			</div>				
		</div>
	</c:if>
</form:form>
