<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST Tutorship.

    FenixEdu IST Tutorship is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST Tutorship is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST Tutorship.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page isELIgnored="true"%>
<%@ page language="java" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr" %>


<h2><bean:message key="title.tutorship.view" bundle="PEDAGOGICAL_COUNCIL" /></h2>

<fr:edit id="tutorsBean" name="tutorsBean" action="/viewTutors.do?method=listTutors">
	<fr:schema type="pt.ist.fenixedu.tutorship.ui.Action.pedagogicalCouncil.ViewTutorsDA$ViewTutorsBean" bundle="PEDAGOGICAL_COUNCIL">
		<fr:slot name="executionSemester" layout="menu-select-postback" key="label.tutorship.year">
			<fr:property name="providerClass"
				value="pt.ist.fenixedu.tutorship.ui.Action.pedagogicalCouncil.ViewTutorsDA$ExecutionSemestersProvider" />
			<fr:property name="bundle" value="PEDAGOGICAL_COUNCIL" />
			<fr:property name="format" value="${executionYear.year}" />
			<fr:property name="destination" value="semesterPostBack" />
		</fr:slot>
		<fr:slot name="executionDegree" layout="menu-select" key="label.degree">
			<fr:property name="providerClass"
				value="pt.ist.fenixedu.tutorship.ui.Action.pedagogicalCouncil.ViewTutorsDA$ContextDegreesProvider"/>
			<fr:property name="bundle" value="PEDAGOGICAL_COUNCIL" />
			<fr:property name="format" value="${presentationName}" />
			<fr:property name="sortBy" value="presentationName" />
		</fr:slot>
	</fr:schema>
	<fr:layout>
		<fr:property name="classes" value="tstyle5 thlight thleft mtop0" />
		<fr:property name="columnClasses" value=",,tdclear tderror1" />
	</fr:layout>
	<fr:destination name="semesterPostBack" path="/viewTutors.do?method=listTutors"/>
</fr:edit>

<logic:notEmpty name="tutorsBean" property="tutors">
	<bean:define id="query" value=""/>	
	<logic:notEmpty name="tutorsBean" property="executionDegree">
		<bean:define id="executionDegreeId" name="tutorsBean" property="executionDegree.externalId"/>
		<bean:define id="query" value="<%="&executionDegree="+ executionDegreeId%>"/>
	</logic:notEmpty>	
	<html:img border="0" src="<%= request.getContextPath() + "/images/excel.gif"%>" altKey="excel" bundle="IMAGE_RESOURCES" />
	<html:link page="<%= "/viewTutors.do?method=exportToExcel" + query.toString()%>">
		<bean:message key="link.exportToExcel" bundle="APPLICATION_RESOURCES"/>
	</html:link>
</logic:notEmpty>

<logic:notEmpty name="tutorsBean" property="tutors">		
	<fr:view name="tutorsBean" property="tutors">
		<fr:schema type="pt.ist.fenixedu.tutorship.domain.TutorshipIntention" bundle="APPLICATION_RESOURCES">
			<fr:slot name="teacher.person.username" key="label.username" />
			<fr:slot name="teacher.person.name" key="label.name"/>
		</fr:schema>
		<fr:layout name="tabular">
			<fr:property name="classes" value="tstyle1" />
			<fr:property name="link(view)" value="/viewTutors.do?method=viewStudentsOfTutorship"/>
			<fr:property name="param(view)" value="externalId/tutorshipIntentionID"/>
			<fr:property name="key(view)" value="label.tutorship.students.view"/>
			<fr:property name="bundle(view)" value="PEDAGOGICAL_COUNCIL"/>
			<fr:property name="visibleIfNot(view)" value="deletable"/>
		</fr:layout>
	</fr:view>
</logic:notEmpty>	

<logic:empty name="tutorsBean" property="tutors">
	<logic:notEmpty name="tutorsBean" property="executionSemester">
		<logic:empty name="tutorsBean" property="executionDegree">
			<bean:define id="executionSemesterId" name="tutorsBean" property="executionSemester.externalId"/>
			<bean:define id="query" value="<%="&executionSemester="+ executionSemesterId%>"/>
			<html:img border="0" src="<%= request.getContextPath() + "/images/excel.gif"%>" altKey="excel" bundle="IMAGE_RESOURCES" />
			<html:link page="<%= "/viewTutors.do?method=exportToExcelAllDegrees" + query.toString()%>">
				Exportar para ficheiro Excel
			</html:link>
		</logic:empty>
		<logic:notEmpty name="tutorsBean" property="executionDegree">
			<p><bean:message key="message.tutorship.dontExist.tutors" bundle="PEDAGOGICAL_COUNCIL" /></p>
		</logic:notEmpty>
	</logic:notEmpty>
	<logic:empty name="tutorsBean" property="executionSemester">
		<p><bean:message key="message.tutorship.dontExist.tutors" bundle="PEDAGOGICAL_COUNCIL" /></p>
	</logic:empty>
</logic:empty>