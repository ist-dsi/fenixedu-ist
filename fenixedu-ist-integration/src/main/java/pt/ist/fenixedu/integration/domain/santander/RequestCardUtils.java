package pt.ist.fenixedu.integration.domain.santander;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcessState;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.idcards.utils.SantanderEntryUtils;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveGrantOwner;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveResearchers;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonContractSituation;
import pt.ist.fenixedu.contracts.domain.util.CategoryType;
import pt.ist.fenixframework.FenixFramework;

public class RequestCardUtils {

    private static Logger logger = LoggerFactory.getLogger(RequestCardUtils.class);

    private static String alamedaAddr = "Avenida Rovisco Pais, 1";
    private static String alamedaZip = "1049-001";
    private static String alamedaTown = "Lisboa";
    private static String tagusAddr = "Av. Prof. Doutor Aníbal Cavaco Silva";
    private static String tagusZip = "2744-016";
    private static String tagusTown = "Porto Salvo";
    private static String itnAddr = "Estrada Nacional 10 (ao Km 139,7)";
    private static String itnZip = "2695-066";
    private static String itnTown = "Bobadela";
    private static String IST_FULL_NAME = "Instituto Superior Técnico";

    private static Map<String, CampusAddress> campi = getCampi();

    public static String generateLine(Person person, ExecutionYear executionYear, String action) {
        /*
         * 1. Teacher
         * 2. Researcher
         * 3. Employee
         * 4. GrantOwner
         * 5. Student
         */
        String line = null;
        if (treatAsStudent(person, executionYear)) {
            line = createLine(person, "STUDENT", executionYear, action);
        } else if (treatAsTeacher(person)) {
            line = createLine(person, "TEACHER", executionYear, action);
        } else if (treatAsResearcher(person)) {
            line = createLine(person, "RESEARCHER", executionYear, action);
        } else if (treatAsEmployee(person)) {
            line = createLine(person, "EMPLOYEE", executionYear, action);
        } else if (treatAsGrantOwner(person)) {
            line = createLine(person, "GRANT_OWNER", executionYear, action);
        }

        return line;
    }

    private static boolean treatAsTeacher(Person person) {
        if (person.getTeacher() != null) {
            return person.getTeacher().isActiveContractedTeacher();
        }
        return false;
    }

    private static boolean treatAsResearcher(Person person) {
        if (person.getEmployee() != null) {
            return new ActiveResearchers().isMember(person.getUser());
        }
        return false;
    }

    private static boolean treatAsEmployee(Person person) {
        if (person.getEmployee() != null) {
            return person.getEmployee().isActive();
        }
        return false;
    }

    private static boolean treatAsGrantOwner(Person person) {
        return (isGrantOwner(person)) || (new ActiveGrantOwner().isMember(person.getUser()) && person.getEmployee() != null
                && !new ActiveEmployees().isMember(person.getUser()) && person.getPersonProfessionalData() != null);
    }

    private static boolean treatAsStudent(Person person, ExecutionYear executionYear) {
        if (person.getStudent() != null) {
            final List<Registration> activeRegistrations = person.getStudent().getActiveRegistrations();
            for (final Registration registration : activeRegistrations) {
                if (registration.isBolonha() && !registration.getDegreeType().isEmpty()) {
                    return true;
                }
            }
            final InsuranceEvent event = person.getInsuranceEventFor(executionYear);
            final PhdIndividualProgramProcess phdIndividualProgramProcess =
                    event != null && event.isClosed() ? find(person.getPhdIndividualProgramProcessesSet()) : null;
            return (phdIndividualProgramProcess != null);
        }
        return false;
    }

    private static boolean isGrantOwner(final Person person) {
        if (new ActiveGrantOwner().isMember(person.getUser())) {
            final PersonContractSituation currentGrantOwnerContractSituation = person.getPersonProfessionalData() != null ? person
                    .getPersonProfessionalData().getCurrentPersonContractSituationByCategoryType(CategoryType.GRANT_OWNER) : null;
            if (currentGrantOwnerContractSituation != null && currentGrantOwnerContractSituation.getProfessionalCategory() != null
                    && person.getEmployee() != null && person.getEmployee().getCurrentWorkingPlace() != null) {
                return true;
            }
        }
        return false;
    }

