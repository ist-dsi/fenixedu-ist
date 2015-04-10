package pt.ist.fenixedu.teacher.domain.credits;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.QueueJobResult;
import org.joda.time.DateTime;

public class CloseCreditsQueueJob extends CloseCreditsQueueJob_Base {

    public CloseCreditsQueueJob(ExecutionYear executionYear) {
        super();
        setExecutionYear(executionYear);
    }

    @Override
    public QueueJobResult execute() throws Exception {
        AnnualCreditsState annualCreditsState = AnnualCreditsState.getAnnualCreditsState(getExecutionYear());
        if (!annualCreditsState.getIsCreditsClosed()) {
            if (!annualCreditsState.getIsFinalCreditsCalculated()) {
                CalculateCreditsQueueJob calculateCreditsQueueJob = new CalculateCreditsQueueJob(getExecutionYear());
                calculateCreditsQueueJob.execute();
                calculateCreditsQueueJob.setDone(true);
                calculateCreditsQueueJob.setRootDomainObjectQueueUndone(null);
                calculateCreditsQueueJob.setJobEndTime(new DateTime());
            }
            annualCreditsState.setIsCreditsClosed(true);
        }
        return null;
    }

}
