package pt.ist.fenixedu.teacher.evaluation.ui.spring;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.CreditsPoolBean;

@SpringFunctionality(app = CreditsManagementApplication.class, title = "title.creditsManagement.departmentPool",
        accessGroup = "#scientificCouncil")
@RequestMapping("/creditsPool")
public class CreditsManagementController {

    @Autowired
    CreditsManagementService service;

    @RequestMapping(method = GET)
    public String home(Model model, @ModelAttribute CreditsPoolBean creditsPoolBean) {
        if (creditsPoolBean == null) {
            creditsPoolBean = new CreditsPoolBean();
        } else {
            creditsPoolBean.updateValues();
        }
        model.addAttribute("creditsPoolBean", creditsPoolBean);
        model.addAttribute("executionYears", Bennu.getInstance().getExecutionYearsSet().stream().distinct()
                .sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).limit(4).collect(Collectors.toList()));
        return "/creditsManagement/creditsPool/ ";
    }

    @RequestMapping(method = POST, value = "editCreditsPool")
    public String editCreditsPool(Model model, @ModelAttribute CreditsPoolBean creditsPoolBean) {
        if (creditsPoolBean != null) {
            service.editCreditsPool(creditsPoolBean);
        }
        return home(model, creditsPoolBean);
    }
}