    private static PhdIndividualProgramProcess find(final Set<PhdIndividualProgramProcess> phdIndividualProgramProcesses) {
        PhdIndividualProgramProcess result = null;
        for (final PhdIndividualProgramProcess process : phdIndividualProgramProcesses) {
            if (process.getActiveState() == PhdIndividualProgramProcessState.WORK_DEVELOPMENT) {
                if (result != null) {
                    return null;
                }
                result = process;
            }
        }
        return result;
    }

    private static String createLine(Person person, String role, ExecutionYear executionYear, String action) {

        List<String> values = new ArrayList<>();

        String recordType = "2";

        String idNumber = person.getUsername();

        String[] names = harvestNames(person.getName());
        String name = names[0];
        String surname = names[1];
        String middleNames = names[2];

        String degreeCode = getDegreeDescription(person, role, executionYear);
        if (role.equals("STUDENT") && degreeCode.startsWith(" ")) {
            return null;
        }

        CampusAddress campusAddr = getCampusAddress(person, role);
        if (campusAddr == null) {
            return null;
        }
        String address1 = campusAddr.getAddress();
        String address2 = (IST_FULL_NAME + (degreeCode == null ? "" : " " + degreeCode)).trim();

        String zipCode = campusAddr.getZip();
        String town = campusAddr.getTown();

        String homeCountry = "";

        String residenceCountry = person.getUsername(); // As stipulated this field will carry the istId instead.

        String expireDate = getExpireDate(executionYear);

        String backNumber = makeZeroPaddedNumber(Integer.parseInt(person.getUsername().substring(3)), 10);

        String curricularYear = "00";
        String executionYear_field = "00000000";

        String unit = "";
        if (role.equals("TEACHER")) {
            unit = person.getTeacher().getDepartment().getAcronym();
        }

        String accessControl = "";

        String expireData_AAMM = expireDate.substring(7) + "08"; //TODO

        String templateCode = ""; //TODO

        String actionCode = action; //TODO

        String roleCode = getRoleCode(role);

        String roleDesc = getRoleDescripriton(role);

        String idDocumentType = "0"; // TODO

        String checkDigit = ""; // TODO

        String cardType = "00"; // TODO

        String expedictionCode = "00"; // TODO

        String detourAdress1 = ""; // TODO

        String detourAdress2 = ""; // TODO

        String detourAdress3 = ""; // TODO

        String detourZipCode = ""; // TODO

        String detourTown = ""; // TODO

        String aditionalData = "1"; // TODO

        String cardName = names[0].toUpperCase() + " " + names[1].toUpperCase(); // TODO

        String email = ""; // TODO

        String phone = ""; // TODO

        String photoFlag = "0"; // TODO

        String photoRef = ""; // TODO

        String signatureFlag = "0"; // TODO

        String signatureRef = ""; // TODO

        String digCertificateFlag = "0"; // TODO

        String digCertificateRef = ""; // TODO

        String filler = "";

        String endFlag = "1";

        values.add(recordType); //0
        values.add(idNumber); //1
        values.add(name); //2
        values.add(surname); //3
        values.add(middleNames); //4
        values.add(address1); //5
        values.add(address2); //6
        values.add(zipCode); //7
        values.add(town); //8
        values.add(homeCountry); //9
        values.add(residenceCountry); //10
        values.add(expireDate); //11
        values.add(degreeCode); //12
        values.add(backNumber); //13
        values.add(curricularYear); //14
        values.add(executionYear_field); //15
        values.add(unit); //16
        values.add(accessControl); //17
        values.add(expireData_AAMM); //18
        values.add(templateCode); //19
        values.add(actionCode); //20
        values.add(roleCode); //21
        values.add(roleDesc); //22
        values.add(idDocumentType); //23
        values.add(checkDigit); //24
        values.add(cardType); //25
        values.add(expedictionCode); //26
        values.add(detourAdress1); //27
        values.add(detourAdress2); //28
        values.add(detourAdress3); //29
        values.add(detourZipCode); //30
        values.add(detourTown); //31
        values.add(aditionalData); //32
        values.add(cardName); //33
        values.add(email); //34
        values.add(phone); //35
        values.add(photoFlag); //36
        values.add(photoRef); //37
        values.add(signatureFlag); //38
        values.add(signatureRef); //39
        values.add(digCertificateFlag); //40
        values.add(digCertificateRef); //41
        values.add(filler); //42
        values.add(endFlag); //43

        logger.debug("recordType: " + recordType + "| size: " + recordType.length());
        logger.debug("idNumber: " + idNumber + "| size: " + idNumber.length());
        logger.debug("name: " + name + "| size: " + name.length());
        logger.debug("surname: " + surname + "| size: " + surname.length());
        logger.debug("middleNames: " + middleNames + "| size: " + middleNames.length());
        logger.debug("address1: " + address1 + "| size: " + address1.length());
        logger.debug("address2: " + address2 + "| size: " + address2.length());
        logger.debug("zipCode: " + zipCode + "| size: " + zipCode.length());
        logger.debug("town: " + town + "| size: " + town.length());
        logger.debug("homeCountry: " + homeCountry + "| size: " + homeCountry.length());
        logger.debug("residenceCountry: " + residenceCountry + "| size: " + residenceCountry.length());
        logger.debug("expireDate: " + expireDate + "| size: " + expireDate.length());
        logger.debug("degreeCode: " + degreeCode + "| size: " + degreeCode.length());
        logger.debug("backNumber: " + backNumber + "| size: " + backNumber.length());
        logger.debug("curricularYear: " + curricularYear + "| size: " + curricularYear.length());
        logger.debug("executionYear_field: " + executionYear_field + "| size: " + executionYear_field.length());
        logger.debug("unit: " + unit + "| size: " + unit.length());
        logger.debug("accessContrl: " + accessControl + "| size: " + accessControl.length());
        logger.debug("expireData_AAMM: " + expireData_AAMM + "| size: " + expireData_AAMM.length());
        logger.debug("templateCode: " + templateCode + "| size: " + templateCode.length());
        logger.debug("actionCode: " + actionCode + "| size: " + actionCode.length());
        logger.debug("roleCode: " + roleCode + "| size: " + roleCode.length());
        logger.debug("roleDesc: " + roleDesc + "| size: " + roleDesc.length());
        logger.debug("idDocumentType: " + idDocumentType + "| size: " + idDocumentType.length());
        logger.debug("checkDigit: " + checkDigit + "| size: " + checkDigit.length());
        logger.debug("cardType: " + cardType + "| size: " + cardType.length());
        logger.debug("expedictionCode: " + expedictionCode + "| size: " + expedictionCode.length());
        logger.debug("detourAdress1: " + detourAdress1 + "| size: " + detourAdress1.length());
        logger.debug("detourAdress2: " + detourAdress2 + "| size: " + detourAdress2.length());
        logger.debug("detourAdress3: " + detourAdress3 + "| size: " + detourAdress3.length());
        logger.debug("detourZipCode: " + detourZipCode + "| size: " + detourZipCode.length());
        logger.debug("detourTown: " + detourTown + "| size: " + detourTown.length());
        logger.debug("aditionalData: " + aditionalData + "| size: " + aditionalData.length());
        logger.debug("cardName: " + cardName + "| size: " + cardName.length());
        logger.debug("email: " + email + "| size: " + email.length());
        logger.debug("phone: " + phone + "| size: " + phone.length());
        logger.debug("photoFlag: " + photoFlag + "| size: " + photoFlag.length());
        logger.debug("photoRef: " + photoRef + "| size: " + photoRef.length());
        logger.debug("signatureFlag: " + signatureFlag + "| size: " + signatureFlag.length());
        logger.debug("signatureRef: " + signatureRef + "| size: " + signatureRef.length());
        logger.debug("digCertificateFlag: " + digCertificateFlag + "| size: " + digCertificateFlag.length());
        logger.debug("digCertificateRef: " + digCertificateRef + "| size: " + digCertificateRef.length());
        logger.debug("filler: " + filler + "| size: " + filler.length());
        logger.debug("end_flag: " + endFlag + "| size: " + endFlag.length());

        return SantanderEntryUtils.generateLine(values);
    }

