package pt.ist.fenixedu.domain.documents;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import org.fenixedu.PostalCodeValidator;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.PaymentCode;
import org.fenixedu.academic.domain.accounting.paymentCodes.EventPaymentCodeEntry;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.util.Money;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixedu.giaf.invoices.Utils;

public class LiquidationLetter {

    static final String DATE_FORMAT = "yyyy-MM-dd";
    static final String DOCUMENT_TYPE = "Carta de Liquidacao";

    private static final String UNKNOWN = "Desconhecido";
    private static final int MAX_SIZE_ADDRESS = 100;
    private static final int MAX_SIZE_CITY = 50;
    private static final int MAX_SIZE_REGION = 50;
    private static final int MAX_SIZE_POSTAL_CODE = 20;

    public static void create(final Event event) {
        FinancialDocument.createFinancialDocument(event,
                reportEntry -> toJson(event, reportEntry),
                "carta-de-liquidacao",
                DOCUMENT_TYPE,
                json -> json.get("notificationRef").getAsString());
    }

    private static JsonObject toJson(final Event event, final ReportEntry reportEntry) {
        final JsonObject json = new JsonObject();

        addPersonalInformation(json, event, event.getPerson());

        final LocalDate notificationDate = new LocalDate();
        final LocalDate dueDate = notificationDate.plusDays(30);
        json.addProperty("notificationDate", notificationDate.toString(DATE_FORMAT));
        json.addProperty("notificationRef", notificationRefernece(event, notificationDate));
        json.addProperty("debtDescription", event.getDescription().toString());

        json.addProperty("limitDate",  dueDate.toString(DATE_FORMAT));

        final Money debtTotal = reportEntry.amount.add(reportEntry.interest);
        json.addProperty("debtValue", reportEntry.amount.toPlainString());
        json.addProperty("debtInterest", reportEntry.interest.toPlainString());
        json.addProperty("debtTotal", debtTotal.toPlainString());
        json.addProperty("debtInitialDate", reportEntry.dueDate.toString(DATE_FORMAT));

        json.addProperty("presidentGender", "male");

        final EventPaymentCodeEntry eventPaymentCodeEntry = EventPaymentCodeEntry.create(event, debtTotal);
        eventPaymentCodeEntry.setDueDate(dueDate.plusDays(7));
        final PaymentCode paymentCode = eventPaymentCodeEntry.getPaymentCode();
        json.addProperty("mbEntity", paymentCode.getEntityCode());
        json.addProperty("mbRef", paymentCode.getFormattedCode());


        return json;
    }

    private static String notificationRefernece(final Event event, final LocalDate notificationDate) {
        return Integer.toString(notificationDate.getYear()) + "DA/PP/" + event.getExternalId();
    }

    static void addPersonalInformation(final JsonObject json, final Event event, final Person person) {
        final String clientTIN = ClientMap.uVATNumberFor(person);
        final String[] address = toAddress(clientTIN, person);

        json.addProperty("gender", person.isMale() ? "male" : "female");
        json.addProperty("name", person.getName());
        json.addProperty("addressLine1", address[0]);
        json.addProperty("addressLine2", address[1]);
        json.addProperty("addressLine3", address[2]);
        json.addProperty("tin", clientTIN);
        json.addProperty("username", person.getUsername());
    }

    private static String[] toAddress(final String clientTIN, final Person person) {
        final String countryCode = clientTIN.substring(0, 2);
        final PhysicalAddress physicalAddress = Utils.toAddress(person, countryCode);
        final String street = physicalAddress != null && physicalAddress.getAddress() != null
                && !Strings.isNullOrEmpty(physicalAddress.getAddress().trim()) ?
                    Utils.limitFormat(MAX_SIZE_ADDRESS, physicalAddress.getAddress()) : UNKNOWN;

        final String city = Utils.limitFormat(MAX_SIZE_CITY, person.getDistrictSubdivisionOfResidence()).trim();
        final String cityString = !Strings.isNullOrEmpty(city) ? city : UNKNOWN;
        final String region = Utils.limitFormat(MAX_SIZE_REGION, person.getDistrictOfResidence()).trim();
        final String regionString = !Strings.isNullOrEmpty(region) ? region : UNKNOWN;
        String postalCode =
                physicalAddress == null ? null : Utils.limitFormat(MAX_SIZE_POSTAL_CODE, physicalAddress.getAreaCode()).trim();
        //sometimes the address is correct but the vatNumber doesn't exists and a random one was generated from the birth country
        //in that case we must send a valid postal code for that country, even if it is not the address country
        if (physicalAddress != null && physicalAddress.getCountryOfResidence() != null
                && !physicalAddress.getCountryOfResidence().getCode().equals(countryCode)) {
            postalCode = PostalCodeValidator.examplePostCodeFor(countryCode);
        }
        if (!PostalCodeValidator.isValidAreaCode(countryCode, postalCode)) {
            postalCode = PostalCodeValidator.examplePostCodeFor(countryCode);
        }
        final String postalCodeString = !Strings.isNullOrEmpty(postalCode) ? postalCode : PostalCodeValidator.examplePostCodeFor(countryCode);

        final Country country = Country.readByTwoLetterCode(countryCode);
        final String countryString = country == null ? countryCode : country.getLocalizedName().getContent();
        return new String[] { street, postalCodeString + " " + cityString + " " + regionString, countryString };
    }

}
