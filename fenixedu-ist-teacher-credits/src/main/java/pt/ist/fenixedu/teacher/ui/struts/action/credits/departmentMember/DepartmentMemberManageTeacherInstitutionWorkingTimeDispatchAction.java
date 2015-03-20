/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Credits.
 *
 * FenixEdu IST Teacher Credits is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Credits is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Credits.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.teacher.ui.struts.action.credits.departmentMember;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.ui.struts.config.FenixDomainExceptionHandler;
import org.fenixedu.bennu.struts.annotations.ExceptionHandling;
import org.fenixedu.bennu.struts.annotations.Exceptions;
import org.fenixedu.bennu.struts.annotations.Mapping;

import pt.ist.fenixedu.teacher.ui.struts.action.credits.ManageTeacherInstitutionWorkingTimeDispatchAction;

@Mapping(module = "departmentMember", path = "/institutionWorkingTimeManagement",
        input = "/institutionWorkingTimeManagement.do?method=prepareEdit&page=0",
        functionality = DepartmentMemberViewTeacherCreditsDA.class)
@Exceptions({ @ExceptionHandling(type = DomainException.class, handler = FenixDomainExceptionHandler.class, scope = "request") })
public class DepartmentMemberManageTeacherInstitutionWorkingTimeDispatchAction extends
        ManageTeacherInstitutionWorkingTimeDispatchAction {
}
