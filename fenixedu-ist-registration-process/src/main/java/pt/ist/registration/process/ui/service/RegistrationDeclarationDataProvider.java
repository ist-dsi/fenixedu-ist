package pt.ist.registration.process.ui.service;

import java.util.Locale;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.organizationalStructure.UniversityUnit;
import org.fenixedu.academic.domain.space.SpaceUtils;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
@Service
public class RegistrationDeclarationDataProvider {

    private boolean missingPersonalInfo(Person person) {
        return person.getIdDocumentType() == null || Strings.isNullOrEmpty(person.getDocumentIdNumber()) || person.getCountry()
         == null;
    }
    
    public JsonObject getBasicRegistrationData(Registration registration, ExecutionYear executionYear, Locale locale) {
                                                                                                       
        AdministrativeOffice administrativeOffice = registration.getDegree().getAdministrativeOffice();
        String unitName = administrativeOffice.getUnit().getNameI18n().getContent(locale);
        String institutionName = Bennu.getInstance().getInstitutionUnit().getPartyName().getContent(locale);
        String universityName =
                UniversityUnit.getInstitutionsUniversityUnitByDate(new DateTime()).getPartyName().getContent(locale);
        String responsibleName = administrativeOffice.getCoordinator().getProfile().getFullName();
        Person person = registration.getPerson();
        String executionYearName = executionYear.getName();
        String username = person.getUsername();
        String documentType = person.getIdDocumentType().getLocalizedName(locale);
        String idNumber = person.getDocumentIdNumber();
        String address = person.getAddress();
        String parishName = person.getParishOfResidence();
        String county = person.getDistrictSubdivisionOfResidence();
        String postalCode = person.getPostalCode();

        JsonObject curricularYear = new JsonObject();
        int curricularYearNumber = registration.getCurricularYear(executionYear);
        curricularYear.addProperty("ordinal", BundleUtil.getString(Bundle.ENUMERATION, locale, curricularYearNumber + ".ordinal"));
        curricularYear.addProperty("number", curricularYearNumber);

        String degreeName = getDegreeDescription(registration, executionYear, locale);
        String nationality = person.getCountry().getCountryNationality().getContent(locale);

        JsonObject payload = new JsonObject();

        payload.addProperty("idDocValidationDate",person.getExpirationDateOfDocumentIdYearMonthDay().toString("dd/MM/yyyy"));
        payload.addProperty("dateOfBirth",person.getDateOfBirthYearMonthDay().toString("dd/MM/yyyy"));
        payload.addProperty("address",address);
        payload.addProperty("parish",parishName);
        payload.addProperty("county",county);
        payload.addProperty("postalCode",postalCode);
        payload.addProperty("executionYearName",executionYearName);
        payload.addProperty("degreeName",degreeName);
        payload.addProperty("responsibleName", responsibleName);
        payload.addProperty("unitName", unitName);
        payload.addProperty("institutionName", institutionName);
        payload.addProperty("universityName", universityName);
        payload.addProperty("username", username);
        payload.addProperty("executionYearName", executionYearName);
        payload.addProperty("name", person.getName());
        payload.addProperty("idDocType", documentType);
        payload.addProperty("idDocNumber", idNumber);
        payload.addProperty("nationality", nationality);
        payload.add("curricularYear", curricularYear);
        payload.addProperty("degreeName", degreeName);
        payload.addProperty("numberOfEnrollments", registration.getEnrolments(executionYear).size());

        if (registration.getCampus(executionYear).equals(SpaceUtils.getDefaultCampus())) {
            setAlamedaAddress(payload);
        } else {
            setTagusparkAddress(payload);
        }

        setTodayDate(payload);
        return payload;
    }

    private void setAddress(JsonObject payload, String institutionName, String institutionCode, String
            institutionStreetAddress, String institutionParishName, String institutionCountyName, String institutionPostalCode, String
            institutionPhoneNumber, String institutionFaxNumber, String institutionEmail) {
        payload.addProperty("institutionName",institutionName);
        payload.addProperty("institutionCode",institutionCode);
        payload.addProperty("institutionStreetAddress",institutionStreetAddress);
        payload.addProperty("institutionParishName",institutionParishName);
        payload.addProperty("institutionCountyName",institutionCountyName);
        payload.addProperty("institutionPostalCode",institutionPostalCode);
        payload.addProperty("institutionPhoneNumber",institutionPhoneNumber);
        payload.addProperty("institutionFaxNumber",institutionFaxNumber);
        payload.addProperty("institutionEmail",institutionEmail);
    }

    private void setAlamedaAddress(JsonObject payload) {
        setAddress(payload, "Instituto Superior Técnico","1518", "Av. Rovisco Pais, 1", "Areeiro", "Lisboa",
                "1049-001",
                "+351"
                        + " 218 417 000", "+351 219 499 242", "mail@tecnico.ulisboa.pt");
    }

    private void setTagusparkAddress(JsonObject payload) {
        setAddress(payload, "Instituto Superior Técnico", "1529", "Av. Prof. Doutor Cavaco Silva", "Porto Salvo",
                "Oeiras", "2744-016 Porto Salvo", "+351 214 233 200", "+351 214 233 268", "mail@tecnico.ulisboa.pt");
    }

    private void setTodayDate(JsonObject payload) {
        DateTime todayDate = new DateTime();
        String[] months = new String[] {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto",
                "Setembro", "Outubro", "Novembro", "Dezembro"};
        payload.addProperty("todayDate", String.format("%d de %s de %d", todayDate.getDayOfMonth(), months[todayDate
                .getMonthOfYear() - 1], todayDate.getYear()));
    }

    private String getDegreeDescription(Registration registration, ExecutionYear executionYear, Locale locale) {
        String degreeName = registration.getDegree().getFilteredName(executionYear, locale);
        if (registration.getDegree().isEmpty()) {
            return degreeName;
        }
        
        String degreeTypeName = registration.getDegreeType().getName().getContent(locale).replaceAll("Bolonha", "").replaceAll("Bologna", "");
        return BundleUtil.getString("resources.RegistrationProcessResources", locale, "registration.document.degree.full.name",
                degreeTypeName, degreeName);
    }
}
