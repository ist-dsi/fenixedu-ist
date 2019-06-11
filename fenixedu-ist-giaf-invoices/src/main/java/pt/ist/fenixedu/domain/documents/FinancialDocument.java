package pt.ist.fenixedu.domain.documents;

import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.util.Money;
import org.fenixedu.commons.i18n.I18N;
import pt.ist.fenixframework.Atomic;
import pt.ist.papyrus.PapyrusClient;
import pt.ist.papyrus.PapyrusConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

public class FinancialDocument extends FinancialDocument_Base {
    
    public FinancialDocument(final Event event, final String documentType, final String documentNumber,
                             final Money value, final String displayName, final String fileName, final byte[] content) {
        setEvent(event);
        setDocumentType(documentType);
        setDocumentNumber(documentNumber);
        setValue(value);
        setFinancialDocumentFile(new FinancialDocumentFile(displayName, fileName, content));
    }

    @Atomic
    public static FinancialDocument createFinancialDocument(final Event event, final Function<ReportEntry, JsonObject> toJson,
                                               final String templateId, final String documentType,
                                               final Function<JsonObject, String> toDocumentNumber) {
        final ReportEntry debtEntry = ReportEntry.reportEntryFor(event);
        if (debtEntry != null) {
            final JsonObject json = toJson.apply(debtEntry);
            final PapyrusClient papyrusClient = new PapyrusClient(
                    PapyrusConfiguration.getConfiguration().papyrusUrl(),
                    PapyrusConfiguration.getConfiguration().papyrusToken());
            final InputStream pdf = papyrusClient.render(templateId, I18N.getLocale(), json);
            try {
                final byte[] content = IOUtils.toByteArray(pdf);
                final String documentNumber = toDocumentNumber.apply(json);
                final String filename = documentNumber.replace('/', '_') + pdf;
                final Money value = debtEntry.amount.add(debtEntry.interest);
                return new FinancialDocument(event, documentType, documentNumber, value, documentNumber,
                        filename, content);
            } catch (final IOException ex) {
                throw new Error(ex);
            }
        }
        return null;
    }

}
