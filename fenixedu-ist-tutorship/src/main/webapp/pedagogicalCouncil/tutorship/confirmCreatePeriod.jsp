<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST Tutorship.

    FenixEdu IST Tutorship is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST Tutorship is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST Tutorship.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ page language="java" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html"%>
<html:xhtml/>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean"%>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic"%>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>

<em><bean:message key="pedagogical.council" bundle="PEDAGOGICAL_COUNCIL" /></em>
<h2><bean:message key="label.tutorshipSummaryPeriod" bundle="APPLICATION_RESOURCES" /></h2>

<div class="infoop2">
<p><bean:message key="message.tutorshipSummaryPeriod.confirm" bundle="PEDAGOGICAL_COUNCIL" /></p>
<p>
<html:link page="/tutorshipSummary.do?method=searchTeacher">[voltar]
</html:link></p>
</div>