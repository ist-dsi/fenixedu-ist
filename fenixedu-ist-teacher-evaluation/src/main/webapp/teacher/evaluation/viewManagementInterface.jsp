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
<%@page import="org.fenixedu.academic.predicate.AccessControl"%>
<%@ page import="org.fenixedu.bennu.core.groups.Group" %>
<%@page import="org.fenixedu.bennu.core.security.Authenticate"%>
<%@ page language="java" %>

<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr" %>

<html:xhtml/>

<em><bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.title"/></em>

<h2>
	<bean:message bundle="TEACHER_EVALUATION_RESOURCES" key="label.teacher.evaluation.management.title"/>
</h2>

<span class="warning0"><!-- Error messages go here --><html:errors /></span>

<html:messages id="message" message="true" bundle="APPLICATION_RESOURCES">
	<p><span class="warning0"><bean:write name="message" /></span></p>
</html:messages>


<logic:present name="facultyEvaluationProcessCreationBean">
	<fr:form action="/teacherEvaluation.do?method=createFacultyEvaluationProcess">
		<fr:edit id="facultyEvaluationProcessCreationBean" name="facultyEvaluationProcessCreationBean">
			<fr:schema bundle="TEACHER_EVALUATION_RESOURCES" type="pt.ist.fenixedu.teacher.evaluation.domain.FacultyEvaluationProcessBean">
				<fr:slot name="title" key="label.teacher.evaluation.facultyEvaluationProcess.title" validator="pt.ist.fenixWebFramework.rendererExtensions.validators.RequiredLocalizedStringValidator">
					<fr:property name="size" value="40"/>
				</fr:slot>
				<fr:slot name="autoEvaluationIntervalStart" key="label.teacher.evaluation.facultyEvaluationProcess.autoEvaluationIntervalStart" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator"/>
				<fr:slot name="autoEvaluationIntervalEnd" key="label.teacher.evaluation.facultyEvaluationProcess.autoEvaluationIntervalEnd" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator"/>
				<fr:slot name="evaluationIntervalStart" key="label.teacher.evaluation.facultyEvaluationProcess.evaluationIntervalStart" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator"/>
				<fr:slot name="evaluationIntervalEnd" key="label.teacher.evaluation.facultyEvaluationProcess.evaluationIntervalEnd" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator"/>
				<fr:slot name="allowNoEval" key="label.teacher.evaluation.facultyEvaluationProcess.allowNoEval" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator"/>
				<fr:slot name="suffix" key="label.teacher.evaluation.facultyEvaluationProcess.suffix" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator"/>
				<fr:slot name="beginEvaluationYear" key="label.teacher.evaluation.facultyEvaluationProcess.beginEvaluationYear" validator="pt.ist.fenixWebFramework.renderers.validators.NumberValidator" required="true"/>
               	<fr:slot name="endEvaluationYear" key="label.teacher.evaluation.facultyEvaluationProcess.endEvaluationYear" validator="pt.ist.fenixWebFramework.renderers.validators.NumberValidator" required="true"/>
			</fr:schema>
			<fr:layout name="tabular">
				<fr:property name="classes" value="tstyle5 thlight thright"/>
				<fr:property name="columnClasses" value=",,tderror1 tdclear"/>
			</fr:layout>
			<fr:destination name="invalid" path="/teacherEvaluation.do?method=prepareCreateFacultyEvaluationProcess"/>
			<fr:destination name="cancel" path="/teacherEvaluation.do?method=viewManagementInterface"/>
		</fr:edit>
	
		<p>
			<html:submit bundle="HTMLALT_RESOURCES" altKey="submit.submit" styleClass="inputbutton"><bean:message bundle="MESSAGING_RESOURCES" key="messaging.save.button"/></html:submit>
			<html:cancel bundle="HTMLALT_RESOURCES" altKey="cancel.cancel" styleClass="inputbutton"><bean:message bundle="MESSAGING_RESOURCES" key="messaging.cancel.button"/></html:cancel>
		</p>
	</fr:form>
</logic:present>

