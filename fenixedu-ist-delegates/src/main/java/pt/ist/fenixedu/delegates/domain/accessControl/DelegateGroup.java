/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Delegates.
 *
 * FenixEdu IST Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.domain.accessControl;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.bennu.core.annotation.GroupArgument;
import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.CustomGroup;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;

import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.domain.student.YearDelegate;

import com.google.common.base.Objects;

@GroupOperator("delegate")
public class DelegateGroup extends CustomGroup {
    private static final long serialVersionUID = -7261567414029131275L;

    @GroupArgument("")
    private final Degree degree;

    @GroupArgument
    private final DegreeType degreeType;

    @GroupArgument
    private final Boolean yearDelegate;

    private DelegateGroup() {
        super();
        this.degree = null;
        this.yearDelegate = null;
        this.degreeType = null;
    }

    private DelegateGroup(DegreeType degreeType, Degree degree, Boolean yearDelegate) {
        super();
        this.yearDelegate = yearDelegate;
        this.degreeType = degreeType;
        this.degree = degree;
    }

    public static DelegateGroup get() {
        return new DelegateGroup();
    }

    public static DelegateGroup get(Degree degree) {
        return new DelegateGroup(null, degree, null);
    }

    public static DelegateGroup get(Boolean yearDelegate) {
        return new DelegateGroup(null, null, (yearDelegate == null || yearDelegate == false) ? null : yearDelegate);
    }

    public static DelegateGroup get(Degree degree, Boolean yearDelegate) {
        return new DelegateGroup(null, degree, (yearDelegate == null || yearDelegate == false) ? null : yearDelegate);
    }

    public static DelegateGroup get(DegreeType degreeType, Boolean yearDelegate) {
        return new DelegateGroup(degreeType, null, (yearDelegate == null || yearDelegate == false) ? null : yearDelegate);
    }

    @Override
    public String getPresentationName() {
        boolean yearDelegateValue = yearDelegate != null && yearDelegate.booleanValue();
        String[] args;
        String key;
        if (!yearDelegateValue && degree == null && degreeType == null) {
            key = "label.name.DelegateGroup";
            args = new String[0];
        } else {
            key = "label.name.DelegateGroup.specific";
            args = new String[2];
            if (yearDelegateValue) {
                args[0] = BundleUtil.getString("resources.FenixEduDelegatesResources", "label.name.DelegateGroup.year");
            } else {
                args[0] = BundleUtil.getString("resources.FenixEduDelegatesResources", "label.name.DelegateGroup.degree");
            }

            if (degree != null) {
                args[1] = degree.getPresentationName();
            } else if (degreeType != null) {
                args[1] = degreeType.getName().getContent();
            } else {
                args[1] = BundleUtil.getString("resources.FenixEduDelegatesResources", "label.name.DelegateGroup.allDegrees");
            }
        }
        return BundleUtil.getString("resources.FenixEduDelegatesResources", key, args);
    }

    @Override
    public Set<User> getMembers() {
        return getMembers(new DateTime());
    }

    @Override
    public Set<User> getMembers(DateTime when) {
        return filterMatches(Bennu.getInstance().getDelegatesSet().stream(), when).map(d -> d.getUser()).collect(
                Collectors.toSet());
    }

    @Override
    public boolean isMember(User user) {
        return isMember(user, new DateTime());
    }

    @Override
    public boolean isMember(User user, DateTime when) {
        return user != null && filterMatches(user.getDelegatesSet().stream(), when).findAny().isPresent();
    }

    private Stream<Delegate> filterMatches(Stream<Delegate> stream, DateTime when) {
        if (yearDelegate != null && yearDelegate.booleanValue()) {
            stream = stream.filter(d -> d instanceof YearDelegate);
        }
        if (degreeType != null) {
            stream = stream.filter(d -> d.getDegree().getDegreeType().equals(degreeType));
        }
        if (degree != null) {
            stream = stream.filter(d -> d.getDegree().equals(degree));
        }
        return stream.filter(d -> d.isActive(when));
    }

    @Override
    public PersistentGroup toPersistentGroup() {
        return PersistentDelegateGroup.getInstance(degreeType, degree, yearDelegate);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DelegateGroup) {
            DelegateGroup other = (DelegateGroup) object;
            return Objects.equal(degreeType, other.degreeType) && Objects.equal(degree, other.degree)
                    && Objects.equal(yearDelegate, other.yearDelegate);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(degreeType, degree, yearDelegate);
    }
}
