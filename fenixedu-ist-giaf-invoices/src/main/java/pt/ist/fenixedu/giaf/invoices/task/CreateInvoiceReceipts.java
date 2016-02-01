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
package pt.ist.fenixedu.giaf.invoices.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.CreditNoteEntry;
import org.fenixedu.academic.domain.accounting.Entry;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.giaf.invoices.DebtCycleType;
import pt.ist.fenixedu.giaf.invoices.ErrorConsumer;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.GiafInvoice;
import pt.ist.fenixedu.giaf.invoices.Json2Csv;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.giaf.client.financialDocuments.ClientClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CreateInvoiceReceipts extends CustomTask {

    private static final String DIR = GiafInvoiceConfiguration.getConfiguration().giafInvoiceDir();
    private static final DateTime UNTIL = new DateTime(2015, 12, 1, 0, 0, 0, 0);

    @Override
    public void runTask() throws Exception {
        final Map<String, EventWrapper> eventsByInvoiceNumber = loadEvents();

        final Spreadsheet sheet = new Spreadsheet("Report");
        final ErrorConsumer<AccountingTransactionDetail> consumer = new ErrorConsumer<AccountingTransactionDetail>() {
            @Override
            public void accept(final AccountingTransactionDetail detail, final String error, final String args) {
                final AccountingTransaction transaction = detail.getTransaction();
                final Event event = transaction == null ? null : transaction.getEvent();
                final Person person = event == null || event.getParty().isUnit() ? null : event.getPerson();
                final User user = person == null ? null : person.getUser();
                final ExecutionYear debtYear = event == null ? null : Utils.executionYearOf(event);
                final DebtCycleType cycleType = event == null || debtYear == null ? null : Utils.cycleTypeFor(event, debtYear);
                final String eventDescription = Utils.completeDescriptionFor(event);

                final Row row = sheet.addRow();
                row.setCell("id", detail.getExternalId());
                row.setCell("value", Utils.valueOf(detail));
                row.setCell("error", error);
                row.setCell("args", args == null ? "" : args);
                row.setCell("user", user == null ? "" : user.getUsername());
                row.setCell("cycle type", cycleType == null ? "" : cycleType.getDescription());
                row.setCell("eventDescription", eventDescription);
                row.setCell("when", detail.getWhenRegistered().toString("yyyy-MM-dd"));
            }
        };
        try (final Json2Csv log = new Json2Csv(outputFileName(), "\t")) {
            final Stream<AccountingTransactionDetail> stream = Bennu.getInstance().getAccountingTransactionDetailsSet().stream();
            stream.filter(atd -> atd.getWhenRegistered().getYear() >= 2015).filter(this::needsProcessing)
                    .filter(atd -> Utils.validate(consumer, atd)).forEach(d -> process(d, consumer, log, eventsByInvoiceNumber));

            taskLog("Competed processing normal invoices.");

            storeOverpayments(eventsByInvoiceNumber);
            try (final Json2Csv logEvents = new Json2Csv(eventOutputFileName(), "\t")) {
                eventsByInvoiceNumber.values().forEach(ew -> generateOverPaymentStuff(ew, log, logEvents));
            }
        }
        output("errors.xls", Utils.toBytes(sheet));
    }

    private String outputFileName() {
        final String dir = GiafInvoiceConfiguration.getConfiguration().giafInvoiceDir();
        final DateTime now = new DateTime();
        return dir + "AccountingTransactionInvoicecs_" + now.toString("yyyyMMddHHmm") + ".csv";
    }

    private String eventOutputFileName() {
        final String dir = GiafInvoiceConfiguration.getConfiguration().giafInvoiceDir();
        final DateTime now = new DateTime();
        return dir + "EventInvoicecs_" + now.toString("yyyyMMddHHmm") + ".csv";
    }

    private boolean needsProcessing(final AccountingTransactionDetail detail) {
        return !fileExists(detail) && !isCreditNote(detail);
    }

    private boolean isCreditNote(AccountingTransactionDetail detail) {
        final Entry entry = detail.getTransaction().getToAccountEntry();
        final CreditNoteEntry creditNoteEntry = entry.getAdjustmentCreditNoteEntry();
        return creditNoteEntry != null;
    }

    private boolean fileExists(final AccountingTransactionDetail detail) {
        final String id = Utils.idFor(detail);
        return GiafInvoice.fileForDocument(id).exists();
    }

    private void process(final AccountingTransactionDetail detail, final ErrorConsumer<AccountingTransactionDetail> consumer,
            final Json2Csv log, final Map<String, EventWrapper> eventsByInvoiceNumber) {
        final boolean accountForValue = accountForValue(detail);

        ClientClient.createClient(Utils.toJson(detail.getEvent().getPerson()));
        try {
            final JsonObject jo =
                    GiafInvoice.createInvoice(consumer, detail, (d) -> toJson(d, accountForValue, eventsByInvoiceNumber));
            if (jo != null) {
                log.write(jo, true);
            }
        } catch (final Error e) {
            final String message = e.getMessage();
            if (message.indexOf("PK_2012.GC_FACTURA_DET_I99") > 0 && message.indexOf("unique constraint") > 0
                    && message.indexOf("violated") > 0) {
                taskLog("Skipping event: %s because: %s%n", detail.getExternalId(), message);
            } else if (message.indexOf("Cdigo de Entidade ") >= 0 && message.indexOf(" invlido/inexistente!") > 0) {
                taskLog("Skipping event: %s because: %s%n", detail.getExternalId(), message);
            } else {
                throw e;
            }
        }
//        if (Utils.validate(consumer, detail)) {
//            final JsonObject json = Utils.toJson(detail, accountForValue);
//            if (json != null) {
//                log.write(json, true);
//            }
//        }
    }

    private JsonObject toJson(AccountingTransactionDetail d, boolean accountForValue,
            Map<String, EventWrapper> eventsByInvoiceNumber) {
        final JsonObject json = Utils.toJson(d, accountForValue, eventsByInvoiceNumber);
        final Money unitPrice = getUnitPrice(json);
        return unitPrice.isZero() ? null : json;
    }

    private Money getUnitPrice(final JsonObject json) {
        final JsonArray entries = json.get("entries").getAsJsonArray();
        for (final JsonElement je : entries) {
            final JsonObject jo = je.getAsJsonObject();
            return new Money(jo.get("unitPrice").getAsBigDecimal());
        }
        return Money.ZERO;
    }

    private boolean accountForValue(final AccountingTransactionDetail detail) {
        final AccountingTransaction transaction = detail.getTransaction();
        final DateTime when = transaction.getWhenRegistered();
        return when.isAfter(UNTIL);
    }

    public static Map<String, EventWrapper> loadEvents() {
        final Map<String, EventWrapper> eventsByInvoiceNumber = new HashMap<>();

        final File dir = new File(DIR);
        for (final File file : dir.listFiles()) {
            if (file.getName().indexOf("EventInvoicecs_") >= 0) {
                try {
                    for (final String line : Files.readAllLines(file.toPath())) {
                        if (!line.trim().isEmpty() && !line.startsWith("id")) {
                            final String[] parts = line.split("\t");
                            final String id = parts[0].replace("_1", "");
                            //final String clientId = parts[5];
                            final LocalDate dueDate = parseDate(parts[20]);
                            final String price = parts[27];

                            final String invoiceNumber = toInvoiceNumber(id);
                            if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
                                final Money value = new Money(price);
                                eventsByInvoiceNumber.put(invoiceNumber, new EventWrapper(invoiceNumber, value, dueDate));
                            }
                        }
                    }
                } catch (final IOException e) {
                    throw new Error(e);
                }
            }
        }

        return eventsByInvoiceNumber;
    }

    private static LocalDate parseDate(final String s) {
        final int year = parseInt(s, 0, 4);
        final int month = parseInt(s, 5, 7);
        final int day = parseInt(s, 8, 10);
        return new LocalDate(year, month, day);
    }

    private static int parseInt(final String s, final int from, final int to) {
        return Integer.parseInt(s.substring(from, to));
    }

    private static String toInvoiceNumber(final String invoiceId) {
        final File file = GiafInvoice.fileForDocumentNumber(invoiceId);
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    private void generateOverPaymentStuff(final EventWrapper ew, final Json2Csv log, final Json2Csv logEvents) {
        ew.overPayments.forEach((at, m) -> generateOverPaymentStuff(at, m, ew.dueDate, log, logEvents));
    }

    private void generateOverPaymentStuff(final AccountingTransaction tx, final Money v, final LocalDate dueDate,
            final Json2Csv log, final Json2Csv logEvents) {
        final JsonObject joEvent = Utils.toJsonEventForOverpayment(tx, v, dueDate);
        final Function<AccountingTransaction, String> toEventId = (atx) -> joEvent.get("id").getAsString();
        try {
            final JsonObject jo = GiafInvoice.createInvoice(tx, (atx) -> true, (atx) -> joEvent, toEventId);
            if (jo != null) {
                logEvents.write(jo, true);
            }
        } catch (final Error e) {
            final String message = e.getMessage();
            if (message.indexOf("PK_2012.GC_FACTURA_DET_I99") > 0 && message.indexOf("unique constraint") > 0
                    && message.indexOf("violated") > 0) {
                taskLog("Skipping event: %s because: %s%n", tx.getExternalId(), message);
            } else if (message.indexOf("Cdigo de Entidade ") >= 0 && message.indexOf(" invlido/inexistente!") > 0) {
                taskLog("Skipping event: %s because: %s%n", tx.getExternalId(), message);
            } else {
                throw e;
            }
        }

        final String invoiceId = readInvoiceId(toEventId.apply(tx));
        if (invoiceId != null && !invoiceId.isEmpty()) {
            final JsonObject joTx = Utils.toJsonOverpayment(tx, v, dueDate, invoiceId);
            final Function<AccountingTransaction, String> toId =
                    (atx) -> atx.getEvent().getExternalId() + "_" + atx.getExternalId();
            try {
                final JsonObject jo = GiafInvoice.createInvoice(tx, (atx) -> true, (atx) -> joTx, toId);
                if (jo != null) {
                    log.write(jo, true);
                }
            } catch (final Error e) {
                final String message = e.getMessage();
                if (message.indexOf("PK_2012.GC_FACTURA_DET_I99") > 0 && message.indexOf("unique constraint") > 0
                        && message.indexOf("violated") > 0) {
                    taskLog("Skipping event: %s because: %s%n", tx.getExternalId(), message);
                } else if (message.indexOf("Cdigo de Entidade ") >= 0 && message.indexOf(" invlido/inexistente!") > 0) {
                    taskLog("Skipping event: %s because: %s%n", tx.getExternalId(), message);
                } else {
                    throw e;
                }
            }
        }
    }

    private String readInvoiceId(final String eventId) {
        final File file = GiafInvoice.fileForDocumentNumber(eventId);
        if (file.exists()) {
            try {
                return new String(Files.readAllBytes(file.toPath()));
            } catch (IOException e) {
                throw new Error(e);
            }
        }
        return null;
    }

    private void storeOverpayments(final Map<String, EventWrapper> eventsByInvoiceNumber) {
        final Spreadsheet sheet = new Spreadsheet("OverPayments");
        eventsByInvoiceNumber.entrySet().forEach(e -> toSheet(sheet, e.getKey(), e.getValue()));

        final String dir = GiafInvoiceConfiguration.getConfiguration().giafInvoiceDir();
        final DateTime now = new DateTime();
        final String filename = dir + "Overpayments_" + now.toString("yyyyMMddHHmm") + ".csv";

        try {
            sheet.exportToCSV(new File(filename), "\t");
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    private void toSheet(final Spreadsheet sheet, final String key, final EventWrapper ew) {
        for (final java.util.Map.Entry<AccountingTransaction, Money> e : ew.overPayments.entrySet()) {
            final Row row = sheet.addRow();
            row.setCell("key", key);
            row.setCell("txId", e.getKey().getExternalId());
            row.setCell("remoteDocumentNumber", ew.remoteDocumentNumber);
            row.setCell("dueDate", ew.dueDate.toString("yyyy-MM-dd"));
            row.setCell("value", e.getValue().getAmountAsString());
        }
    }

}
