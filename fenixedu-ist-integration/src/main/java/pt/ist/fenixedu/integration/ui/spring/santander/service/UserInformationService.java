package pt.ist.fenixedu.integration.ui.spring.santander.service;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.idcards.service.IUserInfoService;
import org.springframework.stereotype.Service;
import pt.ist.fenixedu.integration.dto.PersonInformationDTO;

import java.util.List;

@Service
public class UserInformationService implements IUserInfoService {

    @Override
    public List<String> getUserRoles(User user) {
        PersonInformationDTO personInformationDTO = new PersonInformationDTO(user.getPerson());
        return personInformationDTO.getRoles();
    }

    @Override
    public String getUserPhoto(User user) {
        PersonInformationDTO personInformationDTO = new PersonInformationDTO(user.getPerson());
        return personInformationDTO.getPhoto();
    }

    @Override
    public String getUserDepartmentAcronym(User user) {
        PersonInformationDTO personInformationDTO = new PersonInformationDTO(user.getPerson());
        return personInformationDTO.getTeacherDepartment();
    }

    @Override
    public String getCampus(User user) {
        PersonInformationDTO personInformationDTO = new PersonInformationDTO(user.getPerson());
        return personInformationDTO.getCampus();
    }
}
