package pt.ist.fenixedu.integration.task;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.BigDecimalUtil;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEventWithPaymentPlan;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import com.google.gson.JsonObject;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixedu.giaf.invoices.Utils;

public class InitializeSapData extends CustomTask {

    private static final JsonObject EMPTY_JSON_OBJECT = new JsonObject();

    private ExecutionYear startYear = null;
    private ExecutionYear SAP_3RD_CYCLE_THRESHOLD = null;
    private static DateTime FIRST_DAY = new DateTime(2018, 01, 01, 00, 00);
    private static DateTime LAST_DAY = new DateTime(2017, 12, 31, 23, 59, 59);
    private Money payments = Money.ZERO;
    private Money exemptions = Money.ZERO;

    private boolean needsProcessing(final Event event) {
        try {
            final ExecutionYear executionYear = Utils.executionYearOf(event);
            return event.getSapRequestSet().isEmpty() && !event.isCancelled()
                    && (!executionYear.isBefore(startYear)
                            || (event instanceof PhdGratuityEvent && !executionYear.isBefore(SAP_3RD_CYCLE_THRESHOLD)))
                    && (!event.getAccountingTransactionsSet().isEmpty() || !event.getExemptionsSet().isEmpty());
        } catch (final Exception e) {
            taskLog("O evento %s deu o seguinte erro: %s%n", event.getExternalId(), e.getMessage());
            return false;
        }
    }

    @Override
    public void runTask() throws Exception {
        payments = Money.ZERO;
        exemptions = Money.ZERO;

        startYear = ExecutionYear.readExecutionYearByName("2015/2016");
        SAP_3RD_CYCLE_THRESHOLD = ExecutionYear.readExecutionYearByName("2014/2015");
        // in case transaction restarts... reset state.
        payments = Money.ZERO;
        exemptions = Money.ZERO;

        Bennu.getInstance().getAccountingEventsSet().stream()
            .filter(this::needsProcessing)
            .forEach(event -> {
                try {
                    process(event);
                } catch (final Exception e) {
                    taskLog("Erro no evento %s %s\n", event.getExternalId(), e.getMessage());
                    e.printStackTrace();
                }                
            });

        taskLog("O valor dos pagamentos foi de %s\n", payments.toPlainString());
        taskLog("O valor das insenções foi de %s\n", exemptions.toPlainString());
    }

    public void process(final Event event) {
        taskLog("Processing event: %s\n", event.getExternalId());
        final DebtInterestCalculator debtInterestCalculator = event.getDebtInterestCalculator(LAST_DAY);
        final Money amountPayed = processPayments(event, debtInterestCalculator);
        processExemptions(event, amountPayed, debtInterestCalculator);
        processReimbursements(event);
    }

    private Money processPayments(final Event event, final DebtInterestCalculator debtInterestCalculator) {

        final Money paidAmount = new Money(debtInterestCalculator.getPaidDebtAmount());

        final String clientId;
        if (event.getParty().isPerson()) {
            clientId = ClientMap.uVATNumberFor(event.getPerson());
        } else {
            //clientId = event.getParty().getExternalId();
            //taskLog("Dívida de empresa!! - %s\n", clientId);
            return Money.ZERO;
        }

        if (paidAmount.isPositive()) {
            payments = payments.add(paidAmount);

            final SapRequest sapInvoiceRequest =
                    new SapRequest(event, clientId, paidAmount, "ND0", SapRequestType.INVOICE, Money.ZERO, EMPTY_JSON_OBJECT);
            sapInvoiceRequest.setWhenSent(FIRST_DAY);
            sapInvoiceRequest.setSent(true);
            sapInvoiceRequest.setIntegrated(true);
            if (isToProcessDebt(event, event.isGratuity())) {
                final SapRequest sapDebtRequest =
                        new SapRequest(event, clientId, paidAmount, "NG0", SapRequestType.DEBT, Money.ZERO, EMPTY_JSON_OBJECT);
                sapDebtRequest.setWhenSent(FIRST_DAY);
                sapDebtRequest.setSent(true);
                sapDebtRequest.setIntegrated(true);
            }

            final SapRequest sapPaymentRequest =
                    new SapRequest(event, clientId, paidAmount, "NP0", SapRequestType.PAYMENT, Money.ZERO, EMPTY_JSON_OBJECT);
            sapPaymentRequest.setWhenSent(FIRST_DAY);
            sapPaymentRequest.setSent(true);
            sapPaymentRequest.setIntegrated(true);
        }

        final Money interestAndfineAmount =
                new Money(debtInterestCalculator.getPaidInterestAmount().add(debtInterestCalculator.getPaidFineAmount()));
        if (interestAndfineAmount.isPositive()) {
            payments = payments.add(interestAndfineAmount);

            final SapRequest sapInvoiceRequest = new SapRequest(event, clientId, interestAndfineAmount, "ND0",
                    SapRequestType.INVOICE_INTEREST, Money.ZERO, EMPTY_JSON_OBJECT);
            sapInvoiceRequest.setWhenSent(FIRST_DAY);
            sapInvoiceRequest.setSent(true);
            sapInvoiceRequest.setIntegrated(true);

            final SapRequest sapPaymentRequest = new SapRequest(event, clientId, interestAndfineAmount, "NP0",
                    SapRequestType.PAYMENT_INTEREST, Money.ZERO, EMPTY_JSON_OBJECT);
            sapPaymentRequest.setWhenSent(FIRST_DAY);
            sapPaymentRequest.setSent(true);
        }

        if (paidAmount.add(interestAndfineAmount).isZero()) {
            final Money totalAmount = new Money(BigDecimalUtil.sum(debtInterestCalculator.getPayments(), Payment::getAmount));
            if (totalAmount.isPositive()) {
                final SapRequest sapAdvancementRequest = new SapRequest(event, clientId, Money.ZERO, "NP0",
                        SapRequestType.ADVANCEMENT, totalAmount, EMPTY_JSON_OBJECT);
                sapAdvancementRequest.setWhenSent(FIRST_DAY);
                sapAdvancementRequest.setSent(true);
                sapAdvancementRequest.setIntegrated(true);
            }
        }
        return Money.ZERO;
    }

