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
<%@page import="org.fenixedu.bennu.core.security.Authenticate"%>
<%@page import="org.fenixedu.academic.ui.struts.action.HomeAction"%>

<%
    final Object logged = request.getSession().getAttribute("LOGGED_USER_ATTRIBUTE");
    final String path = logged == null ? request.getContextPath() + "/login" : HomeAction.findFirstFuntionalityPath(request);
    response.sendRedirect(path);
%>
