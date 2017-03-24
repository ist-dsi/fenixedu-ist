package pt.ist.fenixedu.giaf.invoices.task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.fenixedu.academic.FenixEduAcademicConfiguration;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.util.email.Sender;
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

@Task(englishTitle = "Sync financial information (debts, reciepts, exemptions and clients) with GIAF.")
public class SyncFinancialInfoToGiafTask extends CronTask {

    private static final String EMAIL_ADDRESSES_TO_SEND_DATA_FILENAME  = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/giaf_sync_errors_to.txt";
    private static final String EMAIL_ADDRESSES_BCC_SEND_DATA_FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/giaf_sync_errors_bcc.txt";

    @Override
    public void runTask() throws Exception {
        touch("Start");

        final Spreadsheet errors = new Spreadsheet("Errors");
        final ErrorLogConsumer errorLogConsumer = new ErrorLogConsumer() {

            private final Set<String> oids = new HashSet<>();

            @Override
            public void accept(final String oid, final String user, final String name, final String amount, final String cycleType, final String error, final String args,
                    final String type, final String countryOfVatNumber, final String vatNumber, final String address, final String locality,
                    final String postCode, final String countryOfAddress, final String paymentMethod) {
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
        sendErrorReport("errors" + new DateTime().toString("yyyy_MM_dd_HH_mm") + ".xls", stream.toByteArray());

        touch("Done");
    }

    private void touch(final String prefix) {
        taskLog("%s: %s%n", prefix, new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
    }

    private Stream<Event> unfilteredEventStream(final ErrorLogConsumer consumer) {
        return EventWrapper.eventsToProcess(consumer, Bennu.getInstance().getAccountingEventsSet().stream(),
                Bennu.getInstance().getAccountingTransactionDetailsSet().stream());
    }

    private void sendErrorReport(String filename, byte[] byteArray) throws AddressException, MessagingException {
        final Properties properties = new Properties();
        properties.put("mail.smtp.host", FenixEduAcademicConfiguration.getConfiguration().getMailSmtpHost());
        properties.put("mail.smtp.name", FenixEduAcademicConfiguration.getConfiguration().getMailSmtpName());
        properties.put("mailSender.max.recipients", FenixEduAcademicConfiguration.getConfiguration().getMailSenderMaxRecipients());
        properties.put("mail.debug", "false");
        final Session session = Session.getDefaultInstance(properties, null);

        final Sender sender = Bennu.getInstance().getSystemSender();

        final Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender.getFromAddress()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(fileContent(EMAIL_ADDRESSES_TO_SEND_DATA_FILENAME)));
        message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(fileContent(EMAIL_ADDRESSES_BCC_SEND_DATA_FILENAME)));
        message.setSubject("Problemas no envio de informação para o GIAF");
        message.setText("Listagem atualizada com os problemas verificados na sincronização de informação financeira entre o Fénix e o GIAF: " + new DateTime().toString("yyyy-MM-dd HH:mm"));

        MimeBodyPart messageBodyPart = new MimeBodyPart();

        Multipart multipart = new MimeMultipart();

        messageBodyPart = new MimeBodyPart();
        DataSource source = new ByteArrayDataSource(byteArray, "application/vnd.ms-excel");
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);
        multipart.addBodyPart(messageBodyPart);

        message.setContent(multipart);

        Transport.send(message);
    }

    private String fileContent(final String filename) {
        try {
            return Files.readAllLines(new File(filename).toPath()).iterator().next();
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

}
