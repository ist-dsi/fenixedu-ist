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
package pt.ist.fenixedu.integration.ui.struts.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.OccupationPeriodType;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeEvent;
import org.fenixedu.academic.domain.alumni.CerimonyInquiryPerson;
import org.fenixedu.academic.domain.person.RoleType;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.ui.struts.action.HomeAction;
import org.fenixedu.academic.ui.struts.action.base.FenixAction;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.exceptions.AuthorizationException;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.struts.annotations.Mapping;

import org.joda.time.YearMonthDay;
import pt.ist.fenixWebFramework.renderers.components.HtmlLink;
import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.GenericChecksumRewriter;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;
import pt.ist.fenixedu.contracts.domain.accessControl.DepartmentPresidentStrategy;
import pt.ist.fenixedu.quc.domain.DelegateInquiryTemplate;
import pt.ist.fenixedu.quc.domain.InquiryStudentCycleAnswer;
import pt.ist.fenixedu.quc.domain.RegentInquiryTemplate;
import pt.ist.fenixedu.quc.domain.StudentInquiryRegistry;
import pt.ist.fenixedu.quc.domain.TeacherInquiryTemplate;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.AnnualCreditsState;
import pt.ist.fenixedu.teacher.evaluation.domain.teacher.ReductionService;
import pt.ist.fenixedu.teacher.evaluation.domain.teacher.TeacherService;
import pt.ist.fenixedu.teacher.evaluation.domain.time.calendarStructure.TeacherCreditsFillingCE;

@Mapping(path = "/login")
public class BaseAuthenticationAction extends FenixAction {

