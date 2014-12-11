<!DOCTYPE html>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<h1>SELECTS ALL COURSESSS!!!11one</h1>
<spring:url var="formActionReload" value="${reload}" />
<form:form modelAttribute="students" role="form" method="post"
	action="${formActionReload}" enctype="multipart/form-data"
	class="form-horizontal">
	<div class="row">
		<div class="col-sm-2">Enviar mail como:</div>
		<div class="col-sm-5">
			<form:select path="selectedPosition" items="${students.positions}"
				itemLabel="title" itemValue="externalId" onchange="submit();"
				class="form-control" />
		</div>
	</div>
</form:form>
<spring:url var="formActionUrl" value="${action}" />
<form:form modelAttribute="students" role="form" method="post"
	action="${formActionUrl}" enctype="multipart/form-data"
	class="form-horizontal">
	<p>
		<form:input type="hidden"
			value="${students.selectedPosition.externalId}"
			path="selectedPosition" />
	<div class="row">
		<div class="col-sm-2">Enviar mail a:</div>
		<div class="col-sm-5">
			<a
				href="${formActionUrl}${students.selectedPosition.externalId}?studentGroups=true">Grupos
				de alunos</a>, <a
				href="${formActionUrl}${students.selectedPosition.externalId}?studentGroups=false">Alunos
				das disciplinas selecionadas</a>
		</div>
	</div>
	<p>
		<c:if test="${executionCourses == null}">
			<div class="row">
				<div class="col-sm-2">Destinatários (BCC):</div>
				<div class="col-sm-5">
					<c:if test="${yearStudents}">
						<div class="row">
							<div class="col-md-5">
								<form:checkbox value="${execCourse.curricularCourse.externalId}"
									path="selectedYearStudents" />
								Alunos do ano do Delegado
							</div>
						</div>
					</c:if>
					<c:if test="${degreeOrCycleStudents}">
						<div class="row">
							<div class="col-md-5">
								<form:checkbox value="${execCourse.curricularCourse.externalId}"
									path="selectedDegreeOrCycleStudents" />
								Alunos do Mestrado
							</div>
						</div>
					</c:if>
				</div>
			</div>
		</c:if>
		<c:if test="${executionCourses != null}">
			<div class="row">
				<div class="col-md-5">
					<table class="table table-condensed">
						<tr>
							<td class="col-sm-1"></td>
							<td class="col-sm-8">Name</td>
							<td class="col-sm-1">Year</td>
							<td class="col-sm-1">Semester</td>
							<td class="col-sm-1">Students</td>
						</tr>
						<c:forEach var="execCourse" items="${executionCourses}">
							<tr>
								<td class="col-sm-1"><form:checkbox
										value="${execCourse.curricularCourse.externalId}"
										path="selectedExecutionCourses" /></td>
								<td class="col-sm-8">${execCourse.curricularCourse.name}</td>
								<td class="col-sm-1">${execCourse.curricularYear}</td>
								<td class="col-sm-1">${execCourse.semester}</td>
								<td class="col-sm-1">${execCourse.enrolledStudents.size()}</td>
							</tr>
						</c:forEach>
					</table>
				</div>
			</div>

		</c:if>
	</p>
	</p>
	<button type="submit" class="btn btn-default">
		<spring:message code="label.submit" />
	</button>
</form:form>
