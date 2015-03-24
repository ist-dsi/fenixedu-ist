<%--

    Copyright © ${project.inceptionYear} Instituto Superior Técnico

    This file is part of Fenix IST.

    Fenix IST is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Fenix IST is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with Fenix IST.  If not, see <http://www.gnu.org/licenses/>.

--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://fenix-ashes.ist.utl.pt/fenix-renderers" prefix="fr" %>
<c:set var="imagesContext" scope="session" value="${pageContext.request.contextPath}/static/images"/>
<c:set var="context" scope="session" value="${pageContext.request.contextPath}/unit/sites"/>
<h2 class="page-header"><spring:message code="unit.site.management.title"/></h2>

<c:choose>
    <c:when test="${not empty unitSites}">
        <c:forEach var="unitSite" items="${unitSites}">
            <div class="row">
                <div class="col-sm-8">
                    <p>${unitSite.name.content}</p>
                </div>

                <div class="btn-group col-sm-4">
                    <div class="pull-right">
                        <a class="btn btn-default" href="${unitSite.editUrl}">
                            <spring:message code="unit.site.admin"/>
                        </a>

                        <a href="#" data-toggle="modal" data-target="#editLayoutModal" class="btn btn-default"
                           role="button">
                            <spring:message code="unit.site.layout"/>
                        </a>

                        <a class="btn btn-default"
                           href="${pageContext.request.contextPath}/unit/sites/${unitSite.slug}">
                            <spring:message code="unit.site.banners"/>
                        </a>

                        <a class="btn btn-primary" href="${unitSite.fullUrl}" target="_blank">
                            <spring:message code="unit.site.link"/>
                        </a>
                    </div>
                </div>
            </div>
            <hr/>
            <!-- Modal panel for deleting an banner -->
            <div class="modal fade" id="editLayoutModal" tabindex="-1" role="dialog" aria-hidden="true">
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
                        <form id="layout-form" method="post" action="${context}/${unitSite.slug}/layout">
                            <div class="modal-body" id="site-layout" slug="${unitSite.slug}">
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
                                <button type="button" data-dismiss="modal" class="btn btn-default">
                                    <spring:message code="action.cancel"/>
                                </button>
                                <button type="submit" class="btn btn-primary">
                                    <spring:message code="action.save"/>
                                </button>
                            </div>
                        </form>

                    </div>
                </div>
            </div>
            <script>
                $('#site-layout[slug=${unitSite.slug}] a').each(function () {
                    var el = $(this);
                    el.click(function () {
                        $('#site-layout[slug=${unitSite.slug}] a').removeClass('active');
                        el.addClass('active');
                        $('#selected-layout').val(el.attr('layout'));
                    })
                });

                $('#site-layout[slug=${unitSite.slug}] a[layout=${unitSite.initialPage.template.type}]').addClass('active');
            </script>
        </c:forEach>
    </c:when>
    <c:otherwise>
        <h4><i><spring:message code="label.unit.site.emptySites"/></i></h4>
    </c:otherwise>


</c:choose>

<style>
    #site-layout a.active > img {
        border: #101010 solid medium;
        padding: 2px;
    }
</style>

${portal.toolkit()}
