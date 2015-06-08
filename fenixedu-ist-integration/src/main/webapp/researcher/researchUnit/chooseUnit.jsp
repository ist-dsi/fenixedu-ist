<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST Integration.

    FenixEdu IST Integration is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST Integration is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>

<h3><bean:message key="label.select.unit" bundle="DEPARTMENT_MEMBER_RESOURCES"></bean:message></h3>
<br />

<c:if test="${empty units}">
	<bean:message key="message.error.notAuthorizedContents" bundle="APPLICATION_RESOURCES" />
</c:if>

<ul>
	<c:forEach var="unit" items="${units}">
		<blockquote><a href="?unitId=${unit.externalId}"><c:out value="${unit.name}"/> (<c:out value="${unit.acronym}"/>)</a></blockquote>
	</c:forEach>
</ul>
