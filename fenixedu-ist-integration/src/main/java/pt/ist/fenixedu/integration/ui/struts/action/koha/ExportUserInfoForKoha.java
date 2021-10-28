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
package pt.ist.fenixedu.integration.ui.struts.action.koha;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.MobilePhone;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.Phone;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.organizationalStructure.DepartmentUnit;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcessState;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveGrantOwner;
import pt.ist.fenixedu.contracts.domain.util.CategoryType;
import pt.ist.fenixedu.integration.FenixEduIstIntegrationConfiguration;
import pt.ist.fenixedu.integration.ui.struts.action.externalServices.ExternalInterfaceDispatchAction;

import com.google.common.base.Strings;

@Mapping(module = "external", path = "/exportUserInfoForKoha", scope = "request", parameter = "method")
public class ExportUserInfoForKoha extends ExternalInterfaceDispatchAction {

    private boolean chackCredentials(final HttpServletRequest request) {
        final String username = (String) getFromRequest(request, "username");
        final String password = (String) getFromRequest(request, "password");
        final String usernameProp = FenixEduIstIntegrationConfiguration.getConfiguration().getExternalServicesKohaUsername();
        final String passwordProp = FenixEduIstIntegrationConfiguration.getConfiguration().getExternalServicesKohaPassword();

        return !Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password) && !Strings.isNullOrEmpty(usernameProp)
                && !Strings.isNullOrEmpty(passwordProp) && username.equals(usernameProp) && password.equals(passwordProp);
    }

    @Override
    public ActionForward execute(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request,
            final HttpServletResponse response) throws Exception {
        if (chackCredentials(request)) {
            super.execute(mapping, actionForm, request, response);
        } else {
            response.sendError(404, "Not authorized");
        }
        return null;
    }

    private ActionForward sendXls(final HttpServletResponse response, final Spreadsheet spreadsheet) throws IOException {
        final OutputStream stream = response.getOutputStream();
        response.setContentType("application/vnd.ms-access");
        response.setHeader("Content-disposition", "attachment; filename=list.xls");
        spreadsheet.exportToXLSSheet(stream);
        stream.close();
        return null;
    }

    public ActionForward getDegreeTypes(final ActionMapping mapping, final ActionForm actionForm,
            final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("DegreeTypes");
        spreadsheet.setHeader("*ID").setHeader("descrição");

        DegreeType.all().forEach(degreeType -> {
            final Row row = spreadsheet.addRow();
            row.setCell(degreeType.getExternalId()).setCell(degreeType.getName().getContent());
        });

        return sendXls(response, spreadsheet);
    }

    public ActionForward getDegrees(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request,
            final HttpServletResponse response) throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Degrees");
        spreadsheet.setHeader("*ID").setHeader("descrição").setHeader("abreviatura").setHeader("*grau");

        for (final Degree degree : Bennu.getInstance().getDegreesSet()) {
            final Row row = spreadsheet.addRow();
            row.setCell(degree.getExternalId()).setCell(degree.getPresentationName()).setCell(degree.getSigla())
                    .setCell(degree.getDegreeType().getExternalId());
        }

        return sendXls(response, spreadsheet);
    }

    public ActionForward getDepartments(final ActionMapping mapping, final ActionForm actionForm,
            final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Departments");
        spreadsheet.setHeader("*ID").setHeader("descrição");

        for (final Department department : Bennu.getInstance().getDepartmentsSet()) {
            final Row row = spreadsheet.addRow();
            row.setCell(department.getExternalId()).setCell(department.getName());
        }

        return sendXls(response, spreadsheet);
    }

    public ActionForward getTeachersAndResearchers(final ActionMapping mapping, final ActionForm actionForm,
            final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("TeachersAndResearchers");
        spreadsheet.setHeader("IST-ID").setHeader("*departamento").setHeader("nome").setHeader("email").setHeader("telefone")
                .setHeader("cgdCode");

        Stream.concat(
                Bennu.getInstance().getTeachersSet().stream().filter(teacher -> teacher.hasTeacherAuthorization())
                        .map(Teacher::getPerson),
                Bennu.getInstance().getEmployeesSet().stream().filter(ExportUserInfoForKoha::isResearcher)
                        .map(Employee::getPerson)).distinct().forEach(p -> addEmployeeInformation(spreadsheet, p));

        return sendXls(response, spreadsheet);
    }

    protected static boolean isResearcher(Employee employee) {
        return (employee.getPerson().getPersonProfessionalData() != null ? employee.getPerson().getPersonProfessionalData()
                .getCurrentPersonContractSituationByCategoryType(CategoryType.RESEARCHER) : null) != null;
    }

    public ActionForward getOfficialsNotTeachers(final ActionMapping mapping, final ActionForm actionForm,
            final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("OfficialsNotTeachers");
        spreadsheet.setHeader("IST-ID").setHeader("nome").setHeader("email").setHeader("telefone").setHeader("cgdCode");

        Bennu.getInstance().getEmployeesSet().stream()
            .filter(this::isEmployeeOrGrantOwner)
            .map(employee -> employee.getPerson())
            .forEach(p -> addEmployeeInformationOfficialsNotTeachers(spreadsheet, p));

        return sendXls(response, spreadsheet);
    }

    private boolean isEmployeeOrGrantOwner(final Employee employee) {
        return employee.isActive() || ActiveGrantOwner.isGrantOwner(employee);
    }

    private void addEmployeeInformation(final Spreadsheet spreadsheet, final Person person) {
        final Row row = spreadsheet.addRow();
        row.setCell(person.getUsername()).setCell(getWorkingDepartment(person)).setCell(person.getName())
                .setCell(getEmail(person)).setCell(getTelefone(person)).setCell(getCGDCode(person));
    }

    private void addEmployeeInformationOfficialsNotTeachers(final Spreadsheet spreadsheet, final Person person) {
        final Row row = spreadsheet.addRow();
        row.setCell(person.getUsername()).setCell(person.getName()).setCell(getEmail(person)).setCell(getTelefone(person))
                .setCell(getCGDCode(person));
    }

    public ActionForward getStudents(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request,
            final HttpServletResponse response) throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Students");
        spreadsheet.setHeader("IST-ID").setHeader("polo").setHeader("curso").setHeader("nome").setHeader("email")
                .setHeader("telefone").setHeader("cgdCode");

        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        final DateTime begin = executionYear.getBeginDateYearMonthDay().toDateTimeAtMidnight();
        final DateTime end = executionYear.getEndDateYearMonthDay().toDateTimeAtMidnight();;

        for (User user : Bennu.getInstance().getUserSet()) {
            Person person = user.getPerson();
            if (person != null && person.getStudent() != null) {
                final Student student = person.getStudent();
                final StudentCurricularPlan scp = findStudentCurricularPlan(student, begin, end);
                if (scp != null) {
                    final Row row = spreadsheet.addRow();
                    row.setCell(person.getUsername()).setCell(getCampus(scp.getCampus(executionYear)));
                    row.setCell(scp.getDegree().getExternalId()).setCell(person.getName());
                    row.setCell(getEmail(person)).setCell(getTelefone(person)).setCell(getCGDCode(person));
                } else {
                    final PhdIndividualProgramProcess phd = findPhd(person);
                    if (phd != null) {
                        final Row row = spreadsheet.addRow();
                        row.setCell(person.getUsername()).setCell(getCampus(phd));
                        row.setCell(getDegree(phd).getExternalId()).setCell(person.getName());
                        row.setCell(getEmail(person)).setCell(getTelefone(person)).setCell(getCGDCode(person));
                    }
                }
            }
        }

        return sendXls(response, spreadsheet);
    }

    private Degree getDegree(final PhdIndividualProgramProcess phd) {
        return phd.getPhdProgram().getDegree();
    }

    private String getCampus(final PhdIndividualProgramProcess phd) {
        final DegreeCurricularPlan degreeCurricularPlan = phd.getPhdProgram().getDegree().getLastActiveDegreeCurricularPlan();
        return getCampus(degreeCurricularPlan.getCurrentCampus());
    }

    private String getCampus(final Space campus) {
        return campus == null ? " " : campus.getName();
    }

    private StudentCurricularPlan findStudentCurricularPlan(final Student student, final DateTime begin, final DateTime end) {
        final Set<StudentCurricularPlan> studentCurricularPlans = getStudentCurricularPlans(begin, end, student);
        if (studentCurricularPlans.size() == 1) {
            return studentCurricularPlans.iterator().next();
        } else if (studentCurricularPlans.size() > 1) {
            return findMaxStudentCurricularPlan(studentCurricularPlans);
        }
        return null;
    }

    public static Set<StudentCurricularPlan> getStudentCurricularPlans(final DateTime begin, final DateTime end,
            final Student student) {
        final Set<StudentCurricularPlan> studentCurricularPlans = new HashSet<StudentCurricularPlan>();

        for (final Registration registration : student.getRegistrationsSet()) {
            if (!registration.isActive()) {
                continue;
            }
            final DegreeType degreeType = registration.getDegreeType();
            if (!degreeType.isBolonhaType()) {
                continue;
            }
            for (final StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
                if (studentCurricularPlan.isActive()) {
                    if (degreeType.isBolonhaDegree() || degreeType.isBolonhaMasterDegree()
                            || degreeType.isIntegratedMasterDegree() || degreeType.isAdvancedSpecializationDiploma()) {
                        studentCurricularPlans.add(studentCurricularPlan);
                    } else {
                        final RegistrationState registrationState = registration.getActiveState();
                        if (registrationState != null) {
                            final DateTime dateTime = registrationState.getStateDate();
                            if (!dateTime.isBefore(begin) && !dateTime.isAfter(end)) {
                                studentCurricularPlans.add(studentCurricularPlan);
                            }
                        }
                    }
                }
            }
        }
        return studentCurricularPlans;
    }

    private static StudentCurricularPlan findMaxStudentCurricularPlan(final Set<StudentCurricularPlan> studentCurricularPlans) {
        return Collections.max(studentCurricularPlans, new Comparator<StudentCurricularPlan>() {

            @Override
            public int compare(final StudentCurricularPlan o1, final StudentCurricularPlan o2) {
                final DegreeType degreeType1 = o1.getDegreeType();
                final DegreeType degreeType2 = o2.getDegreeType();
                if (degreeType1 == degreeType2) {
                    final YearMonthDay yearMonthDay1 = o1.getStartDateYearMonthDay();
                    final YearMonthDay yearMonthDay2 = o2.getStartDateYearMonthDay();
                    final int c = yearMonthDay1.compareTo(yearMonthDay2);
                    return c == 0 ? DomainObjectUtil.COMPARATOR_BY_ID.compare(o1, o2) : c;
                } else {
                    return degreeType1.compareTo(degreeType2);
                }
            }

        });
    }

    private PhdIndividualProgramProcess findPhd(final Person person) {
        final Event event = person.getInsuranceEventFor(ExecutionYear.readCurrentExecutionYear());
        return event != null && event.isClosed() ? findPhd(person.getPhdIndividualProgramProcessesSet()) : null;
    }

    private PhdIndividualProgramProcess findPhd(final Collection<PhdIndividualProgramProcess> phdIndividualProgramProcesses) {
        PhdIndividualProgramProcess result = null;
        for (final PhdIndividualProgramProcess process : phdIndividualProgramProcesses) {
            if (process.getActiveState() == PhdIndividualProgramProcessState.WORK_DEVELOPMENT) {
                if (result != null) {
                    return null;
                }
                result = process;
            }
        }
        return result;
    }

    private String getWorkingDepartment(final Person person) {
        if (person.getEmployee() != null) {
            final Employee employee = person.getEmployee();
            final Unit unit = employee.getCurrentWorkingPlace();
            if (unit != null) {
                final DepartmentUnit departmentUnit = unit.getDepartmentUnit();
                if (departmentUnit != null) {
                    return departmentUnit.getDepartment().getExternalId();
                }
            }
        }
        return " ";
    }

    public String getEmail(final Person person) {
        final EmailAddress email = person.getEmailAddressForSendingEmails();
        return email != null ? email.getValue() : " ";
    }

    private String getTelefone(final Person person) {
        final StringBuilder builder = new StringBuilder();
        for (final PartyContact partyContact : person.getPartyContactsSet()) {
            if (partyContact.isActiveAndValid()) {
                if (partyContact.isPhone()) {
                    final Phone phone = (Phone) partyContact;
                    if (builder.length() > 0) {
                        builder.append(", ");
                    }
                    builder.append(phone.getNumber());
                } else if (partyContact.isMobile()) {
                    final MobilePhone mobilePhone = (MobilePhone) partyContact;
                    if (builder.length() > 0) {
                        builder.append(", ");
                    }
                    builder.append(mobilePhone.getNumber());
                }
            }
        }
        return builder.toString();
    }

    private String getCGDCode(final Person person) {
        return "";
//        CardGenerationEntry result = null;
//        for (final CardGenerationEntry entry : person.getCardGenerationEntriesSet()) {
//            final CardGenerationBatch batch = entry.getCardGenerationBatch();
//            if (batch.getSent() != null && batch.getCardGenerationProblemsSet().size() == 0) {
//                if (result == null || result.getCardGenerationBatch().getSent().isBefore(batch.getSent())) {
//                    result = entry;
//                }
//            }
//        }
//        return result == null ? " " : result.getCgdIdentifier();
    }

}
