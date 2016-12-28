<%@ taglib prefix="fr" uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div>
	<c:forEach var="outputLine" items="${output}">
		<div class="error0"><c:out value="${outputLine}"/></div>
	</c:forEach>
</div>
 

<spring:url var="editUrl" value="/effortRates"></spring:url>
<form:form role="form" modelAttribute="departmentCreditsBean" method="GET" class="form-horizontal" action="${editUrl}">
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
			<button type="submit" class="btn btn-default" id=submit><spring:message code="label.submit" /></button>
		</div>				
	</div>
</form:form>

<spring:url var="exportUrl" value="/effortRates/exportExecutionCoursesEffortRates?executionYear=${departmentCreditsBean.executionYear.externalId}"></spring:url>
<spring:url var="uploadUrl" value="/effortRates/uploadExecutionCoursesEffortRates"></spring:url>

<spring:eval expression="T(pt.ist.fenixedu.teacher.evaluation.domain.credits.AnnualCreditsState).getAnnualCreditsState(departmentCreditsBean.getExecutionYear())" var="annualCreditsState" />
<c:set var="canEditValues" value="${!annualCreditsState.getIsFinalCreditsCalculated() && !annualCreditsState.getIsCreditsClosed()}"></c:set>

	<div class="form-group">
		<div class="col-sm-2">
			<a class="btn btn-default" href="${exportUrl}"><spring:message code="label.export"/></a>
			<c:if test="${canEditValues}">
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
								<input type="hidden" name="executionYear" value="<c:out value='${departmentCreditsBean.executionYear.externalId}'/>"/>
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
			</c:if>
		</div>
	</div>

<table class="table dataTable table-condensed">
	<thead><tr>
			<th>${fr:message('resources.TeacherCreditsSheetResources', 'label.course')}</th>
			<th>${fr:message('resources.TeacherCreditsSheetResources', 'label.course')}</th>
			<th>${fr:message('resources.TeacherCreditsSheetResources', 'label.degrees')}</th>
			<th>${fr:message('resources.TeacherCreditsSheetResources', 'label.effortRate')}</th>
	</tr></thead>
	<tbody><c:forEach var="executionCourse" items="${executionCourses}">
		<spring:eval expression="T(org.fenixedu.academic.domain.reports.GepReportFile).getExecutionCourseCode(executionCourse)" var="executionCourseCode" />
		<tr>
			<td><c:out value="${executionCourseCode}"/></td>
		    <td><c:out value="${executionCourse.name} (${executionCourse.sigla})"/></td>
	        <td><c:out value="${executionCourse.degreePresentationString}"/></td>
		    <td><c:out value="${executionCourse.effortRate}"/></td>
		</tr>
	</c:forEach></tbody>
</table>