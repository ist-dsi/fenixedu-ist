package pt.ist.fenixedu.integration.task.updateData.student;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentPeriod;
import org.fenixedu.academic.domain.EnrolmentPeriodInExtraordinarySeasonEvaluations;
import org.fenixedu.academic.domain.EnrolmentPeriodInSpecialSeasonEvaluations;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.SeniorStatute;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.commons.i18n.I18N;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Task(englishTitle = "Update senior statute 3 weeks before the special season and onwards")
public class SeniorStatuteTask extends CronTask {

    final static int HOW_MANY_WEEKS_SOONER = 3;

    private long cntBSc = 0;
    private long cntMSc = 0;
    private long cntIM = 0;
    private long cntTotal = 0;

    @Override
    public void runTask() {
        cntBSc = 0;
        cntMSc = 0;
        cntIM = 0;
        cntTotal = 0;

        I18N.setLocale(new Locale("pt", "PT"));
        taskLog(".: Checking if a special season enrolment period is coming soon... :.");
        ExecutionYear subjectYear = specialSeasonEnrolmentPeriodOpeningSoonForThisYear(HOW_MANY_WEEKS_SOONER);

        if (subjectYear != null) {
            taskLog("   --> Running massive SeniorStatute grantor now.");

            try {
                massivelyGrantTheFingSeniorStatute(subjectYear);
            } catch (InterruptedException e) {
                throw new Error(e);
            }

            taskLog("\n       Number of Senior statute grantings  (BSc): " + cntBSc + ".");
            taskLog("       Number of Senior statute grantings (MSc): " + cntMSc + ".");
            taskLog("       Number of Senior statute grantings (Int Msc): " + cntIM + ".");
            taskLog("       Total number of Senior statute grantings: " + cntTotal + ".\n\n");

            taskLog("\n\n·: That's all for today folks!!                                  :·");
        } else {
            taskLog("·: Nothing happening today. Over and out.                        :·");
        }

    }