    private static String getRoleCode(String role) {
        switch (role) {
            case "STUDENT":
                return "01";

            case "TEACHER":
                return "02";

            case "EMPLOYEE":
                return "03";

            default:
                return "99";
        }
    }

    private static String[] harvestNames(String name) {
        String[] result = new String[3];
        String purgedName = purgeString(name); //Remove special characters
        String cleanedName = Strings.nullToEmpty(purgedName).trim();
        String[] names = cleanedName.split(" ");
        result[0] = names[0].length() > 15 ? names[0].substring(0, 15) : names[0];
        result[1] = names[names.length - 1].length() > 15 ? names[names.length - 1].substring(0, 15) : names[names.length - 1];
        String midNames = names.length > 2 ? names[1] : "";
        for (int i = 2; i < (names.length - 1); i++) {
            if (midNames.length() + names[i].length() + 1 > 40) {
                break;
            }
            midNames += " ";
            midNames += names[i];
        }
        result[2] = midNames;
        return result;
    }

    private static String purgeString(final String name) {
        if (!CharMatcher.javaLetter().or(CharMatcher.whitespace()).matchesAllOf(name)) {
            final char[] ca = new char[name.length()];
            int j = 0;
            for (int i = 0; i < name.length(); i++) {
                final char c = name.charAt(i);
                if (Character.isLetter(c) || c == ' ') {
                    ca[j++] = c;
                }
            }
            return new String(ca);
        }
        return name;
    }