    private boolean isPartialRegime(final Event event) {
        return event instanceof GratuityEventWithPaymentPlan && ((GratuityEventWithPaymentPlan) event).getGratuityPaymentPlan().isForPartialRegime();
    }

    private void processExemptions(final Event event, final Money amountPayed, final DebtInterestCalculator debtInterestCalculator) {

        final Money amountToRegister = new Money(debtInterestCalculator.getDebtExemptionAmount()
                .add(debtInterestCalculator.getInterestExemptionAmount()).add(debtInterestCalculator.getFineExemptionAmount()));
        if (amountToRegister.isPositive()) {
            //TODO remove when fully tested, if we register a different value for the exemption
            //when the sync script runs it will detect a different amount for the exemptions and it will try to rectify

            //when the events are referring to partial regime
            if (amountPayed.isPositive() && isPartialRegime(event)) {
                final Money originalAmount = event.getOriginalAmountToPay();
                if (amountToRegister.add(amountPayed).greaterThan(originalAmount)) {
                    taskLog("Evento: %s # Montante original: %s # montante pago: %s # montante a registar: %s\n",
                            event.getExternalId(), originalAmount, amountPayed, amountToRegister);
//                    amountToRegister = originalAmount.subtract(amountPayed);
                }
            }
            exemptions = exemptions.add(amountToRegister);

            final String clientId;
            if (event.getParty().isPerson()) {
                clientId = ClientMap.uVATNumberFor(event.getPerson());
            } else {
                clientId = event.getParty().getExternalId();
                //taskLog("Dívida de empresa!! - %s\n", clientId);
                return;
            }

            //an exemption should be considered as a payment for the initialization
            final SapRequest sapInvoiceRequest = new SapRequest(event, clientId, amountToRegister, "ND0", SapRequestType.INVOICE,
                    Money.ZERO, EMPTY_JSON_OBJECT);
            sapInvoiceRequest.setWhenSent(FIRST_DAY);
            sapInvoiceRequest.setSent(true);
            sapInvoiceRequest.setIntegrated(true);

            final SapRequest sapCreditRequest =
                    new SapRequest(event, clientId, amountToRegister, "NA0", SapRequestType.CREDIT, Money.ZERO, EMPTY_JSON_OBJECT);
            sapCreditRequest.setWhenSent(FIRST_DAY);
            sapCreditRequest.setSent(true);
            sapCreditRequest.setIntegrated(true);
            if (isToProcessDebt(event, event.isGratuity())) {
                final SapRequest sapDebtRequest = new SapRequest(event, clientId, amountToRegister, "NG0", SapRequestType.DEBT,
                        Money.ZERO, EMPTY_JSON_OBJECT);
                sapDebtRequest.setWhenSent(FIRST_DAY);
                sapDebtRequest.setSent(true);
                sapDebtRequest.setIntegrated(true);

                final SapRequest sapDebtCreditRequest = new SapRequest(event, clientId, amountToRegister, "NJ0",
                        SapRequestType.DEBT_CREDIT, Money.ZERO, EMPTY_JSON_OBJECT);
                sapDebtCreditRequest.setWhenSent(FIRST_DAY);
                sapDebtCreditRequest.setSent(true);
                sapDebtCreditRequest.setIntegrated(true);
            }
        }
    }

    private void processReimbursements(final Event event) {
        final Money amountToRegister = event.getAccountingTransactionsSet().stream().flatMap(at -> at.getEntriesSet().stream())
                .flatMap(e -> e.getReceiptsSet().stream()).flatMap(r -> r.getCreditNotesSet().stream())
                .filter(cn -> !cn.isAnnulled()).flatMap(cn -> cn.getCreditNoteEntriesSet().stream()).map(cne -> cne.getAmount())
                .reduce(Money.ZERO, Money::add);
        if (amountToRegister.isPositive()) {
            taskLog("O evento: %s - tem nota de crédito associada, no valor de: %s\n", event.getExternalId(),
                    amountToRegister.toPlainString());

            final String clientId;
            if (event.getParty().isPerson()) {
                clientId = ClientMap.uVATNumberFor(event.getPerson());
            } else {
                clientId = event.getParty().getExternalId();
                //taskLog("Dívida de empresa!! - %s\n", clientId);
                return;
            }
            final SapRequest sapRequest = new SapRequest(event, clientId, amountToRegister, "NR0", SapRequestType.REIMBURSEMENT,
                    Money.ZERO, EMPTY_JSON_OBJECT);
            sapRequest.setWhenSent(FIRST_DAY);
            sapRequest.setSent(true);
            sapRequest.setIntegrated(true);
        }
    }

    private boolean isToProcessDebt(Event event, boolean isGratuity) {
        return isGratuity && !event.getWhenOccured().isBefore(FIRST_DAY);
    }
}
