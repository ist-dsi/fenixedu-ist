<%--

    Copyright © 2011 Instituto Superior Técnico

    This file is part of FenixEdu Delegates.

    FenixEdu Delegates is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu Delegates is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu Delegates.  If not, see <http://www.gnu.org/licenses/>.

--%>
<!DOCTYPE html>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<h1><spring:message code="title.delegate.select.groups"/></h1>
<spring:url var="formActionReload" value="${reload}" />
<form:form modelAttribute="students" role="form" method="post"
	action="${formActionReload}" enctype="multipart/form-data"
	class="form-horizontal">
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
	<p>
		<form:input type="hidden"
			value="${students.selectedPosition.externalId}"
			path="selectedPosition" />
	<div class="row">
		<div class="col-sm-2"><spring:message code="delegates.messaging.send.mail.to"/>:</div>
		<div class="col-sm-5">
			<a
				href="${formActionUrl}${students.selectedPosition.externalId}?studentGroups=true"><spring:message code="delegates.messaging.student.groups"/></a>, <a
				href="${formActionUrl}${students.selectedPosition.externalId}?studentGroups=false"><spring:message code="delegates.messaging.student.selected.courses"/></a>
		</div>
	</div>
	<p>
		<c:if test="${executionCourses == null}">
			<div class="row">
				<div class="col-sm-2"><spring:message code="delegates.messaging.bcc"/>:</div>
				<div class="col-sm-5">
					<c:if test="${yearStudents}">
						<div class="row">
							<div class="col-md-5">
								<form:checkbox value="${execCourse.curricularCourse.externalId}"
									path="selectedYearStudents" />
								<spring:message code="delegates.messaging.year.students"/>
							</div>
						</div>
					</c:if>
					<c:if test="${degreeOrCycleStudents}">
						<div class="row">
							<div class="col-md-5">
								<form:checkbox value="${execCourse.curricularCourse.externalId}"
									path="selectedDegreeOrCycleStudents" />
								<spring:message code="delegates.messaging.degreeCycle.students"/>
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
							<td class="col-sm-8"><spring:message code="delegates.messaging.table.name"/></td>
							<td class="col-sm-1"><spring:message code="delegates.messaging.table.year"/></td>
							<td class="col-sm-1"><spring:message code="delegates.messaging.table.semester"/></td>
							<td class="col-sm-1"><spring:message code="delegates.messaging.table.students"/></td>
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