<logic:present name="facultyEvaluationProcessEditnBean">
	<bean:define id="urlEditInvalid">/teacherEvaluation.do?method=prepareEditFacultyEvaluationProcess&amp;facultyEvaluationProcessOID=<bean:write name="facultyEvaluationProcessEditnBean" property="facultyEvaluationProcess.externalId"/></bean:define>
	<fr:form action="/teacherEvaluation.do?method=editFacultyEvaluationProcess">
		<fr:edit id="facultyEvaluationProcessEditnBean" name="facultyEvaluationProcessEditnBean">
			<fr:schema bundle="TEACHER_EVALUATION_RESOURCES" type="pt.ist.fenixedu.teacher.evaluation.domain.FacultyEvaluationProcessBean">
				<fr:slot name="title" key="label.teacher.evaluation.facultyEvaluationProcess.title" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
					<fr:property name="size" value="40"/>
				</fr:slot>
				<fr:slot name="autoEvaluationIntervalStart" key="label.teacher.evaluation.facultyEvaluationProcess.autoEvaluationIntervalStart" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator"/>
				<fr:slot name="autoEvaluationIntervalEnd" key="label.teacher.evaluation.facultyEvaluationProcess.autoEvaluationIntervalEnd" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator"/>
				<fr:slot name="evaluationIntervalStart" key="label.teacher.evaluation.facultyEvaluationProcess.evaluationIntervalStart" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator"/>
				<fr:slot name="evaluationIntervalEnd" key="label.teacher.evaluation.facultyEvaluationProcess.evaluationIntervalEnd" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator"/>
				<fr:slot name="allowNoEval" key="label.teacher.evaluation.facultyEvaluationProcess.allowNoEval" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator"/>
				<fr:slot name="suffix" key="label.teacher.evaluation.facultyEvaluationProcess.suffix" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator"/>
				<fr:slot name="beginEvaluationYear" key="label.teacher.evaluation.facultyEvaluationProcess.beginEvaluationYear" validator="pt.ist.fenixWebFramework.renderers.validators.NumberValidator" required="true"/>
               	<fr:slot name="endEvaluationYear" key="label.teacher.evaluation.facultyEvaluationProcess.endEvaluationYear" validator="pt.ist.fenixWebFramework.renderers.validators.NumberValidator" required="true"/>
			</fr:schema>
			<fr:layout name="tabular">
				<fr:property name="classes" value="tstyle5 thlight thright"/>
				<fr:property name="columnClasses" value=",,tderror1 tdclear"/>
			</fr:layout>
			<fr:destination name="invalid" path="<%= urlEditInvalid %>"/>
		</fr:edit>
	
		<p>
			<html:submit bundle="HTMLALT_RESOURCES" altKey="submit.submit" styleClass="inputbutton"><bean:message bundle="MESSAGING_RESOURCES" key="messaging.save.button"/></html:submit>
			<html:cancel bundle="HTMLALT_RESOURCES" altKey="cancel.cancel" styleClass="inputbutton"><bean:message bundle="MESSAGING_RESOURCES" key="messaging.cancel.button"/></html:cancel>
		</p>
	</fr:form>
</logic:present>

