/**
 * Copyright © 2011 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Delegates.
 *
 * FenixEdu Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.domain.accessControl;

import java.util.Optional;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.bennu.core.groups.Group;

import com.google.common.base.Objects;

public class PersistentDelegateGroup extends PersistentDelegateGroup_Base {

    protected PersistentDelegateGroup(Degree degree, Boolean yearDelegate) {
        super();
        setDegree(degree);
        setYearDelegate(yearDelegate);
        if (degree != null) {
            setRootForFenixPredicate(null);
        }
    }

    @Override
    public Group toGroup() {
        return DelegateGroup.get(getDegree(), getYearDelegate());
    }

    @Override
    protected void gc() {
        setDegree(null);
        super.gc();
    }

    public static PersistentDelegateGroup getInstance(Degree degree, Boolean yearDelegate) {
        return singleton(() -> select(degree, yearDelegate), () -> new PersistentDelegateGroup(degree, yearDelegate));
    }

    private static Optional<PersistentDelegateGroup> select(Degree degree, Boolean yearDelegate) {
        if (degree != null) {
            return degree.getDelegatesGroupSet().stream().filter(g -> Objects.equal(g.getYearDelegate(), yearDelegate)).findAny();
        }
        return filter(PersistentDelegateGroup.class).filter(g -> Objects.equal(g.getYearDelegate(), yearDelegate)).findAny();
    }
}
