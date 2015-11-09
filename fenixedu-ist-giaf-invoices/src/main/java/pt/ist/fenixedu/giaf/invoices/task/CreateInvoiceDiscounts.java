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

import com.google.gson.JsonObject;

import pt.ist.fenixedu.giaf.invoices.DebtCycleType;
import pt.ist.fenixedu.giaf.invoices.ErrorConsumer;
import pt.ist.fenixedu.giaf.invoices.GiafInvoice;
import pt.ist.fenixedu.giaf.invoices.Json2Csv;
import pt.ist.fenixedu.giaf.invoices.Utils;

public class CreateInvoiceDiscounts extends CustomTask {

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
                final String eventDescription = event.getDescription().toString();

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
            eventStream.filter(this::needsProcessing)
                .filter(e -> !hasFile(e))
                .filter(e -> Utils.validate(consumer, e))
                .filter(e -> validValue(e))
                .forEach(e -> process(e, consumer, log));
        }

        output("errors.xls", Utils.toBytes(sheet));
    }

    private boolean validValue(final Event e) {
        final Money value = Utils.discountsAndExcemptions(e);
        return value.isPositive();
    }

    private String outputFileName() {
        final String dir = GiafInvoiceConfiguration.getConfiguration().giafInvoiceDir();
        final DateTime now = new DateTime();
        return dir + "DiscountsAndExemptions_" + now.toString("yyyyMMddHHmm") + ".csv";
    }

    private boolean needsProcessing(final Event event) {
        final ExecutionYear executionYear = Utils.executionYearOf(event);
        return executionYear.isCurrent() || event.getWhenOccured().isAfter(THRESHOLD);
    }

    private void process(final Event event, final ErrorConsumer<Event> consumer, final Json2Csv log) {
        try {
            final JsonObject jo = GiafInvoice.createInvoiceDiscount(consumer, event);
            if (jo != null) {
                log.write(jo, true);
            }
        } catch (final Error e) {
            final String message = e.getMessage();
            if (message.indexOf("PK_2012.GC_FACTURA_DET_I99") > 0
                    && message.indexOf("unique constraint") > 0
                    && message.indexOf("violated") > 0
                    ) {
                taskLog("Skipping event: %s because: %s%n", event.getExternalId(), message);
            } else {
                throw e;
            }
        }

//        if (Utils.validate(consumer, event)) {
//            final JsonObject json = Utils.toJsonDiscount(event);
//            if (json != null) {
//                log.write(json, true);
//            }
//        }
    }

    private boolean hasFile(final Event e) {
        final String id = Utils.idForDiscount(e);
        return GiafInvoice.fileForDocumentNumber(id).exists();
    }

}
