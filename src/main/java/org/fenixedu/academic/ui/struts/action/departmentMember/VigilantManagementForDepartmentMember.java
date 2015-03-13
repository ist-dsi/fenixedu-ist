/**
 * Copyright © 2011 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Exam Vigilancies.
 *
 * FenixEdu Exam Vigilancies is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Exam Vigilancies is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Exam Vigilancies.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.ui.struts.action.departmentMember;

import org.fenixedu.academic.ui.struts.action.departmentMember.DepartmentMemberApp.DepartmentMemberDepartmentApp;
import org.fenixedu.academic.ui.struts.action.vigilancy.VigilantManagement;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

@StrutsFunctionality(app = DepartmentMemberDepartmentApp.class, path = "vigilancies",
        titleKey = "label.navheader.person.vigilant", bundle = "VigilancyResources")
@Mapping(module = "departmentMember", path = "/vigilancy/vigilantManagement")
@Forwards({ @Forward(name = "displayConvokeMap", path = "/departmentMember/vigilancy/manageVigilant.jsp"),
        @Forward(name = "showReport", path = "/departmentMember/vigilancy/showWrittenEvaluationReport.jsp") })
public class VigilantManagementForDepartmentMember extends VigilantManagement {
}