/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.ui.renderers.providers;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.groups.Group;

import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixedu.integration.domain.accessControl.MembersLinkGroup;
import pt.ist.fenixedu.integration.domain.accessControl.PersistentGroupMembers;
import pt.ist.fenixedu.integration.ui.struts.action.research.researchUnit.UnitFileBean;

public class GroupsForUnitFiles implements DataProvider {

    private Comparator<Group> COMPARATOR_BY_NAME = new Comparator<Group>() {
        @Override
        public int compare(final Group g1, final Group g2) {
            return g1.toString().compareTo(g2.toString());
        }
    };

    @Override
    public Converter getConverter() {
        return null;
    }

    @Override
    public Object provide(Object source, Object currentValue) {
        Set<Group> groups = new TreeSet<Group>(COMPARATOR_BY_NAME);
        Unit unit = ((UnitFileBean) source).getUnit();
        groups.addAll(unit.getGroups());
        for (final PersistentGroupMembers persistentMembers : unit.getPersistentGroupsSet()) {
            groups.add(MembersLinkGroup.get(persistentMembers));
        }
//        groups.addAll(((UnitFileBean) source).getGroups());
        return groups;
    }
}
