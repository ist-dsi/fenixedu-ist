package pt.ist.fenixedu.giaf.invoices;

import java.time.Year;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.CreditNoteEntry;
import org.fenixedu.academic.domain.accounting.Entry;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.PostingRule;
import org.fenixedu.academic.domain.accounting.events.AcademicEventExemption;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceExemption;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsurancePenaltyExemption;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeExemption;
import org.fenixedu.academic.domain.accounting.events.ImprovementOfApprovedEnrolmentPenaltyExemption;
import org.fenixedu.academic.domain.accounting.events.InsuranceExemption;
import org.fenixedu.academic.domain.accounting.events.PenaltyExemption;
import org.fenixedu.academic.domain.accounting.events.candidacy.SecondCycleIndividualCandidacyExemption;
import org.fenixedu.academic.domain.accounting.events.gratuity.PercentageGratuityExemption;
import org.fenixedu.academic.domain.accounting.events.gratuity.ValueGratuityExemption;
import org.fenixedu.academic.domain.accounting.events.gratuity.exemption.penalty.InstallmentPenaltyExemption;
import org.fenixedu.academic.domain.phd.debts.PhdEventExemption;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityExternalScholarshipExemption;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityFineExemption;
import org.fenixedu.academic.domain.phd.debts.PhdRegistrationFeePenaltyExemption;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;

public class EventWrapper {

    public final static DateTime THRESHOLD = new DateTime(2015, 12, 1, 0, 0, 0, 0);

    public static Stream<Event> eventsToProcess(final ErrorLogConsumer consumer,
            final Stream<Event> eventStream, final Stream<AccountingTransactionDetail> txStream) {
        final Stream<Event> currentEvents = eventStream
                .filter(EventWrapper::needsProcessing)
                .filter(e -> Utils.validate(consumer, e))
                ;

        final int currentYear = Year.now().getValue();
        final Stream<Event> pastEvents = txStream
            .filter(d -> d.getWhenRegistered().getYear() == currentYear)
            .map(d -> d.getEvent())
            .filter(e -> !needsProcessing(e))
            .filter(e -> Utils.validate(consumer, e))
            ;

        return Stream.concat(currentEvents, pastEvents).distinct();        
    }

    public static boolean needsProcessing(final Event event) {
        final ExecutionYear executionYear = Utils.executionYearOf(event);
        final int year = executionYear.getBeginLocalDate().getYear();
        return year >= THRESHOLD.getYear() || event.getWhenOccured().isAfter(THRESHOLD);
    }

    public final Event event;
    public final Money debt;
    public final Money exempt;
    public final Money payed;
    public final Money fines;

    public EventWrapper(final Event event, final pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer errorLogConsumer) {
        this.event = event;

        final Money payedTotal = calculateAmountPayed(null, errorLogConsumer);
        final Money payedBeforThreshold = calculateAmountPayed(THRESHOLD, errorLogConsumer);
        final Money payedAfterThreshhold = payedTotal.subtract(payedBeforThreshold);

        // calculate debt
        {
            final Money value = event.isCancelled() ? Money.ZERO : Utils.calculateTotalDebtValue(event);
            final Money diff = value.subtract(payedBeforThreshold);
            debt = diff.isPositive() ? diff : Money.ZERO;
        }

        // calculate exemptions and discounts
        {
            final Money discounts = discounts();
            final Money excemptions = excemptions();
            exempt = discounts.add(excemptions);
        }

        // calculate payed amount
        payed = debt.lessThan(payedAfterThreshhold) ? debt : payedAfterThreshhold;

        // calculate fines
        {
            final Money exessPayment = payedAfterThreshhold.subtract(debt);
            fines = exessPayment.isPositive() ? exessPayment : Money.ZERO;
        }
    }

    public Money amountStillInDebt() {
        return debt.subtract(exempt).subtract(payed);
    }

    private Money calculateAmountPayed(final DateTime threshold, final pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer errorLogConsumer) {
        return event.getAccountingTransactionsSet().stream()
                .filter(t -> threshold == null || t.getWhenRegistered().isBefore(threshold))
                .filter(t -> Utils.validate(errorLogConsumer, t.getTransactionDetail()))
                .map(t -> t.getAmountWithAdjustment())
                .reduce(Money.ZERO, Money::add);
    }

