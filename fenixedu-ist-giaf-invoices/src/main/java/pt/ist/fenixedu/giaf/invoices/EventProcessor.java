package pt.ist.fenixedu.giaf.invoices;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.util.Money;

import pt.ist.fenixedu.giaf.invoices.GiafEvent.GiafEventEntry;

public class EventProcessor {

    public static void syncEventWithGiaf(final ClientMap clientMap, final ErrorLogConsumer consumer, final EventLogger elogger, final Event event) {
        final EventWrapper wrapper = new EventWrapper(event);
        process(clientMap, consumer, elogger, wrapper);
    }

    private static void process(final ClientMap clientMap, final ErrorLogConsumer consumer, final EventLogger elogger, final EventWrapper eventWrapper) {
        try {
            final GiafEvent giafEvent = new GiafEvent(eventWrapper.event);
            if (EventWrapper.needsProcessing(eventWrapper.event)) {

                final Money debtFenix = eventWrapper.debt;
                final Money debtGiaf = giafEvent.debt();

                if (debtFenix.isPositive() && !debtFenix.equals(debtGiaf)) {
                    for (final GiafEventEntry entry : giafEvent.entries) {
                        final Money amountStillInDebt = entry.amountStillInDebt();
                        if (amountStillInDebt.isPositive()) {
                            giafEvent.exempt(entry, eventWrapper.event, amountStillInDebt);
                        }
                    }

                    if (debtFenix.isPositive()) { // THIS TEST IS REDUNDANT !  WHY IS IT HERE ?
                        final String clientId = clientMap.getClientId(eventWrapper.event.getPerson());
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

                eventWrapper.payments().filter(d -> !giafEvent.hasPayment(d))
                    .peek(d -> elogger.log("Processing payment %s : %s%n", eventWrapper.event.getExternalId(), d.getExternalId()))
                    .forEach(d -> giafEvent.pay(clientMap, d));
            }

            eventWrapper.payments().filter(d -> !giafEvent.hasPayment(d))
                .peek(d -> elogger.log("Processing past payment %s : %s%n", eventWrapper.event.getExternalId(), d.getExternalId()))
                .forEach(d -> giafEvent.payWithoutDebtRegistration(clientMap, d));        
        } catch (final Error e) {
            final String m = e.getMessage();

            BigDecimal amount;
            DebtCycleType cycleType;
            
            try {
                amount = Utils.calculateTotalDebtValue(eventWrapper.event).getAmount();
                cycleType = Utils.cycleType(eventWrapper.event);
            } catch (Exception ex) {
                amount = null;
                cycleType = null;
            }

            consumer.accept(
                    eventWrapper.event.getExternalId(),
                    eventWrapper.event.getPerson().getUsername(),
                    eventWrapper.event.getPerson().getName(),
                    amount == null ? "" : amount.toPlainString(),
                    cycleType == null ? "" : cycleType.getDescription(),
                    m,
                    "",
                    "", "", "", "", "", "", "", "");
            elogger.log("%s: %s%n", eventWrapper.event.getExternalId(), m);
            if (m.indexOf("digo de Entidade ") > 0 && m.indexOf(" invlido/inexistente!") > 0) {
                return;
            } else if (m.indexOf("O valor da factura") >= 0 && m.indexOf("inferior") >= 0 && m.indexOf("nota de cr") >= 0
                    && m.indexOf("encontrar a factura") >= 0) {
                elogger.log("%s: %s%n", eventWrapper.event.getExternalId(), m);
                return;
            }
            elogger.log("Unhandled giaf error for event " + eventWrapper.event.getExternalId() + " : " + m);
            e.printStackTrace();
//        throw e;
        }

    }

}
