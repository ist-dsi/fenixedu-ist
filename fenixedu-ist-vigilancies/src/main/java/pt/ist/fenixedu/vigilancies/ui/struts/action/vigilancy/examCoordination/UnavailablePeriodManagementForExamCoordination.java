/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Vigilancies.
 *
 * FenixEdu IST Vigilancies is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Vigilancies is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Vigilancies.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.vigilancies.ui.struts.action.vigilancy.examCoordination;

import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenixedu.vigilancies.ui.struts.action.vigilancy.UnavailablePeriodManagement;

@StrutsFunctionality(app = ExamCoordinationApplication.class, path = "unavailability",
        titleKey = "label.vigilancy.unavailablePeriodsShortLabel")
@Mapping(module = "examCoordination", path = "/vigilancy/unavailablePeriodManagement")
@Forwards({
        @Forward(name = "prepareAddPeriodToVigilant", path = "/examCoordinator/vigilancy/addUnavailablePeriodToVigilant.jsp"),
        @Forward(name = "manageUnavailablePeriodsOfVigilants",
                path = "/examCoordinator/vigilancy/manageGroupsUnavailablePeriods.jsp"),
        @Forward(name = "editPeriodOfVigilant", path = "/examCoordinator/vigilancy/editUnavailablePeriodOfVigilant.jsp") })
public class UnavailablePeriodManagementForExamCoordination extends UnavailablePeriodManagement {
}