<logic:present name="facultyEvaluationProcess">
	<h3 class="mtop15 mbottom05">
		<fr:view name="facultyEvaluationProcess" property="title"/>
	</h3>
	<table class="tstyle2 thlight thleft mtop05">
		<tr>
			<th>
				<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.autoEvaluationInterval" bundle="TEACHER_EVALUATION_RESOURCES"/>
			</th>
			<td>
				<fr:view name="facultyEvaluationProcess" property="autoEvaluationInterval.start"/>
				-
				<fr:view name="facultyEvaluationProcess" property="autoEvaluationInterval.end"/>
			</td>
		</tr>
		<tr>
			<th>
				<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.evaluationInterval" bundle="TEACHER_EVALUATION_RESOURCES"/>
			</th>
			<td>
				<fr:view name="facultyEvaluationProcess" property="evaluationInterval.start"/>
				-
				<fr:view name="facultyEvaluationProcess" property="evaluationInterval.end"/>
			</td>
		</tr>
	</table>
	
	
	<ul>
		<% if (AccessControl.getPerson().isTeacherEvaluationCoordinatorCouncilMember() || Group.managers().isMember(Authenticate.getUser())) { %>
		<li>
			<html:link page="/teacherEvaluation.do?method=prepareEditFacultyEvaluationProcess" paramId="facultyEvaluationProcessOID" paramName="facultyEvaluationProcess" paramProperty="OID">
				<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.edit" bundle="TEACHER_EVALUATION_RESOURCES"/>
			</html:link>
		</li>
		<li>
			<html:link page="/teacherEvaluation.do?method=prepareUploadEvaluators" paramId="facultyEvaluationProcessOID" paramName="facultyEvaluationProcess" paramProperty="OID">
				<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.upload" bundle="TEACHER_EVALUATION_RESOURCES"/>
			</html:link>
		</li>
		<li>
			<html:link page="/teacherEvaluation.do?method=prepareUploadApprovedMarks" paramId="facultyEvaluationProcessOID" paramName="facultyEvaluationProcess" paramProperty="OID">
				<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.load.approved.marks" bundle="TEACHER_EVALUATION_RESOURCES"/>
			</html:link>
		</li>
		<% } %>
		<logic:present role="(role(MANAGER) | role(OPERATOR))">
		<li>
			<html:link page="/teacherEvaluation.do?method=deleteFacultyEvaluationProcess" paramId="facultyEvaluationProcessOID" paramName="facultyEvaluationProcess" paramProperty="OID">
				<bean:message key="label.delete" bundle="APPLICATION_RESOURCES"/>
			</html:link>
		</li>
		</logic:present>
	</ul>
	
	
	
	<p class="mtop15 mbottom05">
		<bean:size id="count" name="teacherEvaluationProcesses"/>
		<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.teacherEvaluationProcess.count" bundle="TEACHER_EVALUATION_RESOURCES"/>:
		<%= count %>
	</p>
	<p class="mvert05">
		<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.teacherEvaluationProcess.autoEvaluation.locked.count" bundle="TEACHER_EVALUATION_RESOURCES"/>:
		<bean:write name="autoEvaluatedCount"/>
	</p>
	<p class="mvert05">
		<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.teacherEvaluationProcess.evaluation.locked.count" bundle="TEACHER_EVALUATION_RESOURCES"/>:
		<bean:write name="evaluatedCount"/>
	</p>
	<p class="mvert05">
		<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.teacherEvaluationProcess.evaluation.approved.count" bundle="TEACHER_EVALUATION_RESOURCES"/>:
		<bean:write name="approvedEvaluatedCount"/>
	</p>
	<% if (AccessControl.getPerson().isTeacherEvaluationCoordinatorCouncilMember() || Group.managers().isMember(Authenticate.getUser())) { %>
	<p class="mtop05 mbottom15">
		<logic:equal name="facultyEvaluationProcess" property="areApprovedMarksPublished" value="true">
			<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.areApprovedMarksPublished.yes" bundle="TEACHER_EVALUATION_RESOURCES"/>
			<html:link page="/teacherEvaluation.do?method=unPublish" paramId="facultyEvaluationProcessOID" paramName="facultyEvaluationProcess" paramProperty="OID">
				<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.approved.marks.unPublish" bundle="TEACHER_EVALUATION_RESOURCES"/>
			</html:link>
		</logic:equal>
		<logic:notEqual name="facultyEvaluationProcess" property="areApprovedMarksPublished" value="true">
			<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.areApprovedMarksPublished.no" bundle="TEACHER_EVALUATION_RESOURCES"/>
			<html:link page="/teacherEvaluation.do?method=publish" paramId="facultyEvaluationProcessOID" paramName="facultyEvaluationProcess" paramProperty="OID">
				<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.approved.marks.publish" bundle="TEACHER_EVALUATION_RESOURCES"/>
			</html:link>
		</logic:notEqual>
	</p>
	<% } %>
	
	<logic:notEmpty name="teacherEvaluationProcesses">
		<table class="tstyle2 thlight thleft">
			<tr>
				<th>
					<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.teacherEvaluationProcess.evaluee" bundle="TEACHER_EVALUATION_RESOURCES"/>
				</th>
				<th>
					<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.teacherEvaluationProcess.evaluator" bundle="TEACHER_EVALUATION_RESOURCES"/>
				</th>
				<th>
					<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.teacherEvaluationProcess.coevaluators" bundle="TEACHER_EVALUATION_RESOURCES"/>
				</th>
				<th>
					<bean:message key="label.teacher.evaluation.type" bundle="TEACHER_EVALUATION_RESOURCES"/>
				</th>
				<th>
					<bean:message key="label.teacher.evaluation.state" bundle="TEACHER_EVALUATION_RESOURCES"/>
				</th>
				<th>
					<bean:message key="label.teacher.evaluation.mark" bundle="TEACHER_EVALUATION_RESOURCES"/>
				</th>
				<th>
					<bean:message key="label.teacher.evaluation.mark.approved" bundle="TEACHER_EVALUATION_RESOURCES"/>
				</th>
				<th>
				</th>
			</tr>
			<logic:iterate id="teacherEvaluationProcess" name="teacherEvaluationProcesses">
				<tr>
					<td>
						<bean:write name="teacherEvaluationProcess" property="evaluee.name"/>
					</td>
					<td>
						<logic:present name="teacherEvaluationProcess" property="evaluator">
							<bean:write name="teacherEvaluationProcess" property="evaluator.name"/>
						</logic:present>
					</td>
					<td>
						<bean:write name="teacherEvaluationProcess" property="coEvaluatorsAsString"/>
					</td>
					<td>
						<logic:present name="teacherEvaluationProcess" property="type">
							<fr:view name="teacherEvaluationProcess" property="type">
								<fr:layout><fr:property name="bundle" value="TEACHER_EVALUATION_RESOURCES"/></fr:layout>
							</fr:view>
						</logic:present>
					</td>
					<td>
						<fr:view name="teacherEvaluationProcess" property="state" layout="null-as-label" />
					</td>
					<td>
						<logic:present name="teacherEvaluationProcess" property="evaluationMark">
							<fr:view name="teacherEvaluationProcess" property="evaluationMark">
								<fr:layout><fr:property name="bundle" value="TEACHER_EVALUATION_RESOURCES"/></fr:layout>
							</fr:view>
						</logic:present>
					</td>
					<td>
						<logic:iterate id="approvedTeacherEvaluationProcessMark" name="teacherEvaluationProcess" property="orderedApprovedTeacherEvaluationProcessMark">
							<bean:write name="approvedTeacherEvaluationProcessMark" property="facultyEvaluationProcessYear.year"/>
							<logic:present name="approvedTeacherEvaluationProcessMark" property="approvedEvaluationMark">
								<fr:view name="approvedTeacherEvaluationProcessMark" property="approvedEvaluationMark">
									<fr:layout><fr:property name="bundle" value="TEACHER_EVALUATION_RESOURCES"/></fr:layout>
								</fr:view>
							</logic:present>
							<logic:notPresent name="approvedTeacherEvaluationProcessMark" property="approvedEvaluationMark">
								N/A
							</logic:notPresent>
							<br/>
						</logic:iterate>
					</td>
					<td>
						<html:link page="/teacherEvaluation.do?method=viewEvaluationByCCAD" paramId="processId" paramName="teacherEvaluationProcess" paramProperty="externalId">
							<bean:message key="label.view" bundle="APPLICATION_RESOURCES"/>
						</html:link>
					</td>
				</tr>
			</logic:iterate>
		</table>
	</logic:notEmpty>
	
	
