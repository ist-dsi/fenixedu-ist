package pt.ist.fenixedu.integration.ui.spring.santander.controller;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import pt.ist.fenixedu.integration.ui.spring.santander.service.IdentificationCardService;

@SpringApplication(group = "logged", path = "identification-card", title = "label.identification.card")
@SpringFunctionality(app = IdentificationCardController.class, title = "label.identification.card")
@Controller
@RequestMapping("/identification-card")
public class IdentificationCardController {

    private static final String ACTION_NEW = "NOVO";

    private IdentificationCardService identificationCardService;

    @Autowired
    public IdentificationCardController(IdentificationCardService identificationCardService) {
        this.identificationCardService = identificationCardService;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String showRequests(Model model) {
        Person person = AccessControl.getPerson();

        model.addAttribute("requests", person.getSantanderEntriesNewSet());

        return "fenixedu-ist-integration/identificationCards/showCardInformation";
    }

    @RequestMapping(value = "/request-card", method = RequestMethod.POST)
    public String requestCard() {
        Person person = AccessControl.getPerson();

        identificationCardService.createRegister(person, ExecutionYear.readCurrentExecutionYear(), ACTION_NEW);

        return "redirect:/identification-card";
    }
}
