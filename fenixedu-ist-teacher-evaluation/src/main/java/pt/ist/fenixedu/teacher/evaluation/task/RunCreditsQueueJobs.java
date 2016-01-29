package pt.ist.fenixedu.teacher.task;

import java.io.PrintWriter;

import org.fenixedu.academic.domain.QueueJob;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.teacher.evaluation.domain.credits.AnnualCreditsState;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.CalculateCreditsQueueJob;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.CloseCreditsQueueJob;

@Task(englishTitle = "RunCreditsQueueJobs", readOnly = false)
public class RunCreditsQueueJobs extends CronTask {

    @Override
    public void runTask() throws Exception {
        LocalDate today = new LocalDate();
        PrintWriter logWriter = getTaskLogWriter();
        for (AnnualCreditsState annualCreditsState : Bennu.getInstance().getAnnualCreditsStatesSet()) {
            if (!annualCreditsState.getIsFinalCreditsCalculated() && annualCreditsState.getFinalCalculationDate() != null
                    && !today.isBefore(annualCreditsState.getFinalCalculationDate())
                    && !alreadyLaunched(CalculateCreditsQueueJob.class.getName())) {
                new CalculateCreditsQueueJob(annualCreditsState.getExecutionYear());
                logWriter.println("Lauched CalculateCreditsQueueJob");
            }
            if (!annualCreditsState.getIsCreditsClosed() && annualCreditsState.getCloseCreditsDate() != null
                    && !today.isBefore(annualCreditsState.getCloseCreditsDate())
                    && !alreadyLaunched(CloseCreditsQueueJob.class.getName())) {
                new CloseCreditsQueueJob(annualCreditsState.getExecutionYear());
                logWriter.println("Lauched CloseCreditsQueueJob");
            }
        }
    }

    private boolean alreadyLaunched(String className) {
        for (QueueJob queueJob : Bennu.getInstance().getQueueJobUndoneSet()) {
            if (queueJob.getClass().getName().equals(className)) {
                return true;
            }
        }
        return false;
    }

}
