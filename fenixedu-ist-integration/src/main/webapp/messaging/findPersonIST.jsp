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
<%@page import="com.google.common.base.Strings"%>
<%@page import="com.google.common.base.Joiner"%>
<%@page import="org.fenixedu.bennu.core.domain.User"%>
<%@page import="pt.ist.fenixedu.contracts.domain.Employee"%>
<%@page import="org.fenixedu.academic.domain.student.Student"%>
<%@page import="pt.ist.fenixedu.contracts.domain.LegacyRoleUtils"%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/taglib/enum" prefix="e"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/taglib/collection-pager" prefix="cp"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html:xhtml />

<h2>
	<bean:message key="label.person.findPerson" />
</h2>

<style>
.bcintable {
	padding : 0px 0px;
	margin-bottom : 0px;
}

</style>


<fr:edit id="searchForm" name="bean"
	action="/findPersonIST.do?method=findPerson">
	<fr:schema bundle="APPLICATION_RESOURCES"
		type="pt.ist.fenixedu.integration.ui.struts.action.messaging.FindPersonBean">

		<fr:slot name="roleType" layout="menu-postback" key="label.type">
			<fr:property name="destination" value="postback" />
			<fr:destination name="postback" path="/findPersonIST.do?method=postback" />
		</fr:slot>
		<logic:present name="bean" property="roleType">
			<logic:equal name="bean" property="roleType" value="STUDENT">
				<fr:slot name="degreeType" layout="menu-select-postback" key="label.degree.type">
					<fr:property name="destination" value="postback" />
					<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.DegreeTypeProvider" />
					<fr:property name="format" value="\${name.content}"/>
					<fr:destination name="postback"
						path="/findPersonIST.do?method=postback" />
				</fr:slot>
				<logic:present name="bean" property="degreeType">
					<fr:slot name="degree" layout="menu-select-postback" key="label.degree.name">
						<fr:property name="from" value="possibleDegrees" />
						<fr:destination name="postback"
							path="/findPersonIST.do?method=postback" />
						<fr:property name="destination" value="postback" />
						<fr:property name="format" value="\${presentationName}" />
					</fr:slot>
				</logic:present>
			</logic:equal>


			<logic:equal name="bean" property="roleType" value="TEACHER">
				<fr:slot name="department" layout="menu-select-postback" key="label.teacher.finalWork.department">
					<fr:property name="providerClass"
						value="org.fenixedu.academic.ui.renderers.providers.ActiveDepartmentsProvider" />
					<fr:destination name="postback"
						path="/findPersonIST.do?method=postback" />
					<fr:property name="destination" value="postback" />
					<fr:property name="format" value="\${nameI18n.content}" />
				</fr:slot>
			</logic:equal>
		</logic:present>

		<fr:slot name="name" key="label.name" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
			<fr:property name="size" value="50"/>
		</fr:slot>
		<fr:slot name="viewPhoto" key="label.viewPhoto"/>
	</fr:schema>
	<fr:layout name="tabular">
		<fr:property name="classes" value="tstyle5 thlight thright mtop025 thmiddle"/>
	    <fr:property name="columnClasses" value=",,tdclear tderror1"/>
	    <fr:property name="rowClasses" value="width46em"/>
	</fr:layout>
	<fr:destination name="cancel" path="/findPersonIST.do?method=prepareFindPerson"/>
</fr:edit>

<p>
	<em> <!-- Error messages go here --> <html:errors /> </em>
</p>




