package pt.ist.fenixedu.integration.ui.spring.santander.controller;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.idcards.domain.SantanderEntry;
import org.fenixedu.idcards.service.SantanderIdCardsService;
import org.fenixedu.santandersdk.dto.RegisterAction;
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
    private SantanderIdCardsService santanderRequestCardService;

    @Autowired
    public IdentificationCardController(IdentificationCardService identificationCardService, SantanderIdCardsService santanderRequestCardService) {
        this.identificationCardService = identificationCardService;
        this.santanderRequestCardService = santanderRequestCardService;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String showRequests(Model model) {
        Person person = AccessControl.getPerson();

        model.addAttribute("availableActions", santanderRequestCardService.getPersonAvailableActions(person.getUser()));

        return "fenixedu-ist-integration/identificationCards/showCardInformation";
    }

    @RequestMapping(value = "/request-card", method = RequestMethod.POST)
    public String requestCard(Model model, String action) {
        Person person = AccessControl.getPerson();
        try {

            RegisterAction registerAction = RegisterAction.valueOf(action);
            if (!santanderRequestCardService.getPersonAvailableActions(person.getUser()).contains(registerAction)) {
                // TODO: Return with error? check?
                model.addAttribute("error", String.format("Action %s not available", action));
                return "redirect:/identification-card";
            }

            identificationCardService.createRegister(person, registerAction);
            //TODO ADD error for invalid person (if person no longer as a valid status)
        } catch (IllegalArgumentException e) {
            System.out.println("Wrong action");
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

        RegisterAction registerAction = RegisterAction.valueOf(action);
        identificationCardService.createRegister(person, registerAction);
        return "redirect:/identification-card";
    }
}
