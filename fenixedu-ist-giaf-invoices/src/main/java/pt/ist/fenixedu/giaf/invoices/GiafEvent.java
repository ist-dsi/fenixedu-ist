package pt.ist.fenixedu.giaf.invoices;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.GiafInvoiceConfiguration;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class GiafEvent {

    public static JsonArray readEventFile(final Event event) {
        final File file = giafEventFile(event);
        if (file.exists()) {
            try {
                return new JsonParser().parse(new String(Files.readAllBytes(file.toPath()))).getAsJsonArray();
            } catch (JsonSyntaxException | IOException e) {
                throw new Error(e);
            }
        }        
        return new JsonArray();
    }

    private static File giafEventFile(final Event event) {
        final File dir = dirFor(event);
        return new File(dir, event.getExternalId() + ".json");
    }

    private static File dirFor(final Event event) {
        final String id = event.getExternalId();
        final String dirPath = GiafInvoiceConfiguration.getConfiguration().giafInvoiceDir() + Utils.splitPath(id) + File.separator + id;
        final File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File receiptFile(final Event event, final String filename) {
        final File dir = dirFor(event);
        final File receiptFile = new File(dir, complete(filename));
        return receiptFile.exists() ? receiptFile : null;
    }

    private static String complete(final String filename) {
        return filename.endsWith(".pdf") ? filename : filename + ".pdf";
    }

}
