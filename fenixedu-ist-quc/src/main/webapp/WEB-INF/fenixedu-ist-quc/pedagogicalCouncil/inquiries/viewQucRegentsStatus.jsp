<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<h2>QUC - Envio de Email de Aviso a Regentes</h2>

<c:choose>
    <c:when test="${not empty regentInquiry}">
        <h4>Lista de Regentes com comentários e/ou questões obrigatórias por responder:</h4>
        <p>
            <a href="${pageContext.request.contextPath}/view-quc-regents-status/downloadReport/${regentInquiry.externalId}">Ver ficheiro</a>
        </p>
        <p>
            <a href="${pageContext.request.contextPath}/view-quc-regents-status/sendRegentsMail/${regentInquiry.externalId}">Enviar e-mail de aviso aos regentes</a>
        </p>
    </c:when>
    <c:otherwise>
        <p>O inquérito ao Regente encontra-se fechado.</p>
    </c:otherwise>
</c:choose>