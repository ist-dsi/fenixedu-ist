<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST QUC.

    FenixEdu IST QUC is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST QUC is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST QUC.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<html:xhtml />
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<span class="error"><html:errors bundle="INQUIRIES_RESOURCES" /></span>
<html:messages id="message" message="true" bundle="INQUIRIES_RESOURCES">
    <p><span class="error"><bean:write name="message" /></span></p>
</html:messages>
<logic:present name="success">
	<p><span class="success0">${fr:message('resources.FenixEduQucResources', 'message.results.importation.file.upload.success')}</span></p>
</logic:present>

<h2><bean:message key="title.inquiries.uploadResults" bundle="INQUIRIES_RESOURCES"/></h2>


<p><bean:message key="label.gep.latest.requests.done" bundle="GEP_RESOURCES"/></p>
<table class="table table-striped table-bordered table-condensed">
	<tr>
		<th>${fr:message('resources.FenixEduQucResources', 'label.results.importation.requestDate')}</th>
		<th><bean:message key="label.inquiries.result.date" bundle="INQUIRIES_RESOURCES"/></th>
		<th><bean:message key="label.inquiry.importResults.newResults" bundle="INQUIRIES_RESOURCES"/></th>
		<th>${fr:message('resources.FenixEduQucResources', 'label.results.importation.processedFile')}</th>
	</tr>	
	<c:forEach var="queueJob" items="${queueJobList}">
	<tr>
		<td><c:out value="${queueJob.resultsImportationFile.creationDate.toString('dd-MM-yyyy HH:mm:ss')}"/></td>
		<td><c:out value="${queueJob.resultsDate.toString('dd-MM-yyyy HH:mm:ss')}"/></td>
		<td>
			<c:if test="${queueJob.newResults}"><bean:message key="label.yes" bundle="GEP_RESOURCES"/></c:if>
			<c:if test="${!queueJob.newResults}"><bean:message key="label.no" bundle="GEP_RESOURCES"/></c:if>
		</td>
		<td>
			<c:if test="${queueJob.done}"><bean:message key="label.yes" bundle="GEP_RESOURCES"/></c:if>
			<c:if test="${!queueJob.done}"><bean:message key="label.no" bundle="GEP_RESOURCES"/></c:if>
		</td>
	</tr>
	</c:forEach>	
</table>

<fr:edit id="uploadFileBean" name="uploadFileBean" action="/uploadInquiriesResults.do?method=submitResultsFile" >
	<fr:schema type="pt.ist.fenixedu.quc.dto.ResultsFileBean" bundle="INQUIRIES_RESOURCES">
		<fr:slot name="inputStream" required="true" key="label.inquiries.result.file"/>
		<fr:slot name="resultsDate" required="true" key="label.inquiries.result.date"/>
		<fr:slot name="newResults" required="true" key="label.inquiries.result.uploadType" layout="radio">
			<fr:property name="trueLabel" value="label.inquiry.importResults.newResults" />
			<fr:property name="falseLabel" value="label.inquiry.importResults.update" />
			<fr:property name="bundle" value="INQUIRIES_RESOURCES" />
			<fr:property name="classes" value="dinline liinline nobullet"/>
		</fr:slot>
	</fr:schema>
	
	<fr:layout name="tabular">
	    <fr:property name="classes" value="tstyle1 thlight mtop05 thleft"/>
	    <fr:property name="columnClasses" value=",,tderror1 tdclear"/>
	</fr:layout>
	<fr:destination name="cancel" path="/uploadInquiriesResults.do?method=prepare"/>
</fr:edit>
