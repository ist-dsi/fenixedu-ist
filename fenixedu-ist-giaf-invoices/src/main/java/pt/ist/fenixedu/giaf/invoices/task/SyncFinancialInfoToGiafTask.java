package pt.ist.fenixedu.giaf.invoices.task;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;

import pt.ist.fenixedu.giaf.invoices.ErrorConsumer;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.giaf.client.financialDocuments.ClientClient;

@Task(englishTitle = "Calculate differences and publish information to GIAF.")
public class SyncFinancialInfoToGiafTask extends CronTask {

    @Override
    public void runTask() throws Exception {
        eventStream().map(e -> e.getParty()).filter(p -> p != null && p.isPerson()).map(p -> (Person) p).distinct()
                .map(p -> Utils.toJson(p)).forEach(j -> ClientClient.createClient(j));

        eventStream().forEach(e -> EventProcessor.syncEventWithGiaf(e));
    }

    private Stream<Event> eventStream() {
        return Bennu.getInstance().getAccountingEventsSet().stream().filter(this::needsProcessing)
                .filter(e -> Utils.validate(ErrorConsumer.VOID_EVENT_CONSUMER, e));
    }

    private boolean needsProcessing(final Event event) {
        final ExecutionYear executionYear = Utils.executionYearOf(event);
        return executionYear.isCurrent() || event.getWhenOccured().isAfter(EventWrapper.THRESHOLD);
    }

}
