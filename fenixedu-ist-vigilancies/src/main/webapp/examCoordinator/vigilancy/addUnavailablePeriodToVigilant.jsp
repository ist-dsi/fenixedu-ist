<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST Vigilancies.

    FenixEdu IST Vigilancies is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST Vigilancies is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST Vigilancies.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<html:xhtml/>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/datetime-1.0" prefix="date"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr" %>


<h2><bean:message bundle="VIGILANCY_RESOURCES" key="label.vigilancy.createUnavailablePeriod"/></h2>

<logic:messagesPresent message="true">
	<p>
		<html:messages id="messages" message="true" bundle="VIGILANCY_RESOURCES">
			<span class="error0"><bean:write name="messages"/></span>
		</html:messages>
	</p>
</logic:messagesPresent>

<fr:edit name="bean" id="periodCreation" schema="addUnavailablePeriodToVigilant"
action="<%= "vigilancy/unavailablePeriodManagement.do?method=addUnavailablePeriodToVigilant&gid=" + request.getParameter("gid") %>"
>
<fr:destination name="cancel" path="/vigilancy/unavailablePeriodManagement.do?method=prepareManageUnavailablePeriodsOfVigilants"/>
    <fr:layout name="tabular">
		<fr:property name="classes" value="tstyle5 thlight thright" />
	</fr:layout>
</fr:edit>
