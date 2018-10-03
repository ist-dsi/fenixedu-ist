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
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

public class EventProcessor {

    public static void syncEventWithSap(final ErrorLogConsumer errorLog, final EventLogger elogger, final Event event) {
        try {
            FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<Void>() {

                @Override
                public Void call() {
                    syncToSap(errorLog, elogger, event);
                    return null;
                }
            }, new AtomicInstance(TxMode.SPECULATIVE_READ, false));
        } catch (Exception e) {
            logError(errorLog, elogger, event, e);
            e.printStackTrace();
        }
    }

    private static void syncToSap(final ErrorLogConsumer errorLog, final EventLogger elogger, final Event event) {
        try {
            SapEvent sapEvent = new SapEvent(event);
            sapEvent.processPendingRequests(event, errorLog, elogger);
        } catch (final Exception e) {
            logError(errorLog, elogger, event, e);
        }
    }

    public static void registerEventSapRequests(final ErrorLogConsumer consumer, final EventLogger elogger, final Event event) {
        try {
            FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<Void>() {

                @Override
                public Void call() {
                    processSap(consumer, elogger, event);
                    return null;
                }
            }, new AtomicInstance(TxMode.SPECULATIVE_READ, false));
        } catch (Throwable e) {
            logError(consumer, elogger, event, e);
            e.printStackTrace();
        }
    }

    private static void processSap(final ErrorLogConsumer errorLog, final EventLogger elogger, final Event event) {
        if (!EventWrapper.shouldProcess(errorLog, event)) {
            return;
        }
            if (EventWrapper.needsProcessingSap(event)) {
                final SapEvent sapEvent = new SapEvent(event);
                if (sapEvent.hasPendingDocumentCancelations()) {
                    return;
                }

                final EventWrapper eventWrapper = new EventWrapper(event, errorLog, true);

                sapEvent.updateInvoiceWithNewClientData();

                final Money debtFenix = eventWrapper.debt;
                final Money invoiceSap = sapEvent.getInvoiceAmount();

                if (debtFenix.isPositive()) {
                    if (invoiceSap.isZero()) {
                        sapEvent.registerInvoice(debtFenix, event, eventWrapper.isGratuity(), false);
                    } else if (invoiceSap.isNegative()) {
                        logError(event, errorLog, elogger, "A dívida no SAP é negativa");
                    } else if (!debtFenix.equals(invoiceSap)) {
                        logError(event, errorLog, elogger, "A dívida no SAP é: " + invoiceSap.getAmountAsString()
                                + " e no Fénix é: " + debtFenix.getAmountAsString());
                        if (debtFenix.greaterThan(invoiceSap)) {
                            // criar invoice com a diferença entre debtFenix e invoiceDebtSap (se for propina aumentar a dívida no sap)
                            // passar data actual (o valor do evento mudou, não dá para saber quando, vamos assumir que mudou quando foi detectada essa diferença)
                            logError(event, errorLog, elogger, "A dívida no Fénix é superior à dívida registada no SAP");
                            sapEvent.registerInvoice(debtFenix.subtract(invoiceSap), eventWrapper.event,
                                    eventWrapper.isGratuity(), true);
                        } else {
                            // diminuir divida no sap e registar credit note da diferença na última factura existente
                            logError(event, errorLog, elogger, "A dívida no SAP é superior à dívida registada no Fénix");
                            CreditEntry creditEntry = getCreditEntry(invoiceSap.subtract(debtFenix));
                            sapEvent.registerCredit(eventWrapper.event, creditEntry, eventWrapper.isGratuity(), errorLog,
                                    elogger);
                        }
                    }
                }

                //Payments!!
                DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
                List<Payment> payments = calculator.getPayments().collect(Collectors.toList());
                for (Payment payment : payments) {
                    if (payment.isForDebt() && payment.getAmount().compareTo(BigDecimal.ZERO) > 0
                            && !sapEvent.hasPayment(payment.getId())
                            && payment.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD)) {
                        sapEvent.registerPayment(payment, errorLog, elogger);
                    }
                }

                //Exemptions                    
                for (CreditEntry creditEntry : calculator.getCreditEntries()) {
                    if (creditEntry instanceof DebtExemption) {
                        if (creditEntry.getAmount().compareTo(BigDecimal.ZERO) > 0 && !sapEvent.hasCredit(creditEntry.getId())
                                && creditEntry.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD)) {
                            sapEvent.registerCredit(event, creditEntry, eventWrapper.isGratuity(), errorLog, elogger);
                        }
                    }
                }

                //Reimbursements
                Money sapReimbursements = sapEvent.getReimbursementsAmount();
                if (eventWrapper.reimbursements.greaterThan(sapReimbursements)) {
                    sapEvent.registerReimbursement(eventWrapper.event, eventWrapper.reimbursements.subtract(sapReimbursements),
                            errorLog, elogger);
                }
            } else {
                //processing payments of past events
//                eventWrapper.paymentsSap().filter(d -> !sapEvent.hasPayment(d)).peek(
//                    d -> elogger.log("Processing past payment %s : %s%n", eventWrapper.event.getExternalId(), d.getExternalId()))
//                    .forEach(d -> sapEvent.registerInvoiceAndPayment(clientMap, d, errorLog, elogger));
            }

    }

    static CreditEntry getCreditEntry(final Money creditAmount) {
        return new CreditEntry("", new DateTime(), new LocalDate(), "", creditAmount.getAmount()) {
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
            final Throwable e) {
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