</logic:present>



<logic:present name="facultyEvaluationProcessSet">
	<% if (AccessControl.getPerson().isTeacherEvaluationCoordinatorCouncilMember()) { %>
	<p class="mtop15">
		<html:link page="/teacherEvaluation.do?method=prepareCreateFacultyEvaluationProcess">
			<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.create" bundle="TEACHER_EVALUATION_RESOURCES"/>
		</html:link>
	</p>
	<% } %>
	<logic:empty name="facultyEvaluationProcessSet">
		<p class="mtop1">
			<em>
				<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.none" bundle="TEACHER_EVALUATION_RESOURCES"/>
			</em>
		</p>
	</logic:empty>
	<logic:notEmpty name="facultyEvaluationProcessSet">
		<table class="tstyle2 thlight thleft mtop05">
			<tr>
				<th>
					<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.process" bundle="TEACHER_EVALUATION_RESOURCES"/>
				</th>
				<th>
					<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.autoEvaluationInterval" bundle="TEACHER_EVALUATION_RESOURCES"/>
				</th>
				<th>
					<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.evaluationInterval" bundle="TEACHER_EVALUATION_RESOURCES"/>
				</th>
				<th>
				</th>
			</tr>
			<logic:iterate id="facultyEvaluationProcess" name="facultyEvaluationProcessSet">
			<tr>
				<td>
					<fr:view name="facultyEvaluationProcess" property="title"/>
				</td>
				<td>
					<fr:view name="facultyEvaluationProcess" property="autoEvaluationInterval.start"/>
					-
					<fr:view name="facultyEvaluationProcess" property="autoEvaluationInterval.end"/>
				</td>
				<td>
					<fr:view name="facultyEvaluationProcess" property="evaluationInterval.start"/>
					-
					<fr:view name="facultyEvaluationProcess" property="evaluationInterval.end"/>
				</td>
				<td>
					<html:link page="/teacherEvaluation.do?method=viewFacultyEvaluationProcess" paramId="facultyEvaluationProcessOID" paramName="facultyEvaluationProcess" paramProperty="OID">
						<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.view" bundle="TEACHER_EVALUATION_RESOURCES"/>
					</html:link>
					<logic:present role="(role(MANAGER) | role(OPERATOR))">
						<html:link page="/teacherEvaluation.do?method=deleteFacultyEvaluationProcess" paramId="facultyEvaluationProcessOID" paramName="facultyEvaluationProcess" paramProperty="OID">
							<bean:message key="label.delete" bundle="APPLICATION_RESOURCES"/>
						</html:link>
					</logic:present>
				</td>
			</tr>
			</logic:iterate>
		</table>
	</logic:notEmpty>
