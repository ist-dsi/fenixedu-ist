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
<%@ page language="java" %>

<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr" %>

<html:xhtml/>

<em><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.title"/></em>

<h2><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.autoevaluation.title" /></h2>


<fr:view name="informationBean">
	<fr:schema bundle="TEACHER_EVALUATION_RESOURCES"
		type="pt.ist.fenixedu.teacher.evaluation.domain.TeacherEvaluationInformationBean">
		<fr:slot name="teacherEvaluationProcess.facultyEvaluationProcess.title" key="label.teacher.evaluation.evaluation" layout="null-as-label" />
		<fr:slot name="teacherEvaluationProcess.evaluee.presentationName" key="label.teacher.evaluation.evaluee" layout="null-as-label" />
		<fr:slot name="teacherAuthorization.teacherCategory.name" key="label.category" layout="null-as-label" bundle="TEACHER_CREDITS_SHEET_RESOURCES"/>
		<fr:slot name="teacherAuthorization.department.name" key="label.department" layout="null-as-label" bundle="TEACHER_CREDITS_SHEET_RESOURCES"/>
		<fr:slot name="teacherEvaluationProcess.facultyEvaluationProcess" key="label.teacher.evaluation.facultyEvaluationProcess.evaluationInterval">
			<fr:property name="format" value="\${teacherEvaluationProcess.facultyEvaluationProcess.beginEvaluationYear} - \${teacherEvaluationProcess.facultyEvaluationProcess.endEvaluationYear}"/>
			<fr:property name="useParent" value="true" />
		</fr:slot>
	</fr:schema>
	<fr:layout name="tabular">
		<fr:property name="classes" value="tstyle2 thlight thleft mtop05 mbottom05"/>
	</fr:layout>
</fr:view>

<h2 class="separator2 mtop15"><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.information.orientations"/></h2>
<logic:empty name="informationBean" property="orientations">
	<bean:message bundle="TEACHER_CREDITS_SHEET_RESOURCES" key="label.teacherCreditsSheet.noDataFound"/>
</logic:empty>

<logic:notEmpty name="informationBean" property="orientations">
	<table class="tstyle2 thlight thleft mtop05 mbottom05 table">
		<thead><tr>
			<th><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.professorhip.participationType"/></th><th style="width:1px;"/>
			<th><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.orientation.responsabilityType"/></th><th style="width:1px;"/>
			<th><bean:message bundle="TEACHER_CREDITS_SHEET_RESOURCES" key="label.teacher-assistant-guiding-number"/></th>
			<th><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.orientation.ects"/></th>
			<th><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.description"/></th>
		</tr></thead>
	<tbody><logic:iterate id="orientation" name="informationBean" property="orientations">
		<tr>
			<td><fr:view name="orientation" property="participationType">
				<fr:layout name="slot-as-label">
					<fr:property name="bundle" value="TEACHER_EVALUATION_RESOURCES"></fr:property>
				</fr:layout>
			</fr:view></td><td/> 
			<td><fr:view name="orientation" property="responsabilityType">
				<fr:layout name="slot-as-label">
					<fr:property name="bundle" value="TEACHER_EVALUATION_RESOURCES"></fr:property>
				</fr:layout>
			</fr:view></td><td/> 
			<td><bean:write name="orientation" property="coorientationNumber"/></td>
			<td><bean:write name="orientation" property="credits"/></td>
			<td><bean:write name="orientation" property="description"/></td>
		</tr>
	</logic:iterate>
	</tbody></table>
</logic:notEmpty> 

<h2 class="separator2 mtop15"><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.information.professorships"/></h2>
<logic:empty name="informationBean" property="professorships">
	<bean:message bundle="TEACHER_CREDITS_SHEET_RESOURCES" key="label.teacherCreditsSheet.noDataFound"/>
