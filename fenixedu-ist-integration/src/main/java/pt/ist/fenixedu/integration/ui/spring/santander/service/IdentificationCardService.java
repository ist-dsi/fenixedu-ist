package pt.ist.fenixedu.integration.ui.spring.santander.service;

import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.idcards.service.SantanderRequestCardService;
import org.fenixedu.santandersdk.dto.RegisterAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class IdentificationCardService {

    private SantanderRequestCardService santanderRequestCardService;

    @Autowired
    public IdentificationCardService(SantanderRequestCardService santanderRequestCardService) {
        this.santanderRequestCardService = santanderRequestCardService;
    }

    public void createRegister(Person person, RegisterAction action) {
        santanderRequestCardService.createRegister(person.getUser(), action);
    }

    //TODO encapsulate all needed SantanderCardService functions
}
