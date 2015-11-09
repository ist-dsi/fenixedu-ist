package pt.ist.fenixedu.giaf.invoices;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.util.Money;
import org.joda.time.LocalDate;

public class EventWrapper {

    public String remoteDocumentNumber;
    public Money remoteValue;
    public Set<Money> remotePayedValues = new HashSet<>();
    public LocalDate dueDate;
    public Map<AccountingTransaction, Money> overPayments = new HashMap<>();

    public EventWrapper(final String invoiceNumber, final Money value, final LocalDate dueDate) {
        this.remoteDocumentNumber = invoiceNumber;
        this.remoteValue = value;
        this.dueDate = dueDate;
    }

    public Money calculateRemotePayedValue() {
        return remotePayedValues.stream().reduce(Money.ZERO, Money::add);
    }

    public void registerOverPayment(final AccountingTransaction transaction, final Money amountOverpayed) {
        overPayments.put(transaction, amountOverpayed);
    }

}
