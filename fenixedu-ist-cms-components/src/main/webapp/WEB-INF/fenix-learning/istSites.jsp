<%--

    Copyright © 2013 Instituto Superior Técnico

    This file is part of FenixEdu IST CMS Components.

    FenixEdu IST CMS Components is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FenixEdu IST CMS Components is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with FenixEdu IST CMS Components.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<c:set var="imagesContext" scope="session" value="${pageContext.request.contextPath}/static/images"/>
<c:set var="unitSitesContext" scope="session" value="${pageContext.request.contextPath}/unit/sites"/>

<h1><spring:message code="site.manage.title"/></h1>

<c:if test="${isManager}">
    <p>
        <a href="#" data-toggle="modal" data-target="#defaultSite" class="btn btn-default">Default site</a>
    </p>
</c:if>

<c:choose>
    <c:when test="${sites.size() == 0}">
        <em><spring:message code="site.manage.label.emptySites"/></em>
    </c:when>

    <c:otherwise>
        <table class="table table-striped">
            <thead>
            <tr>
                <th class="col-md-6"><spring:message code="site.manage.label.name"/></th>
                <th class="text-center"><spring:message code="site.manage.label.status"/></th>
                <th><spring:message code="site.manage.label.creationDate"/></th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="i" items="${sites}">
                <tr>
                    <td>
                        <c:choose>
                            <c:when test="${i.getInitialPage()!=null}">
                                <h5><a href="${i.getInitialPage().getAddress()}"
                                       target="_blank">${i.getName().getContent()}</a>
                                    <c:if test="${i.isDefault()}">
                                        <span class="label label-success"><spring:message
                                                code="site.manage.label.default"/></span>
                                    </c:if>
                                </h5>
                            </c:when>
                            <c:otherwise>
                                <h5><c:out value="${i.getName().getContent()}"/></h5>
                            </c:otherwise>
                        </c:choose>
                        <div>
                            <small>Url: <code>${i.baseUrl}</code></small>
                        </div>
                    </td>
                    <td class="text-center">
                        <c:choose>
                            <c:when test="${ i.published }">
                                <span class="label label-primary">Available</span>
                            </c:when>
                            <c:otherwise>
                                <span class="label label-default">Unavailable</span>
                            </c:otherwise>
                        </c:choose>
                        <c:if test="${i.getEmbedded()}">
                            <p><span class="label label-info">Embedded</span></p>
                        </c:if>
                    </td>
                    <td>${i.creationDate.toString('MMM dd, yyyy')}</td>
                    <td>
                        <div class="btn-group">
                            <c:if test="${i.getClass().name == 'pt.ist.fenixedu.cmscomponents.domain.unit.UnitSite'}">

                                <a href="#" data-toggle="modal" data-target="#editLayoutModal-${i.externalId}"
                                   class="btn btn-default" role="button">
                                    <spring:message code="unit.site.layout"/>
                                </a>

                                <a class="btn btn-default" href="${unitSitesContext}/${i.slug}">
                                    <spring:message code="unit.site.banners"/>
                                </a>

                            </c:if>
                            <a href="${i.editUrl}" class="btn btn-sm btn-default">
                                <spring:message code="action.manage"/>
                            </a>
                        </div>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        <c:if test="${numberOfPages != 1}">
            <div class="row">
                <div class="col-md-2 col-md-offset-5">
                    <ul class="pagination">
                        <li class="${currentPage <= 0 ? 'disabled' : 'active'}"><a
                                href="${unitSitesContext}/manage/${page - 1}">«</a></li>
                        <li class="disabled"><a href="#">${currentPage + 1} / ${numberOfPages}</a></li>
                        <li class="${currentPage + 1 >= numberOfPages ? 'disabled' : 'active'}"><a
                                href="${unitSitesContext}/manage/${page + 1}">»</a></li>
                    </ul>
                </div>
            </div>
        </c:if>
    </c:otherwise>
</c:choose>

