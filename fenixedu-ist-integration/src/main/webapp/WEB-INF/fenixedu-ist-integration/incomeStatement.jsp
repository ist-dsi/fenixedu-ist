<%@ page import="org.fenixedu.bennu.core.security.Authenticate" %>
<%@ page import="java.util.Calendar" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

${portal.toolkit()}

<h2>
    <spring:message code="title.income.statement" text="Income Statement"/>
</h2>

<div class="alert-info" style="margin: 10px; padding: 20px;">
    <p>
        <spring:message code="title.income.statement.instructions" text="Income Statement Instructions"/>
    </p>
</div>

<c:if test="${not empty errorMessage}">
    <div class="alert alert-danger">
        <c:out value="${errorMessage}"/>
    </div>
</c:if>

<form class="form-horizontal" method="POST" enctype="multipart/form-data">
    ${csrf.field()}

    <div class="alert well">
        <p>
            <spring:message code="title.income.statement.declaration" text="I hereby declare ..." arguments="<%= Authenticate.getUser().getPerson().getName() %>"/>
        </p>
        <br/>
        <div class="checkbox">
            <label>
                <input id="statementAccept" type="checkbox" name="statementAccept" onchange="toggleAccept();"/>
                <spring:message code="title.income.statement.accept" text="Accept"/>
            </label>
        </div>
    </div>

    <div class="form-group">
        <label class="control-label col-sm-2" for="year">
            <spring:message code="title.income.statement.year" text="Year"/>
        </label>
        <div class="col-sm-10">
            <input id="year" name="year" type="text" required class="form-control" value="<%= Calendar.getInstance().get(Calendar.YEAR) - 1 %>"/>
        </div>
    </div>

    <div class="form-group">
        <label class="control-label col-sm-2" for="year">
            <spring:message code="title.income.statement.file" text="File"/>
        </label>
        <div class="col-sm-10">
            <input name="file" type="file" required class="form-control" accept="application/pdf" />
        </div>
    </div>

    <div class="form-group">
        <div class="col-sm-10 col-sm-offset-2">
            <button id="submitRequest" class="btn btn-primary" disabled>
                <spring:message code="label.submit" text="Submit" />
            </button>
        </div>
    </div>

</form>

<script type='text/javascript'>
    function toggleAccept() {
        var checkBox = document.getElementById("statementAccept");
        document.getElementById("submitRequest").disabled = !checkBox.checked;
    }
</script>