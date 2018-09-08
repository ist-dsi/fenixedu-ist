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
package pt.ist.fenixedu.integration.ui.struts.action.gep;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.QueueJob;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.reports.CourseLoadAndResponsiblesReportFile;
import org.fenixedu.academic.domain.reports.CourseLoadReportFile;
import org.fenixedu.academic.domain.reports.DissertationsProposalsReportFile;
import org.fenixedu.academic.domain.reports.DissertationsWithExternalAffiliationsReportFile;
import org.fenixedu.academic.domain.reports.EctsLabelCurricularCourseReportFile;
import org.fenixedu.academic.domain.reports.EctsLabelDegreeReportFile;
import org.fenixedu.academic.domain.reports.EtiReportFile;
import org.fenixedu.academic.domain.reports.EurAceReportFile;
import org.fenixedu.academic.domain.reports.FlunkedReportFile;
import org.fenixedu.academic.domain.reports.GepReportFile;
import org.fenixedu.academic.domain.reports.GraduationReportFile;
import org.fenixedu.academic.domain.reports.RaidesDfaReportFile;
import org.fenixedu.academic.domain.reports.RaidesGraduationReportFile;
import org.fenixedu.academic.domain.reports.RaidesPhdReportFile;
import org.fenixedu.academic.domain.reports.RaidesSpecializationReportFile;
import org.fenixedu.academic.domain.reports.RegistrationReportFile;
import org.fenixedu.academic.domain.reports.StatusAndApprovalReportFile;
import org.fenixedu.academic.domain.reports.SummaryOccupancyReportFile;
import org.fenixedu.academic.domain.reports.WrittenEvaluationReportFile;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.academic.ui.struts.action.gep.GepApplication.GepPortalApp;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import com.google.common.base.Strings;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.integration.domain.ReportFileFactory;
import pt.ist.fenixedu.integration.domain.reports.StudentMeritReportFile;
import pt.ist.fenixedu.quc.domain.reports.AvailableCoursesForQUCReportFile;
import pt.ist.fenixedu.quc.domain.reports.CoordinatorsAnswersReportFile;
import pt.ist.fenixedu.quc.domain.reports.CoursesAnswersReportFile;
import pt.ist.fenixedu.quc.domain.reports.DelegatesAnswersReportFile;
import pt.ist.fenixedu.quc.domain.reports.FirstTimeCycleAnswersReportFile;
import pt.ist.fenixedu.quc.domain.reports.InitialAnswersReportFile;
import pt.ist.fenixedu.quc.domain.reports.QUCQuestionsReportFile;
import pt.ist.fenixedu.quc.domain.reports.RegentsAnswersReportFile;
import pt.ist.fenixedu.quc.domain.reports.TeachersAnswersReportFile;
import pt.ist.fenixedu.quc.domain.reports.WorkloadSummaryBoardReportFile;
import pt.ist.fenixedu.teacher.evaluation.domain.reports.EffectiveTeachingLoadReportFile;
import pt.ist.fenixedu.teacher.evaluation.domain.reports.TeacherCreditsReportFile;
import pt.ist.fenixedu.teacher.evaluation.domain.reports.TeachersByShiftReportFile;
import pt.ist.fenixedu.teacher.evaluation.domain.reports.TimetablesReportFile;
import pt.ist.fenixedu.tutorship.domain.reports.TutorshipProgramReportFile;
import pt.ist.fenixframework.FenixFramework;

@StrutsFunctionality(app = GepPortalApp.class, path = "reports", titleKey = "link.reports")
@Mapping(module = "gep", path = "/reportsByDegreeType")
@Forwards({ @Forward(name = "selectDegreeType", path = "/gep/reportsByDegreeType.jsp"),
        @Forward(name = "viewReports", path = "/gep/viewReports.jsp") })
public class ReportsByDegreeTypeDA extends FenixDispatchAction {

    private static final int MAX_AUTHORIZED_REPORT_FILES = 20;

    public static class ReportBean implements Serializable {
        private DegreeType degreeType;

        private ExecutionYear executionYearReference;

        public DegreeType getDegreeType() {
            return degreeType;
        }

        public void setDegreeType(DegreeType degreeType) {
            this.degreeType = degreeType;
        }

        public String getDegreeTypeOID() {
            return getDegreeType() != null ? getDegreeType().getExternalId() : null;
        }

        public ExecutionYear getExecutionYear() {
            return executionYearReference;
        }

