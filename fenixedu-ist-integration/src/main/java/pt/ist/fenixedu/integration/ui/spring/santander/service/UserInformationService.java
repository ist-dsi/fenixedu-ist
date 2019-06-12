package pt.ist.fenixedu.integration.ui.spring.santander.service;

import java.awt.image.BufferedImage;
import java.util.List;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.photograph.Picture;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.idcards.service.IUserInfoService;
import org.springframework.stereotype.Service;

import com.google.common.io.BaseEncoding;

import pt.ist.fenixedu.integration.dto.PersonInformationDTO;

@Service
public class UserInformationService implements IUserInfoService {

    @Override
    public List<String> getUserRoles(User user) {
        PersonInformationDTO personInformationDTO = new PersonInformationDTO(user.getPerson());
        return personInformationDTO.getRoles();
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
        return personInformationDTO.getCampus();
    }
}
