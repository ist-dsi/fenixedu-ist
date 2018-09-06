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


<div class="page-header">
    <h2>
        <spring:message code="authorize.personal.data.access.title" />
    </h2>
    <spring:url value="/authorize-personal-data-access/history" var="baseUrl"/>
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
                <form action="${requestScope['javax.servlet.forward.request_uri']}" method="post">
                    ${csrf.field()}
                    <p>
                        <button class="btn btn-primary" type="submit">
                            <spring:message code="label.take.consent" />
                        </button>
                    </p>
                </form>
            </div>
        </div>
    </div>

</div>