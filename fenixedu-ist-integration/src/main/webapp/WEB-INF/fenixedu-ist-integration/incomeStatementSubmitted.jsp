<%@ page import="org.fenixedu.bennu.core.groups.Group" %>
<%@ page import="org.fenixedu.bennu.core.security.Authenticate" %>
<%@ page import="org.fenixedu.bennu.core.util.CoreConfiguration" %>
<%@ page import="java.util.Calendar" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<% final String contextPath = request.getContextPath(); %>

${portal.toolkit()}

<h2>
    <spring:message code="title.income.statement" text="Income Statement"/>
</h2>

<div class="alert-success" style="margin: 10px; padding: 20px;">
    <p>
        <spring:message code="title.income.statement.submit.sucess" text="Statement Submitted"/>
    </p>
</div>
