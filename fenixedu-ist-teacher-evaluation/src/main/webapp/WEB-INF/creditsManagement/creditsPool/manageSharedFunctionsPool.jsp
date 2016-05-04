<%@page contentType="text/html" %>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>


<div>
     	<c:forEach var="outputLine" items="${output}">
                <div class="error0"><c:out value="${outputLine}"/></div>
        </c:forEach>
</div>

<spring:url var="viewUrl" value="/sharedFunctionsPool"></spring:url>
<form:form role="form" modelAttribute="departmentCreditsBean" method="GET" class="form-horizontal" action="${viewUrl}">
        <div class="form-group">
                <label for="executionYear" class="col-sm-2 control-label">${fr:message('resources.ApplicationResources', 'label.executionYear')}:</label>
                <div class="col-sm-10">
                        <form:select path="executionYear" id="executionYear" class="form-control col-sm-11" required="required">
                    <form:options items="${executionYears}" itemLabel="name" itemValue="externalId"/>
                </form:select>
                </div>
        </div>
	<div class="form-group">
                <div class="col-sm-push-2 col-sm-10">
                        <button type="submit" class="btn btn-default" id="search"><spring:message code="label.submit" /></button>
                </div>
        </div>
</form:form>

<br/><br/>

<spring:url var="exportUrl" value="/sharedFunctionsPool/exportSharedFunctionsPool?executionYear=${departmentCreditsBean.executionYear.externalId}"></spring:url>
<spring:url var="uploadUrl" value="/sharedFunctionsPool/uploadSharedFunctionsPool"></spring:url>
<spring:eval expression="T(pt.ist.fenixedu.teacher.evaluation.domain.credits.AnnualCreditsState).getAnnualCreditsState(departmentCreditsBean.getExecutionYear())" var="annualCreditsState" />
<c:set var="canEditValues" value="${!annualCreditsState.getIsFinalCreditsCalculated() && !annualCreditsState.getIsCreditsClosed()}"></c:set>



 <div class="form-group">
        <div class="col-sm-2">
                <a class="btn btn-default" href="${exportUrl}"><spring:message code="label.export"/></a>
                <c:if test="${canEditValues}">
                        <a class="btn btn-default" href="#" data-toggle="modal" data-target="#uploadFile"><spring:message code="label.upload"/></a>
                        <div class="modal fade" id="uploadFile">
                            <div class="modal-dialog">
                                <div class="modal-content">
                                    <div class="modal-header">
                                        <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
                                                        <h4><spring:message code="label.upload"/></h4>
                                    </div>
                                     <form method="POST" class="form-horizontal" enctype="multipart/form-data" action="${uploadUrl}">
                                    <div class="modal-body">
                                        <p><spring:message code="label.upload"/></p>
                                                        <input type="hidden" name="executionYear" value="<c:out value='${departmentCreditsBean.executionYear.externalId}'/>"/>
                                                        <div class="form-group">
                                                                <div class="col-sm-12">
                                                                        <input type="file" name="file" id="file" class="form-control" required/>
                                                                </div>
                                                        </div>
                                    </div>
                                    <div class="modal-footer">
                                                <button class="btn btn-primary"><spring:message code="label.upload"/></button>
                                    <a class="btn btn-default" data-dismiss="modal"><spring:message code="label.cancel"/></a>
                                    </div>
                                    </form>
                                </div>
                            </div>
                        </div>
                </c:if>
        </div>
</div>

<h3><spring:message code="title.creditsManagement.sharedFunctionsPool"/> <c:out value="${departmentCreditsBean.executionYear.year}"/></h3>

<table class="table dataTable table-condensed">
        <tbody>
                <c:forEach var="sharedFunction" items="${sharedFunctions}" varStatus="status">
                        <tr>
                        		<td><c:out value="${sharedFunction.unit.acronym}"/></td>
                                <td><c:out value="${sharedFunction.unit.name}"/></td>
                                <td><c:out value="${sharedFunction.name}"/></td>
                                <td><c:out value="${sharedFunction.credits}"/></td>
                        </tr>
                </c:forEach>
        </tbody>
</table>



