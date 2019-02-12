package pt.ist.fenixedu.giaf.invoices;

import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Refund;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.ExcessRefund;
import org.fenixedu.academic.domain.accounting.calculator.PartialPayment;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;

public class EventWrapper {

    public final static DateTime THRESHOLD = new DateTime(2015, 12, 1, 0, 0, 0, 0);
    public final static ExecutionYear SAP_THRESHOLD = ExecutionYear.readExecutionYearByName("2013/2014");
    public final static DateTime SAP_TRANSACTIONS_THRESHOLD = new DateTime(2017, 12, 31, 23, 59, 59, 999);

    public final static DateTime LIMIT = new DateTime(2017, 12, 31, 23, 59, 59, 999);

    public static boolean needsProcessingSap(final Event event) {
        final ExecutionYear executionYear = Utils.executionYearOf(event);
        return !executionYear.isBefore(SAP_THRESHOLD);
    }

    public static boolean needsToProcessPayments(Event event) {
        final int currentYear = Year.now().getValue();
        return event.getAccountingTransactionsSet().stream().anyMatch(tx -> tx.getWhenRegistered().getYear() == currentYear
                || tx.getWhenRegistered().getYear() == currentYear -1);
    }

    public static boolean shouldProcess(final ErrorLogConsumer consumer, final Event event) {
        return event.getSapRequestSet().stream().allMatch(r -> r.getIntegrated())
                && allAdvancementFromRefundSapIntegration(event)
                && (needsProcessingSap(event) || needsToProcessPayments(event))
                && Utils.validate(consumer, event);
    }

    public final Event event;
    public final Money debt;

    public EventWrapper(final Event event, final pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer errorLogConsumer, boolean sap) {
        this.event = event;

        //only events >= SAP_THRESHOLD are analyzed and there are no payments referring those events made before
        final Money payedBeforThreshold = sap ? Money.ZERO : calculateAmountPayed(THRESHOLD, errorLogConsumer);

        // calculate debt        
        final Money value = event.getOriginalAmountToPay();
        final Money diff = value.subtract(payedBeforThreshold);
        debt = diff.isPositive() ? diff : Money.ZERO;
    }

    private Money calculateAmountPayed(final DateTime threshold,
            final pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer errorLogConsumer) {
        return event.getAccountingTransactionsSet().stream()
                .filter(t -> threshold == null || t.getWhenRegistered().isBefore(threshold))
                .filter(t -> Utils.validate(errorLogConsumer, t.getTransactionDetail())).map(t -> t.getAmountWithAdjustment())
                .reduce(Money.ZERO, Money::add);
    }

    public boolean isGratuity() {
        return this.event.isGratuity();
    }

    private static boolean allAdvancementFromRefundSapIntegration(final Event event) {
        final boolean pendingIntegration = event.getAccountingTransactionsSet().stream()
            .map(tx -> tx.getRefund())
            .filter(r -> r != null)
            .anyMatch(r -> notIntegrated(r));
        return !pendingIntegration;
    }

    private static boolean notIntegrated(final Refund refund) {
        final Event event = refund.getEvent();
        final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
        final ExcessRefund excessRefund = calculator.getExcessRefundStream().filter(r -> r.getId().equals(refund.getExternalId())).findAny().orElse(null);
        final List<PartialPayment> partialPayments = excessRefund.getPartialPayments();
        return excessRefund == null || partialPayments.isEmpty() || !partialPayments.stream().allMatch(pp -> isIntegrated(event, pp));
    }

    private static boolean isIntegrated(final Event event, final PartialPayment partialPayment) {
        final Set<SapRequest> request = event.getSapRequestSet().stream()
            .filter(sr -> sr.getRequestType() == SapRequestType.ADVANCEMENT)
            .filter(sr -> !sr.getIgnore() && !sr.isInitialization() && sr.getIntegrated())
            .filter(sr -> sr.getPayment().getExternalId().equals(partialPayment.getDebtEntry().getId()))
            .collect(Collectors.toSet());
        return !request.isEmpty();
    }

}
