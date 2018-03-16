/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST CMS Components.
 *
 * FenixEdu IST CMS Components is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST CMS Components is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST CMS Components.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.cmscomponents.ui.spring;

import java.util.Optional;

import com.google.common.base.MoreObjects;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.cms.domain.MenuItem;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.FenixFramework;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PagesAdminBean {
    private static final JsonParser PARSER = new JsonParser();
    private Group canViewGroup;
    private MenuItem menuItem;
    private MenuItem parent;
    private LocalizedString title;
    private LocalizedString body;
    private LocalizedString excerpt;
    private final Boolean visible;

    public PagesAdminBean(String json) {
        this(PARSER.parse(json).getAsJsonObject());
    }

    public PagesAdminBean(JsonObject jsonObj) {
        if (asString(jsonObj, "menuItemId").isPresent()) {
            this.menuItem = menuItem(asString(jsonObj, "menuItemId").get());
        }
        if (asString(jsonObj, "menuItemParentId").isPresent()) {
            this.parent = menuItem(asString(jsonObj, "menuItemParentId").get());
        }
        if (jsonObj.has("title") && jsonObj.get("title") != null && !jsonObj.get("title").isJsonNull()) {
            this.title = LocalizedString.fromJson(jsonObj.get("title"));
        }
        if (jsonObj.has("body") && jsonObj.get("body") != null && !jsonObj.get("body").isJsonNull()) {
            this.body = LocalizedString.fromJson(jsonObj.get("body"));
        }
        if (jsonObj.has("excerpt") && jsonObj.get("excerpt") != null && !jsonObj.get("excerpt").isJsonNull()) {
            this.excerpt = LocalizedString.fromJson(jsonObj.get("excerpt"));
        }
        if (asString(jsonObj, "canViewGroupIndex").isPresent()) {
            Integer.parseInt(asString(jsonObj, "canViewGroupIndex").get());
            this.canViewGroup = executionCourseGroup(Integer.parseInt(asString(jsonObj, "canViewGroupIndex").get()));
        }
        this.visible = jsonObj.has("visible") ? jsonObj.get("visible").getAsBoolean() : false;
    }

    public MenuItem getMenuItem() {
        return menuItem;
    }

    public MenuItem getParent() {
        return parent;
    }

    public LocalizedString getTitle() {
        return title;
    }

    public LocalizedString getBody() {
        return body;
    }

    public LocalizedString getExcerpt() {
        return excerpt;
    }

    public Group getCanViewGroup() {
        return this.canViewGroup;
    }

    protected static Optional<String> asString(JsonObject jsonObject, String field) {
        if (jsonObject.has(field)) {
            if (jsonObject.get(field) != null && !jsonObject.isJsonNull() && jsonObject.get(field).isJsonPrimitive()
                    && !"null".equals(jsonObject.get(field).getAsString())) {
                return Optional.ofNullable(jsonObject.get(field).getAsString());
            }
        }
        return Optional.empty();
    }

    protected static MenuItem menuItem(String menuItemId) {
        return Strings.isNullOrEmpty(menuItemId) ? null : FenixFramework.getDomainObject(menuItemId);
    }

    private Group executionCourseGroup(int canViewGroupIndex) {
        return PagesAdminService.permissionGroups(menuItem.getPage().getSite()).get(canViewGroupIndex);
    }

    public Boolean isVisible() {
        return visible;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("menuItem", menuItem).add("parent", parent).add("title", title).add("body", body)
                .toString();
    }

}