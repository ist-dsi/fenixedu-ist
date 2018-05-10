/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Delegates.
 *
 * FenixEdu IST Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.ui;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.messaging.core.ui.MessageBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.domain.util.email.DelegateSender;
import pt.ist.fenixedu.delegates.ui.services.DelegateService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringFunctionality(app = DelegatesController.class, title = "title.delegates.messaging")
@RequestMapping("/delegates-messaging")
public class DelegateMessagingController {

    @Autowired
    DelegateService delegateService;

    @RequestMapping
    public String home(Model model) {
        return messaging(Authenticate.getUser().getDelegatesSet().iterator().next(), true, model);
    }

    @RequestMapping(value = "/messaging/{delegate}", method = RequestMethod.GET)
    public String messaging(@PathVariable Delegate delegate, @RequestParam(defaultValue = "true") Boolean studentGroups,
            Model model) {
        DelegateStudentSelectBean delegateStudentsBean = new DelegateStudentSelectBean();
        delegateStudentsBean.setSelectedPosition(delegate);
        List<DelegateCurricularCourseBean> executionCourses = null;
        if (studentGroups == false) {
            executionCourses = new ArrayList<DelegateCurricularCourseBean>();
            executionCourses.addAll(delegateService.getCurricularCourses(delegateStudentsBean.getSelectedPosition()));
        }
        if (executionCourses != null) {
            model.addAttribute("executionCourses", executionCourses.stream().distinct().collect(Collectors.toList()));
        } else {
            model.addAttribute("executionCourses", executionCourses);
        }
        model.addAttribute("yearStudents", delegateStudentsBean.getSelectedPosition().isYearDelegate());
        model.addAttribute("degreeOrCycleStudents", delegateStudentsBean.getSelectedPosition().isDegreeOrCycleDelegate());
        model.addAttribute("action", "/delegates-messaging/messaging/");
        model.addAttribute("reload", "/delegates-messaging/messaging-reload/");
        model.addAttribute("students", delegateStudentsBean);
        return "delegates/messaging";
    }

    @RequestMapping(value = "/messaging-reload/", method = RequestMethod.POST)
    public String messagingReload(@ModelAttribute DelegateStudentSelectBean delegateStudentsBean, Model model,
            BindingResult errors) {
        return messaging(delegateStudentsBean.getSelectedPosition(), true, model);
    }

    @RequestMapping(value = "/messaging/", method = RequestMethod.POST)
    public RedirectView messaging(@ModelAttribute DelegateStudentSelectBean delegateStudentsBean, Model model,
            BindingResult errors, HttpSession session, HttpServletRequest request, final RedirectAttributes redirectAttributes) {
        DelegateMessageBean delegateMessageBean = new DelegateMessageBean(delegateStudentsBean);
        DelegateSender sender = delegateMessageBean.getSelectedSender().getSender();
        List<Group> recipients = delegateMessageBean.getRecipients();

        MessageBean bean = new MessageBean();
        bean.setLockedSender(sender);
        for (Group recipient : recipients) {
            bean.selectRecipient(recipient);
            bean.addAdHocRecipient(recipient);
        }
        redirectAttributes.addFlashAttribute("messageBean",bean);
        return new RedirectView("/messaging/message",true);
    }

}