<logic:present name="personListFinded">

	<logic:notEqual name="totalFindedPersons" value="1">
		<p>
			<b><bean:message key="label.manager.numberFindedPersons"
					arg0="${totalFindedPersons}" />
			</b>
		</p>
	</logic:notEqual>
	<logic:equal name="totalFindedPersons" value="1">
		<p>
			<b><bean:message key="label.manager.findedOnePersons"
					arg0="${totalFindedPersons}" />
			</b>
		</p>
	</logic:equal>
	
	 <bean:define id="url">/messaging/findPersonIST.do?method=findPerson&amp;name=<bean:write
			name="name" />&amp;roleType=<bean:write name="roleType" />&amp;degreeId=<bean:write
			name="degreeId"  />&amp;degreeType=<bean:write name="degreeType" />&amp;departmentId=<bean:write
			name="departmentId" />&amp;viewPhoto=<bean:write name="viewPhoto" />
	</bean:define> 
	
	<p>
		<bean:message key="label.pages" />
		:
		<cp:collectionPages url="<%= url %>" numberOfVisualizedPages="11"
			pageNumberAttributeName="pageNumber"
			numberOfPagesAttributeName="numberOfPages" />
	</p>

	<logic:iterate id="personalInfo" name="personListFinded"
		indexId="personIndex" type="pt.ist.fenixedu.integration.ui.struts.action.messaging.PersonBean">
		<bean:define id="personID" name="personalInfo" property="externalId" />
		<% 
			String username = personalInfo.getUser() !=null ? personalInfo.getUser().getUsername() : null;
			Integer studentNumber = personalInfo.getStudent() != null ? personalInfo.getStudent().getNumber() : null;
			Integer employeeNumber = personalInfo.getEmployee() != null ? personalInfo.getEmployee().getEmployeeNumber() : null;
			String personalIds = Joiner.on(", ").skipNulls().join(username, studentNumber, employeeNumber);
			personalIds = !Strings.isNullOrEmpty(personalIds) ? "(" + personalIds + ")" : "";
		%>
		<div class="pp">
			<table class="ppid" cellpadding="0" cellspacing="0">
				<tr>
					<td width="70%"><strong> <bean:write name="personalInfo" property="name" /> </strong> <%= personalIds %>
					    <em><%=LegacyRoleUtils.mainRolesStr(personalInfo.getUser()) %></em>
					</td>
					<td width="30%" style="text-align: right;"><bean:define
							id="aa" value="<%= "aa" + personIndex %>" /> <bean:define
							id="id" value="<%= "id" + (personIndex.intValue() + 40)  %>" />
						<bean:define
							id="aa" value="<%= "aa" + personIndex %>" /> <bean:define
							id="id" value="<%= "id" + (personIndex.intValue() + 40) %>" /> 
							<span> <button type="button" alt="input.input" type="button" value="+" data-toggle="collapse" data-target="#collapse<c:out value='${personalInfo.username}'/>">+</button>
					</span> <!-- </td>--></td>
				</tr>
			</table>

			<logic:equal name="viewPhoto" value="true">
				<bean:define id="personIDForPhoto" name="personalInfo" property="username" />
				<html:img
					src="<%= request.getContextPath() + "/user/photo/" + personIDForPhoto.toString()%>"
					altKey="personPhoto" bundle="IMAGE_RESOURCES" />
			</logic:equal>

			<table class="ppdetails">
				<tr class="highlight">
					<td class="ppleft" valign="top"><bean:message
							key="label.person.workPhone.short" /></td>
					<td class="ppright" valign="top" style="width: 25em;"><fr:view
							name="personalInfo" property="phones">
							<fr:layout name="contact-list">
								<fr:property name="classes" value="nobullet list6" />
							</fr:layout>
						</fr:view> 
						<fr:view name="personalInfo" property="mobilePhones">
							<fr:layout name="contact-list">
								<fr:property name="classes" value="nobullet list6" />
							</fr:layout>
						</fr:view></td>
					<td class="ppleft2" valign="top" style="text-align: right;"><bean:message
							key="label.person.email" /></td>
					<td class="ppright" valign="top"><fr:view name="personalInfo"
							property="emailAddresses">
							<fr:layout name="contact-list">
								<fr:property name="classes" value="nobullet list6" />
							</fr:layout>
						</fr:view></td>
				</tr>
			</table>

			<div id="collapse<c:out value='${personalInfo.username}'/>" class="collapse">
				<table class="ppdetails">

					<logic:present name="personalInfo" property="employee">
						<logic:present name="personalInfo"
							property="employee.currentWorkingPlace">
							<bean:define id="infoUnit" name="personalInfo"
								property="employee.currentWorkingPlace" />
							<tr>
								<td valign="top" class="ppleft2"><bean:message
										key="label.person.workPlace" />
								</td>
								<td class="ppright"><bean:write name="infoUnit"
										property="presentationNameWithParentsAndBreakLine"
										filter="false" /></td>
							</tr>
						</logic:present>

						<logic:present name="personalInfo"
							property="employee.currentMailingPlace">
							<tr>
								<td class="ppleft2"><bean:message
										key="label.person.mailingPlace" />
								</td>
								<bean:define id="costCenterNumber" name="personalInfo"
									property="employee.currentMailingPlace.costCenterCode" />
								<bean:define id="unitName" name="personalInfo"
									property="employee.currentMailingPlace.name" />
								<td class="ppright"><bean:write name="costCenterNumber" />
									- <bean:write name="unitName" />
								</td>
							</tr>
						</logic:present>
					</logic:present>
 					<bean:define id="personSpaces" name="personalInfo"
 						property="activePersonSpaces"></bean:define>
 					<logic:notEmpty name="personSpaces">
 						<tr>
 							<td class="ppleft2"><bean:message key="label.person.rooms" />:</td>
 							<td>
 								<logic:iterate id="space" name="personSpaces">
	 								<p><ol class="breadcrumb bcintable">
	 									<bean:define id="thePath" name="space" property="path"/>
		 								<logic:iterate id="spaceWay" name="thePath">
		 									<li><a href="<%= request.getContextPath() %>/spaces-view/view/${spaceWay.externalId}"><c:out value="${spaceWay.name}"/></a></li>
		 								</logic:iterate>
	 								</ol></p>
 								</logic:iterate>
 							</td>
 						</tr>
 					</logic:notEmpty>

					<logic:notEmpty name="personalInfo" property="teacher">
						<logic:notEmpty name="personalInfo"
							property="teacher.category">
							<tr>
								<td class="ppleft2"><bean:message
										key="label.teacher.category" />:</td>
								<td class="ppright"><bean:write name="personalInfo"
										property="teacher.category.name.content" />
								</td>
							</tr>
						</logic:notEmpty>
					</logic:notEmpty>
					<fr:view name="personalInfo" property="webAddresses">
						<fr:layout name="contact-table">
							<fr:property name="types" value="WORK" />
							<fr:property name="bundle" value="APPLICATION_RESOURCES" />
							<fr:property name="label" value="label.person.webSite" />
							<fr:property name="defaultLabel"
								value="label.partyContacts.defaultContact" />
							<fr:property name="leftColumnClasses" value="ppleft2" />
							<fr:property name="rightColumnClasses" value="ppright" />
						</fr:layout>
					</fr:view>

					<logic:present name="personalInfo" property="homepage">
						<logic:equal name="personalInfo" property="homepage.site.published" value="true">
						<tr>
							<td class="ppleft2"><bean:message key="label.homepage" />
							</td>
							<td class="ppright"><html:link href="${personalInfo.homepage.site.fullUrl}"
									target="_blank">
									${personalInfo.homepage.site.fullUrl}
								</html:link></td>
						</tr>
						</logic:equal>
					</logic:present>

					<logic:present name="personalInfo" property="student">
						<logic:notEmpty name="personalInfo"
							property="student.registrations">

							<logic:iterate id="registration" name="personalInfo"
								property="student.registrations">
								<tr>
									<td class="ppleft2" style="vertical-align: top;"><bean:message
											key="label.degree.name" />:</td>
									<td class="ppright"><bean:write name="registration"
											property="degreeName" />
									</td>
								</tr>
							</logic:iterate>
						</logic:notEmpty>
					</logic:present>
				</table>
			</div>
		</div>
	</logic:iterate>

	<logic:notEqual name="numberOfPages" value="1">
		<p class="mtop15">
			<bean:message key="label.pages" />
			:
			<cp:collectionPages url="<%= url %>" numberOfVisualizedPages="11"
				pageNumberAttributeName="pageNumber"
				numberOfPagesAttributeName="numberOfPages" />
		</p>
	</logic:notEqual>

</logic:present>