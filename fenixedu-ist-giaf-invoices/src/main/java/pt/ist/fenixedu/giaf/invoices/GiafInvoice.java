package pt.ist.fenixedu.giaf.invoices;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.function.Function;
import java.util.function.Predicate;

import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.fenixframework.DomainObject;
import pt.ist.giaf.client.financialDocuments.InvoiceClient;

public class GiafInvoice {

    private static final Logger LOGGER = LoggerFactory.getLogger(GiafInvoice.class);

    private static final String DIR = GiafInvoiceConfiguration.getConfiguration().giafInvoiceDir();

    public static JsonObject createInvoice(final ErrorConsumer<Event> consumer, final Event event, final DateTime threshold) {
        return createInvoice(event, (e) -> Utils.validate(consumer, e), (e) -> Utils.toJson(e, threshold), (e) -> Utils.idFor(e));
    }

    public static JsonObject createInvoice(final ErrorConsumer<AccountingTransactionDetail> consumer,
            final AccountingTransactionDetail detail, final Function<AccountingTransactionDetail, JsonObject> f) {
        return createInvoice(detail, (d) -> Utils.validate(consumer, d), f, (e) -> Utils.idFor(e));
    }

    public static JsonObject createInvoice(final ErrorConsumer<AccountingTransactionDetail> consumer,
            final AccountingTransactionDetail detail, final boolean accountForValue) {
        return createInvoice(detail, (d) -> Utils.validate(consumer, d), (d) -> Utils.toJson(d, accountForValue, null), (e) -> Utils.idFor(e));
    }

    public static JsonObject createInvoiceDiscount(final ErrorConsumer<Event> consumer, final Event event) {
        return createInvoice(event, (e) -> Utils.validate(consumer, e), (e) -> Utils.toJsonDiscount(event), (e) -> Utils.idForDiscount(e));
    }

    public static InputStream invoiceStream(final Event event) throws IOException {
        return streamFor(event);
    }

    public static InputStream invoiceStream(final AccountingTransactionDetail detail) throws IOException {
        return streamFor(detail);
    }

    public static String documentNumberFor(final Event event) {
        final String id = Utils.idFor(event);
        final File file = fileForDocumentNumber(id);
        if (file.exists()) {
            try {
                return new String(Files.readAllBytes(file.toPath()));
            } catch (final IOException e) {
                throw new Error(e);
            }
        }
        return null;
    }

    private static <T extends DomainObject> InputStream streamFor(final T t) throws IOException {
        final String id = Utils.idFor(t);
        final File file = fileForDocument(id);
        return new FileInputStream(file);
    }

    public static <T extends DomainObject> JsonObject createInvoice(final T t, final Predicate<T> p, final Function<T, JsonObject> f, final Function<T, String> toId) {
        final String id = toId.apply(t);
        final File file = fileForDocumentNumber(id);
        if (!file.exists() && p.test(t)) {
            final JsonObject jo = f.apply(t);
            if (jo != null) {
                final String documentNumber = createInvoice(jo);
                if (documentNumber != null) {
                    writeFileWithoutFailuer(file.toPath(), documentNumber.getBytes());
                }
                return jo;
            }
        }
        return null;
    }

    private static String createInvoice(final JsonObject jo) {
        final String id = jo.get("id").getAsString();
        final File file = fileForDocument(id);
        if (!file.exists()) {
            final JsonObject result = produceInvoice(jo);

            final JsonElement documentNumber = result.get("documentNumber");
            final JsonElement pdfBase64 = result.get("pdfBase64");
            if (pdfBase64 != null) {
                writeFileWithoutFailuer(file.toPath(), Base64.getDecoder().decode(pdfBase64.getAsString()));
            }
            return documentNumber == null ? null : documentNumber.getAsString();
        }
        return null;
    }

    private static void writeFileWithoutFailuer(final Path path, final byte[] content) {
        for (int c = 0; ; c++) {
            try {
                Files.write(path, content);
                return;
            } catch (final Throwable e) {
                if (c > 0 && c % 5 == 0) {
                    LOGGER.debug("Failed write of invoice file: % - Fail count: %s", path.toString(), c);
                }
                try {
                    Thread.sleep(5000);
                } catch (final InterruptedException e1) {
                }
            }
        }
    }

    private static JsonObject produceInvoice(final JsonObject jo) {
        final JsonObject result = InvoiceClient.produceInvoice(jo);
        final JsonElement errorMessage = result.get("errorMessage");
        if (errorMessage != null) {
            final String message = errorMessage.getAsString();
            if (message.indexOf("PK_2012.GC_FACTURA_DET_I99") > 0
                    && message.indexOf("unique constraint") > 0
                    && message.indexOf("violated") > 0) {
                jo.addProperty("id", jo.get("id").getAsString() + "_1");
                return produceInvoice(jo);
            } else {
                throw new Error(errorMessage.getAsString());
            }
        }
        return result;
    }

    public static File fileForDocumentNumber(final String id) {
        return fileFor(id, ".txt");
    }

    public static File fileForDocument(final String id) {
        return fileFor(id, ".pdf");
    }

    private static File fileFor(final String id, final String extension) {
        final String dirPath = DIR + splitPath(id);
        final File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        final String filePath = dirPath + id + extension;
        return new File(filePath);
    }

    private static String splitPath(final String id) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < id.length() - 1; i++, i++) {
            b.append(id.charAt(i));
            b.append(id.charAt(i + 1));
            b.append(File.separatorChar);
        }
        return b.toString();
    }

}
