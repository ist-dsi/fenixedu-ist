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
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr" %>

<html:html xhtml="true">
    <head>
        <title>
            <bean:message key="message.inquiries.title" bundle="INQUIRIES_RESOURCES"/>
        </title>

        <link href="${pageContext.request.contextPath}/themes/<%= org.fenixedu.bennu.portal.domain.PortalConfiguration.getInstance().getTheme() %>/css/style.css" rel="stylesheet" type="text/css" />

        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            .container {
                background-color: #fefefe;
                padding: 30px;
                border-radius: 10px;
                max-width: 760px;
                margin-top: 30px;
            }
            .title {
                border-bottom: 1px solid #eee;
                padding-bottom: 5px;
                font-size: 25px;
                min-height: 35px;
                margin-bottom: 20px;
            }
            @media (max-width: 768px) {
                .title > * {
                    text-align: center !important;
                }
                ul {
                    padding-left: 20px;
                }
            }
        </style>
    </head>
    <body>
    
        <div class="container">
            <div class="title row">
                <div class="col-sm-4 text-right col-sm-push-8">
                    <img src="${pageContext.request.contextPath}/api/bennu-portal/configuration/logo"/>
                </div>
                <div class="col-sm-8 col-sm-pull-4">
                    <bean:write name="cerimonyInquiryPerson" property="cerimonyInquiry.description"/>
                </div>
            </div>

			<bean:write name="cerimonyInquiryPerson" property="cerimonyInquiry.text" filter="false"/>

			<br />
			<br />
            <div id="txt" class="col-sm-8 col-sm-offset-2">

				<fr:form action="/respondToAlumniInquiriesQuestion.do">
					<html:hidden property="method" value="registerAlumniResponseNow"/>
					<fr:edit id="cerimonyInquiryPerson" name="cerimonyInquiryPerson">
						<fr:schema bundle="APPLICATION_RESOURCES" type="org.fenixedu.academic.domain.alumni.CerimonyInquiryPerson">
							<fr:slot name="cerimonyInquiryAnswer" key="label.response" layout="menu-select" validator="pt.ist.fenixWebFramework.renderers.validators.RequiredValidator">
								<fr:property name="providerClass" value="org.fenixedu.academic.ui.renderers.providers.CerimonyInquiryAnswerProvider"/>
								<fr:property name="format" value="\${text}" />
							</fr:slot>
							<logic:equal name="cerimonyInquiryPerson" property="cerimonyInquiry.allowComments" value="true">
								<fr:slot name="comment" key="label.observations"/>
							</logic:equal>
						</fr:schema>
						<fr:layout name="tabular">
							<fr:property name="classes" value="thlight"/>
							<fr:property name="columnClasses" value=",,"/>
						</fr:layout>
					</fr:edit>
					<fr:destination name="cancel" path="/respondToAlumniInquiriesQuestion.do?method=registerAlumniResponseRespondLater"/>
					<div class="form-group">
						<div class="col-sm-10 col-sm-offset-2">
							<html:submit bundle="HTMLALT_RESOURCES" altKey="inquiries.respond.now" property="ok">
								<bean:message key="button.inquiries.respond.now" />
							</html:submit>
							<a href="${pageContext.request.contextPath}/home.do" class="btn btn-default"><bean:message key="button.inquiries.respond.later" /></a>
						</div>
					</div>
				</fr:form>
            </div>
        </div>
        
    </body>
</html:html>