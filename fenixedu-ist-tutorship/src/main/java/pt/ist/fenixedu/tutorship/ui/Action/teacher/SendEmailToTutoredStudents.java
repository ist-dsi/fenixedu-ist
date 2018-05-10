/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Tutorship.
 *
 * FenixEdu IST Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.ui.Action.teacher;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;
import org.fenixedu.messaging.core.ui.MessageBean;
import org.fenixedu.messaging.core.ui.MessagingUtils;
import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixedu.tutorship.dto.teacher.tutor.StudentsByTutorBean;
import pt.ist.fenixedu.tutorship.dto.teacher.tutor.TutorshipBean;
import pt.ist.fenixedu.tutorship.ui.TutorshipApplications.TeacherTutorApp;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@StrutsFunctionality(app = TeacherTutorApp.class, path = "send-email-to-students",
        titleKey = "link.teacher.tutorship.sendMailToTutoredStudents")
@Mapping(path = "/sendMailToTutoredStudents", module = "teacher")
@Forwards(@Forward(name = "chooseReceivers", path = "/teacher/tutor/chooseReceivers.jsp"))
public class SendEmailToTutoredStudents extends FenixDispatchAction {

    public Teacher getTeacher(HttpServletRequest request) {
        return getLoggedPerson(request).getTeacher();
    }

    protected List<Group> getRecipients(HttpServletRequest request) {

        StudentsByTutorBean receivers = (StudentsByTutorBean) request.getAttribute("receivers");

        List<Group> recipients = new ArrayList<>();

        if (receivers != null) {
            for (TutorshipBean tutorshipBean : receivers.getStudentsList()) {
                Person person = tutorshipBean.getTutorship().getStudent().getPerson();
                recipients.add(person.getPersonGroup());
            }
        }

        return recipients;
    }

    @EntryPoint
    public ActionForward prepare(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {

        final Teacher teacher = getTeacher(request);

        if (!Tutorship.getActiveTutorships(teacher).isEmpty()) {
            request.setAttribute("receiversBean", getOrCreateBean(teacher));
        }

        request.setAttribute("tutor", teacher.getPerson());
        return mapping.findForward("chooseReceivers");
    }

    public StudentsByTutorBean getOrCreateBean(Teacher teacher) {
        StudentsByTutorBean receiversBean = getRenderedObject("receiversBean");
        RenderUtils.invalidateViewState();
        if (receiversBean == null) {
            receiversBean = new StudentsByTutorBean(teacher);
        }

        return receiversBean;
    }

    public ActionForward prepareCreateMail(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        final Teacher teacher = getTeacher(request);

        StudentsByTutorBean receivers;
        if (RenderUtils.getViewState("receivers") != null) {
            receivers = (StudentsByTutorBean) RenderUtils.getViewState("receivers").getMetaObject().getObject();

            if (receivers.getStudentsList().isEmpty()) {
                addActionMessage(request, "error.teacher.tutor.sendMail.chooseReceivers.mustSelectOne");
                request.setAttribute("receiversBean", receivers);
            } else {
                request.setAttribute("receivers", receivers);
                return createMail(mapping, actionForm, request, response);
            }
        }

        request.setAttribute("tutor", teacher.getPerson());
        return mapping.findForward("chooseReceivers");
    }

    public ActionForward createMail(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        final Person teacherPerson = getLoggedPerson(request);

        MessageBean messageBean = new MessageBean();
        messageBean.setLockedSender(teacherPerson.getSender());
        for (Group group : getRecipients(request)) {
            messageBean.addAdHocRecipient(group);
            messageBean.selectRecipient(group);
        }

        return MessagingUtils.redirectToNewMessage(request, response, messageBean);
    }
}