        public String getExecutionYearOID() {
            return getExecutionYear() == null ? null : getExecutionYear().getExternalId();
        }

        public void setExecutionYear(final ExecutionYear executionYear) {
            executionYearReference = executionYear;
        }

        public ExecutionYear getExecutionYearFourYearsBack() {
            final ExecutionYear executionYear = getExecutionYear();
            return executionYear == null ? null : ReportsByDegreeTypeDA.getExecutionYearFourYearsBack(executionYear);
        }
    }

    public static ExecutionYear getExecutionYearFourYearsBack(final ExecutionYear executionYear) {
        ExecutionYear executionYearFourYearsBack = executionYear;
        if (executionYear != null) {
            for (int i = 5; i > 1; i--) {
                final ExecutionYear previousExecutionYear = executionYearFourYearsBack.getPreviousExecutionYear();
                if (previousExecutionYear != null) {
                    executionYearFourYearsBack = previousExecutionYear;
                }
            }
        }
        return executionYearFourYearsBack;
    }

    public List<QueueJob> getLatestJobs() {
        return QueueJob.getLastJobsForClassOrSubClass(GepReportFile.class, 5);
    }

    @EntryPoint
    public ActionForward selectDegreeType(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        ReportBean reportBean = getRenderedObject();

        DegreeType degreeType;
        ExecutionYear executionYear;
        if (reportBean == null) {
            degreeType = getDegreeType(request);
            executionYear = getExecutionYear(request);
            reportBean = new ReportBean();
            reportBean.setDegreeType(degreeType);
            reportBean.setExecutionYear(executionYear);
        } else {
            degreeType = reportBean.getDegreeType();
            executionYear = reportBean.getExecutionYear();
        }
        RenderUtils.invalidateViewState();

        request.setAttribute("reportBean", reportBean);
        request.setAttribute("queueJobList", getLatestJobs());
        countReports(request, degreeType, executionYear);
        return mapping.findForward("selectDegreeType");
    }

    private void countReports(HttpServletRequest request, DegreeType degreeType, ExecutionYear executionYear) {
        for (Integer i = 1;; ++i) {
            Class aClass = getClassForParameter(i.toString());
            if (aClass == null) {
                break;
            }
            request.setAttribute("numberOfReportsType" + i, getCountReportsForParameters(degreeType, executionYear, aClass));
        }
    }

    private int getCountReportsForParameters(DegreeType degreeType, ExecutionYear executionYear, Class reportClass) {
        FindSelectedGepReports predicate = new FindSelectedGepReports(executionYear, degreeType, reportClass);

        return getValidCounterForReports((int) Bennu.getInstance().getQueueJobSet().stream().filter(j -> predicate.evaluate(j))
                .count());
    }

    private int getValidCounterForReports(int totalCounter) {
        if (totalCounter > MAX_AUTHORIZED_REPORT_FILES) {
            return MAX_AUTHORIZED_REPORT_FILES;
        } else {
            return totalCounter;
        }
    }

