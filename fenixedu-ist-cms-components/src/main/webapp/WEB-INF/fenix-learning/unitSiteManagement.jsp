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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>

<c:set var="context" scope="session"
       value="${pageContext.request.contextPath}/unit/sites/${unitSite.slug}"/>

${portal.toolkit()}

<h2 class="page-header">
    <spring:message code="unit.site.management.title"/>
    <div class="button-group pull-right">
        <a href="#" data-toggle="modal" data-target="#createModal" class="btn btn-default" role="button">
            <spring:message code="action.create"/>
        </a>
    </div>
</h2>

<c:choose>
    <c:when test="${not empty banners}">
        <c:forEach var="banner" items="${banners}">
            <div class="row">
                <div class="col-md-8">
                    <h5>${banner.name.content}
                        <small>${banner.post.creationDate}</small>
                    </h5>
                </div>
                <div class="col-md-4">
                    <div class="btn-group pull-right">
                        <button type="button" class="btn btn-default" data-toggle="modal"
                                data-target="#editModal-${banner.post.slug}">
                            <spring:message code="action.edit"/>
                        </button>
                        <button class="btn btn-danger"
                                onclick="showDeleteConfirmation('${banner.post.slug}');">
                            <spring:message code="action.delete"/>
                        </button>
                    </div>
                    </div>
            </div>
            <hr/>

            <!-- Modal panel for banner creation -->
            <div class="modal fade" id="editModal-${banner.post.slug}" tabindex="-1" role="dialog" aria-hidden="true">
                <div class="modal-dialog modal-lg">
                    <form method="post" class="form-horizontal" action="${context}/${banner.post.slug}/update"
                          enctype="multipart/form-data">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal">
                                    <span aria-hidden="true">&times;</span>
                                    <span class="sr-only"><spring:message code="action.cancel"/></span>
                                </button>
                                <h4>${banner.name.content}</h4>
                            </div>

                            <div class="modal-body">
                                <div class="form-group">
                                    <label for="name" class="control-label col-sm-3">Name</label>

                                    <div class="col-sm-9">
                                        <input bennu-localized-string required-any type="text" name="name"
                                               placeholder="<spring:message code='label.name'/>"
                                               value='${banner.name.json()}'/>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <label class="control-label col-sm-3"><spring:message
                                            code="label.unit.site.introduction.show"/> </label>

                                    <div class="col-sm-9">
                                        <input type="checkbox" name="showIntroduction"
                                            ${not empty banner.showIntroduction and banner.showIntroduction ? "checked='checked'" : ""} />
                                    </div>
                                </div>

                                <div class="form-group">
                                    <label class="control-label col-sm-3"><spring:message
                                            code="label.unit.site.announcements.show"/> </label>

                                    <div class="col-sm-9">
                                        <input type="checkbox" name="showAnnouncements"
                                            ${not empty banner.showAnnouncements and banner.showAnnouncements ? "checked='checked'" : ""} />
                                    </div>
                                </div>

                                <div class="form-group">
                                    <label class="control-label col-sm-3"><spring:message
                                            code='label.unit.site.events.show'/></label>

                                    <div class="col-sm-9">
                                        <input type="checkbox" name="showEvents"
                                            ${not empty banner.showEvents and banner.showEvents ? "checked='checked'" : ""} />
                                    </div>
                                </div>

                                <div class="form-group">
                                    <label class="control-label col-sm-3"><spring:message
                                            code="label.unit.site.image.show"/></label>

                                    <div class="col-sm-9">
                                        <input type="checkbox"
                                               name="showBanner"
                                            ${not empty banner.showBanner and banner.showBanner ? "checked='checked'" : ""} />
                                    </div>
                                </div>

                                <div class="form-group">
                                    <label class="control-label col-sm-3">
                                        <spring:message code="label.unit.site.banner.color"/>
                                    </label>

                                    <div class="col-sm-9">
                                        <input type="color" name="color"
                                               value="${not empty banner.color ? banner.color : '#ffffff'}"/>
                                    </div>
                                </div>

                                <div class="form-group">
                                    <label class="control-label col-sm-3">
                                        <spring:message code="label.unit.site.banner.image"/>
                                    </label>

                                    <div class="col-sm-3">
                                        <c:if test="${not empty banner.mainImageUrl}">
                                            <img src="${banner.mainImageUrl}" class="img-responsive"
                                                 width="100" height="200"
                                                 alt="<spring:message code='label.unit.site.banner.image' />"/>
                                        </c:if>
                                    </div>

                                    <div class="col-sm-6">
                                        <input type="file" name="mainImage"/>
                                    </div>
                                </div>

                            </div>
                            <div class="modal-footer">
                                <button type="button" data-dismiss="modal" class="btn btn-default">
                                    <spring:message code="action.cancel"/>
                                </button>
                                <button type="submit" class="btn btn-primary">
                                    <spring:message code="action.save"/>
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </c:forEach>

    </c:when>

    <c:otherwise>
        <h4><i><spring:message code="label.unit.site.banners.empty"/></i></h4>
    </c:otherwise>

