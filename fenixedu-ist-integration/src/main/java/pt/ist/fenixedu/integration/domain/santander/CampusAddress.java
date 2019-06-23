package pt.ist.fenixedu.integration.domain.santander;

public class CampusAddress {
    private final String address;
    private final String zip;
    private final String town;

    CampusAddress(String address, String zip, String town) {
        this.address = address;
        this.zip = zip;
        this.town = town;
    }

    public String getAddress() {
        return address;
    }

    public String getZip() {
        return zip;
    }

    public String getTown() {
        return town;
    }
}
