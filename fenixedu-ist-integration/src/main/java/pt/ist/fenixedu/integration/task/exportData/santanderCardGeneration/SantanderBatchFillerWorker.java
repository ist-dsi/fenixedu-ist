/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.task.exportData.santanderCardGeneration;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.idcards.domain.SantanderBatch;
import org.fenixedu.idcards.domain.SantanderEntry;
import org.fenixedu.idcards.domain.SantanderPhotoEntry;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveGrantOwner;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveResearchers;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonContractSituation;
import pt.ist.fenixedu.contracts.domain.util.CategoryType;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

public class SantanderBatchFillerWorker {
    private static String recordEnd = "*";
    private static String lineEnd = "\r\n";
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

    protected final Logger logger;

    public SantanderBatchFillerWorker(final Logger logger) {
        this.logger = logger;
    }

    public void run() {
        logger.info("[" + (new DateTime()).toString("yyyy-MM-dd HH:mm") + "] Looking for open batches to populate...\n");
        for (SantanderBatch batch : Bennu.getInstance().getSantanderBatchesSet()) {
            if (batch.getGenerated() != null) {
                continue;
            }
            final Set<Object[]> lines = Bennu.getInstance().getUserSet().stream().parallel()
                .map(user -> {
                    try {
                        return FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<Object[]>() {
                            @Override
                            public Object[] call() {
                                final Person person = user.getPerson();
                                return person == null ? null : generateLine(batch, person);
                            }
                        }, new AtomicInstance(TxMode.READ, false));
                    } catch (final Exception ex) {
                        throw new Error(ex);
                    }                    
                })
                .filter(o -> o != null)
                .collect(Collectors.toSet());
            fillBatch(batch, lines);
        }
        logger.info("[" + (new DateTime()).toString("yyyy-MM-dd HH:mm") + "] Work finished. :)");
    }

    @Atomic
    private void fillBatch(final SantanderBatch batch, final Set<Object[]> lines) {
        for (final Object[] o : lines) {
            final Person person = (Person) o[0];
            final String line = (String) o[1];
            final SantanderEntry entry = new SantanderEntry(batch, person, line);
            if (person.getSantanderPhotoEntry() == null) {
                final SantanderPhotoEntry photoEntry = SantanderPhotoEntry.getOrCreatePhotoEntryForPerson(person);
                if (photoEntry != null) {
                    photoEntry.setSantanderEntry(entry);
                }
            }
        }
        batch.setGenerated(new DateTime());
        logger.info("Processed batch #" + batch.getExternalId());
        logger.info("Total number of records: " + batch.getSantanderEntriesSet().size() + "\n");
    }

    private Object[] generateLine(final SantanderBatch batch, final Person person) {
        /*
         * 1. Teacher
         * 2. Researcher
         * 3. Employee
         * 4. GrantOwner
         * 5. Student
         */
        String line = null;
        if (treatAsStudent(person, batch.getExecutionYear())) {
            line = createLine(batch, person, "STUDENT");
        } else if (treatAsTeacher(person)) {
            line = createLine(batch, person, "TEACHER");
        } else if (treatAsResearcher(person)) {
            line = createLine(batch, person, "RESEARCHER");
        } else if (treatAsEmployee(person)) {
            line = createLine(batch, person, "EMPLOYEE");
        } else if (treatAsGrantOwner(person)) {
            line = createLine(batch, person, "GRANT_OWNER");
        }
        return line == null ? null : new Object[] { person, line };
    }

    private boolean treatAsTeacher(Person person) {
        if (person.getTeacher() != null) {
            return person.getTeacher().isActiveContractedTeacher();
        }
        return false;
    }

    private boolean treatAsResearcher(Person person) {
        if (person.getEmployee() != null) {
            return new ActiveResearchers().isMember(person.getUser());
        }
        return false;
    }

    private boolean treatAsEmployee(Person person) {
        if (person.getEmployee() != null) {
            return person.getEmployee().isActive();
        }
        return false;
    }

    private boolean treatAsGrantOwner(Person person) {
        return (isGrantOwner(person))
                || (new ActiveGrantOwner().isMember(person.getUser()) && person.getEmployee() != null
                        && !new ActiveEmployees().isMember(person.getUser()) && person.getPersonProfessionalData() != null);
    }

    private boolean treatAsStudent(Person person, ExecutionYear executionYear) {
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

    private boolean isGrantOwner(final Person person) {
        if (new ActiveGrantOwner().isMember(person.getUser())) {
            final PersonContractSituation currentGrantOwnerContractSituation =
                    person.getPersonProfessionalData() != null ? person.getPersonProfessionalData()
                            .getCurrentPersonContractSituationByCategoryType(CategoryType.GRANT_OWNER) : null;
            if (currentGrantOwnerContractSituation != null
                    && currentGrantOwnerContractSituation.getProfessionalCategory() != null && person.getEmployee() != null
                    && person.getEmployee().getCurrentWorkingPlace() != null) {
                return true;
            }
        }
        return false;
    }

    private PhdIndividualProgramProcess find(final Set<PhdIndividualProgramProcess> phdIndividualProgramProcesses) {
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

    private String makeStringBlock(String content, int size) {
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

    private String makeZeroPaddedNumber(int number, int size) {
        if (String.valueOf(number).length() > size) {
            throw new DomainException("Number has more digits than allocated room.");
        }
        String format = "%0" + size + "d";
        return String.format(format, number);
    }

    private String purgeString(final String name) {
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

    private String[] harvestNames(String name) {
        String[] result = new String[3];
        String purgedName = purgeString(name);
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

    private String getExpireDate(ExecutionYear year) {
        String result = "";
        int beginYear = year.getBeginCivilYear();
        int endYear = beginYear + 3;
        result = beginYear + "/" + endYear;
        return result;
    }

    private Degree getDegree(SantanderBatch batch, Person person) {
        final Student student = person.getStudent();
        if (student == null) {
            return null;
        }

        final DateTime begin = batch.getExecutionYear().getBeginDateYearMonthDay().toDateTimeAtMidnight();
        final DateTime end = batch.getExecutionYear().getEndDateYearMonthDay().toDateTimeAtMidnight();
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
//        final String degreeNameForIdCard = degree.getIdCardName();
//        if (degreeNameForIdCard == null || degreeNameForIdCard.isEmpty()) {
//            throw new Error("No degree name for id card specified.");
//        }
//        if (degreeNameForIdCard.length() > 50) {
//            throw new Error("Length of degree name for id card to long: " + degreeNameForIdCard + " has more than 50 characters.");
//        }
//        return degreeNameForIdCard;
        //return pickedSCP.getRegistration().getDegree().getSigla();
    }

    private CampusAddress getCampusAddress(Person person, String role) {
        Space campus = null;
        Map<String, CampusAddress> campi = getCampi();
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
                campus =
                        person.getPersonProfessionalData().getGiafProfessionalDataByCategoryType(CategoryType.TEACHER)
                                .getCampus();
            } catch (NullPointerException npe) {
                return null;
            }
            break;
        case "RESEARCHER":
            try {
                campus =
                        person.getPersonProfessionalData().getGiafProfessionalDataByCategoryType(CategoryType.RESEARCHER)
                                .getCampus();
            } catch (NullPointerException npe) {
                return null;
            }
            break;
        case "GRANT_OWNER":
            try {
                campus =
                        person.getPersonProfessionalData().getGiafProfessionalDataByCategoryType(CategoryType.GRANT_OWNER)
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

    private String createLine(SantanderBatch batch, Person person, String role) {
//        if (SantanderPhotoEntry.getOrCreatePhotoEntryForPerson(person) == null) {
//            return null;
//        }
        StringBuilder strBuilder = new StringBuilder(1505);
        String recordType = "2";

        String idNumber = makeStringBlock(person.getUsername(), 10);

        String[] names = harvestNames(person.getName());
        String name = makeStringBlock(names[0], 15);
        String surname = makeStringBlock(names[1], 15);
        String middleNames = makeStringBlock(names[2], 40);

        String degreeCode = makeStringBlock(getDegreeDescription(batch, person, role), 16);
        if (role.equals("STUDENT") && degreeCode.startsWith(" ")) {
            return null;
        }

        CampusAddress campusAddr = getCampusAddress(person, role);
        if (campusAddr == null) {
            return null;
        }
        String address1 = makeStringBlock(campusAddr.getAddress(), 50);
        String address2 = makeStringBlock((IST_FULL_NAME + (degreeCode == null ? "" : " " + degreeCode)).trim(), 50); //makeStringBlock(getDegreeDescription(batch, person, role), 50);
        String zipCode = campusAddr.getZip();
        String town = makeStringBlock(campusAddr.getTown(), 30);

        String homeCountry = makeStringBlock("", 10);

        String residenceCountry = makeStringBlock(person.getUsername(), 10); // As stipulated this field will carry the istId instead.

        String expireDate = getExpireDate(batch.getExecutionYear());

//        if (role == RoleType.STUDENT) {
//            degreeCode = getDegreeCode(batch, person);
//            if (degreeCode == null) {
//                return null;
//            }
//            degreeCode = makeStringBlock(degreeCode, 16);
//        } else if (role == RoleType.GRANT_OWNER) {
//            degreeCode = getDegreeCode(batch, person);
//            if (degreeCode == null) {
//                degreeCode = makeStringBlock("", 16);
//            } else {
//                degreeCode = makeStringBlock(degreeCode, 16);
//            }
//        } else {
//            degreeCode = makeStringBlock("", 16);
//        }

        String backNumber = makeZeroPaddedNumber(Integer.parseInt(person.getUsername().substring(3)), 10);

        String chip1 = buildChip1Block(batch, person, role);

        String chip2 = makeStringBlock("", 180);

        String chip3 = makeStringBlock("", 180);

        String chip4 = makeStringBlock("", 180);

        String chip5 = makeStringBlock("", 180);

        String chip6 = makeStringBlock("", 180);

        String filler = makeStringBlock("", 145);

        strBuilder.append(recordType);
        strBuilder.append(idNumber);
        strBuilder.append(name);
        strBuilder.append(surname);
        strBuilder.append(middleNames);
        strBuilder.append(address1);
        strBuilder.append(address2);
        strBuilder.append(zipCode);
        strBuilder.append(town);
        strBuilder.append(homeCountry);
        strBuilder.append(residenceCountry);
        strBuilder.append(expireDate);
        strBuilder.append(degreeCode);
        strBuilder.append(backNumber);
        strBuilder.append(chip1);
        strBuilder.append(chip2);
        strBuilder.append(chip3);
        strBuilder.append(chip4);
        strBuilder.append(chip5);
        strBuilder.append(chip6);
        strBuilder.append(filler);
        strBuilder.append(recordEnd);
        strBuilder.append(lineEnd);

        return strBuilder.toString();
    }

    private String getDegreeDescription(final SantanderBatch batch, final Person person, String roleType) {
        if (roleType.equals("STUDENT") || roleType.equals("GRANT_OWNER")) {
            final PhdIndividualProgramProcess process = getPhdProcess(person);
            if (process != null) {
                logger.debug("phdProcess: " + process.getExternalId());
                return process.getPhdProgram().getAcronym();
            }
            final Degree degree = //getDegree(person.getStudent());
                    getDegree(batch, person);
            if (degree != null) {
                return degree.getSigla();
//              final String degreeNameForIdCard = degree.getIdCardName();
//              if (degreeNameForIdCard == null || degreeNameForIdCard.isEmpty()) {
//                  throw new Error("No degree name for id card specified.");
//              }
//              if (degreeNameForIdCard.length() > 50) {
//                  throw new Error("Length of degree name for id card to long: " + degreeNameForIdCard + " has more than 50 characters.");
//              }
//              return degreeNameForIdCard;
                //return pickedSCP.getRegistration().getDegree().getSigla();
            }
        }
        if (roleType.equals("TEACHER")) {
            final Teacher teacher = person.getTeacher();
            final Department department = teacher.getDepartment();
            if (department != null) {
                //return department.getDepartmentUnit().getIdentificationCardLabel();
                return department.getAcronym();
            }
        }
        //return "Técnico Lisboa";
        return "";
    }

    private PhdIndividualProgramProcess getPhdProcess(final Person person) {
        final InsuranceEvent event = person.getInsuranceEventFor(ExecutionYear.readCurrentExecutionYear());
        return event != null && event.isClosed() ? find(person.getPhdIndividualProgramProcessesSet()) : null;
    }

    private String buildChip1Block(SantanderBatch batch, Person person, String role) {
        StringBuilder chip1String = new StringBuilder(185);

        String idNumber = makeZeroPaddedNumber(Integer.parseInt(person.getUsername().substring(3)), 10);

        String roleCode = "7";

        /*switch (role) {
        case STUDENT:
            roleCode = "1";
            break;

        case TEACHER:
            roleCode = "2";
            break;

        case EMPLOYEE:
            roleCode = "3";
            break;

        default:
            roleCode = "7";
            break;
        }*/

        String curricularYear = "00";
        String executionYear = "00000000";

        String unit = makeStringBlock("", 11);
        if (role.equals("TEACHER")) {
            unit = makeStringBlock(person.getTeacher().getDepartment().getAcronym(), 11);
        }

        String activeCard = "0";
        String personPin = "0000";
        String institutionPin = "0000";
        String accessContrl = makeStringBlock("", 10);

        String altRoleKey = makeStringBlock("", 5);
        String altRoleCode = " ";
        String altRoleTemplate = "  ";
        String altRoleDescription = makeStringBlock("", 20);
        if (roleCode.equals("7")) {
            switch (role) {
            case "STUDENT":
                altRoleKey = "CATEG";
                altRoleCode = "1";
                altRoleTemplate = "02";
                altRoleDescription = makeStringBlock("Estudante/Student", 20);
                break;

            case "TEACHER":
                altRoleKey = "CATEG";
                altRoleCode = "2";
                altRoleTemplate = "02";
                altRoleDescription = makeStringBlock("Docente/Faculty", 20);
                break;

            case "EMPLOYEE":
                altRoleKey = "CATEG";
                altRoleCode = "3";
                altRoleTemplate = "02";
                altRoleDescription = makeStringBlock("Funcionario/Staff", 20);
                break;

            case "RESEARCHER":
                altRoleKey = "CATEG";
                altRoleCode = "8";
                altRoleTemplate = "02";
                altRoleDescription = makeStringBlock("Invest./Researcher", 20);
                break;

            case "GRANT_OWNER":
                altRoleKey = "CATEG";
                altRoleCode = "9";
                altRoleTemplate = "02";
                altRoleDescription = makeStringBlock("Bolseiro/Grant Owner", 20);
                break;

            default:
                break;
            }
        }

        String filler = makeStringBlock("", 101);

        chip1String.append(idNumber);
        chip1String.append(roleCode);
        chip1String.append(curricularYear);
        chip1String.append(executionYear);
        chip1String.append(unit);
        chip1String.append(activeCard);
        chip1String.append(personPin);
        chip1String.append(institutionPin);
        chip1String.append(accessContrl);
        chip1String.append(altRoleKey);
        chip1String.append(altRoleCode);
        chip1String.append(altRoleTemplate);
        chip1String.append(altRoleDescription);
        chip1String.append(filler);

        return chip1String.toString();
    }

    private class CampusAddress {
        private final String address;
        private final String zip;
        private final String town;

        private CampusAddress(String address, String zip, String town) {
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

    private Map<String, CampusAddress> getCampi() {
        Map<String, CampusAddress> exports = new HashMap<String, CampusAddress>();
        exports.put("alameda", new CampusAddress(alamedaAddr, alamedaZip, alamedaTown));
        exports.put("tagus", new CampusAddress(tagusAddr, tagusZip, tagusTown));
        exports.put("itn", new CampusAddress(itnAddr, itnZip, itnTown));
        return exports;
    }
}
