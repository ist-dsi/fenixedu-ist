package pt.ist.fenixedu.integration.ui.spring.santander.controller;

import java.util.List;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.idcards.domain.RegisterAction;
import org.fenixedu.idcards.service.SantanderCardMissingDataException;
import org.fenixedu.idcards.service.SantanderRequestCardService;
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

        List<String> status = SantanderRequestCardService.getRegister(person);

        String formatedStatus = status.get(1) + " : " + status.get(0) + " - " + status.get(2);

        model.addAttribute("currentState", formatedStatus);
        model.addAttribute("availableActions", SantanderRequestCardService.getPersonAvailableActions(person));

        return "fenixedu-ist-integration/identificationCards/showCardInformation";
    }

    @RequestMapping(value = "/request-card", method = RequestMethod.POST)
    public String requestCard(Model model, String action) {
        Person person = AccessControl.getPerson();

        if (!SantanderRequestCardService.getPersonAvailableActions(person).contains(RegisterAction.valueOf(action))) {
            // TODO: Return with error? check?
            model.addAttribute("error", String.format("Action %s not available", action));
            return "redirect:/identification-card";
        }

        try {
            identificationCardService.createRegister(person, ExecutionYear.readCurrentExecutionYear(), action);
        } catch (SantanderCardMissingDataException e) {
            //TODO Missing photo / name error
        }

        return "redirect:/identification-card";
    }

    @RequestMapping(value = "/request-card-test", method = RequestMethod.POST)
    public String requestCardTest(String action) {
        Person person = AccessControl.getPerson();

        try {
            identificationCardService.createRegister(person, ExecutionYear.readCurrentExecutionYear(), action);
        } catch (SantanderCardMissingDataException e) {
            //TODO Missing photo / name error
        }

        return "redirect:/identification-card";
    }
}