    @Override
    public final ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        try {

            if (!Authenticate.isLogged() && !"true".equals(request.getParameter("login_failed"))) {
                response.sendRedirect(request.getContextPath() + "/login?callback=" + request.getRequestURL().toString());
                return null;
            }

            final User userView = Authenticate.getUser();

            if (userView == null || userView.isLoginExpired()) {
                return getAuthenticationFailedForward(request, response);
            }

            final HttpSession httpSession = request.getSession(false);

            if (hasMissingTeacherService(userView)) {
                return handleSessionCreationAndForwardToTeachingService(request, userView, httpSession);
            } else if (hasPendingTeachingReductionService(userView)) {
                return handleSessionCreationAndForwardToPendingTeachingReductionService(request, userView, httpSession);
            } else if (hasMissingRAIDESInformation(userView)) {
                return handleSessionCreationAndForwardToRAIDESInquiriesResponseQuestion(request, userView, httpSession);
            } else if (isAlumniAndHasInquiriesToResponde(userView)) {
                return handleSessionCreationAndForwardToAlumniInquiriesResponseQuestion(request, userView, httpSession);
            } else if (isStudentAndHasQucInquiriesToRespond(userView)) {
                return handleSessionCreationAndForwardToQucInquiriesResponseQuestion(request, userView, httpSession);
            } else if (isDelegateAndHasInquiriesToRespond(userView)) {
                return handleSessionCreationAndForwardToDelegateInquiriesResponseQuestion(request, userView, httpSession);
            } else if (isTeacherAndHasInquiriesToRespond(userView)) {
                return handleSessionCreationAndForwardToTeachingInquiriesResponseQuestion(request, userView, httpSession);
            } else if (isRegentAndHasInquiriesToRespond(userView)) {
                return handleSessionCreationAndForwardToRegentInquiriesResponseQuestion(request, userView, httpSession);
            } else if (isStudentAndHasFirstTimeCycleInquiryToRespond(userView)) {
                return handleSessionCreationAndForwardToFirstTimeCycleInquiry(request, userView, httpSession);
            } else if (isStudentAndHasGratuityDebtsToPay(userView)) {
                return handleSessionCreationAndForwardToGratuityPaymentsReminder(request, userView, httpSession);
            } else if (isAlumniWithNoData(userView)) {
                return handleSessionCreationAndForwardToAlumniReminder(request, userView, httpSession);
            } else if (hasPendingPartyContactValidationRequests(userView)) {
                return handlePartyContactValidationRequests(request, userView, httpSession);
            } else {
                return handleSessionCreationAndGetForward(mapping, request, userView, httpSession);
            }
        } catch (AuthorizationException e) {
            return getAuthenticationFailedForward(request, response);
        }
    }

    private ActionForward handleSessionCreationAndForwardToFirstTimeCycleInquiry(HttpServletRequest request, User userView,
            HttpSession session) {
        return new ActionForward("/respondToFirstTimeCycleInquiry.do?method=showQuestion");
    }

    private boolean isStudentAndHasFirstTimeCycleInquiryToRespond(User userView) {
        if (userView.getPerson() != null && userView.getPerson().getStudent() != null) {
            final Student student = userView.getPerson().getStudent();
            return hasActiveClassPeriod(student) && InquiryStudentCycleAnswer.hasFirstTimeCycleInquiryToRespond(student);
        }
        return false;
    }

    private boolean hasMissingTeacherService(User userView) {
        if (userView.getPerson() != null && userView.getPerson().getTeacher() != null
                && RoleType.DEPARTMENT_MEMBER.isMember(userView)) {
            ExecutionSemester executionSemester = ExecutionSemester.readActualExecutionSemester();
            if (executionSemester != null && (userView.getPerson().getTeacher().hasTeacherAuthorization())) {
                TeacherService teacherService =
                        TeacherService.getTeacherServiceByExecutionPeriod(userView.getPerson().getTeacher(), executionSemester);
                return (teacherService == null || teacherService.getTeacherServiceLock() == null)
                        && TeacherCreditsFillingCE.isInValidCreditsPeriod(executionSemester, userView);
            }
        }
        return false;
    }

    private boolean hasPendingTeachingReductionService(User userView) {
        if (userView.getPerson() != null && userView.getPerson().getTeacher() != null
                && RoleType.DEPARTMENT_MEMBER.isMember(userView)) {
            Department department = userView.getPerson().getTeacher().getDepartment();
            if (department != null && DepartmentPresidentStrategy.isCurrentUserCurrentDepartmentPresident(department)) {
                ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
                if (executionYear != null && AnnualCreditsState.isInValidReductionServiceApprovalPeriod(executionYear)) {
                    for (ExecutionSemester executionSemester : executionYear.getExecutionPeriodsSet()) {
                        boolean inValidTeacherCreditsPeriod = AnnualCreditsState.isInValidCreditsPeriod(executionSemester);
                        for (ReductionService reductionService : department.getPendingReductionServicesSet()) {
                            if ((reductionService.getTeacherService().getTeacherServiceLock() != null || !inValidTeacherCreditsPeriod)
                                    && reductionService.getTeacherService().getExecutionPeriod().equals(executionSemester)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private ActionForward handlePartyContactValidationRequests(HttpServletRequest request, User userView, HttpSession session) {
        return new ActionForward("/partyContactValidationReminder.do?method=showReminder");
    }

    private boolean hasMissingRAIDESInformation(User userView) {
        return userView.getPerson() != null && userView.getPerson().getStudent() != null
                && hasActiveClassPeriod(userView.getPerson().getStudent())
                && hasFirstTimeCycleRAIDESToRespond(userView.getPerson().getStudent());
    }

    public static boolean hasFirstTimeCycleRAIDESToRespond(Student student) {
        for (Registration registration : student.getActiveRegistrations()) {
            if (!registration.getDegreeType().isEmpty() && registration.isFirstTime()
                    && !registration.getStudent().hasAnyCompletedPersonalInformationSince(registration.getStartExecutionYear())) {
                return true;
            }
        }
        for (final PhdIndividualProgramProcess phdProcess : student.getPerson().getPhdIndividualProgramProcessesSet()) {
            if (student.isValidAndActivePhdProcess(phdProcess)
                    && !student.hasAnyCompletedPersonalInformationSince(ExecutionYear.readByDateTime(phdProcess.getWhenStartedStudies()))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasActiveClassPeriod(final Student student) {
        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        final YearMonthDay currentDay = new YearMonthDay();
        return student.getActiveRegistrationStream()
                .flatMap(r -> r.getDegree().getExecutionDegreesForExecutionYear(currentYear).stream())
                .anyMatch(ed -> ed.getPeriods(OccupationPeriodType.LESSONS).anyMatch(op -> op.nestedOccupationPeriodsContainsDay(currentDay)));
    }

    private boolean hasPendingPartyContactValidationRequests(User userView) {
        final Person person = userView.getPerson();
        return person.hasPendingPartyContacts() && person.getCanValidateContacts();
    }

    private boolean isAlumniAndHasInquiriesToResponde(final User userView) {
        return userView.getPerson().getCerimonyInquiryPersonSet().stream().anyMatch(CerimonyInquiryPerson::isPendingResponse);
    }

    private ActionForward handleSessionCreationAndForwardToAlumniReminder(HttpServletRequest request, User userView,
            HttpSession session) {
        return new ActionForward("/alumniReminder.do");
    }

    /**
     * Checks if all the person that have the Alumni object have the any
     * formation filled in with the exception for those that are active teachers
     * or haver a role of EMPLOYEE or RESEARCHER
     * 
     * @param userView
     * @return true if it has alumni and the formations list is not empty, false
     *         otherwise and if it falls under the specific cases described
     *         above
     */
    private boolean isAlumniWithNoData(User userView) {
        Person person = userView.getPerson();
        if (person.getStudent() != null && person.getStudent().getAlumni() != null && RoleType.ALUMNI.isMember(userView)) {
            if ((person.getTeacher() != null && person.getTeacher().isActiveContractedTeacher())
                    || new ActiveEmployees().isMember(userView) || RoleType.RESEARCHER.isMember(userView)) {
                return false;
            }
            return person.getFormations().isEmpty();
        }
        return false;
    }

    private ActionForward handleSessionCreationAndForwardToGratuityPaymentsReminder(HttpServletRequest request, User userView,
            HttpSession session) {
        return new ActionForward("/gratuityPaymentsReminder.do?method=showReminder");
    }

    private boolean isStudentAndHasGratuityDebtsToPay(final User userView) {
        return RoleType.STUDENT.isMember(userView)
                && hasGratuityOrAdministrativeOfficeFeeAndInsuranceDebtsFor(userView.getPerson(),
                        ExecutionYear.readCurrentExecutionYear());
    }

    public static boolean hasGratuityOrAdministrativeOfficeFeeAndInsuranceDebtsFor(Person person,
            final ExecutionYear executionYear) {
        return person.getAnnualEventsFor(executionYear).stream()
                .filter(annualEvent -> annualEvent.isGratuity() || annualEvent.isInsuranceEvent() || annualEvent
                        .isAdministrativeOfficeAndInsuranceEvent() || annualEvent instanceof AdministrativeOfficeFeeEvent)
                .anyMatch(Event::isOpen);
    }

    private boolean isTeacherAndHasInquiriesToRespond(User userView) {
        if (RoleType.TEACHER.isMember(userView)
                || (TeacherInquiryTemplate.getCurrentTemplate() != null && !userView.getPerson()
                        .getProfessorships(TeacherInquiryTemplate.getCurrentTemplate().getExecutionPeriod()).isEmpty())) {
            return !TeacherInquiryTemplate.getExecutionCoursesWithTeachingInquiriesToAnswer(userView.getPerson()).isEmpty();
        }
        return false;
    }

    private boolean isRegentAndHasInquiriesToRespond(User userView) {
        if (RoleType.TEACHER.isMember(userView)
                || (RegentInquiryTemplate.getCurrentTemplate() != null && !userView.getPerson()
                        .getProfessorships(RegentInquiryTemplate.getCurrentTemplate().getExecutionPeriod()).isEmpty())) {
            return !RegentInquiryTemplate.getExecutionCoursesWithRegentInquiriesToAnswer(userView.getPerson()).isEmpty();
        }
        return false;
    }

    private boolean isStudentAndHasQucInquiriesToRespond(final User userView) {
        if (RoleType.STUDENT.isMember(userView)) {
            final Student student = userView.getPerson().getStudent();
            return student != null && StudentInquiryRegistry.hasInquiriesToRespond(student);
        }
        return false;
    }

    private boolean isDelegateAndHasInquiriesToRespond(final User userView) {
        if (!userView.getDelegatesSet().isEmpty()) {
            final Student student = userView.getPerson().getStudent();
            return student != null && DelegateInquiryTemplate.hasYearDelegateInquiriesToAnswer(student);
        }
        return false;
    }

    protected ActionForward getAuthenticationFailedForward(final HttpServletRequest request, final HttpServletResponse response) {
        Authenticate.logout(request, response);
        return new ActionForward("/authenticationFailed.jsp");
    }

    private ActionForward handleSessionCreationAndGetForward(ActionMapping mapping, HttpServletRequest request, User userView,
            final HttpSession session) {
        final String path = HomeAction.findFirstFuntionalityPath(request);
        return new ActionForward(path, true);
    }

    private ActionForward handleSessionCreationAndForwardToTeachingService(HttpServletRequest request, User userView,
            HttpSession session) {
        String teacherOid = userView.getPerson().getTeacher().getExternalId();
        String executionYearOid = ExecutionYear.readCurrentExecutionYear().getExternalId();

        HtmlLink link = new HtmlLink();
        link.setModule("/departmentMember");
        link.setUrl("/credits.do?method=viewAnnualTeachingCredits&teacherOid=" + teacherOid + "&executionYearOid="
                + executionYearOid);
        link.setEscapeAmpersand(false);
        String calculatedUrl = link.calculateUrl();
        return new ActionForward("/departmentMember/credits.do?method=viewAnnualTeachingCredits&teacherOid=" + teacherOid
                + "&executionYearOid=" + executionYearOid + "&_request_checksum_="
                + GenericChecksumRewriter.calculateChecksum(calculatedUrl, session), true);
    }

    private ActionForward handleSessionCreationAndForwardToPendingTeachingReductionService(HttpServletRequest request,
            User userView, HttpSession session) {
        HtmlLink link = new HtmlLink();
        link.setModule("/departmentMember");
        link.setUrl("/creditsReductions.do?method=showReductionServices");
        link.setEscapeAmpersand(false);
        String calculatedUrl = link.calculateUrl();
        return new ActionForward("/departmentMember/creditsReductions.do?method=showReductionServices&_request_checksum_="
                + GenericChecksumRewriter.calculateChecksum(calculatedUrl, session), true);
    }

    private ActionForward handleSessionCreationAndForwardToRAIDESInquiriesResponseQuestion(HttpServletRequest request,
            User userView, HttpSession session) {
        HtmlLink link = new HtmlLink();
        link.setModule("/student");
        link.setUrl("/editMissingCandidacyInformation.do?method=prepareEdit");
        link.setEscapeAmpersand(false);
        String calculatedUrl = link.calculateUrl();
        return new ActionForward("/student/editMissingCandidacyInformation.do?method=prepareEdit&_request_checksum_="
                + GenericChecksumRewriter.calculateChecksum(calculatedUrl, session), true);
    }

    private ActionForward handleSessionCreationAndForwardToAlumniInquiriesResponseQuestion(HttpServletRequest request,
            User userView, HttpSession session) {
        return new ActionForward("/respondToAlumniInquiriesQuestion.do?method=showQuestion");
    }

    private ActionForward handleSessionCreationAndForwardToQucInquiriesResponseQuestion(HttpServletRequest request,
            User userView, HttpSession session) {
        return new ActionForward("/respondToInquiriesQuestion.do?method=showQuestion");
    }

    private ActionForward handleSessionCreationAndForwardToDelegateInquiriesResponseQuestion(HttpServletRequest request,
            User userView, HttpSession session) {
        return new ActionForward("/respondToYearDelegateInquiriesQuestion.do?method=showQuestion");
    }

    private ActionForward handleSessionCreationAndForwardToTeachingInquiriesResponseQuestion(HttpServletRequest request,
            User userView, HttpSession session) {
        return new ActionForward("/respondToTeachingInquiriesQuestion.do?method=showQuestion");
    }

    private ActionForward handleSessionCreationAndForwardToRegentInquiriesResponseQuestion(HttpServletRequest request,
            User userView, HttpSession session) {
        return new ActionForward("/respondToRegentInquiriesQuestion.do?method=showQuestion");
    }
}