package pt.ist.fenixedu.giaf.invoices;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.util.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixedu.giaf.invoices.GiafEvent.GiafEventEntry;

public class EventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);

    public static void syncEventWithGiaf(final Event event) {
        final EventWrapper wrapper = new EventWrapper(event);
        process(wrapper);
    }

    private static void process(final EventWrapper eventWrapper) {
        try {
            final GiafEvent giafEvent = new GiafEvent(eventWrapper.event);

            final Money debtFenix = eventWrapper.debt;
            final Money debtGiaf = giafEvent.debt();

            if (debtFenix.isPositive() && !debtFenix.equals(debtGiaf)) {
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
                LOGGER.warn("%s: %s%n", eventWrapper.event.getExternalId(), m);
                return;
            } else if (m.indexOf("O valor da factura") >= 0 && m.indexOf("inferior") >= 0 && m.indexOf("nota de cr") >= 0
                    && m.indexOf("encontrar a factura") >= 0) {
                LOGGER.warn("%s: %s%n", eventWrapper.event.getExternalId(), m);
                return;
            }
            LOGGER.error("Unhandled giaf error for event " + eventWrapper.event.getExternalId() + " : " + m);
            e.printStackTrace();
//        throw e;
        }
    }

}
