/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.domain.accounting.events.export;

import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.accounting.PaymentCode;

import com.google.common.base.Splitter;

public class PrintedPaymentCodes {

    private Set<String> paymentCodes;

    public PrintedPaymentCodes() {
        this.paymentCodes = new HashSet<>();
    }

    public String exportAsString() {
        return String.join(",", paymentCodes);
    }

    public Set<String> getPaymentCodes() {
        return paymentCodes;
    }

    public void addPaymentCode(final PaymentCode paymentCode) {
        this.paymentCodes.add(paymentCode.getCode());
    }

    public static PrintedPaymentCodes importFromString(final String value) {
        PrintedPaymentCodes printPaymentCodes = new PrintedPaymentCodes();
        printPaymentCodes.paymentCodes.addAll(Splitter.on(",").splitToList(value));
        return printPaymentCodes;
    }
}
