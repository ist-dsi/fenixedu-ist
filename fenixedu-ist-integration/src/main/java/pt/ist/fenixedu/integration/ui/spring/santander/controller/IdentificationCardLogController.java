package pt.ist.fenixedu.integration.ui.spring.santander.controller;

import java.util.List;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.idcards.domain.SantanderEntryNew;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import pt.ist.fenixedu.integration.ui.spring.santander.bean.SantanderEntrySearchBean;
import pt.ist.fenixedu.integration.ui.spring.santander.service.IdentificationCardService;

@SpringApplication(group = "logged", path = "identification-card-log", title = "label.identification.card.log")
@SpringFunctionality(app = IdentificationCardLogController.class, title = "label.identification.card.log")
@Controller
@RequestMapping("/identification-card-log")
public class IdentificationCardLogController {

    private IdentificationCardService identificationCardService;

    @Autowired
    public IdentificationCardLogController(IdentificationCardService identificationCardService) {
        this.identificationCardService = identificationCardService;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String showRequests(Model model) {

        model.addAttribute("santanderEntrySearchBean", new SantanderEntrySearchBean());

        return "fenixedu-ist-integration/identificationCards/searchUser";
    }

    @RequestMapping(value = "/search-entries", method = RequestMethod.POST)
    public String searchUser(@ModelAttribute SantanderEntrySearchBean bean, Model model) {
        User user = User.findByUsername(bean.getUsername());

        if (user == null) {
            return "redirect:/identification-card-log";
        }

        List<SantanderEntryNew> requests = bean.search();
        
        model.addAttribute("requests", requests);

        return "fenixedu-ist-integration/identificationCards/showCardLog";
    }
}
