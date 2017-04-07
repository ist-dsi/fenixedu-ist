package pt.ist.fenixedu.giaf.invoices.task;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;

import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.task.TaskUtils.LockManager;
import pt.ist.fenixframework.Atomic.TxMode;

@Task(englishTitle = "Sync financial information (debts, reciepts, exemptions and clients) with GIAF.")
public class SyncFinancialInfoToGiafTask extends CronTask {

    @Override
    protected TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        try (final LockManager locker = TaskUtils.getLockManager()) {
            runSyncScript();
        }
    }

    private void runSyncScript() throws IOException, AddressException, MessagingException {
        touch("Start");

        final Spreadsheet errors = new Spreadsheet("Errors");
        final ErrorLogConsumer errorLogConsumer = new ErrorLogConsumer() {

            private final Set<String> oids = new HashSet<>();

            @Override
            public void accept(final String oid, final String user, final String name, final String amount, final String cycleType, final String error, final String args,
                    final String type, final String countryOfVatNumber, final String vatNumber, final String address, final String locality,
                    final String postCode, final String countryOfAddress, final String paymentMethod,
                    final String documentNumber, final String action) {
                if (!oids.contains(oid)) {
                    oids.add(oid);

                    final Row row = errors.addRow();
                    row.setCell("OID", oid);
                    row.setCell("user", user);
                    row.setCell("name", name);
                    row.setCell("amount", amount);
                    row.setCell("cycleType", cycleType);
                    row.setCell("error", error);
                    row.setCell("args", args);
                    row.setCell("type", type);
                    row.setCell("countryOfVatNumber", countryOfVatNumber);
                    row.setCell("vatNumber", vatNumber);
                    row.setCell("address", address);
                    row.setCell("locality", locality);
                    row.setCell("postCode", postCode);
                    row.setCell("countryOfAddress", countryOfAddress);
                    row.setCell("paymentMethod", paymentMethod);
                    row.setCell("documentNumber", documentNumber);
                    row.setCell("action", action);
                    
                    taskLog("Logging error for %s: %s : %s%n", oid, error, args);
                }
            }
        };
        final EventLogger elogger = (msg, args) -> taskLog(msg, args);

        final ClientMap clientMap = new ClientMap();
        touch("Completed loading client map.");

        unfilteredEventStream(errorLogConsumer)
            .map(e -> e.getParty())
            .filter(p -> p != null && p.isPerson())
            .map(p -> (Person) p)
            .distinct()
            .filter(p -> clientMap.containsClient(p))
            .filter(p -> !"PT999999990".equals(ClientMap.uVATNumberFor(p)))
            .map(p -> ClientMap.toJson(p))
            .forEach(j -> ClientMap.createClient(errorLogConsumer, j));
        touch("Updated existing client information.");

        long[] count = new long[] { 0l };
        unfilteredEventStream(errorLogConsumer)
            .map(e -> e.getParty())
            .filter(p -> p != null && p.isPerson())
            .map(p -> (Person) p)
            .distinct()
            .filter(p -> !clientMap.containsClient(p))
            .filter(p -> !"PT999999990".equals(ClientMap.uVATNumberFor(p)))
            .map(p -> ClientMap.toJson(p))
            .forEach(j -> {
                taskLog("Registering new client %s %s %s %s%n", j.get("id").getAsString(), j.get("countryOfVatNumber").getAsString(), j.get("vatNumber").getAsString(), j.get("name").getAsString());
                if (ClientMap.createClient(errorLogConsumer, j)) {
                    final String clientCode = j.get("id").getAsString();
                    clientMap.register(clientCode, clientCode);
                }
            });
        touch("Created " + count[0] + " new clients.");

        
        touch("Processing events...");
        unfilteredEventStream(errorLogConsumer).forEach(e -> EventProcessor.syncEventWithGiaf(clientMap, errorLogConsumer, elogger, e));
        touch("Completed processing events.");

        touch("Dumping error messages.");
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        errors.exportToXLSSheet(stream);
        final String subject = "Problemas no envio de informação para o GIAF";
        final String body = "Listagem atualizada com os problemas verificados na sincronização de informação financeira entre o Fénix e o GIAF: " + new DateTime().toString("yyyy-MM-dd HH:mm");
        TaskUtils.sendReport("errors" + new DateTime().toString("yyyy_MM_dd_HH_mm") + ".xls", stream.toByteArray(), subject, body);

        touch("Done");
    }

    private void touch(final String prefix) {
        taskLog("%s: %s%n", prefix, new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
    }

    private Stream<Event> unfilteredEventStream(final ErrorLogConsumer consumer) {
        return EventWrapper.eventsToProcess(consumer, Bennu.getInstance().getAccountingEventsSet().stream(),
                Bennu.getInstance().getAccountingTransactionDetailsSet().stream());
    }

}
