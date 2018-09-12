<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<style>
    .alert.well {
        font-weight: 400;
        max-width: 700px;
    }

    .alert.well ul {
        margin: 16px 0;
        font-size: 16px;
    }

    .alert.well form p {
        margin: 24px 0;
    }

    .alert.well p {
        margin-bottom: 16px;
    }

    dd {
        margin-bottom: 5px;
    }

    #main-content-wrapper p {
        font-size: 16px;
    }


</style>

<spring:url value="/authorize-personal-data-access/history" var="historyUrl"/>
<spring:url value="/authorize-personal-data-access/review" var="baseUrl"/>

<div class="page-header">
    <h2>
        <spring:message code="authorize.personal.data.access.title" />
    </h2>
    <a href="${historyUrl}"><spring:message code="authorize.personal.data.access.history" /></a>
</div>
<c:if test="${not empty invocationMessage}">
    <div class="alert alert-info">
        <p style="font-weight: bold; font-size: medium;"> <c:out value="${invocationMessage}"/></p>
    </div>
</c:if>

<c:if test="${success}">
    <div class="success3" style="margin-top: 10px; margin-bottom: 10px;">
        <span><spring:message code="authorize.personal.data.access.saved" /></span>
    </div>
</c:if>

<div>
    
        <h2>
            ${santanderBankTitle}
        </h2>

        <div class="alert well">
            ${santanderBankMessage}


            <div class="row">
                <div class="col-lg-12 text-left">
                    <p><strong>Autoriza a cedência de dados acima descrita?</strong></p>
                    <form action="${baseUrl}/santander-bank" method="post">
                        ${csrf.field()}
                        <p>
                            <span style="line-height: 20px; vertical-align: bottom; margin-right: 55px;">
                                <input type="radio" name="allowSantanderBankAccess" value="true" onclick="removeSantanderDisabled()" <c:if test="${allowSantanderBankAccess}">checked</c:if>>
                                <spring:message code="authorize.personal.data.access.yes"/>
                            </span>
                            <span>
                                <input type="radio" name="allowSantanderBankAccess" value="false" onclick="removeSantanderDisabled()" <c:if test="${!allowSantanderBankAccess}">checked</c:if>>
                                <spring:message code="authorize.personal.data.access.no"/>
                            </span>
                        </p>

                        <button id="santander-form-submit-button" class="btn btn-primary disabled" type="submit">
                            <spring:message code="authorize.personal.data.access.submit"/>
                        </button>
                    </form>
                </div>
            </div>
        </div>

        <h2>
            ${cgdBankTitle}
        </h2>

        <div class="alert well">
            ${cgdBankMessage}


            <div class="row">
                <div class="col-lg-12 text-left">
                    <p><strong>Autoriza a cedência de dados acima descrita?</strong></p>

                    <form action="${baseUrl}/cgd-bank" method="post">
                        ${csrf.field()}
                        <p>
                            <span style="line-height: 20px; vertical-align: bottom; margin-right: 55px;">
                                <input type="radio" name="allowCgdBankAccess" value="true" onclick="removeCgdDisabled()" <c:if test="${allowCgdBankAccess}">checked</c:if>>
                                <spring:message code="authorize.personal.data.access.yes"/>
                            </span>
                            <span>
                                <input type="radio" name="allowCgdBankAccess" value="false" onclick="removeCgdDisabled()" <c:if test="${!allowCgdBankAccess}">checked</c:if>>
                                <spring:message code="authorize.personal.data.access.no"/>
                            </span>
                        </p>

                            <button id="cgd-form-submit-button" class="btn btn-primary disabled" type="submit">
                                <spring:message code="authorize.personal.data.access.submit"/>
                            </button>
                    </form>
                </div>
            </div>
        </div>

        <h2>
            ${bpiBankTitle}
        </h2>

        <div class="alert well">
            ${bpiBankMessage}


            <div class="row">
                <div class="col-lg-12 text-left">
                    <p><strong>Autoriza a cedência de dados acima descrita?</strong></p>

                    <form action="${baseUrl}/bpi-bank" method="post">
                        ${csrf.field()}
                        <p>
                            <span style="line-height: 20px; vertical-align: bottom; margin-right: 55px;">
                                <input type="radio" name="allowBpiBankAccess" value="true" onclick="removeBpiDisabled()" <c:if test="${allowBpiBankAccess}">checked</c:if>>
                                <spring:message code="authorize.personal.data.access.yes"/>
                            </span>
                            <span>
                                <input type="radio" name="allowBpiBankAccess" value="false" onclick="removeBpiDisabled()" <c:if test="${!allowBpiBankAccess}">checked</c:if>>
                                <spring:message code="authorize.personal.data.access.no"/>
                            </span>
                        </p>

                        <button id="bpi-form-submit-button" class="btn btn-primary disabled" type="submit">
                            <spring:message code="authorize.personal.data.access.submit"/>
                        </button>
                    </form>
                </div>
            </div>
        </div>
</div>

<script type="text/javascript">
    function removeSantanderDisabled() {
        $('#santander-form-submit-button').removeClass('disabled');
    }
    function removeCgdDisabled() {
        $('#cgd-form-submit-button').removeClass('disabled');
    }
    function removeBpiDisabled() {
        $('#bpi-form-submit-button').removeClass('disabled');
    }
</script>