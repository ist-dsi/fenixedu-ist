package pt.ist.fenixedu.integration.ui.spring.santander.service;

import com.google.common.io.BaseEncoding;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.ActivePhdProcessesGroup;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.photograph.Picture;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.idcards.service.IUserInfoService;
import org.fenixedu.spaces.domain.Space;
import org.springframework.stereotype.Service;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveGrantOwner;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveResearchers;
import pt.ist.fenixedu.contracts.domain.accessControl.CampusEmployeeGroup;
import pt.ist.fenixedu.contracts.domain.accessControl.CampusGrantOwnerGroup;
import pt.ist.fenixedu.contracts.domain.accessControl.CampusResearcherGroup;
import pt.ist.fenixedu.integration.dto.PersonInformationDTO;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@Service
public class UserInformationService implements IUserInfoService {

    private final static BiFunction<Space, User, Boolean> employeeGroupFunction =
            (space, employeeUser) -> CampusEmployeeGroup.get(space).isMember(employeeUser);

    private final static BiFunction<Space, User, Boolean> grantOwnerGroupFunction =
            (space, grantOwnerUser) -> CampusGrantOwnerGroup.get(space).isMember(grantOwnerUser);

    private final static BiFunction<Space, User, Boolean> researcherGroupFunction =
            (space, researcherUser) -> CampusResearcherGroup.get(space).isMember(researcherUser);

    @Override
    public List<String> getUserRoles(User user) {
        List<String> roles = new ArrayList<>();

        if (treatAsStudent(user, ExecutionYear.readCurrentExecutionYear())) {
            roles.add("STUDENT");
        }
        if (treatAsTeacher(user)) {
            roles.add("TEACHER");
        }
        if (treatAsResearcher(user)) {
            roles.add("RESEARCHER");
        }
        if (treatAsEmployee(user)) {
            roles.add("EMPLOYEE");
        }
        if (treatAsGrantOwner(user)) {
            roles.add("GRANT_OWNER");
        }

        return roles;
    }

    private boolean treatAsTeacher(User user) {
        Person person = user.getPerson();
        if (person.getTeacher() != null) {
            return person.getTeacher().isActiveContractedTeacher();
        }
        return false;
    }

    private boolean treatAsResearcher(User user) {
        return new ActiveResearchers().isMember(user) && new ActiveResearchers().isFromInstitution(user, "IST");
    }

    private boolean treatAsEmployee(User user) {
        return new ActiveEmployees().isMember(user) && new ActiveEmployees().isFromInstitution(user, "IST");
    }

    private boolean treatAsGrantOwner(User user) {
        return new ActiveGrantOwner().isMember(user) && new ActiveGrantOwner().isFromInstitution(user, "IST");
    }

    private boolean treatAsStudent(User user, ExecutionYear executionYear) {
        Person person = user.getPerson();
        if (person.getStudent() != null) {
            final List<Registration> activeRegistrations = person.getStudent().getActiveRegistrations();
            for (final Registration registration : activeRegistrations) {
                if (registration.isBolonha()) {
                    return true;
                }
            }
            final Event event = person.getInsuranceEventFor(executionYear);
            return event != null && event.isClosed() && new ActivePhdProcessesGroup().isMember(person.getUser());
        }
        return false;
    }

    @Override
    public BufferedImage getUserPhoto(User user) {
        //Might not work if image is not in JPG format
        PersonInformationDTO personInformationDTO = new PersonInformationDTO(user.getPerson());
        if (personInformationDTO.getPhoto() == null) {
            return null;
        }
        byte[] photo = BaseEncoding.base64().decode(personInformationDTO.getPhoto());
        return Picture.readImage(photo);
    }

    @Override
    public String getUserDepartmentAcronym(User user) {
        Person person = user.getPerson();
        if (person.getTeacher() != null && person.getTeacher().getDepartment() != null) {
            return person.getTeacher().getDepartment().getAcronym();
        }
        return null;
    }

    @Override
    public String getCampus(User user) {
        PersonInformationDTO personInformationDTO = new PersonInformationDTO(user.getPerson());
        String campus = personInformationDTO.getCampus();

        if (campus == null) {
            final BiFunction<Space, User, Boolean> groupFunction =
                    new ActiveEmployees().isMember(user) && new ActiveEmployees().isFromInstitution(user, "IST") ? employeeGroupFunction :
                    new ActiveGrantOwner().isMember(user) && new ActiveGrantOwner().isFromInstitution(user, "IST") ? grantOwnerGroupFunction :
                    new ActiveResearchers().isMember(user) && new ActiveResearchers().isFromInstitution(user, "IST") ? researcherGroupFunction : null;

            campus = groupFunction == null ? null : Space.getTopLevelSpaces().stream()
                    .filter(space -> groupFunction.apply(space, user)).findAny().map(Space::getName).orElse(null);
        }
        return campus;
    }
}
