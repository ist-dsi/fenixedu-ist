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
<%@ page import="org.joda.time.LocalDate" %>

<html:html xhtml="true">
    <head>
        <title>
            <bean:message key="message.inquiries.firstTimeCycle.title" bundle="INQUIRIES_RESOURCES"/>
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
                    <bean:message key="message.inquiries.firstTimeCycle.title" bundle="INQUIRIES_RESOURCES"/>
                </div>
            </div>

            <% 
                LocalDate now = new LocalDate();
                LocalDate limitDate = new LocalDate(2015, 12, 1);
            %>
            <div id="txt">
                <p>
                    <bean:message key="message.inquiries.firstTimeCycle.body" arg0="<%=org.fenixedu.academic.domain.organizationalStructure.Unit.getInstitutionAcronym()%>" arg1="<%=org.fenixedu.academic.domain.organizationalStructure.Unit.getInstitutionName().getContent()%>" bundle="INQUIRIES_RESOURCES"/>
                </p>
                <br />
                <div class="alert alert-warning">
                    <bean:message key="message.inquiries.firstTimeCycle.warningDate" arg0="<%= limitDate.toString() %>" bundle="INQUIRIES_RESOURCES"/>
                </div>
            </div>

            <br />

            <div align="center">
            	<a href="${pageContext.request.contextPath}/student/studentCycleInquiry.do?method=prepare" class="btn btn-default"><bean:message key="button.inquiries.respond.now" /></a>
                <% 
                    if(now.isBefore(limitDate)) {
                %>
                    <a tabindex="1" href="${pageContext.request.contextPath}/home.do" class="btn btn-default" tabindex="1"><bean:message bundle="APPLICATION_RESOURCES" key="label.proceed"/></a>
                <% } %>
            </div>
        </div>
        
    </body>
</html:html>
