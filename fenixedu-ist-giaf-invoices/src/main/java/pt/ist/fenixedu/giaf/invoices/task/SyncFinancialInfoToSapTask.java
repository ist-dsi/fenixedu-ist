package pt.ist.fenixedu.giaf.invoices.task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;

import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixframework.Atomic.TxMode;

@Task(englishTitle = "Sync financial information (debts, reciepts, exemptions and clients) with SAP.", readOnly = true)
public class SyncFinancialInfoToSapTask extends CronTask {

    @Override
    public void runTask() throws Exception {
        runSyncScript();
    }
    
    @Override
    protected TxMode getTxMode() {
        return TxMode.READ;
    }

    private void runSyncScript() throws IOException, AddressException, MessagingException {
        touch("Start");

        final Spreadsheet errors = new Spreadsheet("Errors");
        final ErrorLogConsumer errorLogConsumer = new ErrorLogConsumer() {

            @Override
            public void accept(final String oid, final String user, final String name, final String amount,
                    final String cycleType, final String error, final String args, final String type,
                    final String countryOfVatNumber, final String vatNumber, final String address, final String locality,
                    final String postCode, final String countryOfAddress, final String paymentMethod, final String documentNumber,
                    final String action) {

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

            }
        };
        final EventLogger elogger = (msg, args) -> taskLog(msg, args);

        touch("Processing events...");
        unfilteredEventStream(errorLogConsumer)
                .forEach(e -> EventProcessor.syncEventWithSap(errorLogConsumer, elogger, e));
//        Money amountInDebt = unfilteredEventStream(errorLogConsumer)
//                .map(e -> EventProcessor.syncEventWithSap(clientMap, errorLogConsumer, elogger, e))
//                .reduce(Money.ZERO, Money::add);
//        touch("O valor em dívida desde 2016/2017 neste momento é de: " + amountInDebt.toPlainString());
//        touch("Completed processing events.");

        touch("Dumping error messages.");
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        errors.exportToCSV(stream, "\t");
        final String subject = "Problemas no envio de informação para o SAP";
        final String body =
                "Listagem atualizada com os problemas verificados na sincronização de informação financeira entre o Fénix e o SAP: "
                        + new DateTime().toString("yyyy-MM-dd HH:mm");

        try {
            final String dirPath = GiafInvoiceConfiguration.getConfiguration().sapInvoiceDir() + "Error";
            final File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            final File documentFile = new File(dir, "SapErrors.xls");
            Utils.writeFileWithoutFailuer(documentFile.toPath(), stream.toByteArray(), false);
        } catch (Exception e) {
            System.out.println("Erro a gravar o ficheiro de erros! damn!");
            e.printStackTrace();
        }

        TaskUtils.sendSapReport("errors" + new DateTime().toString("yyyy_MM_dd_HH_mm") + ".xls", stream.toByteArray(), subject,
                body);

        touch("Done");
    }

    private void touch(final String prefix) {
        taskLog("%s: %s%n", prefix, new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
    }

    private Stream<Event> unfilteredEventStream(final ErrorLogConsumer consumer) {
        return EventWrapper.eventsToProcessSap(consumer, Bennu.getInstance().getAccountingEventsSet().stream(),
                Bennu.getInstance().getAccountingTransactionDetailsSet().stream());
    }

}