    static ExecutionYear specialSeasonEnrolmentPeriodOpeningSoonForThisYear(Integer howSoon) {
        Set<EnrolmentPeriod> enrolmentPeriods = Bennu.getInstance().getEnrolmentPeriodsSet();
        Map<DegreeType, ExecutionSemester> fallTermEnrolmentPeriods = new HashMap<DegreeType, ExecutionSemester>();
        Map<DegreeType, ExecutionSemester> springTermEnrolmentPeriods = new HashMap<DegreeType, ExecutionSemester>();
        Map<DegreeType, ExecutionSemester> switcherTermEnrolmentPeriods = null;

        for (EnrolmentPeriod enrolmentPeriod : enrolmentPeriods) {
            if (!(enrolmentPeriod instanceof EnrolmentPeriodInSpecialSeasonEvaluations)
                    && !(enrolmentPeriod instanceof EnrolmentPeriodInExtraordinarySeasonEvaluations)) {
                continue;
            }
            LocalDate statuteGrantorStartDate = new LocalDate(enrolmentPeriod.getStartDateDateTime().toLocalDate());
            statuteGrantorStartDate = statuteGrantorStartDate.minusWeeks(howSoon);
            LocalDate statuteGrantorStopDate = new LocalDate(enrolmentPeriod.getEndDateDateTime().toLocalDate());
            statuteGrantorStopDate = statuteGrantorStopDate.plusDays(1); //inc 1 so that today is compared as before or equal to enrolmentPeriod end date
            LocalDate today = new LocalDate();
            if (today.isAfter(statuteGrantorStartDate) && today.isBefore(statuteGrantorStopDate)) {

                if (enrolmentPeriod.getExecutionPeriod().isFirstOfYear()) {
                    switcherTermEnrolmentPeriods = fallTermEnrolmentPeriods;
                } else {
                    switcherTermEnrolmentPeriods = springTermEnrolmentPeriods;
                }

                DegreeType degreeType = enrolmentPeriod.getDegree().getDegreeType();
                if (degreeType.isBolonhaDegree()) {
                    if (switcherTermEnrolmentPeriods.get(degreeType) == null) {
                        switcherTermEnrolmentPeriods.put(degreeType, enrolmentPeriod.getExecutionPeriod());
                    }
                }

                if (degreeType.isBolonhaMasterDegree()) {
                    if (switcherTermEnrolmentPeriods.get(degreeType) == null) {
                        switcherTermEnrolmentPeriods.put(degreeType, enrolmentPeriod.getExecutionPeriod());
                    }
                }

                if (degreeType.isIntegratedMasterDegree()) {
                    if (switcherTermEnrolmentPeriods.get(degreeType) == null) {
                        switcherTermEnrolmentPeriods.put(degreeType, enrolmentPeriod.getExecutionPeriod());
                    }
                }
            }
        }

        DegreeType degreeBolonhaDegreeType = DegreeType.matching(DegreeType::isBolonhaDegree).get();
        DegreeType masterDegreeBolonhaType = DegreeType.matching(DegreeType::isBolonhaMasterDegree).get();
        DegreeType intregatedMasterDegreeBolonhaType = DegreeType.matching(DegreeType::isIntegratedMasterDegree).get();

        if (fallTermEnrolmentPeriods.get(degreeBolonhaDegreeType) == null) {
            return null;
        }
        if (fallTermEnrolmentPeriods.get(masterDegreeBolonhaType) == null) {
            return null;
        }
        if (fallTermEnrolmentPeriods.get(intregatedMasterDegreeBolonhaType) == null) {
            return null;
        }

        if (springTermEnrolmentPeriods.get(degreeBolonhaDegreeType) == null) {
            return null;
        }
        if (springTermEnrolmentPeriods.get(masterDegreeBolonhaType) == null) {
            return null;
        }
        if (springTermEnrolmentPeriods.get(intregatedMasterDegreeBolonhaType) == null) {
            return null;
        }

        if (!(fallTermEnrolmentPeriods.get(degreeBolonhaDegreeType).getExecutionYear() == springTermEnrolmentPeriods.get(
                degreeBolonhaDegreeType).getExecutionYear())) {
            return null;
        }

        if (!(fallTermEnrolmentPeriods.get(masterDegreeBolonhaType).getExecutionYear() == springTermEnrolmentPeriods.get(
                masterDegreeBolonhaType).getExecutionYear())) {
            return null;
        }

        if (!(fallTermEnrolmentPeriods.get(intregatedMasterDegreeBolonhaType).getExecutionYear() == springTermEnrolmentPeriods
                .get(intregatedMasterDegreeBolonhaType).getExecutionYear())) {
            return null;
        }

        if (!(fallTermEnrolmentPeriods.get(degreeBolonhaDegreeType) == fallTermEnrolmentPeriods.get(masterDegreeBolonhaType))) {
            return null;
        }

        if (!(fallTermEnrolmentPeriods.get(degreeBolonhaDegreeType) == fallTermEnrolmentPeriods
                .get(intregatedMasterDegreeBolonhaType))) {
            return null;
        }

        return fallTermEnrolmentPeriods.get(degreeBolonhaDegreeType).getExecutionYear();
    }

    protected void massivelyGrantTheFingSeniorStatute(ExecutionYear executionYear) throws InterruptedException {

        for (Registration registration : generateRegistrationSet(executionYear)) {
            if (isSeniorStatuteApplicable(registration, executionYear)) {
                try {
                    registration.grantSeniorStatute(executionYear);
                } catch (final Exception e) {
                    taskLog("Error while granting SeniorStatute to '" + registration.getPerson().getName()
                            + "' for his/her registration in <" + registration.getDegreeNameWithDescription() + ">.", e);
                    throw new Error(e);
                }

                if (registration.getDegreeType().isBolonhaDegree()) {
                    cntBSc++;
                } else if (registration.getDegreeType().isBolonhaMasterDegree()) {
                    cntMSc++;
                } else if (registration.getDegreeType().isIntegratedMasterDegree()) {
                    cntIM++;
                }
                cntTotal++;
            }
        }
    }

