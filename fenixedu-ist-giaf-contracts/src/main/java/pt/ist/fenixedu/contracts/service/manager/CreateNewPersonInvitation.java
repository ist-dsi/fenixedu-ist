/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Contracts.
 *
 * FenixEdu IST GIAF Contracts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Contracts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Contracts.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.contracts.service.manager;

import pt.ist.fenixedu.contracts.domain.organizationalStructure.Invitation;
import pt.ist.fenixedu.contracts.dto.InvitedPersonBean;
import pt.ist.fenixframework.Atomic;

public class CreateNewPersonInvitation {

    @Atomic
    public static void run(InvitedPersonBean bean) {
        new Invitation(bean.getInvitedPerson(), bean.getUnit(), bean.getResponsible(), bean.getBegin(), bean.getEnd());
    }
}