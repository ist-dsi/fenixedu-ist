package pt.ist.fenixedu.integration.util.contacts;

import org.fenixedu.academic.util.PhoneUtil;
import org.fenixedu.academic.util.PhoneUtil.AcademicPhoneNumberHandler;
import org.springframework.util.StringUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class ISTPhoneNumberHandler extends AcademicPhoneNumberHandler implements PhoneUtil.PhoneNumberHandler {

    private static final int ALAMEDA_PHONE = 218416000;
    private static final int TAGUS_PHONE_000 = 214233200;
    private static final int TAGUS_PHONE_100 = 214233500;

    private static final String COUNTRY_CODE = "PT";

    public static String getPresentationValue(String numberText) {
        return numberText + (ISTPhoneNumberHandler.isExtension(numberText) ? " | "
                + ISTPhoneNumberHandler.getExternalNumberForExtension(numberText) : "");
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
        return phoneNumber != null ? phoneNumber.getCountryCode() == 351 : false;
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

}
