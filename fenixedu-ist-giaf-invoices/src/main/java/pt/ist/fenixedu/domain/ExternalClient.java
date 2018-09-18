package pt.ist.fenixedu.domain;

import com.google.gson.JsonObject;

import pt.ist.fenixframework.Atomic;

public class ExternalClient extends ExternalClient_Base {

    public ExternalClient(final String accountId, final String clientId) {
        super();
        setSapRoot(SapRoot.getInstance());
        setAccountId(accountId);
        setClientId(clientId);
    }

    public ExternalClient(String accountId, String clientId, String companyName, String country, String street, String city,
            String region, String postalCode, String vatNumber, String fiscalCountry, String nationality,
            Integer billingIndicator) {
        this(accountId, clientId);
        edit(companyName, country, street, city, region, postalCode, vatNumber, fiscalCountry, nationality, billingIndicator);
    }

    public void edit(String companyName, String country, String street, String city,
            String region, String postalCode, String vatNumber, String fiscalCountry, String nationality,
            Integer billingIndicator) {
        setCompanyName(companyName);
        setCountry(country);
        setStreet(street);
        setCity(city);
        setRegion(region);
        setPostalCode(postalCode);
        setVatNumber(vatNumber);
        setFiscalCountry(fiscalCountry);
        setNationality(nationality);
        setBillingIndicator(billingIndicator);
    }

    public static ExternalClient find(final String uvat) {
        return SapRoot.getInstance().getExternalClientSet().stream()
            .filter(c -> c.getClientId().equals(uvat))
            .findAny().orElse(null);
    }

    @Atomic
    public static void createOrUpdate(final String clientId, final String vatNumber, final String fiscalCountry, final String companyName,
            final String country, final String street, final String city, final String region, final String postalCode, final String nationality) {
        final ExternalClient client = SapRoot.getInstance().getExternalClientSet().stream()
                .filter(c -> c.getClientId().equals(clientId))
                .findAny().orElseGet(() -> new ExternalClient(clientId, clientId));
        client.edit(companyName, country, street, city, region, postalCode, vatNumber, fiscalCountry, nationality, Integer.valueOf(0));
    }

    public JsonObject toJson() {
        final JsonObject result = new JsonObject();
        result.addProperty("accountId", getAccountId());
        result.addProperty("clientId", getClientId());
        result.addProperty("companyName", getCompanyName());
        result.addProperty("country", getCountry());
        result.addProperty("street", getStreet());
        result.addProperty("city", getCity());
        result.addProperty("region", getRegion());
        result.addProperty("postalCode", getPostalCode());
        result.addProperty("vatNumber", getVatNumber());
        result.addProperty("fiscalCountry", getFiscalCountry());
        result.addProperty("nationality", getNationality());
        result.addProperty("billingIndicator", getBillingIndicator());
        return result;
    }

    public String getPresentationName() {
        final StringBuilder b = new StringBuilder();
        b.append(getClientId());
        b.append(" - ");
        b.append(getFiscalCountry() + getVatNumber());
        b.append(" - ");
        b.append(getCompanyName());
        b.append(" - ");
        b.append(getCountry());
        b.append(" - ");
        b.append(getRegion());
        b.append(" - ");
        b.append(getPostalCode());
        b.append(" - ");
        b.append(getCity());
        b.append(" - ");
        b.append(getStreet());
        return b.toString();
    }

}
