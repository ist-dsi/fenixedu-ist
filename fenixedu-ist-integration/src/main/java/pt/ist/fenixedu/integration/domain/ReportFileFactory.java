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
package pt.ist.fenixedu.integration.domain;

import org.fenixedu.academic.domain.ExecutionYear;
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
import pt.ist.fenixedu.teacher.evaluation.domain.reports.TeachersListFromGiafReportFile;
import pt.ist.fenixedu.teacher.evaluation.domain.reports.TimetablesReportFile;
import pt.ist.fenixedu.tutorship.domain.reports.TutorshipProgramReportFile;
import pt.ist.fenixframework.Atomic;

public class ReportFileFactory {

    @Atomic
    static public GepReportFile createStatusAndApprovalReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final StatusAndApprovalReportFile statusAndApprovalReportFile = new StatusAndApprovalReportFile();
        statusAndApprovalReportFile.setType(type);
        statusAndApprovalReportFile.setDegreeType(degreeType);
        statusAndApprovalReportFile.setExecutionYear(executionYear);
        return statusAndApprovalReportFile;
    }

    @Atomic
    public static GepReportFile createDissertationsProposalsReportFile(String type, DegreeType degreeType,
            ExecutionYear executionYear) {
        final DissertationsProposalsReportFile dissertationsProposalsReportFile = new DissertationsProposalsReportFile();
        dissertationsProposalsReportFile.setType(type);
        dissertationsProposalsReportFile.setDegreeType(degreeType);
        dissertationsProposalsReportFile.setExecutionYear(executionYear);
        return dissertationsProposalsReportFile;
    }

    @Atomic
    public static GepReportFile createSummaryOccupancyReportFile(String type, ExecutionYear executionYear) {
        final SummaryOccupancyReportFile summaryOccupancyReportFile = new SummaryOccupancyReportFile();
        summaryOccupancyReportFile.setType(type);
        summaryOccupancyReportFile.setExecutionYear(executionYear);
        return summaryOccupancyReportFile;
    }

    @Atomic
    public static GepReportFile createWrittenEvaluationReportFile(String type, ExecutionYear executionYear) {
        final WrittenEvaluationReportFile writtenEvaluationReportFile = new WrittenEvaluationReportFile();
        writtenEvaluationReportFile.setType(type);
        writtenEvaluationReportFile.setExecutionYear(executionYear);
        return writtenEvaluationReportFile;
    }

    @Atomic
    public static GepReportFile createDissertationsWithExternalAffiliationsReportFile(String type, DegreeType degreeType,
            ExecutionYear executionYear) {
        final DissertationsWithExternalAffiliationsReportFile dissertationsWithExternalAffiliationsReportFile =
                new DissertationsWithExternalAffiliationsReportFile();
        dissertationsWithExternalAffiliationsReportFile.setType(type);
        dissertationsWithExternalAffiliationsReportFile.setDegreeType(degreeType);
        dissertationsWithExternalAffiliationsReportFile.setExecutionYear(executionYear);
        return dissertationsWithExternalAffiliationsReportFile;
    }

    @Atomic
    public static GepReportFile createTeachersListFromGiafReportFile(String type, DegreeType degreeType,
            ExecutionYear executionYear) {
        final TeachersListFromGiafReportFile teachersListFromGiafReportFile = new TeachersListFromGiafReportFile();
        teachersListFromGiafReportFile.setType(type);
        teachersListFromGiafReportFile.setDegreeType(degreeType);
        teachersListFromGiafReportFile.setExecutionYear(executionYear);
        return teachersListFromGiafReportFile;
    }

    @Atomic
    public static GepReportFile createTimetablesReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final TimetablesReportFile timetablesReportFile = new TimetablesReportFile();
        timetablesReportFile.setType(type);
        timetablesReportFile.setDegreeType(degreeType);
        timetablesReportFile.setExecutionYear(executionYear);
        return timetablesReportFile;
    }

    @Atomic
    public static GepReportFile createGraduationReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final GraduationReportFile graduationReportFile = new GraduationReportFile();
        graduationReportFile.setType(type);
        graduationReportFile.setDegreeType(degreeType);
        graduationReportFile.setExecutionYear(executionYear);
        return graduationReportFile;
    }

    @Atomic
    public static GepReportFile createEctsLabelCurricularCourseReportFile(String type, DegreeType degreeType,
            ExecutionYear executionYear) {
        final EctsLabelCurricularCourseReportFile ectsLabelCurricularCourseReportFile = new EctsLabelCurricularCourseReportFile();
        ectsLabelCurricularCourseReportFile.setType(type);
        ectsLabelCurricularCourseReportFile.setDegreeType(degreeType);
        ectsLabelCurricularCourseReportFile.setExecutionYear(executionYear);
        return ectsLabelCurricularCourseReportFile;
    }

    @Atomic
    public static GepReportFile createCourseLoadReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final CourseLoadReportFile courseLoadReportFile = new CourseLoadReportFile();
        courseLoadReportFile.setType(type);
        courseLoadReportFile.setDegreeType(degreeType);
        courseLoadReportFile.setExecutionYear(executionYear);
        return courseLoadReportFile;
    }

    @Atomic
    public static GepReportFile createEctsLabelDegreeReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final EctsLabelDegreeReportFile ectsLabelDegreeReportFile = new EctsLabelDegreeReportFile();
        ectsLabelDegreeReportFile.setType(type);
        ectsLabelDegreeReportFile.setDegreeType(degreeType);
        ectsLabelDegreeReportFile.setExecutionYear(executionYear);
        return ectsLabelDegreeReportFile;
    }

    @Atomic
    public static GepReportFile createEtiReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final EtiReportFile etiReportFile = new EtiReportFile();
        etiReportFile.setType(type);
        etiReportFile.setDegreeType(degreeType);
        etiReportFile.setExecutionYear(executionYear);
        return etiReportFile;
    }

    @Atomic
    public static GepReportFile createCourseLoadAndResponsiblesReportFile(String type, DegreeType degreeType,
            ExecutionYear executionYear) {
        final CourseLoadAndResponsiblesReportFile courseLoadAndResponsiblesReportFile = new CourseLoadAndResponsiblesReportFile();
        courseLoadAndResponsiblesReportFile.setType(type);
        courseLoadAndResponsiblesReportFile.setDegreeType(degreeType);
        courseLoadAndResponsiblesReportFile.setExecutionYear(executionYear);
        return courseLoadAndResponsiblesReportFile;
    }

    @Atomic
    public static GepReportFile createEurAceReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final EurAceReportFile eurAceReportFile = new EurAceReportFile();
        eurAceReportFile.setType(type);
        eurAceReportFile.setDegreeType(degreeType);
        eurAceReportFile.setExecutionYear(executionYear);
        return eurAceReportFile;
    }

    @Atomic
    public static GepReportFile createFlunkedReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final FlunkedReportFile flunkedReportFile = new FlunkedReportFile();
        flunkedReportFile.setType(type);
        flunkedReportFile.setDegreeType(degreeType);
        flunkedReportFile.setExecutionYear(executionYear);
        return flunkedReportFile;
    }

    @Atomic
    public static GepReportFile createRegistrationReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final RegistrationReportFile registrationReportFile = new RegistrationReportFile();
        registrationReportFile.setType(type);
        registrationReportFile.setDegreeType(degreeType);
        registrationReportFile.setExecutionYear(executionYear);
        return registrationReportFile;
    }

    @Atomic
    public static GepReportFile createTeachersByShiftReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final TeachersByShiftReportFile teachersByShiftReportFile = new TeachersByShiftReportFile();
        teachersByShiftReportFile.setType(type);
        teachersByShiftReportFile.setDegreeType(degreeType);
        teachersByShiftReportFile.setExecutionYear(executionYear);
        return teachersByShiftReportFile;
    }

    @Atomic
    public static GepReportFile createTutorshipProgramReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final TutorshipProgramReportFile tutorshipProgramReportFile = new TutorshipProgramReportFile();
        tutorshipProgramReportFile.setType(type);
        tutorshipProgramReportFile.setDegreeType(degreeType);
        tutorshipProgramReportFile.setExecutionYear(executionYear);
        return tutorshipProgramReportFile;
    }

    @Atomic
    public static GepReportFile createRaidesGraduationReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final RaidesGraduationReportFile reportFile = new RaidesGraduationReportFile();
        reportFile.setType(type);
        reportFile.setDegreeType(degreeType);
        reportFile.setExecutionYear(executionYear);
        return reportFile;
    }

    @Atomic
    public static GepReportFile createRaidesDfaReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final RaidesDfaReportFile reportFile = new RaidesDfaReportFile();
        reportFile.setType(type);
        reportFile.setDegreeType(degreeType);
        reportFile.setExecutionYear(executionYear);
        return reportFile;
    }

    @Atomic
    public static GepReportFile createRaidesPhdReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final RaidesPhdReportFile reportFile = new RaidesPhdReportFile();
        reportFile.setType(type);
        reportFile.setDegreeType(degreeType);
        reportFile.setExecutionYear(executionYear);
        return reportFile;
    }

    @Atomic
    public static GepReportFile createRaidesSpecializationReportFile(String type, DegreeType degreeType,
            ExecutionYear executionYear) {
        final RaidesSpecializationReportFile reportFile = new RaidesSpecializationReportFile();
        reportFile.setType(type);
        reportFile.setDegreeType(degreeType);
        reportFile.setExecutionYear(executionYear);
        return reportFile;
    }

    @Atomic
    public static GepReportFile createTeacherCreditsReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final TeacherCreditsReportFile TeacherCreditsReportFile = new TeacherCreditsReportFile();
        TeacherCreditsReportFile.setType(type);
        TeacherCreditsReportFile.setDegreeType(degreeType);
        TeacherCreditsReportFile.setExecutionYear(executionYear);
        return TeacherCreditsReportFile;
    }

    @Atomic
    public static GepReportFile createEffectiveTeachingLoadReportFile(String type, DegreeType degreeType,
            ExecutionYear executionYear) {
        final EffectiveTeachingLoadReportFile effectiveTeachingLoadReportFile = new EffectiveTeachingLoadReportFile();
        effectiveTeachingLoadReportFile.setType(type);
        effectiveTeachingLoadReportFile.setDegreeType(degreeType);
        effectiveTeachingLoadReportFile.setExecutionYear(executionYear);
        return effectiveTeachingLoadReportFile;
    }

    @Atomic
    public static GepReportFile createAvailableCoursesForQUCReportFile(String type, DegreeType degreeType,
            ExecutionYear executionYear) {
        final AvailableCoursesForQUCReportFile availableCoursesForQUCReportFile = new AvailableCoursesForQUCReportFile();
        availableCoursesForQUCReportFile.setType(type);
        availableCoursesForQUCReportFile.setDegreeType(degreeType);
        availableCoursesForQUCReportFile.setExecutionYear(executionYear);
        return availableCoursesForQUCReportFile;
    }

    @Atomic
    public static GepReportFile createWorkloadSummaryBoardReportFile(String type, DegreeType degreeType,
            ExecutionYear executionYear) {
        final WorkloadSummaryBoardReportFile workloadSummaryBoardReportFile = new WorkloadSummaryBoardReportFile();
        workloadSummaryBoardReportFile.setType(type);
        workloadSummaryBoardReportFile.setDegreeType(degreeType);
        workloadSummaryBoardReportFile.setExecutionYear(executionYear);
        return workloadSummaryBoardReportFile;
    }

    @Atomic
    public static GepReportFile createInitialAnswersReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final InitialAnswersReportFile initialAnswersReportFile = new InitialAnswersReportFile();
        initialAnswersReportFile.setType(type);
        initialAnswersReportFile.setDegreeType(degreeType);
        initialAnswersReportFile.setExecutionYear(executionYear);
        return initialAnswersReportFile;
    }

    @Atomic
    public static GepReportFile createCoursesAnswersReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final CoursesAnswersReportFile coursesAnswersReportFile = new CoursesAnswersReportFile();
        coursesAnswersReportFile.setType(type);
        coursesAnswersReportFile.setDegreeType(degreeType);
        coursesAnswersReportFile.setExecutionYear(executionYear);
        return coursesAnswersReportFile;
    }

    @Atomic
    public static GepReportFile createQUCQuestionsReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final QUCQuestionsReportFile qucQuestionsReportFile = new QUCQuestionsReportFile();
        qucQuestionsReportFile.setType(type);
        qucQuestionsReportFile.setDegreeType(degreeType);
        qucQuestionsReportFile.setExecutionYear(executionYear);
        return qucQuestionsReportFile;
    }

    @Atomic
    public static GepReportFile createDelegatesAnswersReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final DelegatesAnswersReportFile delegatesAnswersReportFile = new DelegatesAnswersReportFile();
        delegatesAnswersReportFile.setType(type);
        delegatesAnswersReportFile.setDegreeType(degreeType);
        delegatesAnswersReportFile.setExecutionYear(executionYear);
        return delegatesAnswersReportFile;
    }

    @Atomic
    public static GepReportFile createTeachersAnswersReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final TeachersAnswersReportFile teachersAnswersReportFile = new TeachersAnswersReportFile();
        teachersAnswersReportFile.setType(type);
        teachersAnswersReportFile.setDegreeType(degreeType);
        teachersAnswersReportFile.setExecutionYear(executionYear);
        return teachersAnswersReportFile;
    }

    @Atomic
    public static GepReportFile createRegentsAnswersReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final RegentsAnswersReportFile regentsAnswersReportFile = new RegentsAnswersReportFile();
        regentsAnswersReportFile.setType(type);
        regentsAnswersReportFile.setDegreeType(degreeType);
        regentsAnswersReportFile.setExecutionYear(executionYear);
        return regentsAnswersReportFile;
    }

    @Atomic
    public static GepReportFile createCoordinatorsAnswersReportFile(String type, DegreeType degreeType,
            ExecutionYear executionYear) {
        final CoordinatorsAnswersReportFile coordinatorsAnswersReportFile = new CoordinatorsAnswersReportFile();
        coordinatorsAnswersReportFile.setType(type);
        coordinatorsAnswersReportFile.setDegreeType(degreeType);
        coordinatorsAnswersReportFile.setExecutionYear(executionYear);
        return coordinatorsAnswersReportFile;
    }

    @Atomic
    public static GepReportFile createFirstTimeCycleAnswersReportFile(String type, DegreeType degreeType,
            ExecutionYear executionYear) {
        final FirstTimeCycleAnswersReportFile firstTimeCycleAnswersReportFile = new FirstTimeCycleAnswersReportFile();
        firstTimeCycleAnswersReportFile.setType(type);
        firstTimeCycleAnswersReportFile.setDegreeType(degreeType);
        firstTimeCycleAnswersReportFile.setExecutionYear(executionYear);
        return firstTimeCycleAnswersReportFile;
    }

    @Atomic
    public static GepReportFile createMeritReportFile(String type, DegreeType degreeType, ExecutionYear executionYear) {
        final StudentMeritReportFile studentMeritReportFile = new StudentMeritReportFile();
        studentMeritReportFile.setType(type);
        studentMeritReportFile.setDegreeType(degreeType);
        studentMeritReportFile.setExecutionYear(executionYear);
        return studentMeritReportFile;
    }
}
