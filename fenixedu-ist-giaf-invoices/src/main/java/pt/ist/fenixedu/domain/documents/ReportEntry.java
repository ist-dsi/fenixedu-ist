package pt.ist.fenixedu.domain.documents;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.Debt;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

class ReportEntry {

    final Money amount;
    final LocalDate dueDate;
    final Money interest;

    ReportEntry() {
        amount = Money.ZERO;
        dueDate = null;
        interest = Money.ZERO;
    }

    ReportEntry(final Debt debt) {
        amount = new Money(debt.getOpenAmount());
        dueDate = debt.getDueDate();
        interest = new Money(debt.getOpenFineAmount().add(debt.getOpenInterestAmount()));
    }

    ReportEntry(final ReportEntry entry1, final ReportEntry entry2) {
        amount = entry1.amount.add(entry2.amount);
        dueDate = min(entry1.dueDate, entry2.dueDate);
        interest = entry1.interest.add(entry2.interest);
    }

    private LocalDate min(final LocalDate localDate1, final LocalDate localDate2) {
        return localDate1 == null ? localDate2 : localDate2 == null ? localDate1 :
                localDate1.compareTo(localDate2) < 0 ? localDate1 : localDate2;
    }

    ReportEntry add(final ReportEntry other) {
        return new ReportEntry(this, other);
    }

    static ReportEntry reportEntryFor(final Event event) {
        final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
        final ReportEntry debtEntry = calculator.getDebtsOrderedByDueDate().stream()
                .map(ReportEntry::new)
                .filter(entry -> entry.amount.isPositive())
                .reduce(new ReportEntry(), ReportEntry::add);
        return debtEntry.amount.isPositive() ? debtEntry : null;
    }

}
