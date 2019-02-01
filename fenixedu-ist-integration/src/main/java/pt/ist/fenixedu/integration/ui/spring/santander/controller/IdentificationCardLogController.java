package pt.ist.fenixedu.integration.ui.spring.santander.controller;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import pt.ist.fenixedu.integration.ui.spring.santander.bean.UsernameBean;

@SpringApplication(group = "logged", path = "identification-card-log", title = "label.identification.card.log")
@SpringFunctionality(app = IdentificationCardLogController.class, title = "label.identification.card.log")
@Controller
@RequestMapping("/identification-card-log")
public class IdentificationCardLogController {

    @RequestMapping(value = "", method = RequestMethod.GET)
    public String showRequests(Model model) {

        model.addAttribute("usernameBean", new UsernameBean());

        return "fenixedu-ist-integration/identificationCards/searchUser";
    }

    @RequestMapping(value = "/search-person", method = RequestMethod.POST)
    public String searchUser(@ModelAttribute UsernameBean bean, Model model) {

        User user = User.findByUsername(bean.getUsername());

        if (user == null) {
            return "redirect:/identification-card-log";
        }

        return "redirect:/identification-card-log/person/" + user.getPerson().getExternalId();
    }

    @RequestMapping(value = "/person/{person}", method = RequestMethod.GET)
    public String searchUser(@PathVariable Person person, Model model) {

        //TODO

        return "fenixedu-ist-integration/identificationCards/showCardInformation";
    }

}
