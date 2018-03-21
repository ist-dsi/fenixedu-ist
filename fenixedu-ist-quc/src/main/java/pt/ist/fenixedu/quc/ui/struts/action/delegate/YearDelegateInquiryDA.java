/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST QUC.
 *
 * FenixEdu IST QUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST QUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST QUC.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * 
 */
package pt.ist.fenixedu.quc.ui.struts.action.delegate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.domain.student.YearDelegate;
import pt.ist.fenixedu.quc.domain.DelegateInquiryTemplate;
import pt.ist.fenixedu.quc.domain.InquiryDelegateAnswer;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.ResultPersonCategory;
import pt.ist.fenixedu.quc.dto.CurricularCourseResumeResult;
import pt.ist.fenixedu.quc.dto.DelegateInquiryBean;
import pt.ist.fenixedu.quc.ui.struts.action.delegate.DelegateApplication.DelegateParticipateApp;
import pt.ist.fenixedu.quc.ui.struts.action.publico.ViewCourseInquiryPublicResults;
import pt.ist.fenixedu.quc.ui.struts.action.publico.ViewTeacherInquiryPublicResults;
import pt.ist.fenixedu.quc.util.DelegateUtils;
import pt.ist.fenixframework.FenixFramework;

/**
 * @author - Shezad Anavarali (shezad@ist.utl.pt)
 * 
 */
@StrutsFunctionality(app = DelegateParticipateApp.class, path = "inquiry", titleKey = "link.yearDelegateInquiries")
@Mapping(path = "/delegateInquiry", module = "delegate")
@Forwards({ @Forward(name = "chooseCoursesToAnswer", path = "/delegate/inquiries/chooseCoursesToAnswer.jsp"),
        @Forward(name = "inquiry1stPage", path = "/delegate/inquiries/inquiry1stPage.jsp"),
        @Forward(name = "delegateInquiry", path = "/delegate/inquiries/delegateInquiry.jsp"),
        @Forward(name = "inquiriesClosed", path = "/delegate/inquiries/inquiriesClosed.jsp") })
public class YearDelegateInquiryDA extends FenixDispatchAction {

