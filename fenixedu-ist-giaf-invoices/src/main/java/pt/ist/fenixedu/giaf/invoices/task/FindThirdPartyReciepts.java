package pt.ist.fenixedu.giaf.invoices.task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Entry;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Receipt;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixedu.giaf.invoices.DebtCycleType;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixframework.FenixFramework;

@Task(englishTitle = "Identify third party receipts and report them.")
public class FindThirdPartyReciepts extends CronTask {

    private static final String DIR = GiafInvoiceConfiguration.getConfiguration().giafInvoiceDir();

    @Override
    public void runTask() throws Exception {
        final Spreadsheet sheet = new Spreadsheet("Invoices");

        process(sheet, new File(DIR));

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        sheet.exportToXLSSheet(stream);

        output("invoices.xls", stream.toByteArray());

        final String subject = "Recibos passados a entidades terceiras";
        final String body = "Listagem atualizada com os 'recibos' passados no Fénix entidades terceiras: " + new DateTime().toString("yyyy-MM-dd HH:mm");

        TaskUtils.sendReport("invoices_" + new DateTime().toString("yyyy_MM_dd_HH_mm") + ".xls", stream.toByteArray(), subject, body);
    }

    private void process(final Spreadsheet sheet, final File file) {
        if (file.isDirectory()) {
            for (final File child : file.listFiles()) {
                process(sheet, child);
            }
        } else {
            if (isOperationsFile(file)) {
                final JsonArray array = toJsonArray(file);
                for (final JsonElement je : array) {
                    final JsonObject jo = je.getAsJsonObject();
                    final String type = jo.get("type").getAsString();
                    if (Objects.equal("payment", type)) {
                        final String receiptId = jo.get("receiptId").getAsString();
                        final AccountingTransactionDetail detail = FenixFramework.getDomainObject(receiptId);
                        final Receipt receipt = getThirdPartyReciept(detail);
                        if (receipt != null) {
                            final JsonElement clientId = jo.get("clientId");
                            final JsonElement receiptNumber = jo.get("receiptNumber");
                            final Entry entry = detail.getTransaction().getToAccountEntry();
                            final Event event = detail.getTransaction().getEvent();
                            final Person person = event.getPerson();
                            final ExecutionYear debtYear = Utils.executionYearOf(event);
                            final DebtCycleType cycleType = Utils.cycleTypeFor(event, debtYear);
                            final String eventDescription = event.getDescription().toString();
                            final String articleCode = Utils.mapToArticleCode(event, eventDescription);

                            final String name = receipt.getContributorName();
                            final String number = receipt.getContributorNumber();
                            final String personName = receipt.getPerson().getPresentationName();
                            final String vat = toVatNumber(person);

                            final Row row = sheet.addRow();
                            row.setCell("entryId", entry.getExternalId());
                            row.setCell("NIF", number);
                            row.setCell("Name", name);
                            row.setCell("Address", receipt.getContributorAddress());
                            row.setCell("Number", receipt.getNumberWithSeries());
                            row.setCell("OwnerUnit", receipt.getOwnerUnit() == null ? "" : receipt.getOwnerUnit().getPresentationName());
                            row.setCell("Person", personName);
                            row.setCell("ReceiptDate", receipt.getReceiptDate().toString("yyyy-MM-dd"));
                            row.setCell("Responsible", receipt.getResponsible().getPresentationName());
                            row.setCell("State", receipt.getState().getName());
                            row.setCell("Amount", receipt.getTotalAmount().toPlainString());

                            row.setCell("id", detail.getExternalId());
                            row.setCell("clientId", clientId == null || clientId.isJsonNull() ? "" : clientId.getAsString());
                            row.setCell("vat", vat);
                            row.setCell("receiptNumber", receiptNumber == null || receiptNumber.isJsonNull() ? "" : receiptNumber.getAsString());

                            row.setCell("reference", debtYear.getName());
                            row.setCell("observation", cycleType == null ? "Outros" : cycleType.getDescription());

                            row.setCell("article", articleCode);
                            row.setCell("description", eventDescription);
                            row.setCell("unitPrice", detail.getTransaction().getAmountWithAdjustment().getAmount());
                        }
                    }
                }
            }
        }
    }

    private Receipt getThirdPartyReciept(final AccountingTransactionDetail detail) {
        final Entry entry = detail.getTransaction().getToAccountEntry();
        for (final Receipt receipt : entry.getReceiptsSet()) {
            if (receipt.isAnnulled()) {
                continue;
            }

            final AccountingTransaction transaction = detail.getTransaction();
            final Event event = transaction.getEvent();
            final Person person = event.getPerson();

            final String name = receipt.getContributorName();
            final String number = receipt.getContributorNumber();
            final String personName = receipt.getPerson().getPresentationName();
            final String vat = toVatNumber(person);

            if (number != null && vat != null && number.trim().toLowerCase().equals(vat.trim().toLowerCase())) {
                continue;
            }
            if (personName.indexOf(name) < 0) {
                return receipt;
            }
        }
        return null;
    }

    private static String toVatNumber(final Person person) {
        final Country country = person.getCountry();
        final String ssn = person.getSocialSecurityNumber();
        final String vat = toVatNumber(ssn);
        if (vat != null && isVatValidForPT(vat)) {
            return vat;
        }
        if (country != null && "PT".equals(country.getCode())) {
            return null;
        }
        final User user = person.getUser();
        return user == null ? makeUpSomeRandomNumber(person) : user.getUsername();
    }

    private static String toVatNumber(final String ssn) {
        return ssn == null ? null : ssn.startsWith("PT") ? ssn.substring(2) : ssn;
    }

    private static String makeUpSomeRandomNumber(final Person person) {
        final String id = person.getExternalId();
        return "FE" + id.substring(id.length() - 10, id.length());
    }

    private static boolean isVatValidForPT(final String vat) {
        if (vat.length() != 9) {
            return false;
        }
        for (int i = 0; i < 9; i++) {
            if (!Character.isDigit(vat.charAt(i))) {
                return false;
            }
        }
        if (Integer.parseInt(vat) <= 0) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            final int c = Character.getNumericValue(vat.charAt(i));
            sum += c * (9 - i);
        }
        final int controleDigit = Character.getNumericValue(vat.charAt(8));
        final int remainder = sum % 11;
        int digit = 11 - remainder;
        return digit > 9 ? controleDigit == 0 : digit == controleDigit;
    }

    private boolean isOperationsFile(final File file) {
        final String name = file.getName();
        return name.endsWith(".json") && !name.startsWith("log");
    }

    private JsonArray toJsonArray(final File file) {
        try {
            final byte[] content = Files.readAllBytes(file.toPath());
            return new JsonParser().parse(new String(content)).getAsJsonArray();
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

}
