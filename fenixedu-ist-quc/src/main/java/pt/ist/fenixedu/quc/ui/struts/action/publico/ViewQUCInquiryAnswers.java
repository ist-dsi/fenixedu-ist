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
package pt.ist.fenixedu.quc.ui.struts.action.publico;

import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.Coordinator;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.struts.annotations.Mapping;

import pt.ist.fenixedu.quc.domain.CoordinatorInquiryTemplate;
import pt.ist.fenixedu.quc.domain.DelegateInquiryTemplate;
import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.InquiryCoordinatorAnswer;
import pt.ist.fenixedu.quc.domain.InquiryDelegateAnswer;
import pt.ist.fenixedu.quc.domain.InquiryRegentAnswer;
import pt.ist.fenixedu.quc.domain.InquiryTeacherAnswer;
import pt.ist.fenixedu.quc.domain.RegentInquiryTemplate;
import pt.ist.fenixedu.quc.domain.TeacherInquiryTemplate;
import pt.ist.fenixedu.quc.dto.InquiryBlockDTO;
import pt.ist.fenixframework.FenixFramework;

@Mapping(path = "/viewQUCInquiryAnswers", module = "publico")
public class ViewQUCInquiryAnswers extends FenixDispatchAction {

    public ActionForward showCoordinatorInquiry(ActionMapping actionMapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        ExecutionDegree executionDegree =
                FenixFramework.getDomainObject(getFromRequest(request, "executionDegreeOID").toString());
        ExecutionSemester executionSemester =
                FenixFramework.getDomainObject(getFromRequest(request, "executionPeriodOID").toString());
        CoordinatorInquiryTemplate coordinatorInquiryTemplate =
                CoordinatorInquiryTemplate.getTemplateByExecutionPeriod(executionSemester);
        Coordinator coordinator = FenixFramework.getDomainObject(getFromRequest(request, "coordinatorOID").toString());
        InquiryCoordinatorAnswer inquiryCoordinatorAnswer = null;
        if (coordinatorInquiryTemplate.getShared()) {
            inquiryCoordinatorAnswer = InquiryCoordinatorAnswer.getInquiryCoordinationAnswers(executionDegree, executionSemester);
        } else {
            inquiryCoordinatorAnswer = InquiryCoordinatorAnswer.getInquiryCoordinatorAnswer(coordinator, executionSemester);
            request.setAttribute("person", coordinator.getPerson());
        }

        Set<InquiryBlockDTO> coordinatorInquiryBlocks = new TreeSet<InquiryBlockDTO>();
        for (InquiryBlock inquiryBlock : coordinatorInquiryTemplate.getInquiryBlocksSet()) {
            coordinatorInquiryBlocks.add(new InquiryBlockDTO(inquiryCoordinatorAnswer, inquiryBlock));
        }

        request.setAttribute("executionPeriod", executionSemester);
        request.setAttribute("executionDegree", executionDegree);
        request.setAttribute("coordinatorInquiryBlocks", coordinatorInquiryBlocks);

        return new ActionForward(null, "/inquiries/showCoordinatorInquiry.jsp", false, "/coordinator");
    }

    public ActionForward showRegentInquiry(ActionMapping actionMapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        Professorship professorship = FenixFramework.getDomainObject(getFromRequest(request, "professorshipOID").toString());

        RegentInquiryTemplate regentInquiryTemplate =
                RegentInquiryTemplate.getTemplateByExecutionPeriod(professorship.getExecutionCourse().getExecutionPeriod());
        InquiryRegentAnswer inquiryRegentAnswer = professorship.getInquiryRegentAnswer();

        Set<InquiryBlockDTO> regentInquiryBlocks = new TreeSet<InquiryBlockDTO>();
        for (InquiryBlock inquiryBlock : regentInquiryTemplate.getInquiryBlocksSet()) {
            regentInquiryBlocks.add(new InquiryBlockDTO(inquiryRegentAnswer, inquiryBlock));
        }

        request.setAttribute("executionPeriod", professorship.getExecutionCourse().getExecutionPeriod());
        request.setAttribute("executionCourse", professorship.getExecutionCourse());
        request.setAttribute("person", professorship.getPerson());
        request.setAttribute("regentInquiryBlocks", regentInquiryBlocks);

        return new ActionForward(null, "/inquiries/showRegentInquiry.jsp", false, "/teacher");
    }

    public ActionForward showTeacherInquiry(ActionMapping actionMapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        Professorship professorship = FenixFramework.getDomainObject(getFromRequest(request, "professorshipOID").toString());

        TeacherInquiryTemplate teacherInquiryTemplate =
                TeacherInquiryTemplate.getTemplateByExecutionPeriod(professorship.getExecutionCourse().getExecutionPeriod());
        InquiryTeacherAnswer inquiryTeacherAnswer = professorship.getInquiryTeacherAnswer();

        Set<InquiryBlockDTO> teacherInquiryBlocks = new TreeSet<InquiryBlockDTO>();
        for (InquiryBlock inquiryBlock : teacherInquiryTemplate.getInquiryBlocksSet()) {
            teacherInquiryBlocks.add(new InquiryBlockDTO(inquiryTeacherAnswer, inquiryBlock));
        }

        request.setAttribute("executionPeriod", professorship.getExecutionCourse().getExecutionPeriod());
        request.setAttribute("executionCourse", professorship.getExecutionCourse());
        request.setAttribute("person", professorship.getPerson());
        request.setAttribute("teacherInquiryBlocks", teacherInquiryBlocks);

        return new ActionForward(null, "/inquiries/showTeacherInquiry.jsp", false, "/teacher");
    }

    public ActionForward showDelegateInquiry(ActionMapping actionMapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        InquiryDelegateAnswer inquiryDelegateAnswer = FenixFramework.getDomainObject(getFromRequest(request, "inquiryDelegateAnswerOID").toString());
        ExecutionCourse executionCourse = inquiryDelegateAnswer.getExecutionCourse();

        DelegateInquiryTemplate delegateInquiryTemplate =
                DelegateInquiryTemplate.getTemplateByExecutionPeriod(executionCourse.getExecutionPeriod());
        Set<InquiryBlockDTO> delegateInquiryBlocks = new TreeSet<InquiryBlockDTO>();
        for (InquiryBlock inquiryBlock : delegateInquiryTemplate.getInquiryBlocksSet()) {
            delegateInquiryBlocks.add(new InquiryBlockDTO(inquiryDelegateAnswer, inquiryBlock));
        }

        Integer year = inquiryDelegateAnswer != null ? inquiryDelegateAnswer.getDelegate().getCurricularYear().getYear() : null;
        request.setAttribute("year", year);
        request.setAttribute("executionPeriod", executionCourse.getExecutionPeriod());
        request.setAttribute("executionCourse", executionCourse);
        request.setAttribute("executionDegree", inquiryDelegateAnswer.getExecutionDegree());
        request.setAttribute("delegateInquiryBlocks", delegateInquiryBlocks);

        return new ActionForward(null, "/inquiries/showDelegateInquiry.jsp", false, "/delegate");
    }
}
