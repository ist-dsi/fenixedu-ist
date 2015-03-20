/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Tutorship.
 *
 * FenixEdu IST Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.ui;

import org.fenixedu.bennu.struts.portal.StrutsApplication;

public class TutorshipApplications {

    @StrutsApplication(bundle = "PedagogicalCouncilResources", path = "tutorship", titleKey = "link.tutorship",
            accessGroup = "#tutorship", hint = "Tutorship")
    public static class TutorshipApp {
    }

    @StrutsApplication(bundle = "ApplicationResources", path = "tutor", titleKey = "link.teacher.tutor.operations",
            accessGroup = "activeTeachers | professors", hint = "Teacher")
    public static class TeacherTutorApp {
    }

}
