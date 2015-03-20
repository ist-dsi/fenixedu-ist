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
package pt.ist.fenixedu.quc.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.beanutils.BeanComparator;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.util.Bundle;

import pt.ist.fenixedu.quc.domain.CurricularCourseInquiryTemplate;
import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.InquiryGlobalComment;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.QUCResultsLog;
import pt.ist.fenixedu.quc.domain.ResultPersonCategory;

public class CoordinatorResultsBean extends GlobalCommentsResultsBean {

    private static final long serialVersionUID = 1L;
    private List<BlockResultsSummaryBean> curricularBlockResults;
    private ExecutionDegree executionDegree;

    public CoordinatorResultsBean(ExecutionCourse executionCourse, ExecutionDegree executionDegree, Person coordinator,
            boolean backToResume) {
        super(executionCourse, coordinator, backToResume);
        setExecutionDegree(executionDegree);
        initResultComment(coordinator, true);
        initCurricularBlocksResults(executionCourse, executionDegree, coordinator);
    }

    private void initCurricularBlocksResults(ExecutionCourse executionCourse, ExecutionDegree executionDegree, Person person) {
        CurricularCourseInquiryTemplate courseInquiryTemplate =
                CurricularCourseInquiryTemplate.getTemplateByExecutionPeriod(executionCourse.getExecutionPeriod());
        setCurricularBlockResults(new ArrayList<BlockResultsSummaryBean>());
        List<InquiryResult> results =
                InquiryResult.getInquiryResultsByExecutionDegreeAndForTeachers(executionCourse, executionDegree);
        if (results != null && results.size() > 5) {
            for (InquiryBlock inquiryBlock : courseInquiryTemplate.getInquiryBlocksSet()) {
                getCurricularBlockResults().add(new BlockResultsSummaryBean(inquiryBlock, results, person, getPersonCategory()));
            }
        }
        Collections.sort(getCurricularBlockResults(), new BeanComparator("inquiryBlock.blockOrder"));
    }

    @Override
    protected InquiryGlobalComment createGlobalComment() {
        return new InquiryGlobalComment(getExecutionCourse(), getExecutionDegree());
    }

    @Override
    protected ResultPersonCategory getPersonCategory() {
        return ResultPersonCategory.DEGREE_COORDINATOR;
    }

    @Override
    public InquiryGlobalComment getInquiryGlobalComment() {
        return InquiryGlobalComment.getInquiryGlobalComment(getExecutionCourse(), getExecutionDegree());
    }

    public List<BlockResultsSummaryBean> getCurricularBlockResults() {
        return curricularBlockResults;
    }

    public void setCurricularBlockResults(List<BlockResultsSummaryBean> curricularBlockResults) {
        this.curricularBlockResults = curricularBlockResults;
    }

    public void setExecutionDegree(ExecutionDegree executionDegree) {
        this.executionDegree = executionDegree;
    }

    public ExecutionDegree getExecutionDegree() {
        return executionDegree;
    }

    public void qucResultsLog() {
        QUCResultsLog.createLog(getExecutionDegree().getDegree(), getExecutionDegree().getExecutionYear(), Bundle.MESSAGING,
                "log.degree.qucresults.comment", getExecutionDegree().getDegree().getPresentationName(), getExecutionCourse()
                        .getNameI18N().getContent());
    }
}
