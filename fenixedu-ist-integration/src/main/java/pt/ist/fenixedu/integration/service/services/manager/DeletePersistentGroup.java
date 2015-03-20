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
package pt.ist.fenixedu.integration.service.services.manager;

import static org.fenixedu.academic.predicate.AccessControl.check;

import org.fenixedu.academic.predicate.RolePredicates;

import pt.ist.fenixedu.integration.domain.accessControl.PersistentGroupMembers;
import pt.ist.fenixframework.Atomic;

public class DeletePersistentGroup {

    @Atomic
    public static void run(PersistentGroupMembers groupMembers) {
        check(RolePredicates.MANAGER_PREDICATE);
        if (groupMembers != null) {
            groupMembers.delete();
        }
    }
}