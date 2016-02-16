package pt.ist.fenixedu.giaf.invoices.task;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;

import pt.ist.fenixedu.giaf.invoices.ErrorConsumer;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.GiafEvent;
import pt.ist.fenixedu.giaf.invoices.GiafEvent.GiafEventEntry;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.giaf.client.financialDocuments.ClientClient;

@Task(englishTitle = "Calculate differences and publish information to GIAF.")
public class SyncFinancialInfoToGiafTask extends CronTask {

    @Override
    public void runTask() throws Exception {
        eventStream().map(e -> e.getParty())
            .filter(p -> p != null && p.isPerson())
            .map(p -> (Person) p)
            .distinct()
            .map(p -> Utils.toJson(p))
            .forEach(j -> ClientClient.createClient(j));

        eventStream().map(e -> new EventWrapper(e)).forEach(this::process);
    }

    private Stream<Event> eventStream() {
        return Bennu.getInstance().getAccountingEventsSet().stream().filter(this::needsProcessing)
                .filter(e -> Utils.validate(ErrorConsumer.VOID_EVENT_CONSUMER, e));
    }

    private boolean needsProcessing(final Event event) {
        final ExecutionYear executionYear = Utils.executionYearOf(event);
        return executionYear.isCurrent() || event.getWhenOccured().isAfter(EventWrapper.THRESHOLD);
    }

    private void process(final EventWrapper eventWrapper) {
        try {
            final GiafEvent giafEvent = new GiafEvent(eventWrapper.event);

            final Money debtFenix = eventWrapper.debt;
            final Money debtGiaf = giafEvent.debt();

            if (!debtFenix.equals(debtGiaf)) {
                for (final GiafEventEntry entry : giafEvent.entries) {
                    final Money amountStillInDebt = entry.amountStillInDebt();
                    if (amountStillInDebt.isPositive()) {
                        giafEvent.exempt(entry, eventWrapper.event, amountStillInDebt);
                    }
                }

                if (debtFenix.isPositive()) {
                    final String clientId = Utils.toClientCode(eventWrapper.event.getPerson());
                    final GiafEventEntry entry = giafEvent.newGiafEventEntry(eventWrapper.event, clientId, debtFenix);
                    final Money exempt = eventWrapper.exempt.add(giafEvent.payed());
                    if (exempt.isPositive()) {
                        giafEvent.exempt(entry, eventWrapper.event, exempt.greaterThan(debtFenix) ? debtFenix : exempt);
                    }
                }
            }

            final GiafEventEntry openEntry = giafEvent.openEntry();
            if (openEntry != null) {
                final Money giafActualExempt = giafEvent.entries.stream().filter(e -> e != openEntry).map(e -> e.payed)
                        .reduce(openEntry.exempt, Money::subtract);
                if (eventWrapper.exempt.greaterThan(giafActualExempt)) {
                    final Money exemptDiff = eventWrapper.exempt.subtract(giafActualExempt);
                    if (exemptDiff.isPositive()) {
                        final Money amountStillInDebt = openEntry.amountStillInDebt();
                        if (exemptDiff.greaterThan(amountStillInDebt)) {
                            taskLog("Exemption value exceeds amount still in debt for event: %s%n",
                                    eventWrapper.event.getExternalId());
                            giafEvent.exempt(openEntry, eventWrapper.event, amountStillInDebt);
                        } else {
                            giafEvent.exempt(openEntry, eventWrapper.event, exemptDiff);
                        }
                    }
                }
            }

            eventWrapper.payments().filter(d -> !giafEvent.hasPayment(d)).forEach(d -> giafEvent.pay(d));
        } catch (final Error e) {
            final String m = e.getMessage();
            if (m.indexOf("digo de Entidade ") > 0 && m.indexOf(" invlido/inexistente!") > 0) {
                taskLog("%s: %s%n", eventWrapper.event.getExternalId(), m);
                return ;
            } else if (m.indexOf("O valor da factura") >= 0
                    && m.indexOf("inferior") >= 0
                    && m.indexOf("nota de cr") >= 0
                    && m.indexOf("encontrar a factura") >= 0) {
                taskLog("%s: %s%n", eventWrapper.event.getExternalId(), m);
                return;
            }
            taskLog("Unhandled giaf error for event %s : %s%n", eventWrapper.event.getExternalId(), m);
            e.printStackTrace();
//            throw e;
        }
    }

}
