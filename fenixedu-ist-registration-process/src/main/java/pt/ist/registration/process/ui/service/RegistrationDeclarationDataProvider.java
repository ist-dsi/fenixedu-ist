package pt.ist.registration.process.ui.service;

import static org.fenixedu.bennu.RegistrationProcessConfiguration.RESOURCE_BUNDLE;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.organizationalStructure.UniversityUnit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;
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
        String curricularYear = BundleUtil.getString(Bundle.ENUMERATION, locale,
                Integer.toString(registration.getCurricularYear(executionYear)) + ".ordinal");
        String degreeName = getDegreeDescription(registration, executionYear, locale);
        String nationality = person.getCountry().getCountryNationality().getContent(locale);

        JsonObject payload = new JsonObject();
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
        payload.addProperty("curricularYear", curricularYear);
        payload.addProperty("degreeName", degreeName);
        payload.addProperty("numberOfEnrollments", registration.getEnrolments(executionYear).size());
        return payload;
    }

    private String getDegreeDescription(Registration registration, ExecutionYear executionYear, Locale locale) {
        String degreeName = registration.getDegree().getFilteredName(executionYear, locale);
        if (registration.getDegree().isEmpty()) {
            return degreeName;
        }
        String degreeTypeName = registration.getDegreeType().getName().getContent(locale).replaceAll("Bolonha", ""); //dirty
        // hack until degree type has display form ?
        return BundleUtil.getString("resources.RegistrationProcessResources", locale, "registration.document.degree.full.name",
                degreeTypeName, degreeName);
    }
}
