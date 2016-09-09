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
<%@ page language="java" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/taglib/collection-pager" prefix="cp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://fenixedu.com/cms/permissions" prefix="permissions" %>

<h2><bean:message key="label.uploadersManagement" bundle="RESEARCHER_RESOURCES"/></h2>
<bean:define id="actionName" name="functionalityAction"/>

<bean:define id="unitID" name="unit" property="externalId"/>

<c:if test="${permissions:canDoThis(unit.site, 'MANAGE_ROLES')}">
	<fr:edit name="unit" schema="edit-uploaders">
		<fr:layout>
			<fr:property name="classes" value="tstyle5 thlight thmiddle"/>
			<fr:property name="columnClasses" value=",,tderror1 tdclear"/>
		</fr:layout>
	<fr:destination name="success" path="<%= "/" + actionName + ".do?method=configureGroups&unitId=" + unitID %>"/>
	<fr:destination name="cancel" path="<%= "/" + actionName + ".do?method=configureGroups&unitId=" + unitID %>"/>
	</fr:edit>
</c:if>

