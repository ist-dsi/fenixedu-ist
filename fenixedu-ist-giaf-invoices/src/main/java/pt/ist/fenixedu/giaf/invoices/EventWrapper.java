/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Invoices.
 *
 * FenixEdu IST GIAF Invoices is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Invoices is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Invoices.  If not, see <http://www.gnu.org/licenses/>.
 */
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
