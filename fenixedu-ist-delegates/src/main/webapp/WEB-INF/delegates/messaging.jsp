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

<style>
	.delegate-messaging-groups {
		display: flex;
		flex-wrap: wrap;
		margin-bottom: 16px;
	}

	.delegate-messaging-groups > div:not(:last-child) {
		margin-right: 16px;
	}
</style>

<h1><spring:message code="title.delegate.select.groups"/></h1>
<spring:url var="formActionReload" value="${reload}" />
<form:form modelAttribute="students" role="form" method="post"
	action="${formActionReload}" enctype="multipart/form-data"
	class="form-horizontal">
	${csrf.field()}
	<div class="row">
		<div class="col-sm-2"><spring:message code="delegates.messaging.send.email.as"/>:</div>
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
	${csrf.field()}
	<form:input type="hidden" value="${students.selectedPosition.externalId}" path="selectedPosition" />
	<form:input type="hidden" value="${responsibleTeachers}" path="selectedResponsibleTeachers" />
	<div role="tabpanel" style="margin-top: 16px;">
		<ul class="nav nav-tabs" role="tablist">
			<li class="${executionCourses == null ? "active" : ""}">
				<a href="${formActionUrl}${students.selectedPosition.externalId}?studentGroups=true">
					<spring:message code="delegates.messaging.student.groups"/>
				</a>
			</li>
			<li class="${executionCourses != null && not responsibleTeachers ? "active" : ""}">
				<a href="${formActionUrl}${students.selectedPosition.externalId}?studentGroups=false">
					<spring:message code="delegates.messaging.student.selected.courses"/>
				</a>
			</li>
			<li class="${executionCourses != null && responsibleTeachers ? "active" : ""}">
				<a href="${formActionUrl}${students.selectedPosition.externalId}?studentGroups=false&responsibleTeachers=true">
					<spring:message code="delegates.messaging.responsibleTeachers.selected.courses"/>
				</a>
			</li>
		</ul>
		<div class="tab-content">
			<div role="tabpanel" class="tab-pane active">
				<div class="well">
					<c:if test="${executionCourses == null}">
						<div class="delegate-messaging-groups">
							<div><spring:message code="delegates.messaging.bcc"/>:</div>
							<div>
								<c:if test="${yearStudents}">
									<c:forEach var="execYear" items="${mandateYears}">
										<div>
											<form:checkbox value="${execYear.externalId}" path="selectedGroupsExecutionYears" />
											<spring:message code="delegates.messaging.year.students">
												<spring:argument>${execYear.name}</spring:argument>
											</spring:message>
										</div>
									</c:forEach>
								</c:if>
								<c:if test="${degreeOrCycleStudents}">
									<c:forEach var="execYear" items="${mandateYears}">
										<div>
											<form:checkbox  value="${execYear.externalId}" path="selectedGroupsExecutionYears" />
											<spring:message code="delegates.messaging.degreeCycle.students">
												<spring:argument>${execYear.name}</spring:argument>
											</spring:message>
										</div>
									</c:forEach>
								</c:if>
							</div>
						</div>
					</c:if>
					<c:if test="${executionCourses != null}">
						<table class="table table-condensed">
							<tr>
								<td class="col-sm-1"></td>
								<td class="col-sm-8"><spring:message code="delegates.messaging.table.name"/></td>
								<td class="col-sm-2"><spring:message code="delegates.messaging.table.academic_year"/></td>
								<td class="col-sm-1"><spring:message code="delegates.messaging.table.semester"/></td>
								<c:if test="${not responsibleTeachers}">
									<td class="col-sm-1"><spring:message code="delegates.messaging.table.students"/></td>
								</c:if>
							</tr>
							<c:forEach var="execCourse" items="${executionCourses}">
								<tr>
									<td class="col-sm-1"><form:checkbox value="${execCourse.executionCourse.externalId}" path="selectedExecutionCourses" /></td>
									<td class="col-sm-8"><a href="${execCourse.executionCourse.siteUrl}" target="_blank"><c:out value="${execCourse.executionCourse.name}"/></a></td>
									<td class="col-sm-2"><c:out value="${execCourse.executionYear.name}"/></td>
									<td class="col-sm-1"><c:out value="${execCourse.semester}"/></td>
									<c:if test="${not responsibleTeachers}">
										<td class="col-sm-1">${execCourse.enrollmentCount}</td>
									</c:if>
								</tr>
							</c:forEach>
						</table>
					</c:if>
					<button type="submit" class="btn btn-default"><spring:message code="delegates.messaging.send.email.submit" /></button>
				</div>
			</div>
		</div>
	</div>

</form:form>