    @EntryPoint
    public ActionForward showCoursesToAnswerPage(ActionMapping actionMapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {

        final DelegateInquiryTemplate delegateInquiryTemplate = DelegateInquiryTemplate.getCurrentTemplate();
        if (delegateInquiryTemplate == null) {
            return actionMapping.findForward("inquiriesClosed");
        }
        YearDelegate yearDelegate = null;
        ExecutionSemester executionPeriod = delegateInquiryTemplate.getExecutionPeriod();
        for (Delegate delegate : Authenticate.getUser().getDelegatesSet()) {
            if (delegate instanceof YearDelegate) {
                if (DelegateUtils.DelegateIsActiveForFirstExecutionYear(delegate, executionPeriod.getExecutionYear())) {
                    if (yearDelegate == null || ((YearDelegate) delegate).getEnd().isAfter(yearDelegate.getEnd())) {
                        yearDelegate = (YearDelegate) delegate;
                    }
                }
            }
        }

        if (yearDelegate != null) {
            Delegate lastYearDelegatePersonFunction =
                    DelegateUtils.getLastYearDelegateByExecutionYearAndCurricularYear(yearDelegate.getDegree(),
                            executionPeriod.getExecutionYear(), yearDelegate.getCurricularYear());
            if (lastYearDelegatePersonFunction != yearDelegate) {
                return actionMapping.findForward("inquiriesClosed");
            }

            final ExecutionDegree executionDegree =
                    yearDelegate.getDegree().getExecutionDegreesForExecutionYear(executionPeriod.getExecutionYear()).stream()
                            .findFirst().orElse(null);

            Set<ExecutionCourse> executionCoursesToInquiries =
                    DelegateUtils.getExecutionCoursesToInquiries(yearDelegate, executionPeriod, executionDegree);

            List<CurricularCourseResumeResult> coursesResultResume = new ArrayList<CurricularCourseResumeResult>();
            for (ExecutionCourse executionCourse : executionCoursesToInquiries) {
                coursesResultResume.add(new CurricularCourseResumeResult(executionCourse, executionDegree, yearDelegate));
            }
            Collections.sort(
                    coursesResultResume,
                    Comparator.comparing(CurricularCourseResumeResult::getExecutionCourse,
                            Comparator.comparing(ExecutionCourse::getName)));
            request.setAttribute("executionDegree", executionDegree);
            request.setAttribute("executionPeriod", executionPeriod);
            request.setAttribute("coursesResultResume", coursesResultResume);
            return actionMapping.findForward("chooseCoursesToAnswer");
        }

        return actionMapping.findForward("inquiriesClosed");
    }

    public ActionForward showFillInquiryPage(ActionMapping actionMapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {

        YearDelegate yearDelegate = FenixFramework.getDomainObject(getFromRequest(request, "yearDelegateOID").toString());
        ExecutionCourse executionCourse =
                FenixFramework.getDomainObject(getFromRequest(request, "executionCourseOID").toString());
        ExecutionDegree executionDegree =
                FenixFramework.getDomainObject(getFromRequest(request, "executionDegreeOID").toString());

        List<InquiryResult> results =
                InquiryResult.getInquiryResultsByExecutionDegreeAndForTeachers(executionCourse, executionDegree);
        DelegateInquiryTemplate delegateInquiryTemplate = DelegateInquiryTemplate.getCurrentTemplate();
        InquiryDelegateAnswer inquiryDelegateAnswer = null;
        for (InquiryDelegateAnswer delegateAnswer : yearDelegate.getInquiryDelegateAnswersSet()) {
            if (delegateAnswer.getExecutionCourse() == executionCourse) {
                inquiryDelegateAnswer = delegateAnswer;
            }
        }

        DelegateInquiryBean delegateInquiryBean =
                new DelegateInquiryBean(executionCourse, executionDegree, delegateInquiryTemplate, results, yearDelegate,
                        inquiryDelegateAnswer);

        request.setAttribute("hasNotRelevantData", InquiryResult.hasNotRelevantDataFor(executionCourse, executionDegree));
        request.setAttribute("executionPeriod", executionCourse.getExecutionPeriod());
        request.setAttribute("delegateInquiryBean", delegateInquiryBean);

        ViewTeacherInquiryPublicResults.setTeacherScaleColorException(executionCourse.getExecutionPeriod(), request);
        return actionMapping.findForward("delegateInquiry");
    }

    public ActionForward saveChanges(ActionMapping actionMapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        final DelegateInquiryBean delegateInquiryBean = getRenderedObject("delegateInquiryBean");
        if (delegateInquiryBean.getInquiryDelegateAnswer() == null) {
            InquiryDelegateAnswer inquiryDelegateAnswer = null;
            for (InquiryDelegateAnswer delegateAnswer : delegateInquiryBean.getYearDelegate().getInquiryDelegateAnswersSet()) {
                if (delegateAnswer.getExecutionCourse() == delegateInquiryBean.getExecutionCourse()) {
                    inquiryDelegateAnswer = delegateAnswer;
                }
            }
            delegateInquiryBean.setInquiryDelegateAnswer(inquiryDelegateAnswer);
        }
        if (!delegateInquiryBean.isValid()) {
            request.setAttribute("delegateInquiryBean", delegateInquiryBean);
            RenderUtils.invalidateViewState();
            addActionMessage(request, "error.inquiries.fillAllRequiredFields");
            return actionMapping.findForward("delegateInquiry");
        }
        RenderUtils.invalidateViewState("delegateInquiryBean");
        delegateInquiryBean.saveChanges(getUserView(request).getPerson(), ResultPersonCategory.DELEGATE);

        return showCoursesToAnswerPage(actionMapping, actionForm, request, response);
    }

    public ActionForward viewCourseInquiryResults(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        return ViewCourseInquiryPublicResults.getCourseResultsActionForward(mapping, form, request, response);
    }

    public ActionForward viewTeacherShiftTypeInquiryResults(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        return ViewTeacherInquiryPublicResults.getTeacherResultsActionForward(mapping, form, request, response);
    }
}
