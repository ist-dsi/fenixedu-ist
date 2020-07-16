package pt.ist.fenixedu.integration.task.updateData.student;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.StudentStatute;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.Singleton;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Supplier;

@Task(englishTitle = "Set First Cycle Finalists for Integrated Degrees")
public class SetFirstCycleFinalistStatus extends CronTask {

    private static final Locale PT = new Locale("pt");
    private static final Locale UK = Locale.UK;

    private static final String CODE = "INTEGRATED_FIRST_CYCLE_FINALIST";
    private static final LocalizedString NAME = new LocalizedString(PT, "Finalista 1º Ciclo Integrado")
            .with(UK, "1st Cycle Integrated Finalist");

    private static final Supplier<StatuteType> GETTER = () -> Bennu.getInstance().getStatuteTypesSet().stream()
            .filter(t -> t.getCode().equals(CODE))
            .findAny().orElse(null);
    private static final Supplier<StatuteType> CREATOR = () -> new StatuteType(CODE, NAME, false,
            false,false, false, false,
            false,true, false, false, true);

    @Override
    public void runTask() throws Exception {
        ExecutionYear subjectYear = SeniorStatuteTask.specialSeasonEnrolmentPeriodOpeningSoonForThisYear(SeniorStatuteTask.HOW_MANY_WEEKS_SOONER);
        if (subjectYear != null) {
            final StatuteType statuteType = Singleton.getInstance(GETTER, CREATOR);
            final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
            Bennu.getInstance().getDegreesSet().stream()
                    .filter(d -> d.getDegreeType().isIntegratedMasterDegree())
                    .flatMap(d -> d.getRegistrationsSet().stream())
                    .filter(r -> r.isActive())
                    .filter(r -> !hasStatus(statuteType, r))
                    .filter(r -> canCompleteFirstCycle(r, executionYear))
                    .peek(r -> taskLog("Creating statute for: %s%n", r.getStudent().getPerson().getUser().getUsername()))
                    .forEach(r -> new StudentStatute(r.getStudent(), statuteType, executionYear.getFirstExecutionPeriod(),
                            executionYear.getLastExecutionPeriod()));
        }
    }

    private boolean hasStatus(final StatuteType statuteType, final Registration registration) {
        return registration.getStudent().getStudentStatutesSet().stream()
                .filter(statute -> statute.getType() == statuteType)
                .anyMatch(statute -> statute.getBeginExecutionPeriod().isCurrent() || statute.getBeginExecutionPeriod().isCurrent());
    }

    private boolean canCompleteFirstCycle(final Registration registration, final ExecutionYear executionYear) {
        final StudentCurricularPlan studentCurricularPlan = registration.getLastStudentCurricularPlan();
        if (studentCurricularPlan != null) {
            final CycleCurriculumGroup firstCycle = studentCurricularPlan.getFirstCycle();
            if (firstCycle != null) {
                final double creditsToCompleteCycle = firstCycle.getDegreeModule()
                        .getMinEctsCredits(executionYear.getFirstExecutionPeriod()).doubleValue();
                final double aprovedCredits = firstCycle.getCurriculumLineStream()
                        .filter(cl -> cl.isApproved())
                        .map(cl -> cl.getEctsCreditsForCurriculum())
                        .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
                final double enrolledCredits = firstCycle.getCurriculumLineStream()
                        .filter(cl -> !cl.isApproved() && cl.getExecutionPeriod().getExecutionYear().isCurrent())
                        .map(cl -> cl.getEctsCreditsForCurriculum())
                        .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();

                return aprovedCredits < creditsToCompleteCycle && (aprovedCredits + enrolledCredits) >= creditsToCompleteCycle;
            }
        }
        return false;
    }

}
