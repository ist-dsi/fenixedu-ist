package pt.ist.fenixedu.giaf.invoices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.TINValidator;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import eu.europa.ec.taxud.tin.algorithm.TINValid;

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

    public static String uVATNumberFor(final Party party) {
        final String tin = party.getSocialSecurityNumber();
        if (tin != null && !tin.trim().isEmpty()) {
            if (tin.length() > 2
                    && Character.isAlphabetic(tin.charAt(0))
                    && Character.isAlphabetic(tin.charAt(1))
                    && Character.isUpperCase(tin.charAt(0))
                    && Character.isUpperCase(tin.charAt(1))) {
                final String countryCode = tin.substring(0, 2);
                final String code = tin.substring(2);
                if (TINValidator.isValid(countryCode, code)) {
                    // all is ok
                    return tin;
                }
            }
            final Country country = getValidCountry(tin, party.getCountry(), party.getCountryOfResidence(), (party.isPerson() ? ((Person) party).getCountryOfBirth() : null), Country.readByTwoLetterCode("PT"));
            if (country != null) {
                return country.getCode() + tin;
            }
        }        
        if (tin != null && tin.length() > 2 && !"PT".equals(tin.substring(0, 2)) && Country.readByTwoLetterCode(tin.substring(0, 2)) != null) {
            return tin;
        }
        final Country country = party.getCountry();
        if (country != null && !country.getCode().equals("PT")) {
            return country.getCode() + party.getExternalId();
        }
        return "PT999999990";
    }

    private static Country getValidCountry(final String tin, final Country... countries) {
        for (int i = 0; i < countries.length; i++) {
            final Country country = countries[i];
            if (country != null && TINValidator.isValid(country.getCode().toUpperCase(), tin) == 0) {
                return country;
            }
        }
        return null;
    }

}
