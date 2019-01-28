package pt.ist.fenixedu.integration.ui.spring.santander.controller;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.idcards.service.SantanderRequestCardService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import pt.ist.fenixedu.integration.domain.santander.RequestCardUtils;

import java.util.List;

@SpringApplication(group = "logged", path = "identification-card", title = "label.identification.card")
@SpringFunctionality(app = IdentificationCardController.class, title = "label.identification.card")
@Controller
@RequestMapping("/identification-card")
public class IdentificationCardController {

    private static final String ACTION_NEW = "NOVO";

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String showRequests() {
        return "fenixedu-ist-integration/identificationCards/showCardInformation";
    }

    @RequestMapping(value = "/request-card", method = RequestMethod.POST)
    public String requestCard() {
        Person person = AccessControl.getPerson();

        String tuiEntry = RequestCardUtils.generateLine(person, ExecutionYear.readCurrentExecutionYear(), ACTION_NEW);
        List<String> result = SantanderRequestCardService.createRegister(tuiEntry, person);

        return "fenixedu-ist-integration/identificationCards/showCardInformation";
    }
}