    public ActionForward cancelQueuedJob(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        cancelQueuedJob(request);
        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward cancelQueuedJobFromViewReports(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        cancelQueuedJob(request);
        return viewReports(mapping, actionForm, request, response);
    }

    private void cancelQueuedJob(HttpServletRequest request) {
        QueueJob job = getDomainObject(request, "id");
        job.cancel();
    }

    public ActionForward resendJob(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        resendJob(request);
        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward resendJobFromViewReports(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        resendJob(request);
        return viewReports(mapping, actionForm, request, response);
    }

    private void resendJob(HttpServletRequest request) {
        QueueJob job = getDomainObject(request, "id");
        job.resend();
    }

    private boolean isRepeatedJob(Person person, HttpServletRequest request, Class aClass) {
        final DegreeType degreeType = getDegreeType(request);
        request.setAttribute("degreeType", degreeType);
        final ExecutionYear executionYear = getExecutionYear(request);
        request.setAttribute("executionYearID", (executionYear == null) ? null : executionYear.getExternalId());
        final String fileType = getFileType(request);
        for (QueueJob queueJob : getLatestJobs()) {
            GepReportFile gepReportFile = (GepReportFile) queueJob;
            if ((gepReportFile.getPerson() == person) && (gepReportFile.getClass() == aClass) && (!gepReportFile.getDone())
                    && (gepReportFile.getExecutionYear() == executionYear) && (gepReportFile.getDegreeType() == degreeType)
                    && (fileType.equals(gepReportFile.getType()))) {
                return true;
            }
        }
        return false;
    }

    private String getFileType(final HttpServletRequest httpServletRequest) {
        return httpServletRequest.getParameter("format");
    }

    private DegreeType getDegreeType(final HttpServletRequest httpServletRequest) {
        final String degreeTypeString = httpServletRequest.getParameter("degreeType");
        return Strings.isNullOrEmpty(degreeTypeString) ? null : FenixFramework.getDomainObject(degreeTypeString);
    }

    private ExecutionYear getExecutionYear(final HttpServletRequest httpServletRequest) {
        final String OIDString = httpServletRequest.getParameter("executionYearID");
        return Strings.isNullOrEmpty(OIDString) ? null : FenixFramework.<ExecutionYear> getDomainObject(OIDString);
    }

    private String getFormat(final HttpServletRequest httpServletRequest) {
        return httpServletRequest.getParameter("format");
    }

    @SuppressWarnings("unused")
    public ActionForward downloadMeritReportFile(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createMeritReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadEurAce(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createEurAceReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadEctsLabelForCurricularCourses(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }

        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request,
                ReportFileFactory.createEctsLabelCurricularCourseReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadEctsLabelForDegrees(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createEctsLabelDegreeReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadStatusAndAproval(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createStatusAndApprovalReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadEti(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createEtiReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadCourseLoadAndResponsibles(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request,
                ReportFileFactory.createCourseLoadAndResponsiblesReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadRegistrations(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createRegistrationReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadFlunked(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createFlunkedReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadTimetables(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createTimetablesReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadDissertationsProposals(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request,
                ReportFileFactory.createDissertationsProposalsReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadSummaries(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createSummaryOccupancyReportFile(format, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadWrittenEvaluations(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createWrittenEvaluationReportFile(format, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadDissertationsWithExternalAffiliations(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request,
                ReportFileFactory.createDissertationsWithExternalAffiliationsReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadGraduations(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createGraduationReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadTeachersByShift(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createTeachersByShiftReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadCourseLoads(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createCourseLoadReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    @SuppressWarnings("unused")
    public ActionForward downloadTutorshipProgram(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createTutorshipProgramReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadRaidesGraduation(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        prepareNewJobResponse(request, ReportFileFactory.createRaidesGraduationReportFile(getFormat(request),
                getDegreeType(request), getExecutionYear(request)));
        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadRaidesDfa(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        prepareNewJobResponse(request, ReportFileFactory.createRaidesDfaReportFile(getFormat(request), getDegreeType(request),
                getExecutionYear(request)));
        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadRaidesPhd(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        prepareNewJobResponse(request, ReportFileFactory.createRaidesPhdReportFile(getFormat(request), getDegreeType(request),
                getExecutionYear(request)));
        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadRaidesSpecialization(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        prepareNewJobResponse(request, ReportFileFactory.createRaidesSpecializationReportFile(getFormat(request),
                getDegreeType(request), getExecutionYear(request)));
        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadTeacherCreditsReportFile(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createTeacherCreditsReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadEffectiveTeachingLoadReportFile(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createEffectiveTeachingLoadReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadAvailableCoursesForQUCReportFile(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request,
                ReportFileFactory.createAvailableCoursesForQUCReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadWorkloadSummaryBoardReportFile(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createWorkloadSummaryBoardReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadInitialAnswersReportFile(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createInitialAnswersReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadCoursesAnswersReportFile(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createCoursesAnswersReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadQUCQuestionsReportFile(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createQUCQuestionsReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadDelegatesAnswersReportFile(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createDelegatesAnswersReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadTeachersAnswersReportFile(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createTeachersAnswersReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadRegentsAnswersReportFile(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createRegentsAnswersReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadCoordinatorsAnswersReportFile(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createCoordinatorsAnswersReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    public ActionForward downloadFirstTimeCycleAnswersReportFile(ActionMapping mapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (isRepeatedJob(AccessControl.getPerson(), request, getClassForParameter(request.getParameter("type")))) {
            return selectDegreeType(mapping, actionForm, request, response);
        }
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        final String format = getFormat(request);

        prepareNewJobResponse(request, ReportFileFactory.createFirstTimeCycleAnswersReportFile(format, degreeType, executionYear));

        return selectDegreeType(mapping, actionForm, request, response);
    }

    private void prepareNewJobResponse(HttpServletRequest request, GepReportFile job) {

        ReportBean reportBean = getRenderedObject();
        if (reportBean == null) {
            reportBean = new ReportBean();
            reportBean.setDegreeType(job.getDegreeType());
            reportBean.setExecutionYear(job.getExecutionYear());
        }
        RenderUtils.invalidateViewState();

        request.setAttribute("job", job);
        request.setAttribute("reportBean", reportBean);
        request.setAttribute("queueJobList", getLatestJobs());
    }

    public Class getClassForParameter(String type) {
        int i = Integer.valueOf(type);
        switch (i) {
        case 1:
            return EurAceReportFile.class;
        case 2:
            return EctsLabelDegreeReportFile.class;
        case 3:
            return EctsLabelCurricularCourseReportFile.class;
        case 4:
            return StatusAndApprovalReportFile.class;
        case 5:
            return EtiReportFile.class;
        case 6:
            return RegistrationReportFile.class;
        case 7:
            return FlunkedReportFile.class;
        case 8:
            return TeachersByShiftReportFile.class;
        case 9:
            return CourseLoadReportFile.class;
        case 10:
            return GraduationReportFile.class;
        case 11:
            return DissertationsWithExternalAffiliationsReportFile.class;
        case 12:
            return DissertationsProposalsReportFile.class;
        case 13:
            return RaidesGraduationReportFile.class;
        case 14:
            return RaidesDfaReportFile.class;
        case 15:
            return RaidesPhdReportFile.class;
        case 16:
            return TutorshipProgramReportFile.class;
        case 17:
            return CourseLoadAndResponsiblesReportFile.class;
        case 18:
            return TimetablesReportFile.class;
        case 19:
            return RaidesSpecializationReportFile.class;
        case 20:
            return SummaryOccupancyReportFile.class;
        case 21:
            return WrittenEvaluationReportFile.class;
        case 22:
            return TeacherCreditsReportFile.class;
        case 23:
            return EffectiveTeachingLoadReportFile.class;
        case 24:
            return AvailableCoursesForQUCReportFile.class;
        case 25:
            return WorkloadSummaryBoardReportFile.class;
        case 26:
            return InitialAnswersReportFile.class;
        case 27:
            return CoursesAnswersReportFile.class;
        case 28:
            return QUCQuestionsReportFile.class;
        case 29:
            return DelegatesAnswersReportFile.class;
        case 30:
            return TeachersAnswersReportFile.class;
        case 31:
            return RegentsAnswersReportFile.class;
        case 32:
            return CoordinatorsAnswersReportFile.class;
        case 33:
            return FirstTimeCycleAnswersReportFile.class;
        case 34:
            return StudentMeritReportFile.class;
        default:
            return null;
        }
    }

    public static class FindSelectedGepReports {

        ExecutionYear executionYear;

        DegreeType degreeType;

        Class reportClass;

        int elements = 0;

        public FindSelectedGepReports(ExecutionYear executionYear, DegreeType degreeType, Class reportClass) {
            this.executionYear = executionYear;
            this.degreeType = degreeType;
            this.reportClass = reportClass;
        }

        public boolean evaluate(Object object) {
            QueueJob queueJob = (QueueJob) object;
            try {
                GepReportFile gepReportFile = (GepReportFile) queueJob;
                if (gepReportFile.getClass() == this.reportClass) {
                    if (this.executionYear == gepReportFile.getExecutionYear()
                            && this.degreeType == gepReportFile.getDegreeType() && elements < MAX_AUTHORIZED_REPORT_FILES) {
                        elements++;
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } catch (ClassCastException E) {
                return false;
            }

        }
    }

    public ActionForward viewReports(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        final String type = request.getParameter("type");

        Class reportClass = getClassForParameter(type);
        final DegreeType degreeType = getDegreeType(request);
        final ExecutionYear executionYear = getExecutionYear(request);
        FindSelectedGepReports predicate = new FindSelectedGepReports(executionYear, degreeType, reportClass);

        List<GepReportFile> selectedJobs =
                Bennu.getInstance().getQueueJobSet().stream().filter(j -> predicate.evaluate(j)).map(r -> (GepReportFile) r)
                        .collect(Collectors.toList());
        String reportName = "";
        if (selectedJobs.size() > 0) {
            reportName = selectedJobs.iterator().next().getJobName();
        }

        request.setAttribute("degreeType", degreeType);
        request.setAttribute("executionYear", executionYear);
        request.setAttribute("list", reportName);
        request.setAttribute("queueJobList", selectedJobs);

        return mapping.findForward("viewReports");
    }

}
