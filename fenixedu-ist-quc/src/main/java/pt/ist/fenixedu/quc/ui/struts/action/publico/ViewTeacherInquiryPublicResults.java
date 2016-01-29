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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.bennu.portal.servlet.PortalLayoutInjector;
import org.fenixedu.bennu.struts.annotations.Mapping;

import pt.ist.fenixedu.quc.domain.GroupResultType;
import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.InquiryResultType;
import pt.ist.fenixedu.quc.domain.ResultsInquiryTemplate;
import pt.ist.fenixedu.quc.domain.StudentTeacherInquiryTemplate;
import pt.ist.fenixedu.quc.dto.BlockResultsSummaryBean;
import pt.ist.fenixedu.quc.dto.GroupResultsSummaryBean;
import pt.ist.fenixframework.FenixFramework;

@Mapping(path = "/viewTeacherResults", module = "publico")
public class ViewTeacherInquiryPublicResults extends ViewInquiryPublicResults {

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        return getTeacherResultsActionForward(mapping, actionForm, request, response);
    }

    public static ActionForward getTeacherResultsActionForward(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) {
        Professorship professorship = FenixFramework.getDomainObject(request.getParameter("professorshipOID"));
        ShiftType shiftType = ShiftType.valueOf(request.getParameter("shiftType"));

        List<InquiryResult> inquiryResults = InquiryResult.getInquiryResults(professorship, shiftType);

        ExecutionSemester executionPeriod = professorship.getExecutionCourse().getExecutionPeriod();
        ResultsInquiryTemplate resultsInquiryTemplate = ResultsInquiryTemplate.getTemplateByExecutionPeriod(executionPeriod);
        Collection<InquiryBlock> resultBlocks = resultsInquiryTemplate.getInquiryBlocksSet();

        GroupResultsSummaryBean teacherGroupResultsSummaryBean =
                getGeneralResults(inquiryResults, resultBlocks, GroupResultType.TEACHER_RESULTS);
        request.setAttribute("teacherGroupResultsSummaryBean", teacherGroupResultsSummaryBean);

        InquiryResult teacherEvaluation = getTeacherEvaluation(inquiryResults);
        request.setAttribute("teacherEvaluation", teacherEvaluation);

        StudentTeacherInquiryTemplate teacherInquiryTemplate =
                StudentTeacherInquiryTemplate.getTemplateByExecutionPeriod(executionPeriod);
        List<BlockResultsSummaryBean> blockResultsSummaryBeans = new ArrayList<BlockResultsSummaryBean>();
        for (InquiryBlock inquiryBlock : teacherInquiryTemplate.getInquiryBlocksSet()) {
            blockResultsSummaryBeans.add(new BlockResultsSummaryBean(inquiryBlock, inquiryResults, null, null));
        }
        Collections.sort(blockResultsSummaryBeans, Comparator.comparing(BlockResultsSummaryBean::getInquiryBlock));
        request.setAttribute("executionCourse", professorship.getExecutionCourse());
        request.setAttribute("shiftType", shiftType);
        request.setAttribute("professorship", professorship);
        request.setAttribute("executionPeriod", executionPeriod);
        request.setAttribute("blockResultsSummaryBeans", blockResultsSummaryBeans);
        request.setAttribute("resultsDate", inquiryResults.iterator().next().getResultDate());

        setTeacherScaleColorException(executionPeriod, request);
        request.setAttribute("publicContext", true);
        PortalLayoutInjector.skipLayoutOn(request);
        return new ActionForward(null, "/inquiries/showTeacherInquiryResult_v3.jsp", false, "/teacher");
    }

    private static InquiryResult getTeacherEvaluation(List<InquiryResult> inquiryResults) {
        for (InquiryResult inquiryResult : inquiryResults) {
            if (InquiryResultType.TEACHER_EVALUATION.equals(inquiryResult.getResultType())) {
                return inquiryResult;
            }
        }
        return null;
    }

    public static void setTeacherScaleColorException(ExecutionSemester executionSemester, HttpServletRequest request) {
        if (executionSemester.getSemester() == 1 && executionSemester.getYear().equals("2010/2011")) {
            request.setAttribute("first-sem-2010", "first-sem-2010");
        }
    }
}