    private static String getDegreeDescription(final Person person, String roleType, ExecutionYear executionYear) {
        if (roleType.equals("STUDENT") || roleType.equals("GRANT_OWNER")) {
            final PhdIndividualProgramProcess process = getPhdProcess(person);
            if (process != null) {
                return process.getPhdProgram().getAcronym();
            }
            final Degree degree = getDegree(person, executionYear);
            if (degree != null) {
                return degree.getSigla();
            }
        }
        if (roleType.equals("TEACHER")) {
            final Teacher teacher = person.getTeacher();
            final Department department = teacher.getDepartment();
            if (department != null) {
                return department.getAcronym();
            }
        }
        return "";
    }

    private static PhdIndividualProgramProcess getPhdProcess(final Person person) {
        final InsuranceEvent event = person.getInsuranceEventFor(ExecutionYear.readCurrentExecutionYear());
        return event != null && event.isClosed() ? find(person.getPhdIndividualProgramProcessesSet()) : null;
    }

    private static Degree getDegree(Person person, ExecutionYear executionYear) {
        final Student student = person.getStudent();
        if (student == null) {
            return null;
        }

        final DateTime begin = executionYear.getBeginDateYearMonthDay().toDateTimeAtMidnight();
        final DateTime end = executionYear.getEndDateYearMonthDay().toDateTimeAtMidnight();
        final Set<StudentCurricularPlan> studentCurricularPlans = new HashSet<StudentCurricularPlan>();
        StudentCurricularPlan pickedSCP;

        for (final Registration registration : student.getRegistrationsSet()) {
            if (!registration.isActive()) {
                continue;
            }
            if (registration.getDegree().isEmpty()) {
                continue;
            }
            final RegistrationProtocol registrationAgreement = registration.getRegistrationProtocol();
            if (!registrationAgreement.allowsIDCard()) {
                continue;
            }
            final DegreeType degreeType = registration.getDegreeType();
            if (!degreeType.isBolonhaType()) {
                continue;
            }
            for (final StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
                if (studentCurricularPlan.isActive()) {
                    if (degreeType.isBolonhaDegree() || degreeType.isBolonhaMasterDegree()
                            || degreeType.isIntegratedMasterDegree() || degreeType.isAdvancedSpecializationDiploma()) {
                        studentCurricularPlans.add(studentCurricularPlan);
                    } else {
                        final RegistrationState registrationState = registration.getActiveState();
                        if (registrationState != null) {
                            final DateTime dateTime = registrationState.getStateDate();
                            if (!dateTime.isBefore(begin) && !dateTime.isAfter(end)) {
                                studentCurricularPlans.add(studentCurricularPlan);
                            }
                        }
                    }
                }
            }
        }
        if (studentCurricularPlans.isEmpty()) {
            return null;
        }
        pickedSCP = Collections.max(studentCurricularPlans, new Comparator<StudentCurricularPlan>() {

            @Override
            public int compare(final StudentCurricularPlan o1, final StudentCurricularPlan o2) {
                final DegreeType degreeType1 = o1.getDegreeType();
                final DegreeType degreeType2 = o2.getDegreeType();
                if (degreeType1 == degreeType2) {
                    final YearMonthDay yearMonthDay1 = o1.getStartDateYearMonthDay();
                    final YearMonthDay yearMonthDay2 = o2.getStartDateYearMonthDay();
                    final int c = yearMonthDay1.compareTo(yearMonthDay2);
                    return c == 0 ? o1.getExternalId().compareTo(o2.getExternalId()) : c;
                } else {
                    return degreeType1.compareTo(degreeType2);
                }
            }

        });
        return pickedSCP.getRegistration().getDegree();
    }

