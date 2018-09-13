package pt.ist.fenixedu.integration.task;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEventWithPaymentPlan;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.gson.JsonObject;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class InitializeSapData extends CustomTask {

    private static final JsonObject EMPTY_JSON_OBJECT = new JsonObject();

    private ExecutionYear startYear = null;
    private static DateTime FIRST_DAY = new DateTime(2018, 01, 01, 00, 00);
    private static DateTime NOW = new DateTime();
    private static LocalDate LAST_DAY = new LocalDate(2017, 12, 31);

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    private boolean needsProcessing(final Event event) {
        try {
            final ExecutionYear executionYear = Utils.executionYearOf(event);
            return event.getParty().isPerson() && !event.isCancelled()
                    && !executionYear.isBefore(startYear) && (!event.getNonAdjustingTransactions().isEmpty()
                            || !event.getExemptionsSet().isEmpty() || !event.getDiscountsSet().isEmpty());
        } catch (final Exception e) {
            taskLog("O evento %s deu o seguinte erro: %s%n", event.getExternalId(), e.getMessage());
            return false;
        }
    }

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingEventsSet().stream()
            .parallel()
            .forEach(this::clearInit);

        startYear = ExecutionYear.readExecutionYearByName("2013/2014");

        Bennu.getInstance().getAccountingEventsSet().stream()
            .parallel()
            .forEach(this::init);
    }

    private void clearInit(final Event event) {
        FenixFramework.atomic(() -> event.getSapRequestSet().stream()
            .filter(sr -> sr.getRequest().equals("{}"))
            .forEach(sr -> sr.delete()));
    }

    private void init(final Event event) {
        try {
            FenixFramework.atomic(() -> {
                if (needsProcessing(event)) {
                    process(event);
                }
            });
        } catch (final Exception e) {
            taskLog("Erro no evento %s %s\n", event.getExternalId(), e.getMessage());
            e.printStackTrace();
        }
    }

    public void process(final Event event) {
        final DebtInterestCalculator debtInterestCalculator = event.getDebtInterestCalculator(NOW);
        final Money amountPayed = processPayments(event, debtInterestCalculator);
        processExemptions(event, amountPayed, debtInterestCalculator);
        processReimbursements(event);
    }

    private Money processPayments(final Event event, final DebtInterestCalculator debtInterestCalculator) {

        final Money paidAmount = new Money(debtInterestCalculator.getPaidDebtAmount());

        final String clientId = ClientMap.uVATNumberFor(event.getParty());

        if (paidAmount.isPositive()) {
            getLastYearPayments(debtInterestCalculator).filter(p -> p.getUsedAmountInDebts().compareTo(BigDecimal.ZERO) > 0)
                    .forEach(payment -> {

                        Money usedAmountInDebts = new Money(payment.getUsedAmountInDebts());
                        final SapRequest sapInvoiceRequest = new SapRequest(event, clientId, usedAmountInDebts, "ND0",
                                SapRequestType.INVOICE, Money.ZERO, EMPTY_JSON_OBJECT);
                        sapInvoiceRequest.setPayment(FenixFramework.getDomainObject(payment.getId()));
                        sapInvoiceRequest.setWhenSent(FIRST_DAY);
                        sapInvoiceRequest.setSent(true);
                        sapInvoiceRequest.setIntegrated(true);
                        sapInvoiceRequest.setOrder(0);

                        if (isToProcessDebt(event, event.isGratuity())) {
                            final SapRequest sapDebtRequest = new SapRequest(event, clientId, usedAmountInDebts, "NG0",
                                    SapRequestType.DEBT, Money.ZERO, EMPTY_JSON_OBJECT);
                            sapDebtRequest.setPayment(FenixFramework.getDomainObject(payment.getId()));
                            sapDebtRequest.setWhenSent(FIRST_DAY);
                            sapDebtRequest.setSent(true);
                            sapDebtRequest.setIntegrated(true);
                            sapDebtRequest.setOrder(0);
                        }

                        final SapRequest sapPaymentRequest = new SapRequest(event, clientId, usedAmountInDebts, "NP0",
                                SapRequestType.PAYMENT, Money.ZERO, EMPTY_JSON_OBJECT);
                        sapPaymentRequest.setPayment(FenixFramework.getDomainObject(payment.getId()));
                        sapPaymentRequest.setWhenSent(FIRST_DAY);
                        sapPaymentRequest.setSent(true);
                        sapPaymentRequest.setIntegrated(true);
                        sapPaymentRequest.setOrder(0);
                    });
        }

        final Money interestAndFineAmount =
                new Money(debtInterestCalculator.getPaidInterestAmount().add(debtInterestCalculator.getPaidFineAmount()));

        if (interestAndFineAmount.isPositive()) {
            getLastYearPayments(debtInterestCalculator)
                    .filter(p -> p.getUsedAmountInFines().add(p.getUsedAmountInInterests()).compareTo(BigDecimal.ZERO) > 0)
                    .forEach(payment -> {
                        Money payedInterestAndFineAmount =
                                new Money(payment.getUsedAmountInFines().add(payment.getUsedAmountInInterests()));

                        final SapRequest sapInvoiceRequest = new SapRequest(event, clientId, payedInterestAndFineAmount, "ND0",
                                SapRequestType.INVOICE_INTEREST, Money.ZERO, EMPTY_JSON_OBJECT);
                        sapInvoiceRequest.setPayment(FenixFramework.getDomainObject(payment.getId()));
                        sapInvoiceRequest.setWhenSent(FIRST_DAY);
                        sapInvoiceRequest.setSent(true);
                        sapInvoiceRequest.setIntegrated(true);
                        sapInvoiceRequest.setOrder(0);

                        final SapRequest sapPaymentRequest = new SapRequest(event, clientId, payedInterestAndFineAmount, "NP0",
                                SapRequestType.PAYMENT_INTEREST, Money.ZERO, EMPTY_JSON_OBJECT);
                        sapPaymentRequest.setPayment(FenixFramework.getDomainObject(payment.getId()));
                        sapPaymentRequest.setWhenSent(FIRST_DAY);
                        sapPaymentRequest.setSent(true);
                        sapPaymentRequest.setIntegrated(true);
                        sapPaymentRequest.setOrder(0);
                    });
        }

        getLastYearPayments(debtInterestCalculator).forEach(payment -> {

            Money unusedAmount = new Money(payment.getUnusedAmount());
            if (unusedAmount.isPositive()) {
                final SapRequest sapAdvancementRequest = new SapRequest(event, clientId, Money.ZERO, "NP0",
                        SapRequestType.ADVANCEMENT, unusedAmount, EMPTY_JSON_OBJECT);
                sapAdvancementRequest.setPayment(FenixFramework.getDomainObject(payment.getId()));
                sapAdvancementRequest.setWhenSent(FIRST_DAY);
                sapAdvancementRequest.setSent(true);
                sapAdvancementRequest.setIntegrated(true);
                sapAdvancementRequest.setOrder(0);
            }
        });

        return paidAmount;
    }

    private Stream<Payment> getLastYearPayments(final DebtInterestCalculator debtInterestCalculator) {
        return debtInterestCalculator.getPayments().filter(p -> !p.getDate().isAfter(LAST_DAY));
    }

    private boolean isPartialRegime(final Event event) {
        return event instanceof GratuityEventWithPaymentPlan
                && ((GratuityEventWithPaymentPlan) event).getGratuityPaymentPlan().isForPartialRegime();
    }

    private Stream<CreditEntry> getLastYearExemptions(final DebtInterestCalculator debtInterestCalculator) {
        return debtInterestCalculator.getCreditEntries().stream().filter(DebtExemption.class::isInstance)
                .filter(c -> !c.getDate().isAfter(LAST_DAY));
    }

    private void processExemptions(final Event event, final Money amountPayed,
            final DebtInterestCalculator debtInterestCalculator) {

        getLastYearExemptions(debtInterestCalculator).filter(c -> c.getAmount().compareTo(BigDecimal.ZERO) > 0).forEach(c -> {

            Money amountToRegister = new Money(c.getUsedAmountInDebts());

            //TODO remove when fully tested, if we register a different value for the exemption
            //when the sync script runs it will detect a different amount for the exemptions and it will try to rectify
            //when the events are referring to partial regime
            if (amountPayed.isPositive() && isPartialRegime(event)) {
                final Money originalAmount = event.getOriginalAmountToPay();
                if (amountToRegister.add(amountPayed).greaterThan(originalAmount)) {
                    taskLog("Evento: %s # Montante original: %s # montante pago: %s # montante a registar: %s\n",
                            event.getExternalId(), originalAmount, amountPayed, amountToRegister);
                    amountToRegister = originalAmount.subtract(amountPayed);
                }
            }

            final String clientId = ClientMap.uVATNumberFor(event.getParty());

            //an exemption should be considered as a payment for the initialization
            final SapRequest sapInvoiceRequest = new SapRequest(event, clientId, amountToRegister, "ND0", SapRequestType.INVOICE,
                    Money.ZERO, EMPTY_JSON_OBJECT);
            sapInvoiceRequest.setCreditId(c.getId());
            sapInvoiceRequest.setWhenSent(FIRST_DAY);
            sapInvoiceRequest.setSent(true);
            sapInvoiceRequest.setIntegrated(true);
            sapInvoiceRequest.setOrder(0);

            final SapRequest sapCreditRequest = new SapRequest(event, clientId, amountToRegister, "NA0", SapRequestType.CREDIT,
                    Money.ZERO, EMPTY_JSON_OBJECT);
            sapCreditRequest.setCreditId(c.getId());
            sapCreditRequest.setWhenSent(FIRST_DAY);
            sapCreditRequest.setSent(true);
            sapCreditRequest.setIntegrated(true);
            sapCreditRequest.setOrder(0);
            if (isToProcessDebt(event, event.isGratuity())) {
                final SapRequest sapDebtRequest = new SapRequest(event, clientId, amountToRegister, "NG0", SapRequestType.DEBT,
                        Money.ZERO, EMPTY_JSON_OBJECT);
                sapDebtRequest.setCreditId(c.getId());
                sapDebtRequest.setWhenSent(FIRST_DAY);
                sapDebtRequest.setSent(true);
                sapDebtRequest.setIntegrated(true);
                sapDebtRequest.setOrder(0);

                final SapRequest sapDebtCreditRequest = new SapRequest(event, clientId, amountToRegister, "NJ0",
                        SapRequestType.DEBT_CREDIT, Money.ZERO, EMPTY_JSON_OBJECT);
                sapDebtCreditRequest.setCreditId(c.getId());
                sapDebtCreditRequest.setWhenSent(FIRST_DAY);
                sapDebtCreditRequest.setSent(true);
                sapDebtCreditRequest.setIntegrated(true);
                sapDebtCreditRequest.setOrder(0);
            }
        });
    }

    private void processReimbursements(final Event event) {
        final Money amountToRegister = event.getAccountingTransactionsSet().stream()
                .filter(at -> at.getWhenRegistered().getYear() < 2018).flatMap(at -> at.getEntriesSet().stream())
                .flatMap(e -> e.getReceiptsSet().stream()).flatMap(r -> r.getCreditNotesSet().stream())
                .filter(cn -> !cn.isAnnulled()).flatMap(cn -> cn.getCreditNoteEntriesSet().stream()).map(cne -> cne.getAmount())
                .reduce(Money.ZERO, Money::add);
        if (amountToRegister.isPositive()) {
            taskLog("O evento: %s - tem nota de crédito associada, no valor de: %s\n", event.getExternalId(),
                    amountToRegister.toPlainString());

            final String clientId = ClientMap.uVATNumberFor(event.getParty());

            final SapRequest sapRequest = new SapRequest(event, clientId, amountToRegister, "NR0", SapRequestType.REIMBURSEMENT,
                    Money.ZERO, EMPTY_JSON_OBJECT);
            sapRequest.setWhenSent(FIRST_DAY);
            sapRequest.setSent(true);
            sapRequest.setIntegrated(true);
            sapRequest.setOrder(0);
        }
    }

    private boolean isToProcessDebt(Event event, boolean isGratuity) {
        return isGratuity && !event.getWhenOccured().isBefore(FIRST_DAY);
    }
}