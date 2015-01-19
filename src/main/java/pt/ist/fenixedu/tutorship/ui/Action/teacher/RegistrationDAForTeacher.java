/**
 * Copyright © 2011 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Tutorship.
 *
 * FenixEdu Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.ui.Action.teacher;

import org.fenixedu.academic.ui.struts.action.administrativeOffice.student.RegistrationDA;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;

@Mapping(module = "teacher", path = "/registration", functionality = TutorshipSummaryDA.class)
@Forwards({
        @Forward(name = "/student/curriculum/viewRegistrationCurriculum.jsp", path = "view-registration-curriculum"),
        @Forward(name = "/student/curriculum/chooseCycleForViewRegistrationCurriculum.jsp",
                path = "chooseCycleForViewRegistrationCurriculum") })
public class RegistrationDAForTeacher extends RegistrationDA {
}