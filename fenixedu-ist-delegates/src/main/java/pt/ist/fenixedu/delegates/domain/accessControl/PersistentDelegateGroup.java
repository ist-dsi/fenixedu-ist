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

import java.util.Optional;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.bennu.core.groups.Group;

import com.google.common.base.Objects;

public class PersistentDelegateGroup extends PersistentDelegateGroup_Base {

    protected PersistentDelegateGroup(DegreeType degreeType, Degree degree, Boolean yearDelegate) {
        super();
        setDegreeType(degreeType);
        setDegree(degree);
        setYearDelegate(yearDelegate);
        if (degree != null) {
            setRootForFenixPredicate(null);
        }
    }

    @Override
    public Group toGroup() {
        if (getDegree() != null) {
            return DelegateGroup.get(getDegree(), getYearDelegate());
        }
        return DelegateGroup.get(getDegreeType(), getYearDelegate());
    }

    @Override
    protected void gc() {
        setDegreeType(null);
        setDegree(null);
        super.gc();
    }

    public static PersistentDelegateGroup getInstance(DegreeType degreeType, Degree degree, Boolean yearDelegate) {
        return singleton(() -> select(degreeType, degree, yearDelegate), () -> new PersistentDelegateGroup(degreeType, degree,
                yearDelegate));
    }

    private static Optional<PersistentDelegateGroup> select(DegreeType degreeType, Degree degree, Boolean yearDelegate) {
        if (degree != null) {
            return degree.getDelegatesGroupSet().stream().filter(g -> Objects.equal(g.getYearDelegate(), yearDelegate)).findAny();
        }
        if (degreeType != null) {
            return degreeType.getDelegatesGroupSet().stream().filter(g -> Objects.equal(g.getYearDelegate(), yearDelegate))
                    .findAny();
        }
        return filter(PersistentDelegateGroup.class).filter(g -> Objects.equal(g.getYearDelegate(), yearDelegate)).findAny();
    }
}
