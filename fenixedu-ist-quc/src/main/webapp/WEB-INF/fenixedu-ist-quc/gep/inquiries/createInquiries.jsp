<%--

    Copyright © 2017 Instituto Superior Técnico

    This file is part of FenixEdu Academic.

    FenixEdu Academic is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu Academic is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<spring:url var="createUrl" value="/create-quc-inquiries/create"></spring:url>

<div class="page-header">
    <h1>
        <spring:message code="label.create.inquiries" />
        <small><spring:message code="label.create.inquiries" arguments="${executionSemester.qualifiedName}"/></small>
    </h1>
</div>

<c:choose>
    <c:when test="${success}">
        <div class="alert alert-success">
            <spring:message code="label.create.success" arguments="${executionSemester.qualifiedName}"/>
        </div>
    </c:when>
    <c:otherwise>
        <c:choose>
            <c:when test="${alreadyExists}">
                <div class="alert alert-info">
                    <spring:message code="label.create.alreadyExists" arguments="${executionSemester.qualifiedName}"/>
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-info">
                    <spring:message code="message.create.inquiries" arguments="${executionSemester.qualifiedName}"/>
                </div>
                <a href="${createUrl}" class="btn btn-primary" role="button"><spring:message code="label.create.inquiries"/></a>
            </c:otherwise>
        </c:choose>
    </c:otherwise>
</c:choose>