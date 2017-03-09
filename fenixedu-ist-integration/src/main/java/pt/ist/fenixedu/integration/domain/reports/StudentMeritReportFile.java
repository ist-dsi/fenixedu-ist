package pt.ist.fenixedu.integration.domain.reports;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Set;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grade;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.studentCurriculum.Credits;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.studentCurriculum.EnrolmentWrapper;
import org.fenixedu.academic.domain.studentCurriculum.RootCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.Substitution;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

public class StudentMeritReportFile extends StudentMeritReportFile_Base {

    private static final MathContext MATH_CONTEXT = new MathContext(3, RoundingMode.HALF_EVEN);

    public StudentMeritReportFile() {
        super();
    }

    @Override
    public String getJobName() {
        return BundleUtil.getString("resources.FenixeduIstIntegrationResources", "title.student.merit.report");
    }

    @Override
    protected String getPrefix() {
        return BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.student.merit.report.prefix");
    }

    @Override
    public void renderReport(Spreadsheet spreadsheet) throws Exception {
        listStudents(spreadsheet, getExecutionYear(), getDegreeType());
    }

    private void listStudents(Spreadsheet spreadsheet, ExecutionYear executionYear, DegreeType degreeType) {
        generateHeaders(spreadsheet, executionYear);

        for (final Degree degree : Bennu.getInstance().getDegreesSet()) {
            if (degreeType != degree.getDegreeType()) {
                continue;
            }

            for (final DegreeCurricularPlan degreeCurricularPlan : degree.getDegreeCurricularPlansSet()) {
                for (final StudentCurricularPlan studentCurricularPlan : degreeCurricularPlan.getStudentCurricularPlansSet()) {
                    final Registration registration = studentCurricularPlan.getRegistration();
                    final Student student = registration.getStudent();
                    if (registration.hasAnyActiveState(executionYear)) {
                        renderRow(spreadsheet, executionYear, degree, registration, student);
                    }
                }
            }
        }
    }

