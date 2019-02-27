package pt.ist.fenixedu.integration.ui.spring.santander.controller;

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
import pt.sibscartoes.portal.wcf.register.info.dto.RegisterData;

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

        /*List<String> status = new ArrayList<>();
        RegisterData registerData = SantanderRequestCardService.getRegister(person);
        status.add(registerData.getStatus().getValue());
        status.add(registerData.getStatusDate().getValue());
        status.add(registerData.getStatusDescription().getValue());
        
        String formatedStatus = status.get(1) + " : " + status.get(0) + " - " + status.get(2);*/

        /* model.addAttribute("currentState", formatedStatus);*/

        RegisterData status = SantanderRequestCardService.getRegister(person);
        String currentStatus = status.getStatus().getValue() + " - " + status.getStatusDescription().getValue();
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
        } catch (IllegalArgumentException e) {
            //TODO Invalid action error
        } catch (SantanderCardMissingDataException e) {
            //TODO Missing photo / name error
        } catch (Throwable t) {
            //TODO Generic error
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
