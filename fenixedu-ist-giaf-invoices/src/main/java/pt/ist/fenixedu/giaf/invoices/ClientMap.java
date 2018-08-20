package pt.ist.fenixedu.giaf.invoices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.bennu.GiafInvoiceConfiguration;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import eu.europa.ec.taxud.tin.algorithm.TINValid;
import pt.ist.giaf.client.financialDocuments.ClientClient;

public class ClientMap {

    private final SortedMap<String, String> map = new TreeMap<>();

    public ClientMap() {
        try {
            for (final String line : Files.readAllLines(new File(GiafInvoiceConfiguration.getConfiguration().clientMapFilename()).toPath())) {
                final String[] parts = line.split("\t");

                final String uVATNumber = parts[0];
                final String clientId = parts[1];

                map.put(uVATNumber, clientId);
            }
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    public void register(final String uVATNumber, final String clientId) {
        if ("PT999999990".equals(uVATNumber)) {
            return ;
        }
        map.put(uVATNumber, clientId);

        try (final FileOutputStream stream = new FileOutputStream(GiafInvoiceConfiguration.getConfiguration().clientMapFilename(), true)) {
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
        return uVATNumber == null ? null : "PT999999990".equals(uVATNumber) ? "999996097" : map.get(uVATNumber);
    }

    public boolean containsClient(final Person p) {
        return getClientId(p) != null;
    }

    public static String uVATNumberFor(final Party party) {
        final Country country = party.getCountry();
        final String ssn = party.getSocialSecurityNumber();

        if (ssn != null && ssn.length() > 2 && TINValid.checkTIN(ssn.substring(0, 2), ssn.substring(2)) == 0) {
            return ssn;
        }

        final String vat = toVatNumber(ssn);
        if (vat != null && isVatValidForPT(vat)) {
            return "PT" + vat;
        }
        if (country == null || "PT".equals(country.getCode())) {
            return "PT999999990";
            //return null;
        }
        if (vat != null) {
            return /*trimVatTo12Digits(*/country.getCode() + vat/*)*/;
        }
        return trimVatTo12Digits(country.getCode() + makeUpSomeRandomNumber(party));
    }

    private static String trimVatTo12Digits(final String uVat) {
        return uVat.substring(0, 2) + StringUtils.right(uVat, Math.min(uVat.length() - 2, 10));
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

    private static String makeUpSomeRandomNumber(final Party party) {
        final String id = party.getExternalId();
        return "FE" + id.substring(id.length() - 10, id.length());
    }

    public static JsonObject toJson(final Person person) {
        final String uVATNumber = ClientMap.uVATNumberFor(person);
        final String clientCode = uVATNumber;

        final String vat = uVATNumber.substring(2);
        final String vatCountry = uVATNumber.substring(0, 2);

        final PhysicalAddress address = Utils.toAddress(person, vatCountry);
        final String street = Utils.limitFormat(60, address.getAddress()).replace('\t', ' ');
        final String locality = Utils.limitFormat(35, address.getAreaOfAreaCode());
        final String postCode = Utils.hackAreaCode(address.getAreaCode(), address.getCountryOfResidence(), person);
        final String country = address.getCountryOfResidence().getCode();
        final String nationality = person.getCountry().getCountryNationality().getContent(new Locale("pt"));
        final String name = Utils.limitFormat(50, Utils.getDisplayName(person));
        final String city = !Strings.isNullOrEmpty(person.getDistrictSubdivisionOfResidence()) ? person
                .getDistrictSubdivisionOfResidence() : "Desconhecido";
        final String region =
                !Strings.isNullOrEmpty(person.getDistrictOfResidence()) ? person.getDistrictOfResidence() : "Desconhecido";

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
        jo.addProperty("city", city);
        jo.addProperty("region", region);
        jo.addProperty("nationality", nationality);

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

    public static boolean createClient(final ErrorLogConsumer errorLogConsumer, final JsonObject j) {
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
                            j.get("paymentMethod").getAsString(),
                            "",
                            "");
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
                    j.get("paymentMethod").getAsString(),
                    "", 
                    "");
            return false;
        }
    }

}
