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
package pt.ist.fenixedu.tutorship.ui.Action.pedagogicalCouncil.highPerformance;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.QueueJob;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenixedu.tutorship.domain.StudentHighPerformanceQueueJob;
import pt.ist.fenixedu.tutorship.ui.TutorshipApplications.TutorshipApp;

@StrutsFunctionality(app = TutorshipApp.class, path = "high-performance-students",
        titleKey = "link.tutorship.students.ListHighPerformance")
@Mapping(path = "/studentHighPerformance", module = "pedagogicalCouncil")
@Forwards({ @Forward(name = "listRequests", path = "/pedagogicalCouncil/highPerformance/listHighPerformanceRequests.jsp") })
public class StudentHighPerformance extends FenixDispatchAction {

    @EntryPoint
    public ActionForward listRequests(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        List<QueueJob> jobs = QueueJob.getLastJobsForClassOrSubClass(StudentHighPerformanceQueueJob.class, 5);
        request.setAttribute("jobs", jobs);
        return mapping.findForward("listRequests");
    }

    public ActionForward resendJob(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        QueueJob job = getDomainObject(request, "id");
        job.resend();
        return listRequests(mapping, actionForm, request, response);
    }

    public ActionForward cancelJob(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        QueueJob job = getDomainObject(request, "id");
        job.cancel();
        return listRequests(mapping, actionForm, request, response);
    }
}
