<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST Teacher Evaluation.

    FenixEdu IST Teacher Evaluation is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST Teacher Evaluation is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST Teacher Evaluation.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page isELIgnored="true"%>
<%@ page language="java" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<html:xhtml/>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr" %>

<h2><bean:message key="link.define.periods" bundle="TEACHER_CREDITS_SHEET_RESOURCES"/></h2>

<p><span class="error"><!-- Error messages go here --><html:errors /></span></p>
<html:messages id="message" message="true">
	<p>
		<span class="error"><!-- Error messages go here -->
			<bean:write name="message"/>
		</span>
	</p>
</html:messages>

<logic:notEmpty name="teacherCreditsBean">
	<bean:define id="closed" name="teacherCreditsBean" property="annualCreditsState.isCreditsClosed"/>

	<fr:form action="/defineCreditsPeriods.do">
		<fr:edit name="teacherCreditsBean" id="teacherCreditsBeanID">
			<fr:schema type="pt.ist.fenixedu.teacher.evaluation.dto.teacherCredits.TeacherCreditsPeriodBean" bundle="TEACHER_CREDITS_SHEET_RESOURCES">
				<fr:slot name="executionPeriod" key="label.execution-period" layout="menu-select-postback">
				  <fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.NotClosedExecutionPeriodsProvider" />
				  <fr:property name="format" value="${year} - ${semester}º Sem." />
				  <fr:property name="destination" value="postBack" />
				</fr:slot>
			</fr:schema>
			<fr:destination name="postBack" path="/defineCreditsPeriods.do?method=showPeriods"/>
			<fr:layout name="tabular">
				<fr:property name="classes" value="tstyle5 thlight thright thmiddle mtop05"/>
				<fr:property name="columnClasses" value=",,tdclear"/>
			</fr:layout>
		</fr:edit>
	</fr:form>
	
	
	<logic:equal name="teacherCreditsBean" property="canCreateTeacherAuthorizations" value="true">
			<fr:form id="teacherCreditsBeanForm" action="/defineCreditsPeriods.do?method=createTeacherAuthorizations">
				<fr:edit name="teacherCreditsBean" visible="false"/>
				<bean:define id="confirmationMessage"><bean:message key="message.confirmation.createTeacherAuthorizations"  bundle="TEACHER_CREDITS_SHEET_RESOURCES"/></bean:define>
				<html:submit bundle="HTMLALT_RESOURCES" altKey="submit.submit" styleClass="inputbutton" onclick="<%="return confirm('"+confirmationMessage+"')" %>">
					<bean:message key="link.teacherAuthorizations.create"  bundle="TEACHER_CREDITS_SHEET_RESOURCES"/>
				</html:submit>
			</fr:form>
	</logic:equal>
	
	
	<h3 class="mtop15 mbottom05"><bean:message key="label.teacher"/></h3>
	<fr:view name="teacherCreditsBean" layout="tabular">
		<fr:schema type="pt.ist.fenixedu.teacher.evaluation.dto.teacherCredits.TeacherCreditsPeriodBean" bundle="TEACHER_CREDITS_SHEET_RESOURCES">
			<fr:slot name="beginForTeacher" key="label.beginDate"/>
			<fr:slot name="endForTeacher" key="label.endDate"/>
		</fr:schema>
		<fr:layout>
			<fr:property name="classes" value="tstyle2 thleft thlight mtop05"/>
		</fr:layout>
	</fr:view>
	<logic:equal name="closed" value="false">
		<html:link page="/defineCreditsPeriods.do?method=prepareEditTeacherCreditsPeriod" paramName="teacherCreditsBean" paramProperty="executionPeriod.externalId" paramId="executionPeriodId">
			<bean:message key="link.change" bundle="TEACHER_CREDITS_SHEET_RESOURCES"/>
		</html:link>
	</logic:equal>

	<h3 class="mtop15 mbottom05"><bean:message key="label.departmentAdmOffice" bundle="TEACHER_CREDITS_SHEET_RESOURCES"/></h3>
	<fr:view name="teacherCreditsBean" layout="tabular">
		<fr:schema type="pt.ist.fenixedu.teacher.evaluation.dto.teacherCredits.TeacherCreditsPeriodBean" bundle="TEACHER_CREDITS_SHEET_RESOURCES">
			<fr:slot name="beginForDepartmentAdmOffice" key="label.beginDate"/>
			<fr:slot name="endForDepartmentAdmOffice" key="label.endDate"/>
		</fr:schema>
		<fr:layout>
			<fr:property name="classes" value="tstyle2 thleft thlight mtop05"/>
		</fr:layout>
	</fr:view>
	<logic:equal name="closed" value="false">
		<html:link page="/defineCreditsPeriods.do?method=prepareEditDepartmentAdmOfficeCreditsPeriod" paramName="teacherCreditsBean" paramProperty="executionPeriod.externalId" paramId="executionPeriodId">
			<bean:message key="link.change" bundle="TEACHER_CREDITS_SHEET_RESOURCES"/>
		</html:link>
	</logic:equal>

	<logic:present name="editInterval">
		<bean:define id="editInterval" name="editInterval" />
	</logic:present>
	<logic:notPresent name="editInterval">
		<bean:define id="editInterval" value="" />
	</logic:notPresent>


	<h3 class="mtop15 mbottom05">Créditos unitarios</h3>
	<fr:form id="teacherCreditsBeanForm" action="/defineCreditsPeriods.do?method=editAnnualCreditsDates">
		<h4 class="mtop15 mbottom05">Aprovação de AD65</h4>
		<fr:edit name="teacherCreditsBean" id="reductionServiceApproval">
			<fr:schema bundle="TEACHER_CREDITS_SHEET_RESOURCES" type="pt.ist.fenixedu.teacher.evaluation.dto.teacherCredits.TeacherCreditsPeriodBean">
				<fr:slot name="reductionServiceApprovalBeginDate" key="label.beginDate" layout="null-as-label" readOnly="<%=Boolean.valueOf(closed.toString())%>" validator="pt.ist.fenixWebFramework.rendererExtensions.validators.DateTimeValidator"/>
				<fr:slot name="reductionServiceApprovalEndDate" key="label.endDate" layout="null-as-label"  readOnly="<%=Boolean.valueOf(closed.toString()) %>" validator="pt.ist.fenixWebFramework.rendererExtensions.validators.DateTimeValidator"/>
			</fr:schema>
			<fr:layout name="tabular">
				<fr:property name="classes" value="tstyle2 thleft thlight mtop05"/>
				<fr:property name="columnClasses" value=",,tderror1 tdclear"/>
			</fr:layout>
			<fr:destination name="invalid" path="/defineCreditsPeriods.do?method=showPeriods"/>
			<fr:destination name="cancel" path="/defineCreditsPeriods.do?method=showPeriods"/>
		</fr:edit>
		<h4 class="mtop15 mbottom05">Período para definir créditos unitários (ck) para as UC's partilhadas</h4>
		<fr:edit name="teacherCreditsBean" id="sharedUnitCredits">
			<fr:schema bundle="TEACHER_CREDITS_SHEET_RESOURCES" type="pt.ist.fenixedu.teacher.evaluation.dto.teacherCredits.TeacherCreditsPeriodBean">
				<fr:slot name="sharedUnitCreditsBeginDate" key="label.beginDate" layout="null-as-label" readOnly="<%=Boolean.valueOf(closed.toString())%>" validator="pt.ist.fenixWebFramework.rendererExtensions.validators.DateTimeValidator"/>
				<fr:slot name="sharedUnitCreditsEndDate" key="label.endDate" layout="null-as-label"  readOnly="<%=Boolean.valueOf(closed.toString()) %>" validator="pt.ist.fenixWebFramework.rendererExtensions.validators.DateTimeValidator"/>
			</fr:schema>
			<fr:layout name="tabular">
				<fr:property name="classes" value="tstyle2 thleft thlight mtop05"/>
				<fr:property name="columnClasses" value=",,tderror1 tdclear"/>
			</fr:layout>
			<fr:destination name="invalid" path="/defineCreditsPeriods.do?method=showPeriods"/>
			<fr:destination name="cancel" path="/defineCreditsPeriods.do?method=showPeriods"/>
		</fr:edit>
		<h4 class="mtop15 mbottom05">Período para definir créditos unitários (ck) para as restantes UC's</h4>
		<fr:edit name="teacherCreditsBean" id="unitCredits">
			<fr:schema bundle="TEACHER_CREDITS_SHEET_RESOURCES" type="pt.ist.fenixedu.teacher.evaluation.dto.teacherCredits.TeacherCreditsPeriodBean">
				<fr:slot name="unitCreditsBeginDate" key="label.beginDate" layout="null-as-label" readOnly="<%=Boolean.valueOf(closed.toString()) %>" validator="pt.ist.fenixWebFramework.rendererExtensions.validators.DateTimeValidator"/>
				<fr:slot name="unitCreditsEndDate" key="label.endDate" layout="null-as-label" readOnly="<%=Boolean.valueOf(closed.toString()) %>" validator="pt.ist.fenixWebFramework.rendererExtensions.validators.DateTimeValidator"/>
			</fr:schema>
			<fr:layout name="tabular">
				<fr:property name="classes" value="tstyle2 thleft thlight mtop05"/>
				<fr:property name="columnClasses" value=",,tderror1 tdclear"/>
			</fr:layout>
			<fr:destination name="invalid" path="/defineCreditsPeriods.do?method=showPeriods"/>
			<fr:destination name="cancel" path="/defineCreditsPeriods.do?method=showPeriods"/>
		</fr:edit>
		<h4 class="mtop15 mbottom05">Outras datas</h4>
		<fr:edit name="teacherCreditsBean" id="annualCreditsState" >
			<fr:schema bundle="TEACHER_CREDITS_SHEET_RESOURCES" type="pt.ist.fenixedu.teacher.evaluation.domain.credits.AnnualCreditsState">
				<fr:slot name="annualCreditsState.orientationsCalculationDate" layout="null-as-label" readOnly="true" key="label.orientationsCalculationDate"/>
				<fr:slot name="finalCalculationDate" layout="null-as-label" readOnly="<%= Boolean.valueOf(closed.toString())%>" validator="pt.ist.fenixWebFramework.rendererExtensions.validators.LocalDateValidator"/>
				<fr:slot name="closeCreditsDate" layout="null-as-label" readOnly="<%= Boolean.valueOf(closed.toString())%>" validator="pt.ist.fenixWebFramework.rendererExtensions.validators.LocalDateValidator"/>
				<logic:present role="role(MANAGER)">
					<fr:slot name="annualCreditsState.isFinalCreditsCalculated" layout="null-as-label" readOnly="true"/>
					<fr:slot name="annualCreditsState.isCreditsClosed" layout="null-as-label" readOnly="true"/>
				</logic:present>
			</fr:schema>
			<fr:layout name="tabular">
				<fr:property name="classes" value="tstyle2 thleft thlight mtop05"/>
			</fr:layout>
		</fr:edit>

	<logic:equal name="closed" value="false">
		<html:submit bundle="HTMLALT_RESOURCES" altKey="submit.submit" styleClass="inputbutton">
		<bean:message key="link.change" bundle="TEACHER_CREDITS_SHEET_RESOURCES"/>
		</html:submit>
	</logic:equal>
	<logic:equal name="teacherCreditsBean" property="canOpenCredits" value="true">
		<bean:define id="confirmationMessage"><bean:message key="message.confirmation.openAnnualTeachingCredits"  bundle="TEACHER_CREDITS_SHEET_RESOURCES"/></bean:define>
		<html:submit bundle="HTMLALT_RESOURCES" altKey="submit.submit" styleClass="inputbutton" onclick="<%="return confirm('"+confirmationMessage+"')" %>">
		<bean:message key="link.open" bundle="TEACHER_CREDITS_SHEET_RESOURCES"/>
		</html:submit>
	</logic:equal>
	</fr:form>
</logic:notEmpty>
