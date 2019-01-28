package pt.ist.fenixedu.integration.ui.spring.santander.service;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.idcards.service.SantanderRequestCardService;
import org.springframework.stereotype.Service;
import pt.ist.fenixedu.integration.domain.santander.RequestCardUtils;

import java.util.List;

@Service
public class IdentificationCardService {

    public List<String> createRegister(Person person, ExecutionYear executionYear, String action) {
        String tuiEntry = RequestCardUtils.generateLine(person, executionYear, action);
        return SantanderRequestCardService.createRegister(tuiEntry, person);
    }
}