</logic:empty>
<logic:notEmpty name="informationBean" property="professorships">
	<table class="tstyle2 thlight thleft mtop05 mbottom05 table">
		<thead><tr>
			<th><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.professorhip.participationType"/></th>
			<th><bean:message bundle="TEACHER_CREDITS_SHEET_RESOURCES" key="label.credits.effectiveTeachingLoad"/></th>
			<th><bean:message bundle="TEACHER_CREDITS_SHEET_RESOURCES" key="label.teacherCreditsSheet.studentsNumber"/></th><th style="width:1px;"/><th style="width:1px;"/>
			<th><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.professorhip.quc"/></th>
			<th><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.description"/></th>
		</tr></thead>
		<tbody><logic:iterate id="professorship" name="informationBean" property="professorships">
		<tr>
			<td><fr:view name="professorship" property="professorship.responsibleFor">
				<fr:layout>
					<fr:property name="trueLabel" value="label.teacher.evaluation.professorhip.participationType.teachingAndResponsability" />
					<fr:property name="falseLabel" value="label.teacher.evaluation.professorhip.participationType.teaching" />
					<fr:property name="bundle" value="TEACHER_EVALUATION_RESOURCES" />
				</fr:layout>
			</fr:view></td>
			<td><bean:write name="professorship" property="hours"/></td>
			<td><bean:write name="professorship" property="enrolmentsNumber"/></td><td/><td/> 
			<td><bean:write name="professorship" property="professorshipEvaluationValue"/></td>
			<td><bean:write name="professorship" property="description"/></td>
		</tr>
	</logic:iterate>
	</tbody></table>
</logic:notEmpty>

<h2 class="separator2 mtop15"><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.information.scientificPublications"/></h2>
<logic:empty name="informationBean" property="publications">
	<bean:message bundle="TEACHER_CREDITS_SHEET_RESOURCES" key="label.teacherCreditsSheet.noDataFound"/>
</logic:empty>
<logic:notEmpty name="informationBean" property="publications">
	<table class="tstyle2 thlight thleft mtop05 mbottom05 table">
		<thead><tr>
			<th><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.publications.author.number"/></th>
			<th style="width:1px;"/>
			<th><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.description"/></th>
		</tr></thead>
		<tbody><logic:iterate id="publication" name="informationBean" property="publications">
		<tr>
			<td><bean:write name="publication" property="authorsNumber"/></td>
			<td/>
			<td><bean:write name="publication" property="publicationString"/></td>
		</tr>
	</logic:iterate>
	</tbody></table>
</logic:notEmpty>

<h2 class="separator2 mtop15"><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.information.scientificProjects"/></h2>
<bean:define id="projects" name="informationBean" property="scientificProjects" type="com.google.gson.JsonArray"/>
<% if(projects.size()==0) { %>
	<bean:message bundle="TEACHER_CREDITS_SHEET_RESOURCES" key="label.teacherCreditsSheet.noDataFound"/>
<% } else { %>
	<table class="tstyle2 thlight thleft mtop05 mbottom05 table">
	  <tr>
		<th><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.information.project.budget"/></th>
		<th/>
		<th><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.description"/></th>
	  </tr>
	  <% for(com.google.gson.JsonElement projectElement : projects ) { 
	      com.google.gson.JsonObject project = (com.google.gson.JsonObject) projectElement;%>
		  <tr>
			<td><%= project.get("budget").getAsString() %></td>
			<td/>
		    <td><%= project.get("institution").getAsString()+" - "+project.get("number").getAsString() +" - " +project.get("name").getAsString() %></td>
		  </tr>
	  <% } %>
	</table>
<% } %>
<h2 class="separator2 mtop15"><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.information.managementFunctions"/></h2>
<logic:empty name="informationBean" property="functions">
	<bean:message bundle="TEACHER_CREDITS_SHEET_RESOURCES" key="label.teacherCreditsSheet.noDataFound"/>
</logic:empty>
<logic:notEmpty name="informationBean" property="functions">
	<table class="tstyle2 thlight thleft mtop05 mbottom05 table">
		<thead><tr>
			<th><bean:message bundle="TEACHER_CREDITS_SHEET_RESOURCES" key="label.hours"/></th><th style="width:1px;"/>
			<th><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.description"/></th>
		</tr></thead>
		<tbody><logic:iterate id="function" name="informationBean" property="functions">
		<tr>
			<td><bean:write name="function" property="credits"/></td><td/>
			<td><bean:write name="function" property="description"/></td>
		</tr>
	</logic:iterate>
	</tbody></table>
</logic:notEmpty>








