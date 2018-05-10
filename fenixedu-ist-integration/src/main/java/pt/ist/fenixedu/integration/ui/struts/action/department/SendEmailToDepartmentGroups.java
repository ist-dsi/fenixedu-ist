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
package pt.ist.fenixedu.integration.ui.struts.action.department;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.organizationalStructure.DepartmentUnit;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.organizationalStructure.ScientificAreaUnit;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.academic.ui.struts.action.departmentMember.DepartmentMemberApp.DepartmentMemberMessagingApp;
import org.fenixedu.academic.ui.struts.action.messaging.UnitMailSenderAction;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;
import org.fenixedu.messaging.core.ui.MessageBean;
import org.fenixedu.messaging.core.ui.MessagingUtils;
import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.contracts.domain.accessControl.DepartmentPresidentStrategy;
import pt.ist.fenixedu.contracts.domain.organizationalStructure.Contract;
import pt.ist.fenixedu.contracts.domain.organizationalStructure.EmployeeContract;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

@StrutsFunctionality(app = DepartmentMemberMessagingApp.class, path = "send-email-to-department-groups",
        titleKey = "label.sendEmailToGroups")
@Mapping(module = "departmentMember", path = "/sendEmailToDepartmentGroups")
@Forwards(@Forward(name = "chooseUnit", path = "/departmentMember/chooseUnit.jsp"))
public class SendEmailToDepartmentGroups extends UnitMailSenderAction {

    @EntryPoint
    public ActionForward chooseUnit(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        final Unit unit = getUnit(request);
        if (unit != null) {
            return prepare(mapping, actionForm, request, response);
        }
        Set<Unit> units = new TreeSet<>(Party.COMPARATOR_BY_NAME);
        Unit departmentUnit = getDepartment();
        if (departmentUnit != null) {
            units.add(departmentUnit);

            for (Unit subUnit : departmentUnit.getAllSubUnits()) {
                if (subUnit.isScientificAreaUnit()) {
                    ScientificAreaUnit scientificAreaUnit = (ScientificAreaUnit) subUnit;
                    if (isCurrentUserMemberOfScientificArea(scientificAreaUnit)) {
                        units.add(scientificAreaUnit);
                    }
                }
            }
        }

        if (units.size() == 1) {
            request.setAttribute("unitId", departmentUnit.getExternalId());
            return prepare(mapping, actionForm, request, response);
        }

        request.setAttribute("units", units);
        return mapping.findForward("chooseUnit");
    }

    private static boolean isCurrentUserMemberOfScientificArea(ScientificAreaUnit scientificAreaUnit) {
        for (Contract contract : EmployeeContract.getWorkingContracts(scientificAreaUnit)) {
            if (contract.getPerson().equals(AccessControl.getPerson())) {
                return true;
            }
        }
        return false;
    }

    private Unit getDepartment() {
        final Person person = AccessControl.getPerson();
        final Teacher teacher = person.getTeacher();
        if (teacher != null) {
            return teacher.getDepartment().getDepartmentUnit();
        }
        final Employee employee = person.getEmployee();
        if (employee != null) {
            return employee.getCurrentWorkingPlace().getDepartmentUnit();
        }
        return null;
    }

    @Override
    public ActionForward prepare(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        MessageBean messageBean = new MessageBean();
        messageBean.setSender(getUnit(request).getSender());
        return MessagingUtils.redirectToNewMessage(request, response, messageBean);
    }

    private boolean userOfficialSender(final Unit unit, final org.fenixedu.messaging.core.domain.Sender unitSender) {
        if (unit instanceof DepartmentUnit) {
            final DepartmentUnit departmentUnit = (DepartmentUnit) unit;
            final Department department = departmentUnit.getDepartment();
            return DepartmentPresidentStrategy.isCurrentUserCurrentDepartmentPresident(department) && unitSender != null;
        }
        return false;
    }


}
