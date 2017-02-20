package pt.ist.fenixedu.integration.util.contacts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.Phone;
import org.fenixedu.academic.util.AcademicPhoneNumberHandler;
import org.fenixedu.academic.util.ContactResolver;
import org.fenixedu.academic.util.PhoneUtil;
import org.springframework.util.StringUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class ISTPhoneNumberHandler extends AcademicPhoneNumberHandler implements PhoneUtil.PhoneNumberHandler {

    private static final Collection<PhoneNumberType> FIXED_NUMBERS;
    private static final Collection<PhoneNumberType> MOBILE_NUMBERS;

    private static final int ALAMEDA_PHONE = 218416000;
    private static final int TAGUS_PHONE_000 = 214233200;
    private static final int TAGUS_PHONE_100 = 214233500;

    private static final Map<Class<? extends PartyContact>, ContactResolver> OVERRIDE_CONTACT_RESOLVER_MAP = new HashMap<>();

    private static final String COUNTRY_CODE = "PT";

    static {
        FIXED_NUMBERS = new ArrayList<PhoneNumberType>();
        FIXED_NUMBERS.add(PhoneNumberType.VOIP);
        FIXED_NUMBERS.add(PhoneNumberType.FIXED_LINE);

        MOBILE_NUMBERS = new ArrayList<PhoneNumberType>();
        MOBILE_NUMBERS.add(PhoneNumberType.MOBILE);

        OVERRIDE_CONTACT_RESOLVER_MAP.put(Phone.class, (pc) -> getPresentationValue(((Phone) pc).getNumber()));
    }

    private static String getPresentationValue(String numberText) {
        return numberText + (isExtension(numberText) ? " | " + getExternalNumberForExtension(numberText) : "");
    }
    
    private static boolean isExtension(String numberText) {
        return getExternalNumberForExtension(numberText) != null;
    }

    public static String getExternalNumberForExtension(String numberText) {
        int extension;
        try {
            extension = Integer.parseInt(numberText);
        } catch (NumberFormatException nfe) {
            return null;
        }
        if (extension >= 1000 && extension <= 3999) {
            return new Integer(ALAMEDA_PHONE + extension).toString();
        } else {
            if (extension >= 5000 && extension <= 5099) {
                extension -= 5000;
                return new Integer(TAGUS_PHONE_000 + extension).toString();
            } else if (extension >= 5100 && extension <= 5199) {
                extension -= 5100;
                return new Integer(TAGUS_PHONE_100 + extension).toString();
            }
        }
        return null;
    }

    public boolean isPortugueseNumber(String numberText) {
        final PhoneNumber phoneNumber = parsePhoneNumber(numberText);
        if (phoneNumber != null) {
            return phoneNumber.getCountryCode() == 351;
        }
        return false;
    }

    @Override
    public boolean shouldReceiveValidationSMS(String number) {
        return isMobileNumber(number);
    }

    @Override
    public boolean shouldReceiveValidationCall(String number) {
        return isFixedNumber(number) || !isPortugueseNumber(number);
    }

    @Override
    public PhoneNumber parsePhoneNumber(String numberText) {
        if (!StringUtils.isEmpty(numberText)) {

            if (numberText.startsWith("00")) {
                numberText = numberText.replaceFirst("00", "+");
            }

            if (isExtension(numberText)) {
                numberText = getExternalNumberForExtension(numberText);
            }

            try {
                final PhoneNumber phoneNumber = PHONE_UTIL.parse(numberText, COUNTRY_CODE);
                if (PHONE_UTIL.isValidNumber(phoneNumber)) {
                    return phoneNumber;
                }
            } catch (NumberParseException e) {
                System.out.println("O n�mero n�o � v�lido:" + e);
                return null;
            }
        }
        return null;
    }

    @Override
    public String getInternationalFormatNumber(String numberText) {
        if (isExtension(numberText)) {
            numberText = getExternalNumberForExtension(numberText);
        }

        final PhoneNumber phoneNumber = parsePhoneNumber(numberText);
        if (phoneNumber != null) {
            return PHONE_UTIL.format(phoneNumber, PhoneNumberFormat.E164);
        }

        return null;
    }

    @Override
    public String resolve(PartyContact contact) {
        ContactResolver resolver = OVERRIDE_CONTACT_RESOLVER_MAP.get(contact.getClass());
        if (resolver != null) {
            return resolver.getPresentationValue(contact);
        } else {
            return super.resolve(contact);
        }
    }

}
