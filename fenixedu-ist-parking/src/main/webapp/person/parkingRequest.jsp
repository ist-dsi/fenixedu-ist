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
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ page
	import="org.fenixedu.academic.ui.struts.action.resourceAllocationManager.utils.PresentationConstants"%>

<h2><bean:message key="label.parking" bundle="PARKING_RESOURCES" /></h2>


<logic:present name="parkingParty">

	<p class="mtop15 mbottom025"><strong><bean:message	key="label.person.title.personal.info"  bundle="APPLICATION_RESOURCES" /></strong></p>

	<fr:view name="parkingParty" property="party" schema="viewPersonInfo">
		<fr:layout name="tabular">
			<fr:property name="classes" value="tstyle1 thright thlight mtop025" />
		</fr:layout>
	</fr:view>
	<logic:empty name="parkingParty" property="parkingRequestsSet">
		<logic:equal name="parkingParty" property="hasAllNecessaryPersonalInfo" value="false">
			<p class="infoop2">
				<bean:message key="message.personalDataCondition" bundle="PARKING_RESOURCES" /><br/>
				<bean:message key="message.no.necessaryPersonalInfo" bundle="PARKING_RESOURCES" />
			</p>
		</logic:equal>
		<logic:notEqual name="parkingParty"	property="hasAllNecessaryPersonalInfo" value="false">
			<p>
				<bean:message key="label.read.parkingRegulation" bundle="PARKING_RESOURCES" />: 
				<a href="<%= org.fenixedu.academic.domain.Installation.getInstance().getInstituitionURL() %>files/viver-IST/gestao-estacionamento/reg_estac.pdf" target="_blank">
					<bean:message key="label.parkingRegulation" arg0="<%=org.fenixedu.academic.domain.organizationalStructure.Unit.getInstitutionAcronym()%>" bundle="PARKING_RESOURCES" />
					<bean:message key="label.parkingRegulation.pdf" bundle="PARKING_RESOURCES" />
				</a>
			</p>


			<logic:equal name="parkingParty" property="acceptedRegulation" value="false">				
				<div class="mvert1 infoop2">
					<p class="mvert05"><bean:message key="message.acceptRegulationCondition" arg0="<%=org.fenixedu.academic.domain.organizationalStructure.Unit.getInstitutionAcronym()%>" bundle="PARKING_RESOURCES" /></p>
					<p class="mvert05"><bean:message key="message.acceptRegulation" arg0="<%=org.fenixedu.academic.domain.organizationalStructure.Unit.getInstitutionAcronym()%>" bundle="PARKING_RESOURCES" /></p>
					<p class="mvert05">
						<strong>
							<html:link page="/personParking.do?method=acceptRegulation">
								<bean:message key="button.acceptRegulation" bundle="PARKING_RESOURCES" /> &raquo;
							</html:link>
						</strong>
					</p>
				</div>
			</logic:equal>

			<logic:notEqual name="parkingParty" property="acceptedRegulation" value="false">
				
				<div class="infoop2 mtop15">
					<div style="padding-bottom: 0.25em;"><bean:write name="parkingParty" property="parkingAcceptedRegulationMessage" filter="false"/></div>
					<p>
						<strong>
							<html:link page="/personParking.do?method=prepareEditParking">
								<bean:message key="label.insertParkingDocuments" bundle="PARKING_RESOURCES" /> &raquo;
							</html:link>
						</strong>
					</p>
				</div>
				

				
			</logic:notEqual>
		</logic:notEqual>
	</logic:empty>
	<logic:notEmpty name="parkingParty" property="parkingRequestsSet">
		<p>
			<bean:message key="label.read.parkingRegulation" bundle="PARKING_RESOURCES" />: 
			<a href="<%= org.fenixedu.academic.domain.Installation.getInstance().getInstituitionURL() %>files/viver-IST/gestao-estacionamento/reg_estac.pdf" target="_blank">
				<bean:message key="label.parkingRegulation" arg0="<%=org.fenixedu.academic.domain.organizationalStructure.Unit.getInstitutionAcronym()%>" bundle="PARKING_RESOURCES" />
				<bean:message key="label.parkingRegulation.pdf" bundle="PARKING_RESOURCES" />
			</a>
		</p>
			
		
		<logic:equal name="canEdit" value="false">
			<logic:equal name="parkingParty" property="canRequestUnlimitedCardAndIsInAnyRequestPeriod" value="true">
				<div class="mvert15">
					<div class="infoop2">
						<bean:message key="message.canRenewToUnlimitedCard" bundle="PARKING_RESOURCES"/>
					</div>
					<ul class="mvert05">
					<bean:size id="size" name="parkingParty" property="submitAsRoles"/>
						<logic:lessEqual name="size" value="1">
							<li><html:link page="/personParking.do?method=renewUnlimitedParkingRequest">
								<bean:message key="label.renewToUnlimitedCard" bundle="PARKING_RESOURCES" />
							</html:link></li>
						</logic:lessEqual>
						<logic:greaterThan name="size" value="1">
							<fr:form action="/personParking.do?method=renewUnlimitedParkingRequest">
								<fr:edit id="renewUnlimitedParkingRequest" name="renewUnlimitedParkingRequest">
									<fr:schema type="pt.ist.fenixedu.parking.domain.ParkingRequest$ParkingRequestFactoryCreator" bundle="PARKING_RESOURCES">
										<fr:slot name="requestAs" key="label.renewToUnlimitedCardAs" bundle="PARKING_RESOURCES" layout="menu-select" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
											<fr:property name="providerClass" value="pt.ist.fenixedu.parking.ui.renderers.providers.parking.ParkingRequestAsProvider" />
											<fr:property name="eachLayout" value="this-does-not-exist" />		
										</fr:slot>
									</fr:schema>
									<fr:layout name="tabular">
										<fr:property name="classes" value="form listInsideClear" />
										<fr:property name="columnClasses" value="width100px,,tderror" />
									</fr:layout>
								</fr:edit>
								<html:submit><bean:message key="label.submit" bundle="APPLICATION_RESOURCES"/></html:submit>	
							</fr:form>
						</logic:greaterThan>
					</ul>
				</div>
			</logic:equal>
			<logic:present name="renewUnlimitedParkingRequest.sucess">
				<logic:equal name="renewUnlimitedParkingRequest.sucess" value="true">
				<p class="mtop15"><strong class="success0"><bean:message key="message.renewUnlimitedParkingRequest.sucess" bundle="PARKING_RESOURCES"/></strong></p>
				</logic:equal>
			</logic:present>
		</logic:equal>
		
		<%-- editar --%>
		<logic:equal name="canEdit" value="true">
			<p>
				<div class="infoop2"><bean:message key="message.pendingParkingRequestState" arg0="<%=org.fenixedu.academic.domain.organizationalStructure.Unit.getInstitutionAcronym()%>" bundle="PARKING_RESOURCES" /></div>
			</p>
			<p>
				<html:link page="/personParking.do?method=prepareEditParking">
					<bean:message key="label.editParkingDocuments"
						bundle="PARKING_RESOURCES" />
				</html:link>
			</p>
		</logic:equal>
		
		
		<bean:define id="parkingPartyOrRequest" name="parkingParty"/>
		<logic:notEmpty name="parkingParty" property="vehiclesSet">
			<h3 class="separator2 mtop2"><bean:message key="label.actualState" bundle="PARKING_RESOURCES"/></h3>
			<fr:view name="parkingParty" schema="show.parkingParty.cardValidPeriod">
				<fr:layout name="tabular">
					<fr:property name="classes" value="tstyle1 thright thlight mtop025 mbottom1" />
					<fr:property name="headerClasses" value="acenter" />
				</fr:layout>
			</fr:view>
		</logic:notEmpty>
		<logic:empty name="parkingParty" property="vehiclesSet">
			<h3 class="separator2 mtop2"><bean:message key="label.request" bundle="PARKING_RESOURCES"/></h3>
			<bean:define id="parkingPartyOrRequest" name="parkingParty" property="lastRequest"/>
		</logic:empty>

		<p class="mtop15 mbottom025"><strong><bean:message key="label.driverLicense" bundle="PARKING_RESOURCES" /></strong></p>		
		
		<table class="tstyle1 thright thlight mtop025 mbottom1">
			<tr>
				<th><bean:message key="label.driverLicense" bundle="PARKING_RESOURCES"/></th>
				<td><bean:write name="parkingPartyOrRequest" property="driverLicenseFileNameToDisplay"/></td>
				<td class="noborder">
				<logic:notEmpty name="parkingPartyOrRequest" property="driverLicenseDocument">			
					<fr:view name="parkingPartyOrRequest" property="driverLicenseDocument.parkingFile">
						<fr:layout name="link">
							<fr:property name="key" value="link.viewDocument"/>
							<fr:property name="bundle" value="PARKING_RESOURCES"/>
						</fr:layout>
					</fr:view>		
				</logic:notEmpty>	
				</td>
			</tr>
		</table>
		
		<p class="mtop1 mbottom025"><strong><bean:message key="label.vehicles" bundle="PARKING_RESOURCES" /></strong></p>
		<table class="tstyle1 thright thlight mtop025 mbottom1">
		<logic:iterate id="vehicle" name="parkingPartyOrRequest" property="vehiclesSet">
			<tr>
				<th><bean:message key="label.vehicleMake" bundle="PARKING_RESOURCES"/>:</th>
				<td><bean:write name="vehicle" property="vehicleMake"/></td>
				<td class="noborder"></td>
			</tr>
			<tr>
				<th><bean:message key="label.vehiclePlateNumber" bundle="PARKING_RESOURCES"/>:</th>
				<td><bean:write name="vehicle" property="plateNumber"/></td>
				<td class="noborder"></td>
			</tr>
			<tr>
				<th><bean:message key="label.vehiclePropertyRegistry" bundle="PARKING_RESOURCES"/>:</th>
				<td><bean:write name="vehicle" property="propertyRegistryFileNameToDisplay"/></td>
				<td class="noborder">
					<logic:notEmpty name="vehicle" property="propertyRegistryDocument">			
						<fr:view name="vehicle" property="propertyRegistryDocument.parkingFile">
							<fr:layout name="link">
								<fr:property name="key" value="link.viewDocument"/>
								<fr:property name="bundle" value="PARKING_RESOURCES"/>
							</fr:layout>
						</fr:view>		
					</logic:notEmpty>
				</td>
			</tr>	
			<tr>
				<th><bean:message key="label.vehicleInsurance" bundle="PARKING_RESOURCES"/>:</th>
				<td><bean:write name="vehicle" property="insuranceFileNameToDisplay"/></td>
				<td class="noborder">
					<logic:notEmpty name="vehicle" property="insuranceDocument">			
						<fr:view name="vehicle" property="insuranceDocument.parkingFile">
							<fr:layout name="link">
								<fr:property name="key" value="link.viewDocument"/>
								<fr:property name="bundle" value="PARKING_RESOURCES"/>
							</fr:layout>
						</fr:view>		
					</logic:notEmpty>		
				</td>
			</tr>
			<tr>
				<th><bean:message key="label.vehicleOwnerID" bundle="PARKING_RESOURCES"/>:</th>
				<td><bean:write name="vehicle" property="ownerIdFileNameToDisplay"/></td>
				<td class="noborder">
					<logic:notEmpty name="vehicle" property="ownerIdDocument">			
						<fr:view name="vehicle" property="ownerIdDocument.parkingFile">
							<fr:layout name="link">
								<fr:property name="key" value="link.viewDocument"/>
								<fr:property name="bundle" value="PARKING_RESOURCES"/>
							</fr:layout>
						</fr:view>		
					</logic:notEmpty>
				</td>
			</tr>
			<tr>
				<th><bean:message key="label.vehicleAuthorizationDeclaration" bundle="PARKING_RESOURCES"/>:</th>
				<td><bean:write name="vehicle" property="authorizationDeclarationFileNameToDisplay"/></td>
				<td class="noborder">
					<logic:notEmpty name="vehicle" property="declarationDocument">			
						<fr:view name="vehicle" property="declarationDocument.parkingFile">
							<fr:layout name="link">
								<fr:property name="key" value="link.viewDocument"/>
								<fr:property name="bundle" value="PARKING_RESOURCES"/>
							</fr:layout>
						</fr:view>		
					</logic:notEmpty>		
				</td>
			</tr>
			<tr>
				<td class="noborder"> </td>
				<td class="noborder"> </td>
				<td class="noborder"> </td>
		</logic:iterate>
		</table>


		<h3 class="separator2 mtop2"><bean:message key="label.requests" bundle="PARKING_RESOURCES"/></h3>

		<fr:view name="parkingParty" property="orderedParkingRequests" schema="show.parkingRequestToUser">
			<fr:layout name="tabular">
				<fr:property name="classes" value="tstyle1 mtop05 printborder" />
				<fr:property name="headerClasses" value="acenter" />
			</fr:layout>
		</fr:view>
	</logic:notEmpty>
</logic:present>
