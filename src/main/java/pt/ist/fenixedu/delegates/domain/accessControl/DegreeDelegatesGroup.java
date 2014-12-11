/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Academic.
 *
 * FenixEdu Academic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.domain.accessControl;

import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.accessControl.FenixGroup;
import org.fenixedu.bennu.core.annotation.GroupArgument;
import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.joda.time.DateTime;

import pt.ist.fenixedu.delegates.domain.student.Delegate;

import com.google.common.base.Objects;

@GroupOperator("degreeDelegates")
public class DegreeDelegatesGroup extends FenixGroup {
    private static final long serialVersionUID = 5431112068108722868L;

    @GroupArgument("")
    private Degree degree;

    private DegreeDelegatesGroup() {
        super();
    }

    private DegreeDelegatesGroup(Degree degree) {
        this();
        this.degree = degree;
    }

    public static DegreeDelegatesGroup get() {
        return new DegreeDelegatesGroup();
    }

    public static DegreeDelegatesGroup get(Degree degree) {
        return new DegreeDelegatesGroup(degree);
    }

    @Override
    public Set<User> getMembers() {
        Set<User> users = new HashSet<>();
        for (final Delegate delegate : Bennu.getInstance().getDelegatesSet()) {
            if (!delegate.isActive()) {
                continue;
            }
            User user = delegate.getUser();
            users.add(user);
        }
        return users;
    }

    @Override
    public Set<User> getMembers(DateTime when) {
        Set<User> users = new HashSet<>();
        for (final Delegate delegate : Bennu.getInstance().getDelegatesSet()) {
            if (!delegate.isActive(when)) {
                continue;
            }
            User user = delegate.getUser();
            users.add(user);
        }
        return users;
    }

    @Override
    public boolean isMember(User user) {
        return isMember(user, DateTime.now());
    }

    @Override
    public boolean isMember(User user, DateTime when) {
        if (degree.getDelegateSet().stream().map(d -> d.isActive(when) && d.getUser().equals(user))
                .reduce(false, (x, y) -> x || y)) {
            return true;
        }
        return false;
    }

    @Override
    public PersistentDegreeDelegatesGroup toPersistentGroup() {
        return PersistentDegreeDelegatesGroup.getInstance(degree);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DegreeDelegatesGroup) {
            return Objects.equal(degree, ((DegreeDelegatesGroup) object).degree);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(degree);
    }

}
