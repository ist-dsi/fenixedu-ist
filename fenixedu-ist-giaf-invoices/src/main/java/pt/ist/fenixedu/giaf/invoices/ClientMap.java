package pt.ist.fenixedu.giaf.invoices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.TreeMap;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.bennu.core.domain.User;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.giaf.client.financialDocuments.ClientClient;

public class ClientMap extends TreeMap<String, String> {

    private static final String MAP_FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/giafVATNumberToClientIdMap.csv";

    public ClientMap() {
        try {
            for (final String line : Files.readAllLines(new File(MAP_FILENAME).toPath())) {
                final String[] parts = line.split("\t");

                final String uVATNumber = parts[0];
                final String clientId = parts[1];

                put(uVATNumber, clientId);
            }
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    public void register(final String uVATNumber, final String clientId) {
        put(uVATNumber, clientId);

        try (final FileOutputStream stream = new FileOutputStream(MAP_FILENAME, true)) {
            stream.write(uVATNumber.getBytes());
            stream.write("\t".getBytes());
            stream.write(clientId.getBytes());
            stream.write("\n".getBytes());
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    public String getClientId(final Person p) {
        final String uVATNumber = uVATNumberFor(p);
        return uVATNumber == null ? null : get(uVATNumber);
    }

    public boolean containsClient(final Person p) {
        return getClientId(p) != null;
    }

    public static String uVATNumberFor(final Person person) {
        final Country country = person.getCountry();
        final String ssn = person.getSocialSecurityNumber();
        final String vat = toVatNumber(ssn);
        if (vat != null && isVatValidForPT(vat)) {
            return "PT" + vat;
        }
        if (country == null || "PT".equals(country.getCode())) {
            return null;
        }
        if (vat != null) {
            return country.getCode() + vat;
        }
        final User user = person.getUser();
        return user == null ? country.getCode() + makeUpSomeRandomNumber(person) : null;
    }

    private static String toVatNumber(final String ssn) {
        return ssn == null ? null : ssn.startsWith("PT") ? ssn.substring(2) : ssn;
    }

    public static boolean isVatValidForPT(final String vat) {
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

    private static String makeUpSomeRandomNumber(final Person person) {
        final String id = person.getExternalId();
        return "FE" + id.substring(id.length() - 10, id.length());
    }

    private static JsonObject toJson(final Person person) {
        final String uVATNumber = ClientMap.uVATNumberFor(person);
        final String clientCode = uVATNumber;

        final String vat = uVATNumber.substring(2);
        final String vatCountry = uVATNumber.substring(0, 2);

        final PhysicalAddress address = Utils.toAddress(person);
        final String street = Utils.limitFormat(60, address.getAddress()).replace('\t', ' ');
        final String locality = Utils.limitFormat(35, address.getAreaOfAreaCode());
        final String postCode = Utils.hackAreaCode(address.getAreaCode(), address.getCountryOfResidence(), person);
        final String country = address.getCountryOfResidence().getCode();
        final String name = Utils.limitFormat(50, Utils.getDisplayName(person));

        final JsonObject jo = new JsonObject();
        jo.addProperty("id", clientCode);
        jo.addProperty("name", Utils.limitFormat(60, name));
        jo.addProperty("type", "S");
        jo.addProperty("countryOfVatNumber", vatCountry);
        jo.addProperty("vatNumber", vat);
        jo.addProperty("address", street);
        jo.addProperty("locality", locality);
        jo.addProperty("postCode", postCode);
        jo.addProperty("countryOfAddress", country);
        jo.addProperty("phone", "");
        jo.addProperty("fax", "");
        jo.addProperty("email", "");
        jo.addProperty("ban", "");
        jo.addProperty("iban", "");
        jo.addProperty("swift", "");
        jo.addProperty("paymentMethod", "CH");

        return jo;
    }

    public static void createOrUpdateClientInfo(final ClientMap clientMap, final ErrorLogConsumer consumer, final Person person) {
        final JsonObject j = toJson(person);
        if (clientMap.containsClient(person)) {
            createClient(consumer, j);
        } else {
            if (createClient(consumer, j)) {
                final String clientCode = j.get("id").getAsString();
                clientMap.register(clientCode, clientCode);
            }
        }
    }

    private static boolean createClient(final ErrorLogConsumer errorLogConsumer, final JsonObject j) {
        try {
            final JsonObject result = ClientClient.createClient(j);
            final JsonElement je = result.get("errorMessage");
            if (je != null && !je.isJsonNull()) {
                final String error = je.getAsString();
                if (error != null && !error.isEmpty()) {
                    errorLogConsumer.accept(
                            j.get("id").getAsString(),
                            j.get("vatNumber").getAsString(),
                            j.get("name").getAsString(),
                            "",
                            "",
                            error,
                            j.toString(),
                            j.get("type").getAsString(),
                            j.get("countryOfVatNumber").getAsString(),
                            j.get("vatNumber").getAsString(),
                            j.get("address").getAsString(),
                            j.get("locality").getAsString(),
                            j.get("postCode").getAsString(),
                            j.get("countryOfAddress").getAsString(),
                            j.get("paymentMethod").getAsString());
                }
            }
            return true;
        } catch (final Exception ex) {
            errorLogConsumer.accept(
                    j.get("id").getAsString(),
                    j.get("vatNumber").getAsString(),
                    j.get("name").getAsString(),
                    "",
                    "",
                    "Failled to create client: " + ex.getMessage(),
                    j.toString(),
                    j.get("type").getAsString(),
                    j.get("countryOfVatNumber").getAsString(),
                    j.get("vatNumber").getAsString(),
                    j.get("address").getAsString(),
                    j.get("locality").getAsString(),
                    j.get("postCode").getAsString(),
                    j.get("countryOfAddress").getAsString(),
                    j.get("paymentMethod").getAsString());
            return false;
        }
    }

}
