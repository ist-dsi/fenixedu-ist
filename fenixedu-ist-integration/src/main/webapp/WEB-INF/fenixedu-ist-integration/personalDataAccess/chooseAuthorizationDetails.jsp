<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

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

<spring:url value="/authorize-personal-data-access/history" var="baseUrl"/>

<div class="page-header">
    <h2>
        <spring:message code="authorize.personal.data.access.title" />
    </h2>
    <a href="${baseUrl}"><spring:message code="authorize.personal.data.access.history" /></a>
</div>

<div>

    <h2>
        ${title}
    </h2>

    <div class="alert well">
        ${message}


        <div class="row">
            <div class="col-lg-12 text-left">
                <p><strong>Autoriza a cedência de dados acima descrita?</strong></p>
                <form action="${requestScope['javax.servlet.forward.request_uri']}" method="post">
                    ${csrf.field()}

                    <p>
                        <span style="line-height: 20px; vertical-align: bottom; margin-right: 55px;">
                            <input type="radio" name="allowAccess" value="true" onclick="removeDisabled()">
                            <spring:message code="authorize.personal.data.access.yes"/>
                        </span>
                        <span>
                            <input type="radio" name="allowAccess" value="false" onclick="removeDisabled()">
                            <spring:message code="authorize.personal.data.access.no"/>
                        </span>
                    </p>

                    <button id="form-submit-button" class="btn btn-primary disabled" type="submit">
                        <spring:message code="authorize.personal.data.access.submit"/>
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<script type="text/javascript">
    function removeDisabled() {
        $('#form-submit-button').removeClass('disabled');
    }
</script>