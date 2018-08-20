package pt.ist.fenixedu.giaf.invoices;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.giaf.invoices.GiafEvent.GiafEventEntry;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

public class EventProcessor {

    public static void syncEventWithGiaf(final ClientMap clientMap, final ErrorLogConsumer consumer, final EventLogger elogger,
            final Event event) {
        final EventWrapper wrapper = new EventWrapper(event, consumer, false);
        process(clientMap, consumer, elogger, wrapper);
    }

    private static void process(final ClientMap clientMap, final ErrorLogConsumer consumer, final EventLogger elogger,
            final EventWrapper eventWrapper) {
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

                final Money totalPayed = eventWrapper.payed.add(eventWrapper.fines);
                eventWrapper.payments().filter(d -> !giafEvent.hasPayment(d)).peek(
                        d -> elogger.log("Processing payment %s : %s%n", eventWrapper.event.getExternalId(), d.getExternalId()))
                        .forEach(d -> giafEvent.pay(clientMap, d, totalPayed));
            }

            eventWrapper.payments().filter(d -> !giafEvent.hasPayment(d)).peek(
                    d -> elogger.log("Processing past payment %s : %s%n", eventWrapper.event.getExternalId(), d.getExternalId()))
                    .forEach(d -> giafEvent.payWithoutDebtRegistration(clientMap, d));
        } catch (final Error e) {
            final String m = e.getMessage();

            BigDecimal amount;
            DebtCycleType cycleType;

            try {
                amount = eventWrapper.event.getOriginalAmountToPay().getAmount();
                cycleType = Utils.cycleType(eventWrapper.event);
            } catch (Exception ex) {
                amount = null;
                cycleType = null;
            }

            consumer.accept(eventWrapper.event.getExternalId(), eventWrapper.event.getPerson().getUsername(),
                    eventWrapper.event.getPerson().getName(), amount == null ? "" : amount.toPlainString(),
                    cycleType == null ? "" : cycleType.getDescription(), m, "", "", "", "", "", "", "", "", "", "", "");
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

    public static void syncEventWithSap(final ErrorLogConsumer consumer, final EventLogger elogger, final Event event) {
        try {
            FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<Void>() {

                @Override
                public Void call() {
                    processSap(consumer, elogger, event);
                    return null;
                }
            }, new AtomicInstance(TxMode.SPECULATIVE_READ, false));
        } catch (Exception e) {
            logError(consumer, elogger, event, e);
            e.printStackTrace();
        }
    }

    private static void processSap(final ErrorLogConsumer errorLog, final EventLogger elogger, final Event event) {
        try {
            if (EventWrapper.needsProcessingSap(event)) {

                final EventWrapper eventWrapper = new EventWrapper(event, errorLog, true);
                final SapEvent sapEvent = new SapEvent(event);

                boolean successful = sapEvent.processPendingRequests(event, errorLog, elogger);
                if (!successful) {
                    return;
                }

                final Money debtFenix = eventWrapper.debt;
                final Money invoiceSap = sapEvent.getInvoiceAmount();

                boolean debtResult = false;
                if (debtFenix.isPositive()) {
                    if (invoiceSap.isZero()) {
                        debtResult =
                                sapEvent.registerInvoice(debtFenix, event, eventWrapper.isGratuity(), false, errorLog, elogger);
                    } else if (invoiceSap.isNegative()) {
                        logError(event, errorLog, elogger, "A dívida no SAP é negativa");
                    } else if (!debtFenix.equals(invoiceSap)) {
                        logError(event, errorLog, elogger, "A dívida no SAP é: " + invoiceSap.getAmountAsString()
                                + " e no Fénix é: " + debtFenix.getAmountAsString());
                        if (debtFenix.greaterThan(invoiceSap)) {
                            // criar invoice com a diferença entre debtFenix e invoiceDebtSap (se for propina aumentar a dívida no sap)
                            // passar data actual (o valor do evento mudou, não dá para saber quando, vamos assumir que mudou quando foi detectada essa diferença)
                            logError(event, errorLog, elogger, "A dívida no Fénix é superior à dívida registada no SAP");
                            debtResult = sapEvent.registerInvoice(debtFenix.subtract(invoiceSap), eventWrapper.event,
                                    eventWrapper.isGratuity(), true, errorLog, elogger);
                        } else {
                            // diminuir divida no sap e registar credit note da diferença na última factura existente
                            logError(event, errorLog, elogger, "A dívida no SAP é superior à dívida registada no Fénix");
                            CreditEntry creditEntry = getCreditEntry(debtFenix, invoiceSap);
                            debtResult = sapEvent.registerCredit(eventWrapper.event, creditEntry, eventWrapper.isGratuity(),
                                    errorLog, elogger);
                        }
                    }
                }

                // there could have been an error comunicating a debt, we can not comunicate payments and such, since there is nothing registered in SAP
                if (debtResult) {
                    //Payments!!
                    DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
                    List<Payment> payments = calculator.getPayments().collect(Collectors.toList());
                    for (Payment payment : payments) {
                        if (payment.isForDebt() && payment.getAmount().compareTo(BigDecimal.ZERO) > 0
                                && !sapEvent.hasPayment(payment.getId())
                                && payment.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD)) {
                            boolean result = sapEvent.registerPayment(payment, errorLog, elogger);
                            if (!result) {
                                return;
                            }
                        }
                    }

                    //Exemptions                    
                    for (CreditEntry creditEntry : calculator.getCreditEntries()) {
                        if (creditEntry instanceof DebtExemption) {
                            if (creditEntry.getAmount().compareTo(BigDecimal.ZERO) > 0 && !sapEvent.hasCredit(creditEntry.getId())
                                    && creditEntry.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD)) {
                                boolean result =
                                        sapEvent.registerCredit(event, creditEntry, eventWrapper.isGratuity(), errorLog, elogger);
                                if (!result) {
                                    return;
                                }
                            }
                        }
                    }

                    //Reimbursements
                    Money sapReimbursements = sapEvent.getReimbursementsAmount();
                    if (eventWrapper.reimbursements.greaterThan(sapReimbursements)) {
                        boolean result = sapEvent.registerReimbursement(eventWrapper.event,
                                eventWrapper.reimbursements.subtract(sapReimbursements), errorLog, elogger);
                        if (!result) {
                            return;
                        }
                    }
                }
            } else {
                //processing payments of past events
//                eventWrapper.paymentsSap().filter(d -> !sapEvent.hasPayment(d)).peek(
//                    d -> elogger.log("Processing past payment %s : %s%n", eventWrapper.event.getExternalId(), d.getExternalId()))
//                    .forEach(d -> sapEvent.registerInvoiceAndPayment(clientMap, d, errorLog, elogger));
            }
        } catch (final Exception e) {
            logError(errorLog, elogger, event, e);
        }
    }

    private static CreditEntry getCreditEntry(final Money debtFenix, final Money invoiceSap) {
        return new CreditEntry("", new DateTime(), new LocalDate(), "",
                invoiceSap.subtract(debtFenix).getAmount()) {

            @Override
            public boolean isToApplyInterest() {
                return false;
            }
            @Override
            public boolean isToApplyFine() {
                return false;
            }
            @Override
            public boolean isForInterest() {
                return false;
            }
            @Override
            public boolean isForFine() {
                return false;
            }
            @Override
            public boolean isForDebt() {
                return false;
            }
        };
    }

    private static void logError(final ErrorLogConsumer errorLog, final EventLogger elogger, final Event event,
            final Exception e) {
        final String errorMessage = e.getMessage();

        BigDecimal amount;
        DebtCycleType cycleType;

        try {
            amount = event.getOriginalAmountToPay().getAmount();
            cycleType = Utils.cycleType(event);
        } catch (Exception ex) {
            amount = null;
            cycleType = null;
        }

        errorLog.accept(event.getExternalId(), Utils.getUserIdentifier(event.getParty()), event.getParty().getName(),
                amount == null ? "" : amount.toPlainString(), cycleType == null ? "" : cycleType.getDescription(), errorMessage,
                "", "", "", "", "", "", "", "", "", "", "");
        elogger.log("%s: %s%n", event.getExternalId(), errorMessage);
        elogger.log(
                "Unhandled SAP error for event " + event.getExternalId() + " : " + e.getClass().getName() + " - " + errorMessage);
        e.printStackTrace();
    }

    private static void logError(Event event, ErrorLogConsumer errorLog, EventLogger elogger, String errorMessage) {
        BigDecimal amount;
        DebtCycleType cycleType;
        try {
            amount = event.getOriginalAmountToPay().getAmount();
            cycleType = Utils.cycleType(event);
        } catch (Exception ex) {
            amount = null;
            cycleType = null;
        }

        errorLog.accept(event.getExternalId(), Utils.getUserIdentifier(event.getParty()), event.getParty().getName(),
                amount == null ? "" : amount.toPlainString(), cycleType == null ? "" : cycleType.getDescription(), errorMessage,
                "", "", "", "", "", "", "", "", "", "", "");
        elogger.log("%s: %s %s %s %n", event.getExternalId(), errorMessage, "", "");
    }
}
