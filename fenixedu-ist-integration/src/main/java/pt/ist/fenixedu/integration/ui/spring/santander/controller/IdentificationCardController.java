package pt.ist.fenixedu.integration.ui.spring.santander.controller;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.idcards.domain.RegisterAction;
import org.fenixedu.idcards.domain.SantanderEntryNew;
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

    private IdentificationCardService identificationCardService;

    @Autowired
    public IdentificationCardController(IdentificationCardService identificationCardService) {
        this.identificationCardService = identificationCardService;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String showRequests(Model model) {
        Person person = AccessControl.getPerson();

        SantanderEntryNew entryNew = SantanderRequestCardService.updateState(person);
        String currentStatus = entryNew == null ? "No Request" : entryNew.getState().getName();

        model.addAttribute("currentState", currentStatus);
        model.addAttribute("availableActions", SantanderRequestCardService.getPersonAvailableActions(person));

        return "fenixedu-ist-integration/identificationCards/showCardInformation";
    }

    @RequestMapping(value = "/request-card", method = RequestMethod.POST)
    public String requestCard(Model model, String action) {
        Person person = AccessControl.getPerson();
        try {

            RegisterAction registerAction = RegisterAction.valueOf(action);
            if (!SantanderRequestCardService.getPersonAvailableActions(person).contains(registerAction)) {
                // TODO: Return with error? check?
                model.addAttribute("error", String.format("Action %s not available", action));
                return "redirect:/identification-card";
            }

            identificationCardService.createRegister(person, ExecutionYear.readCurrentExecutionYear(), registerAction);
            //TODO ADD error for invalid person (if person no longer as a valid satus)
        } catch (IllegalArgumentException e) {
            System.out.println("Wrong action");
            e.printStackTrace();
        } catch (SantanderCardMissingDataException e) {
            System.out.println("Missing photo");
            e.printStackTrace();
        } catch (Throwable t) {
            System.out.println("Generic error");
            t.printStackTrace();
        }

        return "redirect:/identification-card";
    }

    @RequestMapping(value = "/request-card-test", method = RequestMethod.POST)
    public String requestCardTest(String action) {
        Person person = AccessControl.getPerson();

        try {
            RegisterAction registerAction = RegisterAction.valueOf(action);
            identificationCardService.createRegister(person, ExecutionYear.readCurrentExecutionYear(), registerAction);
        } catch (SantanderCardMissingDataException e) {
            //TODO Missing photo / name error
        }

        return "redirect:/identification-card";
    }
}
