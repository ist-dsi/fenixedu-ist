<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ page trimDirectiveWhitespaces="true" %>

<link rel="stylesheet" type="text/css" media="screen" href="<%= request.getContextPath() %>/CSS/accounting.css"/>

${portal.toolkit()}

<spring:url value="/sap-search/search" var="searchUrl"/>

<div class="container-fluid">
    <header>
        <div class="row">
            <div class="col-md-10">
                <h3><spring:message code="title.sap.search"/></h3>
            </div>
        </div>
        <div class="row">
            <c:if test="${not empty sizeWarning}">
                <section>
                    <ul class="nobullet list6">
                        <li><span class="error0"><c:out value="${sizeWarning}"/></span></li>
                    </ul>
                </section>
            </c:if>
        </div>
    </header>
    <div class="row">
        <form method="get" action="<%= request.getContextPath() %>/sap-search/search" class="form-horizontal">
            ${csrf.field()}
            <div class="form-group">
                <label class="control-label col-sm-2"><spring:message code="label.sap.search.number"/></label>
                <div class="col-sm-10">
                    <input id="sapNumber" name="sapNumber" value="${sapNumber}" required class="form-inline"/>
                </div>
            </div>
            <div class="form-group">
                <div class="col-sm-offset-1 col-sm-4">
                    <button class="btn btn-primary" type="submit">
                        <spring:message code="label.submit"/>
                    </button>
                </div>
            </div>
        </form>
    </div>
</div>