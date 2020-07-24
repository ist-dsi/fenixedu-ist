package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.exceptions.DomainException;
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
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Supplier;

@Task(englishTitle = "Aoto set first cycle finalist statute for integrated degrees")
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
        final StatuteType statuteType = Singleton.getInstance(GETTER, CREATOR);
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        final ExecutionSemester executionSemester = ExecutionSemester.readActualExecutionSemester();

        Bennu.getInstance().getDegreesSet().stream()
                .filter(d -> d.getDegreeType().isIntegratedMasterDegree())
                .flatMap(d -> d.getRegistrationsSet().stream())
                .filter(r -> r.isActive())
                .filter(r -> !hasStatus(statuteType, r))
                .filter(r -> canCompleteFirstCycle(r, executionYear))
                .forEach(r -> {
                    new StudentStatute(r.getStudent(), statuteType, executionYear.getFirstExecutionPeriod(),
                            executionYear.getLastExecutionPeriod());
                });
    }

    private boolean hasStatus(final StatuteType statuteType, final Registration registration) {
        return registration.getStudent().getStudentStatutesSet().stream()
                .filter(statute -> statute.getType().getCode().equals(statuteType.getCode()))
                .anyMatch(statute -> statute.getBeginExecutionPeriod().isCurrent() || statute.getEndExecutionPeriod().isCurrent());
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
                final double limitedEnrolledCredits = Math.min(enrolledCredits, 15d);

                return aprovedCredits < creditsToCompleteCycle && (aprovedCredits + limitedEnrolledCredits) >= creditsToCompleteCycle;
            }
        }
        return false;
    }

}
