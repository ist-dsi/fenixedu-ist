<%@ taglib prefix="fr" uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div>
	<c:forEach var="outputLine" items="${output}">
		<div class="error0"><c:out value="${outputLine}"/></div>
	</c:forEach>
</div>
 
<spring:url var="uploadUrl" value="/nonExerciseSituations/uploadOtherServiceExemptions"></spring:url>

<div class="form-group">
	<div class="col-sm-2">
		<a class="btn btn-default" href="#" data-toggle="modal" data-target="#uploadFile"><spring:message code="label.upload"/></a>
		<div class="modal fade" id="uploadFile">
		    <div class="modal-dialog">
		        <div class="modal-content">
		            <div class="modal-header">
		                <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
						<h4><spring:message code="label.upload"/></h4>
		            </div>
		             <form method="POST" class="form-horizontal" enctype="multipart/form-data" action="${uploadUrl}">
							 ${csrf.field()}
						 <div class="modal-body">
		                <p><spring:message code="label.upload"/></p>
						<div class="form-group">
							<div class="col-sm-12">
								<input type="file" name="file" id="file" class="form-control" required/>
							</div>
						</div>
		            </div>
		            <div class="modal-footer">
		           		<button class="btn btn-primary"><spring:message code="label.upload"/></button>
	                    <a class="btn btn-default" data-dismiss="modal"><spring:message code="label.cancel"/></a>
		            </div>
		            </form>
		        </div>
		    </div>
		</div>
	</div>
</div>


<table class="table dataTable table-condensed">
	<thead><tr>
			<th>${fr:message('resources.TeacherCreditsSheetResources', 'label.user')}</th>
			<th>${fr:message('resources.TeacherCreditsSheetResources', 'label.beginDate')}</th>
			<th>${fr:message('resources.TeacherCreditsSheetResources', 'label.endDate')}</th>
			<th>${fr:message('resources.TeacherCreditsSheetResources', 'label.teacher.service.logs.description')}</th>
	</tr></thead>
	<tbody><c:forEach var="situation" items="${otherServiceExemptions}">
		<tr>
			<td><c:out value="${situation.person.name} (${situation.person.username})"/></td>
		    <td><c:out value="${situation.beginDate.toString('dd/MM/yyyy')}"/></td>
	        <td><c:out value="${situation.endDate.toString('dd/MM/yyyy')}"/></td>
		    <td><c:out value="${situation.description}"/></td>
		    <spring:url var="deleteUrl" value="/nonExerciseSituations/deleteOtherServiceExemption/${situation.externalId}"></spring:url>
		    <td><a class="btn btn-default" href="${deleteUrl}"><spring:message code="label.delete"/></a>
		</tr>
	</c:forEach></tbody>
</table>
