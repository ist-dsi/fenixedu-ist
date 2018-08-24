package pt.ist.fenixedu.domain;

public class ExternalClient extends ExternalClient_Base {

    public ExternalClient(String accountId, String companyName, String clientId, String country, String street, String city,
            String region, String postalCode, String vatNumber, String fiscalCountry, String nationality,
            Integer billingIndicator) {
        super();
        setSapRoot(SapRoot.getInstance());
        setAccountId(accountId);
        setCompanyName(companyName);
        setClientId(clientId);
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

}
