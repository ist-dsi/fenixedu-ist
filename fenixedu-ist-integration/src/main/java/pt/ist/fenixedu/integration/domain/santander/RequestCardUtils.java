package pt.ist.fenixedu.integration.domain.santander;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
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
import org.fenixedu.idcards.IdCardsConfiguration;
import org.fenixedu.idcards.domain.SantanderPhotoEntry;
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
import pt.sibscartoes.portal.wcf.IRegistersInfo;
import pt.sibscartoes.portal.wcf.dto.FormData;
import pt.sibscartoes.portal.wcf.dto.RegisterData;
import pt.sibscartoes.portal.wcf.tui.ITUIDetailService;
import pt.sibscartoes.portal.wcf.tui.dto.TUIResponseData;
import pt.sibscartoes.portal.wcf.tui.dto.TuiPhotoRegisterData;
import pt.sibscartoes.portal.wcf.tui.dto.TuiSignatureRegisterData;

public class RequestCardUtils {

    private static final Logger logger = LoggerFactory.getLogger(RequestCardUtils.class);

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

    public static String getRegister(Person person) {

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();

        factory.setServiceClass(IRegistersInfo.class);
        factory.setAddress("https://portal.sibscartoes.pt/wcf/RegistersInfo.svc");
        factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
        factory.getFeatures().add(new WSAddressingFeature());

        //Add loggers to request
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());

        IRegistersInfo port = (IRegistersInfo) factory.create();

        /*define WSDL policy*/
        Client client = ClientProxy.getClient(port);
        HTTPConduit http = (HTTPConduit) client.getConduit();

        //Add username and password properties
        http.getAuthorization().setUserName(IdCardsConfiguration.getConfiguration().sibsWebServiceUsername());
        http.getAuthorization().setPassword(IdCardsConfiguration.getConfiguration().sibsWebServicePassword());

        final String userName = Strings.padEnd(person.getUsername(), 10, 'x');

        RegisterData statusInformation = port.getRegister(userName);

        String result = userName + " : " + statusInformation.getStatusDate().getValue().replaceAll("-", "/") + " : "
                + statusInformation.getStatus().getValue() + " - " + statusInformation.getStatusDesc().getValue();

        FormData formData = port.getFormStatus(userName);

        String template = "%s | Entity: %s | IdentRegNum: %s | NDoc: %s | Status: %s | Date: %s";
        result = String.format(template, userName, formData.getEntityCode().getValue(), formData.getIdentRegNum().getValue(),
                formData.getNDoc().getValue(), formData.getStatus().getValue(), formData.getIdentRegNum().getValue());

