<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

<div class="page-header">
    <h2>
        <spring:message code="authorize.personal.data.access.title" />
    </h2>
</div>


<div class="success3" style="margin-top: 10px; margin-bottom: 10px;">
    <span><spring:message code="authorize.personal.data.access.saved" /></span>
</div>

<spring:url value="/authorize-personal-data-access" var="baseUrl"/>
<a href="${baseUrl}">« <spring:message code="label.back" /></a>
