/**
 * Copyright © 2014 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Spaces.
 *
 * FenixEdu Spaces is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Spaces is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Spaces.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.ui;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import pt.ist.fenixedu.delegates.ui.services.DelegateService;

@SpringApplication(group = "anyone", path = "delegates", title = "title.delegates.management", hint = "delegate-manager")
@SpringFunctionality(app = DelegatesController.class, title = "title.delegates.management")
@RequestMapping("/delegates")
public class DelegatesController {

    @Autowired
    DelegateService delegateService;

    @RequestMapping(method = RequestMethod.GET)
    public String home(Model model) {
        DelegateSearchBean delegateSearchBean = new DelegateSearchBean();
        Student student = Authenticate.getUser().getPerson().getStudent();
        Degree degree = null;
        ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        if (student.getLastActiveRegistration() != null) {
            degree = student.getLastActiveRegistration().getDegree();
        }
        if (degree == null && student.getLastRegistration() != null) {
            degree = student.getLastActiveRegistration().getDegree();
        }
        if (degree == null) {
            // error student has no degree??
        }
        delegateSearchBean.setDegree(degree);
        delegateSearchBean.setExecutionYear(executionYear);
        delegateSearchBean.setDegreeType(null);
        return search(delegateSearchBean, model);
    }

    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public String search(@ModelAttribute DelegateSearchBean searchBean, Model model) {
        searchBean = delegateService.generateNewBean(searchBean);
        model.addAttribute("searchBean", searchBean);
        model.addAttribute("delegates", delegateService.searchDelegates(searchBean));
        return search(model);
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String search(Model model) {
        model.addAttribute("action", "/delegates/search");
        return "delegates/search";
    }
}