</logic:present>

<logic:present name="fileUploadBean">
	<h3 class="mtop15 mbottom05">
		<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.add.list.of.evaluees" bundle="TEACHER_EVALUATION_RESOURCES"/>
	</h3>
	<p class="mbottom1">
		<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.add.list.of.evaluees.instruction" bundle="TEACHER_EVALUATION_RESOURCES"/>
	</p>
	<bean:define id="urlUploadInvalid">/teacherEvaluation.do?method=viewFacultyEvaluationProcess&facultyEvaluationProcessOID=<bean:write name="fileUploadBean" property="facultyEvaluationProcess.externalId"/></bean:define>
	<fr:edit id="fileUploadBean" name="fileUploadBean" action="/teacherEvaluation.do?method=uploadEvaluators">
		<fr:schema bundle="APPLICATION_RESOURCES" type="pt.ist.fenixedu.teacher.evaluation.domain.FileUploadBean">
			<fr:slot name="inputStream" key="label.file" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="fileNameSlot" value="filename"/>
				<fr:property name="size" value="30"/>
			</fr:slot>
		</fr:schema>
		<fr:layout name="tabular">
			<fr:property name="classes" value="tstyle5 thlight thmiddle mtop05"/>
			<fr:property name="columnClasses" value=",,tderror1 tdclear"/>
		</fr:layout>
		<fr:destination name="cancel" path="<%= urlUploadInvalid %>"/>
	</fr:edit>
</logic:present>

<logic:present name="fileUploadBeanForApprovedMarks">
	<h3 class="mtop15 mbottom05">
		<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.load.approved.marks" bundle="TEACHER_EVALUATION_RESOURCES"/>
	</h3>
	<p class="mbottom1">
		<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.load.approved.marks.instruction" bundle="TEACHER_EVALUATION_RESOURCES"/>
	</p>
	<p>
		<html:link page="/teacherEvaluation.do?method=downloadApprovedEvaluationsFile" paramId="facultyEvaluationProcessOID" paramName="fileUploadBeanForApprovedMarks" paramProperty="facultyEvaluationProcess.externalId">
			<bean:message key="label.teacher.evaluation.facultyEvaluationProcess.load.approved.marks.file" bundle="TEACHER_EVALUATION_RESOURCES"/>
		</html:link>
	</p>
	
	<bean:define id="urlUploadInvalid">/teacherEvaluation.do?method=viewFacultyEvaluationProcess&facultyEvaluationProcessOID=<bean:write name="fileUploadBeanForApprovedMarks" property="facultyEvaluationProcess.externalId"/></bean:define>
	<fr:edit id="fileUploadBeanForApprovedMarks" name="fileUploadBeanForApprovedMarks" action="/teacherEvaluation.do?method=uploadApprovedEvaluations">
		<fr:schema bundle="APPLICATION_RESOURCES" type="pt.ist.fenixedu.teacher.evaluation.domain.FileUploadBean">
			<fr:slot name="inputStream" key="label.file" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
				<fr:property name="fileNameSlot" value="filename"/>
				<fr:property name="size" value="30"/>
			</fr:slot>
		</fr:schema>
		<fr:layout name="tabular">
			<fr:property name="classes" value="tstyle5 thlight thmiddle mtop05"/>
			<fr:property name="columnClasses" value=",,tderror1 tdclear"/>
		</fr:layout>
		<fr:destination name="cancel" path="<%= urlUploadInvalid %>"/>
	</fr:edit>
</logic:present>
