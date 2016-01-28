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
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.domain.exceptions.DomainException;

import pt.ist.fenixedu.quc.domain.InquiryQuestion;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixframework.Atomic;

public class DeleteProfessorshipResultsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String professorshipCode;
    private ShiftType shiftType;
    private Long inquiryQuestionCode;
    private String executionCourseCode;

    @Atomic
    public boolean deleteResults() {
        Professorship professorship = InquiryResult.getProfessorship(getProfessorshipCode());
        if (professorship == null) {
            throw new DomainException("error.professorship.dontExist");
        }

        setExecutionCourseCode(null);
        boolean deletedResults = false;
        if (getShiftType() != null) {
            InquiryQuestion inquiryQuestion = null;
            if (getInquiryQuestionCode() != null) {
                if (inquiryQuestion == null) {
                    inquiryQuestion = InquiryResult.getInquiryQuestion(getInquiryQuestionCode());
                    if (inquiryQuestion == null) {
                        throw new DomainException("error.inquiryQuestion.dontExist");
                    }
                }
            }
            for (InquiryResult inquiryResult : InquiryResult.getInquiryResults(professorship, getShiftType())) {
                if (inquiryQuestion == null || inquiryResult.getInquiryQuestion() == inquiryQuestion) {
                    inquiryResult.delete();
                    deletedResults = true;
                }
            }
        } else {
            setInquiryQuestionCode(null);
            for (InquiryResult inquiryResult : professorship.getInquiryResultsSet()) {
                inquiryResult.delete();
                deletedResults = true;
            }
        }
        return deletedResults;
    }

    @Atomic
    public boolean deleteAllTeachersResults() {
        ExecutionCourse executionCourse = InquiryResult.getExecutionCourse(getExecutionCourseCode());
        if (executionCourse == null) {
            throw new DomainException("error.executionCourse.dontExist");
        }

        setProfessorshipCode(null);
        setShiftType(null);
        setInquiryQuestionCode(null);
        boolean deletedResults = false;
        for (InquiryResult inquiryResult : executionCourse.getInquiryResultsSet()) {
            if (inquiryResult.getProfessorship() != null) {
                inquiryResult.delete();
                deletedResults = true;
            }
        }
        return deletedResults;
    }

    public String getProfessorshipCode() {
        return professorshipCode;
    }

    public void setProfessorshipCode(String professorshipCode) {
        this.professorshipCode = professorshipCode;
    }

    public ShiftType getShiftType() {
        return shiftType;
    }

    public void setShiftType(ShiftType shiftType) {
        this.shiftType = shiftType;
    }

    public Long getInquiryQuestionCode() {
        return inquiryQuestionCode;
    }

    public void setInquiryQuestionCode(Long inquiryQuestionCode) {
        this.inquiryQuestionCode = inquiryQuestionCode;
    }

    public String getExecutionCourseCode() {
        return executionCourseCode;
    }

    public void setExecutionCourseCode(String executionCourseCode) {
        this.executionCourseCode = executionCourseCode;
    }
}