        return result;
    }

    public static List<String> createRegister(String tuiEntry, TuiPhotoRegisterData photo) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();

        factory.setServiceClass(ITUIDetailService.class);
        factory.setAddress("https://portal.sibscartoes.pt/tstwcfv2/services/TUIDetailService.svc");
        factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
        factory.getFeatures().add(new WSAddressingFeature());

        //Add loggers to request
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());

        ITUIDetailService port = (ITUIDetailService) factory.create();

        /*define WSDL policy*/
        Client client = ClientProxy.getClient(port);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        //Add username and password properties
        http.getAuthorization().setUserName(IdCardsConfiguration.getConfiguration().sibsWebServiceUsername());
        http.getAuthorization().setPassword(IdCardsConfiguration.getConfiguration().sibsWebServicePassword());

        ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();

        TuiSignatureRegisterData signature = new TuiSignatureRegisterData();

        logger.debug("line: %s %d%n", tuiEntry, tuiEntry.length());

        TUIResponseData tuiResponse = port.saveRegister(tuiEntry, photo, signature);
        
        List<String> result = new ArrayList<>();

        result.add(tuiResponse.getStatus().getValue());
        result.add(tuiResponse.getStatusDescription().getValue());
        result.add(tuiResponse.getTuiResponseLine().getValue());

        logger.debug("Response Status: " + result.get(0) + " -- Description: " + result.get(1));
        logger.debug("Response Line: " + result.get(2));

        return result;

    }

    public static TuiPhotoRegisterData getOrCreateSantanderPhoto(Person person) {
        final QName FILE_NAME =
                new QName("http://schemas.datacontract.org/2004/07/SibsCards.Wcf.Services.DataContracts", "FileName");
        final QName FILE_EXTENSION =
                new QName("http://schemas.datacontract.org/2004/07/SibsCards.Wcf.Services.DataContracts", "Extension");
        final QName FILE_CONTENTS =
                new QName("http://schemas.datacontract.org/2004/07/SibsCards.Wcf.Services.DataContracts", "FileContents");
        final QName FILE_SIZE = new QName("http://schemas.datacontract.org/2004/07/SibsCards.Wcf.Services.DataContracts", "Size");

        final String EXTENSION = ".jpeg";

        TuiPhotoRegisterData photo = new TuiPhotoRegisterData();

        SantanderPhotoEntry photoEntry = SantanderPhotoEntry.getOrCreatePhotoEntryForPerson(person);
        byte[] photo_contents = photoEntry.getPhotoAsByteArray();

        photo.setFileContents(new JAXBElement<byte[]>(FILE_CONTENTS, byte[].class, photo_contents));
        photo.setSize(new JAXBElement<String>(FILE_SIZE, String.class, new Integer(photo_contents.length).toString()));
        photo.setExtension(new JAXBElement<String>(FILE_EXTENSION, String.class, new String(".jpeg")));
        photo.setFileName(new JAXBElement<String>(FILE_NAME, String.class, "foto")); //TODO

        return photo;

    }

    public static List<String> generateLine(Person person, ExecutionYear executionYear, String action) {
        /*
         * 1. Teacher
         * 2. Researcher
         * 3. Employee
         * 4. GrantOwner
         * 5. Student
         */
        List<String> line = new ArrayList<>();
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

    private static List<String> createLine(Person person, String role, ExecutionYear executionYear, String action) {

        List<String> line = new ArrayList<>();

        String recordType = "2";

        String idNumber = makeStringBlock(person.getUsername(), 10);

        String[] names = harvestNames(person.getName());
        String name = makeStringBlock(names[0], 15);
        String surname = makeStringBlock(names[1], 15);
        String middleNames = makeStringBlock(names[2], 40);

        String degreeCode = makeStringBlock(getDegreeDescription(person, role, executionYear), 16);
        if (role.equals("STUDENT") && degreeCode.startsWith(" ")) {
            return null;
        }

        CampusAddress campusAddr = getCampusAddress(person, role);
        if (campusAddr == null) {
            return null;
        }
        String address1 = makeStringBlock(campusAddr.getAddress(), 50);
        String address2 = makeStringBlock((IST_FULL_NAME + (degreeCode == null ? "" : " " + degreeCode)).trim(), 50);

        String zipCode = campusAddr.getZip();
        String town = makeStringBlock(campusAddr.getTown(), 30);

        String homeCountry = makeStringBlock("", 10);

        String residenceCountry = makeStringBlock(person.getUsername(), 10); // As stipulated this field will carry the istId instead.

        String expireDate = getExpireDate(executionYear);

        String backNumber = makeZeroPaddedNumber(Integer.parseInt(person.getUsername().substring(3)), 10);

        String curricularYear = "00";
        String executionYear_field = "00000000";

        String unit = makeStringBlock("", 30); //Size changed from 11 to 30
        if (role.equals("TEACHER")) {
            unit = makeStringBlock(person.getTeacher().getDepartment().getAcronym(), 30);
        }

        String accessContrl = makeStringBlock("", 10);

        String expireData_AAMM = expireDate.substring(7) + "08"; //TODO

        String templateCode = makeStringBlock("", 10); //TODO

        String actionCode = makeStringBlock(action, 4); //TODO

        String roleCode = getRoleCode(role);

        String roleDesc = makeStringBlock(getRoleDescripriton(role), 20);

        String idDocumentType = makeStringBlock("0", 1); // TODO

        String checkDigit = makeStringBlock("", 1); // TODO

        String cardType = makeStringBlock("00", 2); // TODO

        String expedictionCode = makeStringBlock("00", 2); // TODO

        String detourAdress1 = makeStringBlock("", 50); // TODO

        String detourAdress2 = makeStringBlock("", 50); // TODO

        String detourAdress3 = makeStringBlock("", 50); // TODO

        String detourZipCode = makeStringBlock("", 8); // TODO

        String detourTown = makeStringBlock("", 30); // TODO

        String aditionalData = makeStringBlock("1", 1); // TODO

        String cardName = makeStringBlock(names[0].toUpperCase() + " " + names[1].toUpperCase(), 40); // TODO

        String email = makeStringBlock("", 100); // TODO

        String phone = makeStringBlock("", 20); // TODO

        String photoFlag = makeStringBlock("0", 1); // TODO

        String photoRef = makeStringBlock("", 32); // TODO

        String signatureFlag = makeStringBlock("0", 1); // TODO

        String signatureRef = makeStringBlock("", 32); // TODO

        String digCertificateFlag = makeStringBlock("0", 1); // TODO

        String digCertificateRef = makeStringBlock("", 32); // TODO

        String filler = makeStringBlock("", 682);

        line.add(recordType); //0
        line.add(idNumber); //1
        line.add(name); //2
        line.add(surname); //3
        line.add(middleNames); //4
        line.add(address1); //5
        line.add(address2); //6
        line.add(zipCode); //7
        line.add(town); //8
        line.add(homeCountry); //9
        line.add(residenceCountry); //10
        line.add(expireDate); //11
        line.add(degreeCode); //12
        line.add(backNumber); //13
        line.add(curricularYear); //14
        line.add(executionYear_field); //15
        line.add(unit); //16
        line.add(accessContrl); //17
        line.add(expireData_AAMM); //18
        line.add(templateCode); //19
        line.add(actionCode); //20
        line.add(roleCode); //21
        line.add(roleDesc); //22
        line.add(idDocumentType); //23
        line.add(checkDigit); //24
        line.add(cardType); //25
        line.add(expedictionCode); //26
        line.add(detourAdress1); //27
        line.add(detourAdress2); //28
        line.add(detourAdress3); //29
        line.add(detourZipCode); //30
        line.add(detourTown); //31
        line.add(aditionalData); //32
        line.add(cardName); //33
        line.add(email); //34
        line.add(phone); //35
        line.add(photoFlag); //36
        line.add(photoRef); //37
        line.add(signatureFlag); //38
        line.add(signatureRef); //39
        line.add(digCertificateFlag); //40
        line.add(digCertificateRef); //41
        line.add(filler); //42

        logger.debug("recordType: " + recordType + " -- size: " + recordType.length());
        logger.debug("idNumber: " + idNumber + " -- size: " + idNumber.length());
        logger.debug("name: " + name + " -- size: " + name.length());
        logger.debug("surname: " + surname + " -- size: " + surname.length());
        logger.debug("middleNames: " + middleNames + " -- size: " + middleNames.length());
        logger.debug("address1: " + address1 + " -- size: " + address1.length());
        logger.debug("address2: " + address2 + " -- size: " + address2.length());
        logger.debug("zipCode: " + zipCode + " -- size: " + zipCode.length());
        logger.debug("town: " + town + " -- size: " + town.length());
        logger.debug("homeCountry: " + homeCountry + " -- size: " + homeCountry.length());
        logger.debug("residenceCountry: " + residenceCountry + " -- size: " + residenceCountry.length());
        logger.debug("expireDate: " + expireDate + " -- size: " + expireDate.length());
        logger.debug("degreeCode: " + degreeCode + " -- size: " + degreeCode.length());
        logger.debug("backNumber: " + backNumber + " -- size: " + backNumber.length());
        logger.debug("curricularYear: " + curricularYear + " -- size: " + curricularYear.length());
        logger.debug("executionYear_field: " + executionYear_field + " -- size: " + executionYear_field.length());
        logger.debug("unit: " + unit + " -- size: " + unit.length());
        logger.debug("accessContrl: " + accessContrl + " -- size: " + accessContrl.length());
        logger.debug("expireData_AAMM: " + expireData_AAMM + " -- size: " + expireData_AAMM.length());
        logger.debug("templateCode: " + templateCode + " -- size: " + templateCode.length());
        logger.debug("actionCode: " + actionCode + " -- size: " + actionCode.length());
        logger.debug("roleCode: " + roleCode + " -- size: " + roleCode.length());
        logger.debug("roleDesc: " + roleDesc + " -- size: " + roleDesc.length());
        logger.debug("idDocumentType: " + idDocumentType + " -- size: " + idDocumentType.length());
        logger.debug("checkDigit: " + checkDigit + " -- size: " + checkDigit.length());
        logger.debug("cardType: " + cardType + " -- size: " + cardType.length());
        logger.debug("expedictionCode: " + expedictionCode + " -- size: " + expedictionCode.length());
        logger.debug("detourAdress1: " + detourAdress1 + " -- size: " + detourAdress1.length());
        logger.debug("detourAdress2: " + detourAdress2 + " -- size: " + detourAdress2.length());
        logger.debug("detourAdress3: " + detourAdress3 + " -- size: " + detourAdress3.length());
        logger.debug("detourZipCode: " + detourZipCode + " -- size: " + detourZipCode.length());
        logger.debug("detourTown: " + detourTown + " -- size: " + detourTown.length());
        logger.debug("aditionalData: " + aditionalData + " -- size: " + aditionalData.length());
        logger.debug("cardName: " + cardName + " -- size: " + cardName.length());
        logger.debug("email: " + email + " -- size: " + email.length());
        logger.debug("phone: " + phone + " -- size: " + phone.length());
        logger.debug("photoFlag: " + photoFlag + " -- size: " + photoFlag.length());
        logger.debug("photoRef: " + photoRef + " -- size: " + photoRef.length());
        logger.debug("signatureFlag: " + signatureFlag + " -- size: " + signatureFlag.length());
        logger.debug("signatureRef: " + signatureRef + " -- size: " + signatureRef.length());
        logger.debug("digCertificateFlag: " + digCertificateFlag + " -- size: " + digCertificateFlag.length());
        logger.debug("digCertificateRef: " + digCertificateRef + " -- size: " + digCertificateRef.length());
        logger.debug("filler: " + filler + " -- size: " + filler.length());

        return line;
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
                logger.debug("phdProcess: " + process.getExternalId());
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

    private static String makeStringBlock(String content, int size) {
        int fillerLength = size - content.length();
        if (fillerLength < 0) {
            throw new DomainException("Content is bigger than string block.");
        }
        StringBuilder blockBuilder = new StringBuilder(size);
        blockBuilder.append(content);

        for (int i = 0; i < fillerLength; i++) {
            blockBuilder.append(" ");
        }

        return blockBuilder.toString();
    }

    private static Map<String, CampusAddress> getCampi() {
        Map<String, CampusAddress> exports = new HashMap<String, CampusAddress>();
        exports.put("alameda", new CampusAddress(alamedaAddr, alamedaZip, alamedaTown));
        exports.put("tagus", new CampusAddress(tagusAddr, tagusZip, tagusTown));
        exports.put("itn", new CampusAddress(itnAddr, itnZip, itnTown));
        return exports;
    }

}