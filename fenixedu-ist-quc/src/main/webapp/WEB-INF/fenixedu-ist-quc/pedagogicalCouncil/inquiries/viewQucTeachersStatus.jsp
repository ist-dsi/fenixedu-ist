<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<h2>QUC - Garantia da Qualidade das UC</h2>

<c:choose>
	<c:when test="${not empty teacherInquiry}">
		<h4>Lista de Docentes com comentários e/ou questões obrigatórias por responder:</h4>
		<p>
			<a href="${pageContext.request.contextPath}/view-quc-teachers-status/downloadReport/${teacherInquiry.externalId}">Ver ficheiro</a>
		</p>
		<p>
			<a href="${pageContext.request.contextPath}/view-quc-teachers-status/sendTeachersMail/${teacherInquiry.externalId}">Enviar e-mail de aviso aos docentes</a>
		</p>
	</c:when>
	<c:otherwise>
		<p>O inquérito ao Docente encontra-se fechado.</p>
	</c:otherwise>
</c:choose>




