<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<h2>QUC - Envio de Email de Aviso a Regentes</h2>

<div>
    Semestre do Inquérito QUC: <c:out value="${executionSemesterName}"/>
</div>
<form action="${pageContext.request.contextPath}/view-quc-regents-status/sendRegentsMail" method="post">
    ${csrf.field()}
    <input type="hidden" id="regentInquiry" name="regentInquiry" value="${regentInquiry.externalId}"/>
    <div>
        <label for="replyEndDate">Escolha a data de fim do novo prazo de resposta:</label>
        <input type="date" id="replyEndDate" name="replyEndDate" required pattern="[0-9]{4}-[0-9]{2}-[0-9]{2}">
        <span class="validity"></span>
    </div>
    <div>
        <button class="btn btn-primary" type="submit">Enviar E-mail aos Regentes</button>
    </div>
</form>