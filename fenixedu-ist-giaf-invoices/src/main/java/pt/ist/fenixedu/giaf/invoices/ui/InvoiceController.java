package pt.ist.fenixedu.giaf.invoices.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import pt.ist.fenixedu.giaf.invoices.DebtCycleType;
import pt.ist.fenixedu.giaf.invoices.GiafInvoice;
import pt.ist.fenixedu.giaf.invoices.Utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@SpringApplication(group = "logged", path = "giaf-invoice-viewer", title = "title.giaf.invoice.viewer",
        hint = "giaf-invoice-viewer")
@SpringFunctionality(app = InvoiceController.class, title = "title.giaf.invoice.viewer")
@RequestMapping("/giaf-invoice-viewer")
public class InvoiceController {

    @RequestMapping
    public String home(final Model model) {
        final User user = Authenticate.getUser();
        final Person person = user.getPerson();

        final JsonArray details =
                person.getEventsSet().stream().flatMap(e -> e.getAccountingTransactionsSet().stream())
                        .map(tx -> tx.getTransactionDetail()).map(this::toJson).collect(toJsonArray());
        model.addAttribute("details", details);

        return "giaf-invoice-viewer/home";
    }

    private JsonObject toJson(final AccountingTransactionDetail d) {
        final AccountingTransaction transaction = d.getTransaction();
        final Event event = transaction.getEvent();
        final ExecutionYear debtYear = Utils.executionYearOf(event);
        final DebtCycleType cycleType = Utils.cycleTypeFor(event, debtYear);
        final String eventDescription = event.getDescription().toString();
        final String articleCode = Utils.mapToArticleCode(event, eventDescription);

        final JsonObject o = new JsonObject();
        o.addProperty("id", d.getExternalId());
        o.addProperty("reference", debtYear.getName());
        o.addProperty("observation", cycleType == null ? "Outros" : cycleType.getDescription());
        o.addProperty("paymentDate", transaction.getWhenRegistered().toString("yyyy-MM-dd"));
        o.addProperty("paymentMethod", transaction.getPaymentMode().getLocalizedName());
        o.addProperty("documentNumber", Utils.toPaymentDocumentNumber(d));
        o.addProperty("article", articleCode);
        o.addProperty("description", eventDescription);
        o.addProperty("unitPrice", transaction.getAmountWithAdjustment().toPlainString());

        final String invoiceNumber = toInvoiceNumber(d.getExternalId());
        if (invoiceNumber != null) {
            o.addProperty("invoiceNumber", invoiceNumber);
        }
        return o;
    }

    public static String toInvoiceNumber(final String id) {
        final File file = GiafInvoice.fileForDocumentNumber(id);
        if (file.exists()) {
            try {
                return new String(Files.readAllBytes(file.toPath()));
            } catch (final IOException e) {
                throw new Error(e);
            }
        }
        return null;
    }

    public static <T extends JsonElement> Collector<T, JsonArray, JsonArray> toJsonArray() {
        return Collector.of(JsonArray::new, (array, element) -> array.add(element), (one, other) -> {
            one.addAll(other);
            return one;
        }, Characteristics.IDENTITY_FINISH);
    }

}