    private void generateHeaders(Spreadsheet spreadsheet, ExecutionYear executionYear) {
        spreadsheet.setName(
                getPrefix() + " " + Unit.getInstitutionAcronym() + " " + executionYear.getQualifiedName().replace("/", ""));
        String year = executionYear.getQualifiedName();
        spreadsheet.setHeader(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.degree"));
        spreadsheet.setHeader(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.number"));
        spreadsheet.setHeader(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.name"));
        spreadsheet.setHeader(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.credits.enrolled", year));
        spreadsheet.setHeader(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.credits.approved", year));
        spreadsheet.setHeader(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.curricular.year", year));
        spreadsheet.setHeader(BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.average.year", year));
    }

    private void renderRow(Spreadsheet spreadsheet, ExecutionYear executionYear, Degree degree, Registration registration,
            Student student) {
        final Row row = spreadsheet.addRow();

        final double approvedCredits = getCredits(executionYear, student, true);
        final double enrolledCredits = getCredits(executionYear, student, false);
        final Person person = student.getPerson();
        final int curricularYear = registration.getCurricularYear(executionYear);
        final BigDecimal average = calculateAverage(registration, executionYear);

        row.setCell(degree.getSigla()); // Degree
        row.setCell(student.getNumber()); // Number
        row.setCell(person.getName()); // Name
        row.setCell(enrolledCredits); // Credits Enrolled
        row.setCell(approvedCredits); // Credits Approved
        row.setCell(curricularYear); // Curricular Year
        row.setCell(average.toPlainString()); // Average for year

    }

    private double getCredits(final ExecutionYear executionYear, final Student student, final boolean approvedCredits) {
        double creditsCount = 0.0;
        for (final Registration registration : student.getRegistrationsSet()) {
            for (final StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
                final RootCurriculumGroup root = studentCurricularPlan.getRoot();
                final Set<CurriculumModule> modules =
                        root == null ? (Set) studentCurricularPlan.getEnrolmentsSet() : root.getCurriculumModulesSet();
                creditsCount += countCredits(executionYear, modules, approvedCredits);
            }
        }
        return creditsCount;
    }

    private double countCredits(final ExecutionYear executionYear, final Set<CurriculumModule> modules,
            final boolean approvedCredits) {
        double creditsCount = 0.0;
        for (final CurriculumModule module : modules) {
            if (module instanceof CurriculumGroup) {
                final CurriculumGroup courseGroup = (CurriculumGroup) module;
                creditsCount += countCredits(executionYear, courseGroup.getCurriculumModulesSet(), approvedCredits);
            } else if (module instanceof CurriculumLine) {
                final CurriculumLine curriculumLine = (CurriculumLine) module;
                if (curriculumLine.getExecutionYear() == executionYear) {
                    if (approvedCredits) {
                        creditsCount += curriculumLine.getAprovedEctsCredits().doubleValue();
                    } else {
                        creditsCount += curriculumLine.getEctsCredits().doubleValue();
                    }
                }
            }
        }
        return creditsCount;
    }

    private BigDecimal calculateAverage(final Registration registration, final ExecutionYear executionYear) {
        BigDecimal[] result = new BigDecimal[] { new BigDecimal(0.000, MATH_CONTEXT), new BigDecimal(0.000, MATH_CONTEXT) };
        for (final StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
            final RootCurriculumGroup root = studentCurricularPlan.getRoot();
            final Set<CurriculumModule> modules =
                    root == null ? (Set) studentCurricularPlan.getEnrolmentsSet() : root.getCurriculumModulesSet();
            calculateAverage(result, modules, executionYear);
        }
        return result[1].equals(BigDecimal.ZERO) ? result[1] : result[0].divide(result[1], MATH_CONTEXT);
    }

    private void calculateAverage(final BigDecimal[] result, final Set<CurriculumModule> modules,
            final ExecutionYear executionYear) {
        for (final CurriculumModule module : modules) {
            if (module instanceof CurriculumGroup) {
                final CurriculumGroup courseGroup = (CurriculumGroup) module;
                calculateAverage(result, courseGroup.getCurriculumModulesSet(), executionYear);
            } else if (module instanceof Enrolment) {
                final Enrolment enrolment = (Enrolment) module;
                if (enrolment.isApproved()) {
                    if (enrolment.getExecutionYear() == executionYear) {
                        final Grade grade = enrolment.getGrade();
                        if (grade.isNumeric()) {
                            final BigDecimal ectsCredits = new BigDecimal(enrolment.getEctsCredits());
                            final BigDecimal value = grade.getNumericValue().multiply(ectsCredits);
                            result[0] = result[0].add(value);
                            result[1] = result[1].add(ectsCredits);
                        }
                    }
                }
            } else if (module instanceof Dismissal) {
                final Dismissal dismissal = (Dismissal) module;
                if (dismissal.getExecutionYear() == executionYear && dismissal.getCurricularCourse() != null
                        && !dismissal.getCurricularCourse().isOptionalCurricularCourse()) {

                    final Credits credits = dismissal.getCredits();
                    if (credits instanceof Substitution) {
                        final Substitution substitution = (Substitution) credits;
                        for (final EnrolmentWrapper enrolmentWrapper : substitution.getEnrolmentsSet()) {
                            final IEnrolment iEnrolment = enrolmentWrapper.getIEnrolment();

                            final Grade grade = iEnrolment.getGrade();
                            if (grade.isNumeric()) {
                                final BigDecimal ectsCredits = new BigDecimal(iEnrolment.getEctsCredits());
                                final BigDecimal value = grade.getNumericValue().multiply(ectsCredits);
                                result[0] = result[0].add(value);
                                result[1] = result[1].add(ectsCredits);
                            }
                        }
                    } else {
                        final Grade grade = dismissal.getGrade();
                        if (grade.isNumeric()) {
                            final BigDecimal ectsCredits = new BigDecimal(dismissal.getEctsCredits());
                            final BigDecimal value = grade.getNumericValue().multiply(ectsCredits);
                            result[0] = result[0].add(value);
                            result[1] = result[1].add(ectsCredits);
                        }
                    }
                }
            }
        }
    }

}