    private List<Registration> generateRegistrationSet(ExecutionYear executionYear) {
        List<Registration> registrations = new ArrayList<Registration>();

        // Triage for proper DCPs
        for (DegreeCurricularPlan dcp : Bennu.getInstance().getDegreeCurricularPlansSet()) {
            if (dcp.isBolonhaDegree()) {
                // Triage for the right students
                for (Registration registration : dcp.getActiveRegistrations()) {
                    if (registration.hasAnyEnrolmentsIn(executionYear)) {
                        if (registration.isEnrolmentByStudentAllowed()) {
                            registrations.add(registration);
                        }
                    }
                }
            }
        }
        return registrations;
    }

    private boolean isSeniorStatuteApplicable(Registration registration, ExecutionYear executionYear) {
        if (hasAlreadySeniorStatute(registration, executionYear)) {
            return false;
        }

        if (registration.getDegreeType().isBolonhaDegree()) {
            return hasConditionsToFinishBachelorDegree(registration, executionYear);
        } else if (registration.getDegreeType().isBolonhaMasterDegree()) {
            return hasConditionsToFinishMasterDegree(registration, executionYear);
        } else if (registration.getDegreeType().isIntegratedMasterDegree()) {
            return hasSeniorIntegratedMasterEligibility(registration, executionYear);
        }
        return false;
    }

    private boolean hasAlreadySeniorStatute(Registration registration, ExecutionYear executionYear) {
        for (SeniorStatute seniorStatute : registration.getSeniorStatuteSet()) {
            if (seniorStatute.isValidOnAnyExecutionPeriodFor(executionYear)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSeniorIntegratedMasterEligibility(Registration registration, ExecutionYear executionYear) {
        if (!isNotEnrolledInFirstCycleOrIsConcluded(registration, executionYear)) {
            return false;
        }

        return hasConditionsToFinishMasterDegree(registration, executionYear);
    }

    protected boolean hasConditionsToFinishBachelorDegree(final Registration registration, final ExecutionYear executionYear) {
        Double floor = new Double(165.00);
        Double ceiling = new Double(180.00);
        return registration.getStudentCurricularPlan(executionYear).getApprovedEctsCredits(CycleType.FIRST_CYCLE)
                .compareTo(floor) >= 0
                && registration.getStudentCurricularPlan(executionYear).getApprovedEctsCredits(CycleType.FIRST_CYCLE)
                        .compareTo(ceiling) < 0;
    }

    protected boolean hasConditionsToFinishMasterDegree(final Registration registration, final ExecutionYear executionYear) {
        final StudentCurricularPlan scp = registration.getStudentCurricularPlan(executionYear);
        if (scp == null) {
            return false;
        }
        Enrolment dissertationEnrolment = scp.getLatestDissertationEnrolment();

        if (dissertationEnrolment == null) {
            return false;
        }

        if (dissertationEnrolment.getExecutionYear() != executionYear && !dissertationEnrolment.isApproved()) {
            return false;
        }

        Double dissContrib = dissertationEnrolment.isApproved() ? 0.0 : dissertationEnrolment.getEctsCredits();
        Double threshold = 120.00 - (15.00 + dissContrib);
        return registration.getStudentCurricularPlan(executionYear).getApprovedEctsCredits(CycleType.SECOND_CYCLE) >= threshold;
    }

    protected boolean isNotEnrolledInFirstCycleOrIsConcluded(final Registration registration, final ExecutionYear executionYear) {
        return registration.getStudentCurricularPlan(executionYear).getFirstCycle() == null
                || registration.getStudentCurricularPlan(executionYear).getFirstCycle().isConcluded();
    }
}
