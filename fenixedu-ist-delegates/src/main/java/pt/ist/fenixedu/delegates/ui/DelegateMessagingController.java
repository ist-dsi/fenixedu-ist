/**
 * Copyright © 2013 Instituto Superior Técnico
 * <p>
 * This file is part of FenixEdu IST Delegates.
 * <p>
 * FenixEdu IST Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * FenixEdu IST Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.ui;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.messaging.core.ui.MessageBean;
import org.fenixedu.messaging.core.ui.MessagingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.ui.services.DelegateService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@SpringFunctionality(app = DelegatesController.class, title = "title.delegates.messaging")
@RequestMapping("/delegates-messaging")
public class DelegateMessagingController {

    @Autowired
    DelegateService delegateService;

    @RequestMapping
    public String home(Model model) {
        return messaging(Authenticate.getUser().getDelegatesSet().stream().filter(Delegate::isActive).iterator().next(), true, false, model);
    }

    @RequestMapping(value = "/messaging/{delegate}", method = RequestMethod.GET)
    public String messaging(@PathVariable Delegate delegate, @RequestParam(defaultValue = "true") Boolean studentGroups,
                            @RequestParam(defaultValue = "false") Boolean responsibleTeachers, Model model) {
        DelegateStudentSelectBean delegateStudentsBean = new DelegateStudentSelectBean();
        delegateStudentsBean.setSelectedPosition(delegate);
        if (studentGroups) {
            model.addAttribute("executionCourses", null);
        } else {
            List<DelegateExecutionCourseBean> executionCourses = delegateService.getExecutionCourses(delegateStudentsBean.getSelectedPosition());
            model.addAttribute("executionCourses", executionCourses.stream().distinct().collect(Collectors.toList()));
        }
        model.addAttribute("mandateYears", delegateStudentsBean.getSelectedPosition().getMandateExecutionYears());
        model.addAttribute("yearStudents", delegateStudentsBean.getSelectedPosition().isYearDelegate());
        model.addAttribute("degreeOrCycleStudents", delegateStudentsBean.getSelectedPosition().isDegreeOrCycleDelegate());
        model.addAttribute("responsibleTeachers", responsibleTeachers);
        model.addAttribute("action", "/delegates-messaging/messaging/");
        model.addAttribute("reload", "/delegates-messaging/messaging-reload/");
        model.addAttribute("students", delegateStudentsBean);
        return "delegates/messaging";
    }

    @RequestMapping(value = "/messaging-reload/", method = RequestMethod.POST)
    public String messagingReload(@ModelAttribute DelegateStudentSelectBean delegateStudentsBean, Model model) {
        return messaging(delegateStudentsBean.getSelectedPosition(), true, false, model);
    }

    @RequestMapping(value = "/messaging/", method = RequestMethod.POST)
    public RedirectView messaging(@ModelAttribute final DelegateStudentSelectBean delegateStudentsBean, final HttpServletResponse response,
                                  final HttpServletRequest request) throws IOException {
        MessageBean bean = new MessageBean();
        bean.setLockedSender(delegateStudentsBean.getSelectedPosition().getSender());
        for (final Group recipient : delegateStudentsBean.getRecipients()) {
            bean.selectRecipient(recipient);
            bean.addAdHocRecipient(recipient);
        }
        return MessagingUtils.redirectToNewMessage(request, response, bean);
    }

}
