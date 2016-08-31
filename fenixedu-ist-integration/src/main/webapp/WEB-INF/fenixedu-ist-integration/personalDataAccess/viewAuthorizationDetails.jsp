<%--

    Copyright Â© 2013 Instituto Superior TÃ©cnico

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
<%@page import="org.joda.time.Years"%>
<%@page import="org.joda.time.LocalDate"%>
<%@page import="org.fenixedu.bennu.core.domain.User"%>
<%@page import="org.fenixedu.bennu.core.security.Authenticate"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
    final String contextPath = request.getContextPath();
	final boolean allowCGDAccess = (Boolean) request.getAttribute("allowCGDAccess");
	final boolean allowBPIAccess = (Boolean) request.getAttribute("allowBPIAccess");
	final boolean allowSantanderAccess = (Boolean) request.getAttribute("allowSantanderAccess");
%>

        <style>
            .container {
                background-color: #fefefe;
                padding: 30px;
                border-radius: 10px;
                margin-top: 50px;
                max-width: 800px;
            }
            .title {
                border-bottom: 1px solid #eee;
                padding-bottom: 5px;
                font-size: 25px;
                min-height: 35px;
            }
            dd {
                margin-bottom: 5px;
            }
            #banksBody p {
                font-size: 16px;
            }
        </style>

<div class="page-header">
	<h1>
		<spring:message code="authorize.personal.data.access.title" />
	</h1>
</div>

<div id="txt">
    <%
        if ("true".equals(request.getParameter("saved"))) { 
    %>
            <div class="mvert15">
                <span class="success0">
                    <spring:message code="authorize.personal.data.access.saved" />
                </span>
            </div>
    <%
        } else {
    %>

    <br/>
    <div id="banksBody">                    

        <h2 style="border-bottom-width: 1px; border-bottom-color: #ddd; border-bottom-style: solid;">
            <spring:message code="authorize.personal.data.access.title.santander"/>
        </h2>

        <div class="alert well">
            <p style="margin-bottom: 20px;">
                <spring:message code="authorize.personal.data.access.description.santander"/>
            </p>

        <div class="row">
            <div class="col-lg-12 text-left">
                <span style="line-height: 20px; vertical-align: bottom; margin-right: 55px;">
                    <input type="radio" name="santanderRadio" id="santander_yes" value="true" onclick="removeDisabled()">
                    <spring:message code="authorize.personal.data.access.yes"/>
                </span>
                <span>
                    <input type="radio" name="santanderRadio" id="santander_no" value="false" onclick="removeDisabled()">
                    <spring:message code="authorize.personal.data.access.no"/>
                </span>
            </div>                          
        </div>
        </div>


        <h2 style="border-bottom-width: 1px; border-bottom-color: #ddd; border-bottom-style: solid; margin-top: 40px;">
            <spring:message code="authorize.personal.data.access.title.cgd" />
        </h2>

        <div class="alert well">
            <p style="margin-bottom: 20px;">
                <spring:message code="authorize.personal.data.access.description.cgd" />
            </p>

        <div class="row">
            <div class="col-lg-12 text-left">       
                <span style="line-height: 20px; vertical-align: bottom; margin-right: 55px;">
                    <input type="radio" name="cgdRadio" id="cgd_yes" value="true" onclick="removeDisabled()">
                    <spring:message code="authorize.personal.data.access.yes"/>
                </span>
                <span>
                    <input type="radio" name="cgdRadio" id="cgd_no" value="false" onclick="removeDisabled()">
                    <spring:message code="authorize.personal.data.access.no"/>
                </span>
            </div>
        </div>
        </div>


        <h2 style="border-bottom-width: 1px; border-bottom-color: #ddd; border-bottom-style: solid; margin-top: 40px;">
            <spring:message code="authorize.personal.data.access.title.bpi" />
        </h2>

        <div class="alert well">
            <p style="margin-bottom: 20px;">
                <spring:message code="authorize.personal.data.access.description.bpi" />
            </p>

        <div class="row">
            <div class="col-lg-12 text-left">
                <span style="line-height: 20px; vertical-align: bottom; margin-right: 55px;">
                    <input type="radio" name="bpiRadio" id="bpi_yes" value="true" onclick="removeDisabled()">
                    <spring:message code="authorize.personal.data.access.yes"/>
                </span>
                <span>
                    <input type="radio" name="bpiRadio" id="bpi_no" value="false" onclick="removeDisabled()">
                    <spring:message code="authorize.personal.data.access.no"/>
                </span>
            </div>                          
        </div>
        </div>

        <p class="text-center" style="margin-top:  30px;">
            <%
                final User user = Authenticate.getUser();
                if (Years.yearsBetween(user.getPerson().getDateOfBirthYearMonthDay().toLocalDate(), new LocalDate()).getYears() >= 18) {
            %>
                    <a href="#" id="submitButton" class="btn-primary btn btn-lg disabled" onclick="submitForm()" >
                        <spring:message code="authorize.personal.data.access.submit"/>
                    </a>
            <%
                } else {
            %>
                    <span class="error">
                        <spring:message code="authorize.personal.data.access.submit.not.available.underage"/>
                    </span>                            
            <%        
                }
            %>
        </p>
    </div>

    <%
        }
    %>