<c:forEach var="i" items="${sites}">
    <c:if test="${i.getClass().name == 'pt.ist.fenixedu.cmscomponents.domain.unit.UnitSite'}">
        <!-- Modal panel for deleting an banner -->
        <div class="modal fade" id="editLayoutModal-${i.externalId}" tabindex="-1" role="dialog" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal">
                            <span aria-hidden="true">&times;</span>
                        <span class="sr-only">
                            <spring:message code="action.cancel"/>
                        </span>
                        </button>
                        <h4><spring:message code="title.unit.site.layout"/></h4>
                    </div>
                    <div class="modal-body" id="site-layout" slug="${i.slug}">
                        <center>
                            <p><spring:message code="label.unit.site.layout.instructions"/></p>
                            <input type="text" hidden="hidden" name="template" id="selected-layout"/>
                            <ul class="list-inline">
                                <li>
                                    <a href="#" layout='unitHomepageWithBannerIntro'>
                                        <img src="${imagesContext}/banner_intro.gif" class="img-responsive"
                                             width="100" height="200" alt="Banner main image"/>
                                    </a>
                                </li>
                                <li>
                                    <a href="#" layout='unitHomepageWithIntroBanner'>
                                        <img src="${imagesContext}/intro_banner.gif" class="img-responsive"
                                             width="100" height="200" alt="Banner main image"/>
                                    </a>
                                </li>
                                <li>
                                    <a href="#" layout='unitHomepageWithIntroFloat'>
                                        <img src="${imagesContext}/banner_intro_float.gif"
                                             class="img-responsive"
                                             width="100" height="200" alt="Banner main image"/>
                                    </a>
                                </li>
                            </ul>
                        </center>
                    </div>
                    <div class="modal-footer">
                        <button type="reset" data-dismiss="modal" class="btn btn-default">
                            <spring:message code="action.cancel"/>
                        </button>
                        <button type="button" data-dismiss="modal" class="btn btn-primary" id="change-layout-btn">
                            <spring:message code="action.save"/>
                        </button>
                    </div>
                </div>
            </div>
        </div>
        <script type="application/javascript">
            $(function () {
                $('#site-layout[slug=${i.slug}] a').each(function () {
                    var el = $(this);
                    el.click(function () {
                        $('#site-layout[slug=${i.slug}] a').removeClass('active');
                        el.addClass('active');
                    })
                });

                $('#site-layout[slug=${i.slug}] a[layout=<c:out value="${i.initialPage.template.type}]"/>"').addClass('active');
                
                $('#editLayoutModal-${i.externalId} #change-layout-btn').click(function(evt){
                    var selectedEl = $('#site-layout[slug=${i.slug}] a.active');
                    var selectedLayout = selectedEl.attr('layout');
                    $.post("${unitSitesContext}/${i.slug}/layout", { template: selectedLayout });
                });
            });
        </script>
    </c:if>
</c:forEach>


<div class="modal fade" id="defaultSite" tabindex="-1" role="dialog" aria-hidden="true">
    <form action="${unitSitesContext}/default" class="form-horizontal" method="post">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal"><span
                            aria-hidden="true">&times;</span><span
                            class="sr-only">Close</span></button>
                    <h4><spring:message code="action.set.default.site"/></h4>
                </div>
                <div class="modal-body">

                    <div class="form-group">
                        <label for="inputEmail3" class="col-sm-2 control-label"><spring:message
                                code="theme.site"/>:</label>

                        <div class="col-sm-10">
                            <select class="form-control" name="slug">
                                <option value="">-</option>
                                <c:forEach var="i" items="${sites}">
                                    <option value="${i.slug}"><c:out value="${i.name.content}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>

                </div>
                <div class="modal-footer">
                    <button type="submit" class="btn btn-primary"><spring:message code="label.save"/></button>
                </div>
            </div>
        </div>
    </form>
</div>

<style type="text/css">
    #site-layout a.active > img {
        border: #101010 solid medium;
        padding: 2px;
    }
</style>