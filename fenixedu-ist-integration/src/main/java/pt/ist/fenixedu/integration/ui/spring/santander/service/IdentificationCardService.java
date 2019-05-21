package pt.ist.fenixedu.integration.ui.spring.santander.service;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.idcards.service.SantanderIdCardsService;
import org.fenixedu.santandersdk.dto.RegisterAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class IdentificationCardService {

    private SantanderIdCardsService santanderRequestCardService;

    @Autowired
    public IdentificationCardService(SantanderIdCardsService santanderRequestCardService) {
        this.santanderRequestCardService = santanderRequestCardService;
    }

    public void createRegister(Person person, RegisterAction action) {
        santanderRequestCardService.createRegister(person.getUser(), action);
    }

    //TODO encapsulate all needed SantanderCardService functions
}
