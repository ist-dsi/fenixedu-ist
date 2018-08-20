package pt.ist.fenixedu.giaf.invoices;

import java.time.Year;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.CreditNoteEntry;
import org.fenixedu.academic.domain.accounting.CreditNoteState;
import org.fenixedu.academic.domain.accounting.Entry;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;

public class EventWrapper {

    public final static DateTime THRESHOLD = new DateTime(2015, 12, 1, 0, 0, 0, 0);
    public final static ExecutionYear SAP_THRESHOLD = ExecutionYear.readExecutionYearByName("2013/2014");
    public final static DateTime SAP_TRANSACTIONS_THRESHOLD = new DateTime(2017, 12, 31, 23, 59, 59, 999);

    public final static DateTime LIMIT = new DateTime(2017, 12, 31, 23, 59, 59, 999);

    public static Stream<Event> eventsToProcess(final ErrorLogConsumer consumer, final Stream<Event> eventStream,
            final Stream<AccountingTransactionDetail> txStream) {
        final Stream<Event> currentEvents = eventStream.filter(EventWrapper::needsProcessing)
                .filter(e -> Utils.validate(consumer, e)).filter(e -> e.getWhenOccured().isBefore(LIMIT));

        final int currentYear = 2017;
        final Stream<Event> pastEvents = txStream.filter(d -> d.getWhenRegistered().getYear() == currentYear)
                .map(d -> d.getEvent()).filter(e -> !needsProcessing(e)).filter(e -> Utils.validate(consumer, e));

        return Stream.concat(currentEvents, pastEvents).distinct().filter(e -> okToProcessPayments(e));
    }

    private static boolean okToProcessPayments(final Event e) {
        for (final AccountingTransaction tx : e.getAccountingTransactionsSet()) {
            if (tx.getWhenRegistered().isAfter(LIMIT)) {
                return false;
            }
        }
        return true;
    }

    public static boolean needsProcessing(final Event event) {
        final ExecutionYear executionYear = Utils.executionYearOf(event);
        final int year = executionYear.getBeginLocalDate().getYear();
        return year >= THRESHOLD.getYear() || event.getWhenOccured().isAfter(THRESHOLD);
    }

    public static boolean needsProcessingSap(final Event event) {
        final ExecutionYear executionYear = Utils.executionYearOf(event);
        return !executionYear.isBefore(SAP_THRESHOLD);
    }

    public static Stream<Event> eventsToProcessSap(final ErrorLogConsumer consumer, final Stream<Event> eventStream,
            final Stream<AccountingTransactionDetail> txStream) {
        final Stream<Event> currentEvents =
                eventStream.filter(EventWrapper::needsProcessingSap).filter(e -> Utils.validate(consumer, e));

        final int currentYear = Year.now().getValue();
        final Stream<Event> pastEvents = txStream.filter(d -> d.getWhenRegistered().getYear() == currentYear)
                .map(d -> d.getEvent()).filter(e -> !needsProcessingSap(e)).filter(e -> Utils.validate(consumer, e));

        return Stream.concat(currentEvents, pastEvents).distinct();
    }


    public final Event event;
    public final Money debt;
    public final Money exempt;
    public final Money payed;
    public final Money fines;
    public final Money reimbursements;

    public EventWrapper(final Event event, final pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer errorLogConsumer, boolean sap) {
        this.event = event;

        final Money payedTotal = calculateAmountPayed(null, errorLogConsumer);
        //only events >= SAP_THRESHOLD are analyzed and there are no payments referring those events made before
        final Money payedBeforThreshold = sap ? Money.ZERO : calculateAmountPayed(THRESHOLD, errorLogConsumer);
        final Money payedAfterThreshhold = payedTotal.subtract(payedBeforThreshold);

        // calculate debt        
        final Money value = event.getOriginalAmountToPay();
        final Money diff = value.subtract(payedBeforThreshold);
        debt = diff.isPositive() ? diff : Money.ZERO;

        // calculate exemptions and discounts
        {
            final Money discounts = discounts();
            final Money excemptions = exemptionsValue(value);
            exempt = discounts.add(excemptions);
        }

        // calculate payed amount
        payed = debt.lessThan(payedAfterThreshhold) ? debt : payedAfterThreshhold;

        // calculate fines
        {
            final Money exessPayment = payedAfterThreshhold.subtract(debt);
            fines = exessPayment.isPositive() ? exessPayment : Money.ZERO;
        }

        // calculate reimbursements
        {
            reimbursements = event.getAccountingTransactionsSet().stream().flatMap(at -> at.getEntriesSet().stream())
                    .flatMap(e -> e.getReceiptsSet().stream()).flatMap(r -> r.getCreditNotesSet().stream())
                    .filter(cn -> CreditNoteState.PAYED.equals(cn.getState())) //TODO rever este filtro
                    .flatMap(c -> c.getCreditNoteEntriesSet().stream()).map(cne -> cne.getAmount())
                    .reduce(Money.ZERO, Money::add);

//            reimbursements = event.getAccountingTransactionsSet().stream().map(at -> at.getToAccountEntry())
//                    .map(e -> e.getAdjustmentCreditNoteEntry()).filter(Objects::nonNull).map(cne -> cne.getAmount())
//                    .reduce(Money.ZERO, Money::add);
        }
    }

    public Money amountStillInDebt() {
        return debt.subtract(exempt).subtract(payed);
    }

    private Money calculateAmountPayed(final DateTime threshold,
            final pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer errorLogConsumer) {
        return event.getAccountingTransactionsSet().stream()
                .filter(t -> threshold == null || t.getWhenRegistered().isBefore(threshold))
                .filter(t -> Utils.validate(errorLogConsumer, t.getTransactionDetail())).map(t -> t.getAmountWithAdjustment())
                .reduce(Money.ZERO, Money::add);
    }

    private Money discounts() {
        return event.getDiscountsSet().stream().map(d -> d.getAmount()).reduce(Money.ZERO, Money::add);
    }

    private Money exemptionsValue(Money eventAmount) {
        return event.getExemptionsSet().stream().map(e -> e.getExemptionAmount(eventAmount)).reduce(Money.ZERO,
                Money::add);
    }

    public Stream<AccountingTransactionDetail> payments() {
        final Stream<AccountingTransactionDetail> stream =
                event.getAccountingTransactionsSet().stream().map(at -> at.getTransactionDetail());
        return stream
                //.filter(d -> d.getWhenRegistered().getYear() >= START_YEAR_TO_CONSIDER_TRANSACTIONS)
                .filter(d -> d.getWhenRegistered().isAfter(THRESHOLD)).filter(d -> !isCreditNote(d))
                .filter(d -> Utils.validate(null, d));
    }

    private boolean isCreditNote(AccountingTransactionDetail detail) {
        final Entry entry = detail.getTransaction().getToAccountEntry();
        final CreditNoteEntry creditNoteEntry = entry.getAdjustmentCreditNoteEntry();
        return creditNoteEntry != null;
    }

    public boolean isGratuity() {
        return this.event.isGratuity();
    }
}
