<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST Parking.

    FenixEdu IST Parking is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST Parking is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST Parking.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<html:xhtml />
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>

<script language="Javascript" type="text/javascript">
<!--

function confirmation(thisForm){
	var result = confirm("Os ficheiros submetidos electronicamente vão ser apagados. Deseja continuar ?");
	if( result ) {
		thisForm.accepted.value="true";
		thisForm.submit();
	}
}

function displayCardValidPeriod(){
	if(document.getElementById('cardValidPeriodIdYes').checked){
		document.getElementById('cardValidPeriodDivId').style.display='none';
	} else {
		document.getElementById('cardValidPeriodDivId').style.display='block';
	}
}

function hideCardValidPeriod(toShow){
	if(toShow){
		document.getElementById('cardValidPeriodDivId').style.display='block';
	} else {
		document.getElementById('cardValidPeriodDivId').style.display='none';
	}
}
		
// -->
</script>


<h2><bean:message key="label.request" /></h2>
					
<logic:present name="parkingRequest">		
			
	<bean:define id="parkingRequest" name="parkingRequest" toScope="request"/>
	<bean:define id="parkingParty" name="parkingRequest" property="parkingParty" toScope="request"/>	
	<bean:define id="personID" name="parkingParty" property="party.externalId" />
	
	
	<h3><bean:message key="label.parkUserInfo" /></h3>
	<p>
		<img src="${parkingParty.party.user.profile.avatarUrl}" alt="<c:out value='${parkingParty.party.name}'/>" />
	</p>
	<logic:iterate id="occupation" name="parkingParty" property="occupations">
		<p><bean:write name="occupation" filter="false"/></p>
	</logic:iterate>
	
	<logic:notEmpty name="parkingRequest" property="requestedAs">
		<p>
			<span class="warning0">
				<bean:message key="message.userRequestedAs" bundle="PARKING_RESOURCES"/>
				<strong><bean:write name="parkingRequest" property="requestedAs"/></strong>
			</span>
		</p>
	</logic:notEmpty>
	
	<bean:define id="person" name="parkingParty" property="party" type="org.fenixedu.academic.domain.Person"/>
	<logic:notEqual name="parkingParty" property="partyClassification" value="TEACHER">
	<logic:notEqual name="parkingParty" property="partyClassification" value="EMPLOYEE">
		<logic:equal name="parkingRequest" property="limitlessAccessCard" value="false">
			<bean:define id="cardTypeRequest"><bean:message key="label.limitedCard" bundle="PARKING_RESOURCES"></bean:message></bean:define>
		</logic:equal>
		<logic:equal name="parkingRequest" property="limitlessAccessCard" value="true">
			<bean:define id="cardTypeRequest"><bean:message key="label.limitlessCard" bundle="PARKING_RESOURCES"></bean:message></bean:define>
		</logic:equal>
		<p>
			<span class="warning0">
				<bean:message key="message.userRequestedCardType" bundle="PARKING_RESOURCES"/>
				<strong><bean:write name="cardTypeRequest"/></strong>
			</span>
		</p>
	</logic:notEqual>
	</logic:notEqual>
	<logic:present name="monitor">
		<logic:equal name="parkingRequest" property="limitlessAccessCard" value="false">
			<bean:define id="cardTypeRequest"><bean:message key="label.limitedCard" bundle="PARKING_RESOURCES"></bean:message></bean:define>
		</logic:equal>
		<logic:equal name="parkingRequest" property="limitlessAccessCard" value="true">
			<bean:define id="cardTypeRequest"><bean:message key="label.limitlessCard" bundle="PARKING_RESOURCES"></bean:message></bean:define>
		</logic:equal>
		<p><span class="infoop2"><bean:message key="message.userRequestedCardType" bundle="PARKING_RESOURCES"/>
		<strong><bean:write name="cardTypeRequest"/></strong></span></p>
	</logic:present>

	<bean:define id="parkingRequestID" name="parkingRequest" property="externalId" />
	<logic:notEqual name="parkingRequest" property="parkingRequestState" value="PENDING">		
		<logic:equal name="parkingRequest" property="parkingRequestState" value="REJECTED">
			<html:link page="<%="/parking.do?method=unrejectParkingRequest&parkingRequestOid="+parkingRequestID%>">
					<bean:message key="label.unrejectParkingRequest" bundle="PARKING_RESOURCES" />
			</html:link>
		</logic:equal>
		<jsp:include page="viewParkingPartyAndRequest.jsp"/>
	</logic:notEqual>
	<bean:define id="parkingPartyIdint" name="parkingRequest" property="parkingParty.externalId" />
	<logic:equal name="parkingRequest" property="parkingRequestState" value="PENDING">
			
		<bean:define id="groupName" value="" type="java.lang.String"/>		
		<html:form action="/parking">
			<html:hidden bundle="PARKING_RESOURCES" altKey="hidden.code" property="code" value="<%= parkingRequestID.toString()%>"/>
			<html:hidden bundle="HTMLALT_RESOURCES" altKey="hidden.method" property="method" value="editFirstTimeParkingParty"/>
			<html:hidden bundle="PARKING_RESOURCES" altKey="hidden.parkingRequestState" property="parkingRequestState" value="<%= pageContext.findAttribute("parkingRequestState").toString() %>"/>
			<html:hidden bundle="PARKING_RESOURCES" altKey="hidden.partyClassification" property="partyClassification" value="<%= pageContext.findAttribute("partyClassification").toString() %>"/>
			<html:hidden bundle="PARKING_RESOURCES" altKey="hidden.personName" property="personName" value="<%= pageContext.findAttribute("personName").toString() %>"/>
			<html:hidden bundle="PARKING_RESOURCES" altKey="hidden.carPlateNumber" property="carPlateNumber" value="<%= pageContext.findAttribute("carPlateNumber").toString() %>"/>
			<html:hidden bundle="PARKING_RESOURCES" altKey="hidden.accepted" property="accepted" value=""/>	
			<html:hidden bundle="PARKING_RESOURCES" altKey="hidden.parkingPartyID" property="parkingPartyID" value="<%= parkingPartyIdint.toString() %>" />		
			
			<p>
				<span class="error0"><!-- Error messages go here --><html:errors /></span>		
			</p>

			<table class="tstyle5 thlight thright thmiddle">
				<tr>
					<th><bean:message key="label.cardNumber"/>:</th>
					<td><html:text bundle="PARKING_RESOURCES" altKey="text.cardNumber" size="12" property="cardNumber"/></td>
					<td class="tderror1 tdclear">
						<html:messages id="message" property="cardNumber" message="true" bundle="PARKING_RESOURCES">
							<bean:write name="message"/>
						</html:messages>
					</td>
				</tr>
				<tr>
					<th><bean:message key="label.group"/>:</th>
					<td>
						<html:select bundle="HTMLALT_RESOURCES" altKey="select.variationCode" property="groupID">
							<html:option value="0">
								<bean:message key="label.choose"/>
							</html:option>
							<logic:iterate id="groupIt" name="groups" type="pt.ist.fenixedu.parking.domain.ParkingGroup">
								<bean:define id="groupId" name="groupIt" property="externalId"/>					
								<html:option value="<%=groupId.toString()%>">
									<bean:write name="groupIt" property="groupName"/>
								</html:option>
							</logic:iterate>
						</html:select>
					</td>
					<td class="tderror1 tdclear">
						<html:messages id="message" property="group" message="true" bundle="PARKING_RESOURCES">
							<bean:write name="message"/>
						</html:messages>
					</td>
				</tr>
			</table>


			<p>
				<bean:message key="label.cardValidPeriod" bundle="PARKING_RESOURCES"/>
				<html:radio bundle="PARKING_RESOURCES" altKey="radio.cardAlwaysValid" styleId="cardValidPeriodIdYes" name="parkingForm" property="cardAlwaysValid" value="yes" onclick="displayCardValidPeriod(false)">Sim</html:radio>
				<html:radio bundle="PARKING_RESOURCES" altKey="radio.cardAlwaysValid" styleId="cardValidPeriodIdNo" name="parkingForm" property="cardAlwaysValid" value="no" onclick="displayCardValidPeriod(true)">Não</html:radio>
			</p>

			<p>
				<html:messages id="message" property="mustFillInDates" message="true" bundle="PARKING_RESOURCES"><span class="error0"><bean:write name="message"/><br/></span></html:messages>
				<html:messages id="message" property="invalidPeriod" message="true" bundle="PARKING_RESOURCES"><span class="error0"><bean:write name="message"/></span></html:messages>				
			</p>
			
			<div id="cardValidPeriodDivId" style="display:block">
				<fr:edit id="cardValidPeriod" name="parkingPartyBean" schema="edit.parkingPartyBean.cardValidPeriod">
					<fr:layout name="tabular">
						<fr:property name="classes" value="tstyle5 thlight thright"/>
						<fr:property name="columnClasses" value=",,tdclear tderror1"/>
					</fr:layout>
				</fr:edit>
			</div>		
			
			<jsp:include page="viewParkingPartyAndRequest.jsp"/>
			
			<p>
				<span class="error0 mtop0">
					<html:messages id="message" property="note" message="true" bundle="PARKING_RESOURCES">
						<bean:write name="message"/>
					</html:messages>
				</span>
			</p>
			
			<p class="mtop15 mbottom025"><strong><bean:message key="label.note"/>:</strong></p>
			<html:textarea bundle="HTMLALT_RESOURCES" altKey="textarea.note" rows="7" cols="45" property="note"/>
			<p class="mtop05">
				<html:button bundle="HTMLALT_RESOURCES" altKey="submit.submit" property="accept" onclick="confirmation(this.form);"><bean:message key="button.accept"/></html:button>
				<html:submit bundle="HTMLALT_RESOURCES" altKey="submit.submit" property="notify"><bean:message key="button.notify"/></html:submit>
				<html:submit bundle="HTMLALT_RESOURCES" altKey="submit.submit" property="reject"><bean:message key="button.reject"/></html:submit>
			</p>	
			
			<p class="mtop15">
				<html:button property="" onclick="this.form.method.value='exportToPDFParkingCard';this.form.submit();this.form.method.value='editFirstTimeParkingParty';">
					<bean:message key="label.exportToPDF" bundle="PARKING_RESOURCES"/>
				</html:button>
			</p>
		</html:form>
	</logic:equal>
</logic:present>