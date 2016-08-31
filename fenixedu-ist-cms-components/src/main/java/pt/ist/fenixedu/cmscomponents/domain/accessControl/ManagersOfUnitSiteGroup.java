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
package pt.ist.fenixedu.cmscomponents.domain.accessControl;

import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.accessControl.FenixGroup;
import org.fenixedu.bennu.core.annotation.GroupArgument;
import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.cms.domain.DefaultRoles;
import org.fenixedu.cms.domain.Role;
import org.fenixedu.cms.domain.Site;
import org.joda.time.DateTime;

import com.google.common.base.Objects;

@GroupOperator("siteManagers")
public class ManagersOfUnitSiteGroup extends FenixGroup {
    private static final long serialVersionUID = 1045771762725375319L;

    @GroupArgument
    private Site site;

    private ManagersOfUnitSiteGroup() {
        super();
    }

    private ManagersOfUnitSiteGroup(Site site) {
        this();
        this.site = site;
    }

    public static ManagersOfUnitSiteGroup get(Site site) {
        return new ManagersOfUnitSiteGroup(site);
    }

    @Override
    public String[] getPresentationNameKeyArgs() {
        return new String[] { site.getName().getContent() };
    }

    @Override
    public Set<User> getMembers() {
        return getManagers(DateTime.now());
    }

    @Override
    public Set<User> getMembers(DateTime when) {
        return getManagers(when);
    }

    @Override
    public boolean isMember(User user) {
        return getManagers(DateTime.now()).contains(user);
    }

    @Override
    public boolean isMember(User user, DateTime when) {
        return getManagers(when).contains(user);
    }

    @Override
    public PersistentGroup toPersistentGroup() {
        return PersistentManagersOfUnitSiteGroup.getInstance(site);
    }

    private Set<User> getManagers(DateTime when) {
        for (Role role : site.getRolesSet()) {
            if (role.getRoleTemplate().equals(DefaultRoles.getInstance().getAdminRole())) {
                return role.getGroup().getMembers(when);
            }
        }
        return new HashSet<User>();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ManagersOfUnitSiteGroup) {
            return Objects.equal(site, ((ManagersOfUnitSiteGroup) object).site);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(site);
    }

}