</c:choose>

<!-- Modal panel for banner creation -->
<div class="modal fade" id="createModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">
                        <span aria-hidden="true">&times;</span><span class="sr-only">
                            <spring:message code="action.cancel"/>
                        </span>
                    </button>
                    <h4><spring:message code="action.create"/></h4>
                </div>
                <form method="post" class="form-horizontal" action="${context}/create" enctype="multipart/form-data">
                    <div class="modal-body">
                        <div class="form-group">
                            <label for="name" class="control-label col-sm-3"><spring:message code="label.name"/></label>

                            <div class="col-sm-9">
                                <input bennu-localized-string required-any type="text" name="name" id="name"
                                       placeholder="<spring:message code='label.name'/>"/>
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="control-label col-sm-3">
                                <spring:message code="label.unit.site.introduction.show"/>
                            </label>

                            <div class="col-sm-9">
                                <input type="checkbox" name="showIntroduction"
                                ${not empty showIntroduction and showIntroduction ? "checked='checked'" : ""} />
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="control-label col-sm-3">
                                <spring:message code="label.unit.site.announcements.show"/>
                            </label>

                            <div class="col-sm-9">
                                <input type="checkbox" name="showAnnouncements"
                                ${not empty showAnnouncements and showAnnouncements ? "checked='checked'" : ""} />
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="control-label col-sm-3">
                                <spring:message code="label.unit.site.events.show"/>
                            </label>

                            <div class="col-sm-9">
                                <input type="checkbox" name="showEvents"
                                ${not empty showEvents and showEvents ? "checked='checked'" : ""} />
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="control-label col-sm-3">
                                <spring:message code="label.unit.site.image.show"/>
                            </label>

                            <div class="col-sm-9">
                                <input type="checkbox" name="showBanner"
                                ${not empty showBanner and showBanner ? "checked='checked'" : ""} />
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="control-label col-sm-3">
                                <spring:message code="label.unit.site.banner.color"/>
                            </label>

                            <div class="col-sm-9">
                                <input type="color" name="color"/>
                            </div>
                        </div>

                        <div class="form-group">
                            <label class="control-label col-sm-3">
                                <spring:message code="label.unit.site.banner.image"/>
                            </label>

                            <div class="col-sm-9">
                                <input type="file" name="mainImage"/>
                            </div>
                        </div>

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

<!-- Modal panel for deleting an banner -->
<div class="modal fade" id="confirmDeleteModal" tabindex="-1" role="dialog" aria-hidden="true">
    <form id="deleteForm" method="post" action="#">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal">
                        <span aria-hidden="true">&times;</span>
                            <span class="sr-only">
                                <spring:message code="action.cancel"/>
                            </span>
                    </button>
                    <h4><spring:message code="action.delete"/></h4>
                </div>
                <div class="modal-body">
                    <p><spring:message code="label.unit.site.banner.delete.confirmation"/></p>
                </div>
                <div class="modal-footer">
                    <button type="button" data-dismiss="modal" class="btn btn-default">
                        <spring:message code="action.cancel"/>
                    </button>
                    <button type="submit" class="btn btn-danger">
                        <spring:message code="action.delete"/>
                    </button>
                </div>
                </div>
            </div>
    </form>
</div>

<script>
    function showDeleteConfirmation(bannerSlug) {
        $('#deleteForm').attr('action', '${context}/' + bannerSlug + '/delete');
        $('#confirmDeleteModal').modal('show');
        return false;
    }
</script>

<style>
    .modal-backdrop {
        z-index: 900
    }
</style>