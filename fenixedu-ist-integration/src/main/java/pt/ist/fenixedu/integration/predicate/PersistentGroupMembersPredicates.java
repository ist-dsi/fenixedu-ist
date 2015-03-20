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
package pt.ist.fenixedu.integration.predicate;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.person.RoleType;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.academic.predicate.AccessControlPredicate;

import pt.ist.fenixedu.integration.domain.accessControl.PersistentGroupMembers;

public class PersistentGroupMembersPredicates {

    public static final AccessControlPredicate<PersistentGroupMembers> checkPermissionsToManagePersistentGroups =
            new AccessControlPredicate<PersistentGroupMembers>() {
                @Override
                public boolean evaluate(PersistentGroupMembers persistentGroupMembers) {
                    Person person = AccessControl.getPerson();
                    return RoleType.MANAGER.isMember(person.getUser()) || RoleType.RESEARCHER.isMember(person.getUser());
                }
            };
}