</div>

<script type="text/javascript">
    function removeDisabled() {
        var cgdRadio = document.querySelector('input[name="cgdRadio"]:checked');
        var bpiRadio = document.querySelector('input[name="bpiRadio"]:checked');
        var santanderRadio = document.querySelector('input[name="santanderRadio"]:checked');

        var bpiAnswered = bpiRadio != null && bpiRadio.value != undefined;
        var santanderAnswered = santanderRadio != null && santanderRadio.value != undefined;
        if (cgdRadio != null && cgdRadio.value != undefined && bpiAnswered && santanderAnswered) {
            document.getElementById("submitButton").className = "btn-primary btn btn-lg";
        }
    }

    function submitForm() {
        postYes(document.querySelector('input[name="cgdRadio"]:checked').value,
        document.querySelector('input[name="bpiRadio"]:checked').value,
        document.querySelector('input[name="santanderRadio"]:checked').value);
    }

    function replaceTargetWith( targetID, html ) {
          var i, tmp, elm, last, target = document.getElementById(targetID);
          tmp = document.createElement(html.indexOf('<td')!=-1?'tr':'div');
          tmp.innerHTML = html;
          i = tmp.childNodes.length;
          last = target;
          while(i--){
            target.parentNode.insertBefore((elm = tmp.childNodes[i]), last);
            last = elm;
          }
          target.parentNode.removeChild(target);
    }

    function goByeBye() {
        document.getElementById ( "banksBody" ).style.display = "none";
        document.getElementById ( "byeByeBody" ).style.visibility = "visible";
        replaceTargetWith( 'visibleTitle', '<span id="visibleTitle">Processo Concluído</span>' );
    }

    function postYes(allowAccessCgd, allowAccessBpi, allowAccessSantander) {
        var form = document.createElement("form");
        form.setAttribute("method", "post");
        form.setAttribute("action", '<%= contextPath %>' + '/authorize-personal-data-access' );

        var hiddenField = document.createElement("input");
        hiddenField.setAttribute("type", "hidden");
        hiddenField.setAttribute("name", "allowAccessCgd");
        hiddenField.setAttribute("value", allowAccessCgd);
        form.appendChild(hiddenField);

        var hiddenField2 = document.createElement("input");
        hiddenField2.setAttribute("type", "hidden");
        hiddenField2.setAttribute("name", "qs");
        hiddenField2.setAttribute("value", window.location + '?saved=true');
        form.appendChild(hiddenField2);


        if(allowAccessBpi != undefined) {
            var hiddenField3 = document.createElement("input");
            hiddenField3.setAttribute("type", "hidden");
            hiddenField3.setAttribute("name", "allowAccessBpi");
            hiddenField3.setAttribute("value", allowAccessBpi);
            form.appendChild(hiddenField3);
        }

        if(allowAccessSantander != undefined) {
            var hiddenField4 = document.createElement("input");
            hiddenField4.setAttribute("type", "hidden");
            hiddenField4.setAttribute("name", "allowAccessSantander");
            hiddenField4.setAttribute("value", allowAccessSantander);
            form.appendChild(hiddenField4);
        }
        
        document.body.appendChild(form);
        form.submit();
        document.getElementById ( "banksBody" ).style.display = "none";
    }

    replaceTargetWith( 'visibleTitle', '<span id="visibleTitle">Cedência de Dados / Cartões</span>' );
</script>
