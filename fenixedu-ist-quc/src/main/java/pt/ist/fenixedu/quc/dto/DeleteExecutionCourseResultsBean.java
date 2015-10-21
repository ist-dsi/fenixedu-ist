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

import java.io.Serializable;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.exceptions.DomainException;

import pt.ist.fenixedu.quc.domain.InquiryQuestion;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixframework.Atomic;

import com.google.common.base.Strings;

public class DeleteExecutionCourseResultsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String executionCourseCode;
    private String executionDegreeCode;
    private Long inquiryQuestionCode;

    @Atomic
    public boolean deleteResults() {
        ExecutionCourse executionCourse = InquiryResult.getExecutionCourse(getExecutionCourseCode());
        if (executionCourse == null) {
            throw new DomainException("error.executionCourse.dontExist");
        }

        boolean deletedResults = false;
        if (!Strings.isNullOrEmpty(getExecutionDegreeCode())) {
            ExecutionDegree executionDegree = InquiryResult.getExecutionDegree(getExecutionDegreeCode());
            if (executionDegree == null) {
                throw new DomainException("error.executionDegree.dontExist");
            }
            InquiryQuestion inquiryQuestion = null;
            if (getInquiryQuestionCode() != null) {
                inquiryQuestion = InquiryResult.getInquiryQuestion(getInquiryQuestionCode());
                if (inquiryQuestion == null) {
                    throw new DomainException("error.inquiryQuestion.dontExist");
                }
            }
            for (InquiryResult inquiryResult : executionCourse.getInquiryResultsSet()) {
                if (executionDegree == inquiryResult.getExecutionDegree()) {
                    if ((inquiryQuestion == null || inquiryResult.getInquiryQuestion() == inquiryQuestion)
                            && inquiryResult.getProfessorship() == null) { // delete only the direct EC results
                        inquiryResult.delete();
                        deletedResults = true;
                    }
                }
            }
        } else {
            setInquiryQuestionCode(null);
            for (InquiryResult inquiryResult : executionCourse.getInquiryResultsSet()) {
                if (inquiryResult.getProfessorship() == null) { // delete only the direct EC results
                    inquiryResult.delete();
                    deletedResults = true;
                }
            }
        }
        return deletedResults;
    }

    public String getExecutionCourseCode() {
        return executionCourseCode;
    }

    public void setExecutionCourseCode(String executionCourseCode) {
        this.executionCourseCode = executionCourseCode;
    }

    public String getExecutionDegreeCode() {
        return executionDegreeCode;
    }

    public void setExecutionDegreeCode(String executionDegreeCode) {
        this.executionDegreeCode = executionDegreeCode;
    }

    public Long getInquiryQuestionCode() {
        return inquiryQuestionCode;
    }

    public void setInquiryQuestionCode(Long inquiryQuestionCode) {
        this.inquiryQuestionCode = inquiryQuestionCode;
    }
}
