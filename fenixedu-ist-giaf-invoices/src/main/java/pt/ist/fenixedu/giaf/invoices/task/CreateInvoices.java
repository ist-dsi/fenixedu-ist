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

import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;

import pt.ist.fenixedu.giaf.invoices.DebtCycleType;
import pt.ist.fenixedu.giaf.invoices.ErrorConsumer;
import pt.ist.fenixedu.giaf.invoices.GiafInvoice;
import pt.ist.fenixedu.giaf.invoices.Json2Csv;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.giaf.client.financialDocuments.ClientClient;

import com.google.gson.JsonObject;

public class CreateInvoices extends CustomTask {

    private final static DateTime THRESHOLD = new DateTime(2015, 12, 1, 0, 0, 0, 0);

    @Override
    public void runTask() throws Exception {
        final Spreadsheet sheet = new Spreadsheet("Report");
        final ErrorConsumer<Event> consumer = new ErrorConsumer<Event>() {
            @Override
            public void accept(Event event, String error, String args) {
                final Person person = event == null || event.getParty().isUnit() ? null : event.getPerson();
                final User user = person == null ? null : person.getUser();
                final ExecutionYear debtYear = event == null ? null : Utils.executionYearOf(event);
                final DebtCycleType cycleType = event == null || debtYear == null ? null : Utils.cycleTypeFor(event, debtYear);
                final String eventDescription = Utils.completeDescriptionFor(event);

                final Row row = sheet.addRow();
                row.setCell("id", event.getExternalId());
                row.setCell("value", getValue(event));
                row.setCell("error", error);
                row.setCell("args", args == null ? "" : args);
                row.setCell("user", user == null ? "" : user.getUsername());
                row.setCell("cycle type", cycleType == null ? "" : cycleType.getDescription());
                row.setCell("eventDescription", eventDescription);
            }

            private String getValue(Event event) {
                try {
                    return event.getOriginalAmountToPay().getAmount().toString();
                } catch (final DomainException ex) {
                    return "?";
                } catch (final NullPointerException ex) {
                    return "?";
                }
            }
        };
        try (final Json2Csv log = new Json2Csv(outputFileName(), "\t")) {
            final Stream<Event> eventStream = Bennu.getInstance().getAccountingEventsSet().stream();
            eventStream.filter(this::needsProcessing).filter(e -> !hasFile(e)).filter(e -> Utils.validate(consumer, e))
                    .filter(e -> validValueAfterSubtract(e)).forEach(e -> process(e, consumer, log));
        }

        output("errors.xls", Utils.toBytes(sheet));
    }

    private boolean validValueAfterSubtract(final Event e) {
        final Money value = Utils.calculateTotalDebtValue(e);
        final Money payedBeforThreshold = Utils.calculateAmountPayed(e, THRESHOLD);
        final Money amount = value.subtract(payedBeforThreshold);
        return amount.getAmount().signum() > 0;
    }

    private String outputFileName() {
        final String dir = GiafInvoiceConfiguration.getConfiguration().giafInvoiceDir();
        final DateTime now = new DateTime();
        return dir + "EventInvoicecs_" + now.toString("yyyyMMddHHmm") + ".csv";
    }

    private boolean needsProcessing(final Event event) {
        final ExecutionYear executionYear = Utils.executionYearOf(event);
        return (executionYear.isCurrent() || event.getWhenOccured().isAfter(THRESHOLD)) && !event.isCancelled();
    }

    private void process(final Event event, final ErrorConsumer<Event> consumer, final Json2Csv log) {
        ClientClient.createClient(Utils.toJson(event.getPerson()));
        try {
            final JsonObject jo = GiafInvoice.createInvoice(consumer, event, THRESHOLD);
            if (jo != null) {
                log.write(jo, true);
            }
        } catch (final Error e) {
            final String message = e.getMessage();
            if (message.indexOf("PK_2012.GC_FACTURA_DET_I99") > 0 && message.indexOf("unique constraint") > 0
                    && message.indexOf("violated") > 0) {
                taskLog("Skipping event: %s because: %s%n", event.getExternalId(), message);
            } else if (message.indexOf("Cdigo de Entidade ") >= 0 && message.indexOf(" invlido/inexistente!") > 0) {
                taskLog("Skipping event: %s because: %s%n", event.getExternalId(), message);
            } else {
                throw e;
            }
        }
    }

    private boolean hasFile(final Event e) {
        final String id = Utils.idFor(e);
        return GiafInvoice.fileForDocumentNumber(id).exists();
    }

}