    private static CampusAddress getCampusAddress(Person person, String role) {
        Space campus = null;
        switch (role) {
        case "STUDENT":
            boolean matched = false;
            if (person.getStudent() != null) {
                final List<Registration> activeRegistrations = person.getStudent().getActiveRegistrations();
                for (final Registration registration : activeRegistrations) {
                    if (registration.isBolonha() && !registration.getDegreeType().isEmpty()) {
                        matched = true;
                        campus = person.getStudent().getLastActiveRegistration().getCampus();
                    }
                }
            }
            if (!matched) {
                campus = FenixFramework.getDomainObject("2448131360897");
            }
            break;
        case "EMPLOYEE":
            try {
                campus = person.getEmployee().getCurrentCampus();
            } catch (NullPointerException npe) {
                return null;
            }
            break;
        case "TEACHER":
            try {
                campus = person.getPersonProfessionalData().getGiafProfessionalDataByCategoryType(CategoryType.TEACHER)
                        .getCampus();
            } catch (NullPointerException npe) {
                return null;
            }
            break;
        case "RESEARCHER":
            try {
                campus = person.getPersonProfessionalData().getGiafProfessionalDataByCategoryType(CategoryType.RESEARCHER)
                        .getCampus();
            } catch (NullPointerException npe) {
                return null;
            }
            break;
        case "GRANT_OWNER":
            try {
                campus = person.getPersonProfessionalData().getGiafProfessionalDataByCategoryType(CategoryType.GRANT_OWNER)
                        .getCampus();
            } catch (NullPointerException npe) {
                return null;
            }
            break;
        default:
            break;
        }
        if (campus == null) {
            return null;
        }

        if (campus.getName().equals("Alameda")) {
            return campi.get("alameda");
        }

        if (campus.getName().equals("Taguspark")) {
            return campi.get("tagus");
        }

        if (campus.getName().equals("Tecnológico e Nuclear")) {
            return campi.get("itn");
        }

        return null;

    }

    private static String getRoleDescripriton(String role) {
        switch (role) {
        case "STUDENT":
            return "Estudante/Student";
        case "TEACHER":
            return "Docente/Faculty";
        case "EMPLOYEE":
            return "Funcionario/Staff";
        case "RESEARCHER":
            return "Invest./Researcher";
        case "GRANT_OWNER":
            return "Bolseiro/Grant Owner";
        default:
            return "00";
        }
    }

    private static String getExpireDate(ExecutionYear year) {
        String result = "";
        int beginYear = year.getBeginCivilYear();
        int endYear = beginYear + 3;
        result = beginYear + "/" + endYear;
        return result;
    }

    private static String makeZeroPaddedNumber(int number, int size) {
        if (String.valueOf(number).length() > size) {
            throw new DomainException("Number has more digits than allocated room.");
        }
        String format = "%0" + size + "d";
        return String.format(format, number);
    }

    private static Map<String, CampusAddress> getCampi() {
        Map<String, CampusAddress> exports = new HashMap<String, CampusAddress>();
        exports.put("alameda", new CampusAddress(alamedaAddr, alamedaZip, alamedaTown));
        exports.put("tagus", new CampusAddress(tagusAddr, tagusZip, tagusTown));
        exports.put("itn", new CampusAddress(itnAddr, itnZip, itnTown));
        return exports;
    }

}