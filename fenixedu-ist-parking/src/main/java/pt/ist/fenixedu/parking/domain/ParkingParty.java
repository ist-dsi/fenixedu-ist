/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Parking.
 *
 * FenixEdu IST Parking is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Parking is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Parking.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.parking.domain;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.person.RoleType;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.I18N;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveGrantOwner;
import pt.ist.fenixedu.contracts.domain.organizationalStructure.Invitation;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonContractSituation;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonProfessionalData;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.ProfessionalCategory;
import pt.ist.fenixedu.contracts.domain.util.CategoryType;
import pt.ist.fenixedu.parking.domain.ParkingRequest.ParkingRequestFactoryCreator;
import pt.ist.fenixedu.parking.dto.ParkingPartyBean;
import pt.ist.fenixedu.parking.dto.VehicleBean;
import pt.ist.fenixframework.dml.runtime.RelationAdapter;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class ParkingParty extends ParkingParty_Base {

    static {
        Party.getRelationRootDomainObjectParty().addListener(new RelationAdapter<Bennu, Party>() {
            @Override
            public void beforeRemove(Bennu o1, Party o2) {
                if (o2 instanceof Person) {
                    ParkingParty pp = o2.getParkingParty();
                    if (pp != null) {
                        pp.delete();
                    }
                }
            }

        });
    }

    public static ParkingParty readByCardNumber(Long cardNumber) {
        for (ParkingParty parkingParty : Bennu.getInstance().getParkingPartiesSet()) {
            if (parkingParty.getCardNumber() != null && parkingParty.getCardNumber().equals(cardNumber)) {
                return parkingParty;
            }
        }
        return null;
    }

    public ParkingParty(Party party) {
        super();
        setRootDomainObject(Bennu.getInstance());
        setParty(party);
        setAuthorized(Boolean.FALSE);
        setAcceptedRegulation(Boolean.FALSE);
    }

    @Override
    public void setCardNumber(Long cardNumber) {
        if (getCardNumber() == null || getCardNumber() != cardNumber) {
            new ParkingPartyHistory(this, false);
            super.setCardNumber(cardNumber);
        }
    }

    public boolean getHasAllNecessaryPersonalInfo() {
        return ((getParty().getDefaultPhone() != null && !Strings.isNullOrEmpty(getParty().getDefaultPhone().getNumber())) || (getParty()
                .getDefaultMobilePhone() != null && !Strings.isNullOrEmpty(getParty().getDefaultMobilePhone().getNumber())))
                && (isEmployee() || (getParty().getDefaultEmailAddress() != null
                        && getParty().getDefaultEmailAddress().hasValue() && !Strings.isNullOrEmpty(getParty()
                        .getDefaultEmailAddress().getValue())));
    }

    private boolean isEmployee() {
        if (getParty().isPerson()) {
            Person person = (Person) getParty();
            Teacher teacher = person.getTeacher();
            if (teacher == null) {
                Employee employee = person.getEmployee();
                if (employee != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<ParkingRequest> getOrderedParkingRequests() {
        List<ParkingRequest> requests = new ArrayList<>(getParkingRequestsSet());
        requests.sort(Comparator.comparing(ParkingRequest::getCreationDate));
        return requests;
    }

    public ParkingRequest getFirstRequest() {
        List<ParkingRequest> requests = getOrderedParkingRequests();
        if (requests.size() != 0) {
            return requests.iterator().next();
        }
        return null;
    }

    public ParkingRequest getLastRequest() {
        List<ParkingRequest> requests = getOrderedParkingRequests();
        if (requests.size() != 0) {
            return requests.get(requests.size() - 1);
        }
        return null;
    }

    public ParkingRequestFactoryCreator getParkingRequestFactoryCreator() {
        return new ParkingRequestFactoryCreator(this);
    }

    public String getParkingAcceptedRegulationMessage() {
        ResourceBundle bundle = ResourceBundle.getBundle("resources.ParkingResources", I18N.getLocale());
        String name = getParty().getName();
        String number = "";
        if (getParty().isPerson()) {
            Person person = (Person) getParty();
            Employee employee = person.getEmployee();
            if (employee == null) {
                Student student = person.getStudent();
                if (student != null) {
                    number = student.getNumber().toString();
                }
            } else {
                number = employee.getEmployeeNumber().toString();
            }
        }

        return MessageFormat.format(bundle.getString("message.acceptedRegulation"), name, number, Unit.getInstitutionAcronym(),
                Unit.getInstitutionName().getContent());
    }

    public boolean isStudent() {
        if (getParty().isPerson()) {
            Person person = (Person) getParty();
            Teacher teacher = person.getTeacher();
            if (teacher == null) {
                Employee employee = person.getEmployee();
                if (employee == null) {
                    Student student = person.getStudent();
                    if (student != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String getDriverLicenseFileNameToDisplay() {
        NewParkingDocument driverLicenseDocument = getDriverLicenseDocument();
        if (driverLicenseDocument != null) {
            return driverLicenseDocument.getParkingFile().getFilename();
        } else if (getDriverLicenseDeliveryType() != null) {
            ResourceBundle bundle = ResourceBundle.getBundle("resources.ParkingResources", I18N.getLocale());
            return bundle.getString(getDriverLicenseDeliveryType().name());
        }
        return "";
    }

    public String getParkingGroupToDisplay() {
        if (getParkingGroup() != null) {
            return getParkingGroup().getGroupName();
        }
        return null;
    }

    public String getWorkPhone() {
        if (getParty().isPerson()) {
            return getParty().getDefaultPhone() != null ? getParty().getDefaultPhone().getNumber() : "";
        }
        return null;
    }

    public List<String> getSubmitAsRoles() {
        Set<String> roles = new HashSet<>();
        if (getParty().isPerson()) {
            Person person = (Person) getParty();
            Teacher teacher = person.getTeacher();
            if (teacher != null && RoleType.TEACHER.isMember(person.getUser())
                    && !ProfessionalCategory.isMonitor(teacher, ExecutionSemester.readActualExecutionSemester())) {
                roles.add(BundleUtil.getString(Bundle.ENUMERATION, RoleType.TEACHER.name()));
            }
            Employee employee = person.getEmployee();
            if (employee != null && !RoleType.TEACHER.isMember(person.getUser())
                    && new ActiveEmployees().isMember(person.getUser())
                    && employee.getCurrentContractByContractType(AccountabilityTypeEnum.WORKING_CONTRACT) != null) {
                roles.add(BundleUtil.getString(Bundle.ENUMERATION, "EMPLOYEE"));
            }
            Student student = person.getStudent();
            if (student != null && RoleType.STUDENT.isMember(person.getUser())) {
                Collection<Registration> registrations = bestRegistrationsFor(student);
                for (Registration registration : registrations) {
                    StudentCurricularPlan scp = registration.getActiveStudentCurricularPlan();
                    if (scp != null) {
                        roles.add(BundleUtil.getString(Bundle.ENUMERATION, RoleType.STUDENT.name()));
                        break;
                    }
                }
                if (!roles.contains(RoleType.STUDENT)) {
                    for (PhdIndividualProgramProcess phdIndividualProgramProcess : person.getPhdIndividualProgramProcessesSet()) {
                        if (phdIndividualProgramProcess.getActiveState().isPhdActive()) {
                            roles.add(BundleUtil.getString(Bundle.ENUMERATION, RoleType.STUDENT.name()));
                            break;
                        }
                    }
                }
            }
            if (person.getEmployee() != null) {
                PersonContractSituation currentGrantOwnerContractSituation =
                        person.getPersonProfessionalData() != null ? person.getPersonProfessionalData()
                                .getCurrentPersonContractSituationByCategoryType(CategoryType.GRANT_OWNER) : null;
                if (currentGrantOwnerContractSituation != null) {
                    roles.add(BundleUtil.getString(Bundle.ENUMERATION, "GRANT_OWNER"));
                }
            }
            if (!Invitation.getActiveInvitations(person).isEmpty()) {
                roles.add(BundleUtil.getString("resources.ParkingResources", "label.invited"));
                roles.add(BundleUtil.getString("resources.ParkingResources", "label.invitedEmployee"));
                roles.add(BundleUtil.getString("resources.ParkingResources", "label.invitedGrantOwner"));
                roles.add(BundleUtil.getString("resources.ParkingResources", "label.invitedResearcher"));
            }
        }
        if (roles.size() == 0) {
            roles.add(BundleUtil.getString(Bundle.ENUMERATION, RoleType.PERSON.name()));
        }
        return Lists.newArrayList(roles);
    }

    public List<String> getOccupations() {
        List<String> occupations = new ArrayList<>();
        if (getParty().isPerson()) {
            Person person = (Person) getParty();
            Teacher teacher = person.getTeacher();
            if (teacher != null) {
                ExecutionSemester currentExecutionSemester = ExecutionSemester.readActualExecutionSemester();
                String currentWorkingDepartmentName = teacher.getDepartment() != null ? teacher.getDepartment().getName() : null;
                PersonContractSituation currentOrLastTeacherContractSituation =
                        PersonContractSituation.getCurrentOrLastTeacherContractSituation(teacher);
                if (currentOrLastTeacherContractSituation != null) {
                    String employeeType = RoleType.TEACHER.getLocalizedName();
                    if (ProfessionalCategory.isMonitor(teacher, currentExecutionSemester)) {
                        employeeType = "Monitor";
                    }
                    occupations.add(getOccupation(employeeType, teacher.getPerson().getEmployee().getEmployeeNumber().toString(),
                            currentWorkingDepartmentName, currentOrLastTeacherContractSituation.getBeginDate(),
                            currentOrLastTeacherContractSituation.getEndDate()));
                }

                String employeeType = "Docente Autorizado";
                if (teacher.hasTeacherAuthorization()) {
                    occupations.add(getOccupation(employeeType, teacher.getTeacherId(), currentWorkingDepartmentName,
                            currentExecutionSemester.getBeginDateYearMonthDay().toLocalDate(), currentExecutionSemester
                                    .getEndDateYearMonthDay().toLocalDate()));
                }
            }
            PersonContractSituation currentEmployeeContractSituation =
                    person.getEmployee() != null ? person.getEmployee().getCurrentEmployeeContractSituation() : null;
            if (currentEmployeeContractSituation != null) {
                Unit currentUnit = person.getEmployee().getCurrentWorkingPlace();
                String thisOccupation =
                        getOccupation(BundleUtil.getString(Bundle.ENUMERATION, "EMPLOYEE"), person.getEmployee()
                                .getEmployeeNumber().toString(), currentUnit == null ? null : currentUnit.getName(),
                                currentEmployeeContractSituation.getBeginDate(), currentEmployeeContractSituation.getEndDate());
                occupations.add(thisOccupation);
            }
            if (person.getEmployee() != null) {
                PersonContractSituation currentGrantOwnerContractSituation =
                        person.getPersonProfessionalData() != null ? person.getPersonProfessionalData()
                                .getCurrentPersonContractSituationByCategoryType(CategoryType.GRANT_OWNER) : null;
                if (currentGrantOwnerContractSituation != null) {
                    Unit currentUnit = person.getEmployee().getCurrentWorkingPlace();
                    String thisOccupation =
                            getOccupation(BundleUtil.getString(Bundle.ENUMERATION, "GRANT_OWNER"), person.getEmployee()
                                    .getEmployeeNumber().toString(), currentUnit == null ? null : currentUnit.getName(),
                                    currentGrantOwnerContractSituation.getBeginDate(),
                                    currentGrantOwnerContractSituation.getEndDate());
                    occupations.add(thisOccupation);
                }

                PersonContractSituation currentResearcherContractSituation =
                        person.getPersonProfessionalData() != null ? person.getPersonProfessionalData()
                                .getCurrentPersonContractSituationByCategoryType(CategoryType.RESEARCHER) : null;
                if (currentResearcherContractSituation != null) {
                    StringBuilder stringBuilder =
                            new StringBuilder(BundleUtil.getString("resources.ParkingResources", "message.person.identification",
                                    RoleType.RESEARCHER.getLocalizedName(), PartyClassification.getMostSignificantNumber(person).toString()));
                    Unit currentUnit = person.getEmployee() != null ? person.getEmployee().getCurrentWorkingPlace() : null;
                    if (currentUnit != null) {
                        stringBuilder.append(currentUnit.getName()).append("<br/>");
                    }
                    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy/MM/dd");
                    stringBuilder.append("(Data inicio: ").append(fmt.print(currentResearcherContractSituation.getBeginDate()));
                    if (currentResearcherContractSituation.getEndDate() != null) {
                        stringBuilder.append(" - Data fim: ").append(fmt.print(currentResearcherContractSituation.getEndDate()));
                    }
                    occupations.add(stringBuilder.append(")<br/>").toString());
                }
            }

            Student student = person.getStudent();
            if (student != null && RoleType.STUDENT.isMember(person.getUser())) {

                StringBuilder stringBuilder = null;
                for (Registration registration : student.getActiveRegistrations()) {
                    StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
                    if (scp != null) {
                        if (stringBuilder == null) {
                            stringBuilder =
                                    new StringBuilder(BundleUtil.getString("resources.ParkingResources",
                                            "message.person.identification",
                                            RoleType.STUDENT.getLocalizedName(), student.getNumber().toString()));

                        }
                        stringBuilder.append("\n").append(scp.getDegreeCurricularPlan().getName());
                        stringBuilder.append("\n (").append(registration.getCurricularYear()).append("º ano");
                        if (isFirstTimeEnrolledInCurrentYear(registration)) {
                            stringBuilder.append(" - 1ª vez)");
                        } else {
                            stringBuilder.append(")");
                        }
                        stringBuilder.append("<br/>Media: ").append(registration.getRawGrade().getValue());
                        stringBuilder.append("<br/>");
                    }
                }

                if (stringBuilder != null) {
                    occupations.add(stringBuilder.toString());
                }

                for (PhdIndividualProgramProcess phdIndividualProgramProcess : person.getPhdIndividualProgramProcessesSet()) {
                    if (phdIndividualProgramProcess.getActiveState().isPhdActive()) {
                        String thisOccupation =
                                getOccupation(RoleType.STUDENT.getLocalizedName(), student.getNumber().toString(),
                                        "\nPrograma Doutoral: " + phdIndividualProgramProcess.getPhdProgram().getName());
                        occupations.add(thisOccupation);

                    }

                }

            }
            List<Invitation> invitations = Invitation.getActiveInvitations(person);
            if (!invitations.isEmpty()) {
                for (Invitation invitation : invitations) {
                    String thisOccupation =
                            getOccupation("Convidado", "-", invitation.getUnit().getName(), invitation.getBeginDate()
                                    .toLocalDate(), invitation.getEndDate().toLocalDate());
                    occupations.add(thisOccupation);
                }
            }
        }
        return occupations;
    }

    private String getOccupation(String type, String identification, String workingPlace) {
        StringBuilder stringBuilder =
                new StringBuilder(BundleUtil.getString("resources.ParkingResources", "message.person.identification",
                        type, identification));
        if (!Strings.isNullOrEmpty(workingPlace)) {
            stringBuilder.append(workingPlace).append("<br/>");
        }
        return stringBuilder.toString();
    }

    private String getOccupation(String type, String identification, String workingPlace, LocalDate beginDate, LocalDate endDate) {
        StringBuilder stringBuilder = new StringBuilder(getOccupation(type, identification, workingPlace));
        if (beginDate != null) {
            DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy/MM/dd");
            stringBuilder.append("(Data inicio: ").append(fmt.print(beginDate));
            if (endDate != null) {
                stringBuilder.append(" - Data fim: ").append(fmt.print(endDate));
            }
        } else {
            stringBuilder.append("(inactivo");
        }
        stringBuilder.append(")<br/>");
        return stringBuilder.toString();
    }

    public List<String> getDegreesInformation() {
        List<String> result = new ArrayList<>();
        if (getParty().isPerson()) {
            Person person = (Person) getParty();
            Student student = person.getStudent();
            if (student != null && RoleType.STUDENT.isMember(person.getUser())) {
                for (Registration registration : student.getActiveRegistrations()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
                    if (scp != null) {
                        stringBuilder.append(scp.getDegreeCurricularPlan().getName());
                        stringBuilder.append(" ").append(registration.getCurricularYear()).append("º ano");
                        if (isFirstTimeEnrolledInCurrentYear(registration)) {
                            stringBuilder.append(" - 1ª vez");
                        }
                        stringBuilder.append(" - ").append(registration.getRawGrade().getValue());
                        result.add(stringBuilder.toString());
                    }
                }
            }
        }
        return result;
    }

    public boolean hasVehicleContainingPlateNumber(String plateNumber) {
        String plateNumberLowerCase = plateNumber.toLowerCase();
        for (Vehicle vehicle : getVehiclesSet()) {
            if (vehicle.getPlateNumber().toLowerCase().contains(plateNumberLowerCase)) {
                return true;
            }
        }
        return false;
    }

    public void delete() {
        if (canBeDeleted()) {
            setParty(null);
            setParkingGroup(null);
            deleteDriverLicenseDocument();
            for (; getVehiclesSet().size() != 0; getVehiclesSet().iterator().next().delete()) {
                ;
            }
            for (; getParkingRequestsSet().size() != 0; getParkingRequestsSet().iterator().next().delete()) {
                ;
            }
            setRootDomainObject(null);
            deleteDomainObject();
        }
    }

    private void deleteDriverLicenseDocument() {
        NewParkingDocument parkingDocument = getDriverLicenseDocument();
        if (parkingDocument != null) {
            parkingDocument.delete();
        }
    }

    private boolean canBeDeleted() {
        return getVehiclesSet().isEmpty();
    }

    public boolean hasFirstTimeRequest() {
        return getFirstRequest() != null;
    }

    public String getAllNumbers() {
        List<String> result = new ArrayList<>();
        if (getParty().isPerson()) {
            final Person person = (Person) getParty();
            final Employee employee = person.getEmployee();
            if (employee != null
                    && (RoleType.TEACHER.isMember(person.getUser()) || new ActiveEmployees().isMember(person.getUser())
                            || RoleType.RESEARCHER.isMember(person.getUser()) || new ActiveGrantOwner()
                                .isMember(person.getUser()))) {
                result.add(employee.getEmployeeNumber().toString());
            }
            final Student student = person.getStudent();
            if (RoleType.STUDENT.isMember(person.getUser())) {
                result.add(student.getNumber().toString());
            }
        }
        return Joiner.on('\n').join(result);
    }

    public Integer getMostSignificantNumber() {
        if (getParty().isPerson()) {
            final Person person = (Person) getParty();
            final Teacher teacher = person.getTeacher();
            ExecutionSemester actualExecutionSemester = ExecutionSemester.readActualExecutionSemester();
            final boolean isTeacher =
                    teacher != null && !PersonProfessionalData.isTeacherInactive(teacher, actualExecutionSemester);
            final boolean isMonitor = isTeacher && ProfessionalCategory.isMonitor(teacher, actualExecutionSemester);
            final Employee employee = person.getEmployee();

            if (employee != null && employee.getCurrentWorkingContract() != null) {
                if (!RoleType.TEACHER.isMember(person.getUser()) || (teacher != null && isTeacher && !isMonitor)) {
                    return employee.getEmployeeNumber();
                }
            }
            if (employee != null && getPartyClassification().equals(PartyClassification.RESEARCHER)) {
                return employee.getEmployeeNumber();
            }

            final Student student = person.getStudent();

            if (student != null) {
                Collection<Registration> registrations = bestRegistrationsFor(student);
                for (Registration registration : registrations) {
                    StudentCurricularPlan scp = registration.getActiveStudentCurricularPlan();
                    if (scp != null) {
                        return student.getNumber();
                    }
                }
                for (PhdIndividualProgramProcess phdIndividualProgramProcess : person.getPhdIndividualProgramProcessesSet()) {
                    if (phdIndividualProgramProcess.getActiveState().isPhdActive()) {
                        return student.getNumber();
                    }
                }
            }

            if (employee != null && isTeacher && isMonitor) {
                return employee.getEmployeeNumber();
            }
            if (getPhdNumber() != null) {
                return getPhdNumber();
            }
        }

        return 0;
    }

    public static Collection<ParkingParty> getAll() {
        return Bennu.getInstance().getParkingPartiesSet();
    }

    public void edit(ParkingPartyBean parkingPartyBean) {
        if (!parkingPartyBean.getCardAlwaysValid()
                && parkingPartyBean.getCardStartDate().isAfter(parkingPartyBean.getCardEndDate())) {
            throw new DomainException("error.parkingParty.invalidPeriod");
        }
        if (getCardNumber() != null
                && (changedDates(getCardStartDate(), parkingPartyBean.getCardStartDate(), parkingPartyBean.getCardAlwaysValid())
                        || changedDates(getCardEndDate(), parkingPartyBean.getCardEndDate(),
                                parkingPartyBean.getCardAlwaysValid())
                        || changedObject(getCardNumber(), parkingPartyBean.getCardNumber())
                        || changedObject(getParkingGroup(), parkingPartyBean.getParkingGroup()) || changedObject(getPhdNumber(),
                            parkingPartyBean.getPhdNumber()))) {
            new ParkingPartyHistory(this, false);
        }
        setCardNumber(parkingPartyBean.getCardNumber());
        setCardStartDate(parkingPartyBean.getCardStartDate());
        setCardEndDate(parkingPartyBean.getCardEndDate());
        setPhdNumber(parkingPartyBean.getPhdNumber());
        setParkingGroup(parkingPartyBean.getParkingGroup());
        for (VehicleBean vehicleBean : parkingPartyBean.getVehicles()) {
            if (vehicleBean.getVehicle() != null) {
                if (vehicleBean.getDeleteVehicle()) {
                    vehicleBean.getVehicle().delete();
                } else {
                    vehicleBean.getVehicle().edit(vehicleBean);
                }
            } else {
                if (!vehicleBean.getDeleteVehicle()) {
                    new Vehicle(vehicleBean);
                }
            }
        }
        setNotes(parkingPartyBean.getNotes());
    }

    private boolean changedDates(DateTime oldDate, DateTime newDate, Boolean cardAlwaysValid) {
        return cardAlwaysValid ? (oldDate != null) : ((oldDate == null || (!oldDate.equals(newDate))) || oldDate.equals(newDate));
    }

    private boolean changedObject(Object oldObject, Object newObject) {
        return (oldObject != null || newObject != null) && (oldObject == null || newObject == null || (!oldObject.equals(newObject)));
    }

    public void edit(ParkingRequest parkingRequest) {
        setDriverLicenseDeliveryType(parkingRequest.getDriverLicenseDeliveryType());
        setDriverLicenseDocument(parkingRequest.getDriverLicenseDocument());
        for (Vehicle vehicle : parkingRequest.getVehiclesSet()) {
            Vehicle partyVehicle = geVehicleByPlateNumber(vehicle.getPlateNumber());
            if (partyVehicle != null) {
                vehicle.deleteUnnecessaryDocuments();
                partyVehicle.edit(vehicle);
            } else {
                vehicle.deleteUnnecessaryDocuments();
                addVehicles(new Vehicle(vehicle));
            }
        }
        setRequestedAs(parkingRequest.getRequestedAs());
    }

    private Vehicle geVehicleByPlateNumber(String plateNumber) {
        for (Vehicle vehicle : getVehiclesSet()) {
            if (vehicle.getPlateNumber().equalsIgnoreCase(plateNumber)) {
                return vehicle;
            }
        }
        return null;
    }

    public boolean isActiveInHisGroup() {
        if (getParkingGroup() == null) {
            return Boolean.FALSE;
        }
        if (getParty().isPerson()) {
            Person person = (Person) getParty();
            if (getParkingGroup().getGroupName().equalsIgnoreCase("Docentes")) {
                return person.getTeacher() != null && person.getTeacher().getDepartment() != null;
            }
            if (getParkingGroup().getGroupName().equalsIgnoreCase("Não Docentes")) {
                return person.getEmployee() != null && person.getEmployee().getCurrentWorkingPlace() != null;
            }
            PartyClassification classification = PartyClassification.getPartyClassification(person);
            if (getParkingGroup().getGroupName().equalsIgnoreCase("Especiais")) {
                return classification != PartyClassification.PERSON;
            }
            if (getParkingGroup().getGroupName().equalsIgnoreCase("2º ciclo")) {
                if (person.getStudent() != null) {
                    return canRequestUnlimitedCard(person.getStudent());
                } else {
                    return Boolean.FALSE;
                }
            }
            if (getParkingGroup().getGroupName().equalsIgnoreCase("Bolseiros")) {
                if (person.getEmployee() != null) {
                    PersonContractSituation currentGrantOwnerContractSituation =
                            person.getPersonProfessionalData() != null ? person.getPersonProfessionalData()
                                    .getCurrentPersonContractSituationByCategoryType(CategoryType.GRANT_OWNER) : null;
                    return currentGrantOwnerContractSituation != null;
                }
                return false;
            }
            if (getParkingGroup().getGroupName().equalsIgnoreCase("3º ciclo")) {
                if (person.getStudent() != null) {
                    Registration registration =
                            getRegistrationByDegreeType(person.getStudent(), DegreeType::isAdvancedSpecializationDiploma);
                    return registration != null && registration.isActive();
                } else {
                    return Boolean.FALSE;
                }
            }
            if (getParkingGroup().getGroupName().equalsIgnoreCase("Limitados")) {
                return classification != PartyClassification.PERSON && classification != PartyClassification.RESEARCHER;
            }
        }
        return Boolean.FALSE;
    }

    public boolean getCanRequestUnlimitedCardAndIsInAnyRequestPeriod() {
        ParkingRequestPeriod current = ParkingRequestPeriod.getCurrentRequestPeriod();
        return current != null && canRequestUnlimitedCard(current);
    }

    public boolean canRequestUnlimitedCard() {
        return canRequestUnlimitedCard(ParkingRequestPeriod.getCurrentRequestPeriod());
    }

    public boolean canRequestUnlimitedCard(ParkingRequestPeriod parkingRequestPeriod) {
        if (!alreadyRequestParkingRequestTypeInPeriod(ParkingRequestType.RENEW, parkingRequestPeriod)) {
            return hasRolesToRequestUnlimitedCard();
        }
        return Boolean.FALSE;
    }

    public boolean hasRolesToRequestUnlimitedCard() {
        return !Strings.isNullOrEmpty(getRoleToRequestUnlimitedCard());
    }

    public String getRoleToRequestUnlimitedCard() {
        List<String> roles = getSubmitAsRoles();
        if (roles.contains(BundleUtil.getString(Bundle.ENUMERATION, "GRANT_OWNER"))) {
            return BundleUtil.getString(Bundle.ENUMERATION, "GRANT_OWNER");
        } else if (roles.contains(BundleUtil.getString(Bundle.ENUMERATION, RoleType.STUDENT.name()))
                && canRequestUnlimitedCard(((Person) getParty()).getStudent())) {
            return BundleUtil.getString(Bundle.ENUMERATION, RoleType.STUDENT.name());
        } else if (roles.contains(BundleUtil.getString("resources.ParkingResources", "label.invited"))) {
            return BundleUtil.getString("resources.ParkingResources", "label.invited");
        }
        return null;
    }

    public boolean alreadyRequestParkingRequestTypeInPeriod(ParkingRequestType parkingRequestType,
            ParkingRequestPeriod parkingRequestPeriod) {
        List<ParkingRequest> requests = getOrderedParkingRequests();
        for (ParkingRequest parkingRequest : requests) {
            if (parkingRequestPeriod.getRequestPeriodInterval().contains(parkingRequest.getCreationDate())
                    && parkingRequest.getParkingRequestType().equals(parkingRequestType)) {
                return true;
            }
        }
        return false;
    }

    public boolean canRequestUnlimitedCard(Student student) {
        if (student != null && RoleType.STUDENT.isMember(student.getPerson().getUser())) {
            for (PhdIndividualProgramProcess phdIndividualProgramProcess : student.getPerson()
                    .getPhdIndividualProgramProcessesSet()) {
                if (phdIndividualProgramProcess.getActiveState().isPhdActive()) {
                    return true;
                }
            }
        }
        // List<DegreeType> degreeTypes = new ArrayList<DegreeType>();
        // degreeTypes.add(DegreeType.DEGREE);
        // degreeTypes.add(DegreeType.BOLONHA_ADVANCED_FORMATION_DIPLOMA);
        // degreeTypes.add(DegreeType.BOLONHA_MASTER_DEGREE);
        // degreeTypes.add(DegreeType.BOLONHA_INTEGRATED_MASTER_DEGREE);
        //
        // for (DegreeType degreeType : degreeTypes) {
        // Registration registration = getRegistrationByDegreeType(student,
        // degreeType);
        // if (registration != null && registration.isInFinalDegreeYear()) {
        // return
        // degreeType.equals(DegreeType.BOLONHA_ADVANCED_FORMATION_DIPLOMA) ?
        // Boolean.TRUE
        // : isFirstTimeEnrolledInCurrentYear(registration);
        // }
        // }
        return false;
        // DEGREE=Licenciatura (5 anos) - 5� ano
        // MASTER_DEGREE=Mestrado = 2ciclo - n�o tem
        // BOLONHA_DEGREE=Licenciatura Bolonha - n�o podem
        // BOLONHA_MASTER_DEGREE=Mestrado Bolonha - s� no 5+ ano 1� vez
        // BOLONHA_INTEGRATED_MASTER_DEGREE=Mestrado Integrado (ultimo ano 1�
        // vez)
        // BOLONHA_ADVANCED_FORMATION_DIPLOMA =Diploma Forma��o Avan�ada =
        // cota
        // pos grad (sempre)

        // BOLONHA_ADVANCED_SPECIALIZATION_DIPLOMA=Diploma de Estudos
        // Avan�ados
        // -n�o est�o todos no f�nix por isso t�m de se candidatar em
        // papel
        // BOLONHA_SPECIALIZATION_DEGREE=Curso de Especializa��o - n�o
        // est�o no
        // f�nix -este tipo n�o � usado

    }

    public boolean isFirstTimeEnrolledInCurrentYear(Registration registration) {
        ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        return registration.getCurricularYear(executionYear.getPreviousExecutionYear()) != registration
                .getCurricularYear(executionYear);
    }

    private Registration getRegistrationByDegreeType(Student student, Predicate<DegreeType> degreeType) {
        for (Registration registration : student.getRegistrationsMatchingDegreeType(degreeType)) {
            if (registration.isActive()) {
                StudentCurricularPlan scp = registration.getActiveStudentCurricularPlan();
                if (scp != null) {
                    return registration;
                }
            }
        }
        return null;
    }

    public PartyClassification getPartyClassification() {
        return PartyClassification.getPartyClassification(getParty());
    }

    public DateTime getCardEndDateToCompare() {
        if (getCardEndDate() == null) {
            return new DateTime(9999, 9, 9, 9, 9, 9, 9);
        } else {
            return getCardEndDate();
        }
    }

    public DateTime getCardStartDateToCompare() {
        if (getCardStartDate() == null) {
            return new DateTime(9999, 9, 9, 9, 9, 9, 9);
        } else {
            return getCardStartDate();
        }
    }

    public void renewParkingCard(DateTime newBeginDate, DateTime newEndDate, ParkingGroup newParkingGroup) {
        new ParkingPartyHistory(this, false);
        if (newBeginDate != null) {
            setCardStartDate(newBeginDate);
        }
        setCardEndDate(newEndDate);
        if (newParkingGroup != null) {
            setParkingGroup(newParkingGroup);
        }
    }

    public Vehicle getFirstVehicle() {
        List<Vehicle> vehicles = new ArrayList<>(getVehiclesSet());
        vehicles.sort(Comparator.comparing(Vehicle::getPlateNumber));
        return vehicles.size() > 0 ? vehicles.iterator().next() : null;
    }

    public Vehicle getSecondVehicle() {
        List<Vehicle> vehicles = new ArrayList<>(getVehiclesSet());
        vehicles.sort(Comparator.comparing(Vehicle::getPlateNumber));
        return vehicles.size() > 1 ? vehicles.get(1) : null;
    }

    public static Collection<Registration> bestRegistrationsFor(Student student) {
        DegreeType typeToChoose = mostSignificantDegreeType(student);
        return student.getActiveRegistrations().stream().filter(reg -> reg.getDegreeType().equals(typeToChoose))
                .collect(Collectors.toList());
    }

    private static final List<Predicate<DegreeType>> types;

    static {
        types = new ArrayList<>();
        types.add(DegreeType::isSpecializationDegree);
        types.add(DegreeType::isAdvancedFormationDiploma);
        types.add(DegreeType::isAdvancedSpecializationDiploma);
        types.add(DegreeType::isBolonhaMasterDegree);
        types.add(DegreeType::isIntegratedMasterDegree);
        types.add(DegreeType::isBolonhaDegree);
    }

    public static DegreeType mostSignificantDegreeType(Student student) {
        for (Predicate<DegreeType> type : types) {
            for (Registration reg : student.getActiveRegistrations()) {
                if (type.test(reg.getDegreeType())) {
                    return reg.getDegreeType();
                }
            }
        }
        return null;
    }
}