    private Money discounts() {
        return event.getDiscountsSet().stream().map(d -> d.getAmount()).reduce(Money.ZERO, Money::add);
    }

    private Money excemptions() {
        return event.getExemptionsSet().stream().map(e -> amountFor(e)).reduce(Money.ZERO, Money::add);
    }

    private Money amountFor(final Exemption e) {
        final Money amount = Utils.calculateTotalDebtValue(event);
        if (e instanceof AcademicEventExemption) {
            final AcademicEventExemption o = (AcademicEventExemption) e;
            return o.getValue();
        } else if (e instanceof AdministrativeOfficeFeeAndInsuranceExemption) {
            final AdministrativeOfficeFeeAndInsuranceExemption o = (AdministrativeOfficeFeeAndInsuranceExemption) e;
        } else if (e instanceof AdministrativeOfficeFeeExemption) {
            final AdministrativeOfficeFeeExemption o = (AdministrativeOfficeFeeExemption) e;
            final DateTime when = event.getWhenOccured().plusSeconds(1);
            final PostingRule postingRule = event.getPostingRule();
            final Money originalAmount = postingRule.calculateTotalAmountToPay(event, when, false);
            final Money amountToPay = postingRule.calculateTotalAmountToPay(event, when, true);
            return originalAmount.subtract(amountToPay);
        } else if (e instanceof InsuranceExemption) {
            final InsuranceExemption o = (InsuranceExemption) e;
        } else if (e instanceof SecondCycleIndividualCandidacyExemption) {
            final SecondCycleIndividualCandidacyExemption o = (SecondCycleIndividualCandidacyExemption) e;
        } else if (e instanceof PercentageGratuityExemption) {
            final PercentageGratuityExemption o = (PercentageGratuityExemption) e;
            return amount.multiply(o.getPercentage());
        } else if (e instanceof ValueGratuityExemption) {
            final ValueGratuityExemption o = (ValueGratuityExemption) e;
            return o.getValue();
        } else if (e instanceof PhdGratuityExternalScholarshipExemption) {
            final PhdGratuityExternalScholarshipExemption o = (PhdGratuityExternalScholarshipExemption) e;
            return o.getValue();
        } else if (e instanceof PhdGratuityFineExemption) {
            final PhdGratuityFineExemption o = (PhdGratuityFineExemption) e;
            return o.getValue();
        } else if (e instanceof PhdEventExemption) {
            final PhdEventExemption o = (PhdEventExemption) e;
            return o.getValue();
        } else if (e instanceof AdministrativeOfficeFeeAndInsurancePenaltyExemption) {
            final AdministrativeOfficeFeeAndInsurancePenaltyExemption o = (AdministrativeOfficeFeeAndInsurancePenaltyExemption) e;
            return Money.ZERO;
        } else if (e instanceof ImprovementOfApprovedEnrolmentPenaltyExemption) {
            final ImprovementOfApprovedEnrolmentPenaltyExemption o = (ImprovementOfApprovedEnrolmentPenaltyExemption) e;
            return Money.ZERO;
        } else if (e instanceof InstallmentPenaltyExemption) {
            final InstallmentPenaltyExemption o = (InstallmentPenaltyExemption) e;
            return Money.ZERO;
        } else if (e instanceof PhdRegistrationFeePenaltyExemption) {
            final PhdRegistrationFeePenaltyExemption o = (PhdRegistrationFeePenaltyExemption) e;
            return Money.ZERO;
        } else if (e instanceof PenaltyExemption) {
            final PenaltyExemption o = (PenaltyExemption) e;
            return Money.ZERO;
        }
        return amount;
    }

    public Stream<AccountingTransactionDetail> payments() {
        final Stream<AccountingTransactionDetail> stream = event.getAccountingTransactionsSet().stream().map(at -> at.getTransactionDetail());
        return stream
                //.filter(d -> d.getWhenRegistered().getYear() >= START_YEAR_TO_CONSIDER_TRANSACTIONS)
                .filter(d -> d.getWhenRegistered().isAfter(THRESHOLD))
                .filter(d -> !isCreditNote(d))
                .filter(d -> Utils.validate(null, d));
    }

    private boolean isCreditNote(AccountingTransactionDetail detail) {
        final Entry entry = detail.getTransaction().getToAccountEntry();
        final CreditNoteEntry creditNoteEntry = entry.getAdjustmentCreditNoteEntry();
        return creditNoteEntry != null;
    }

}
