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
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.fenixedu.academic.domain.Coordinator;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.joda.time.DateTime;

import pt.ist.fenixedu.quc.domain.CoordinatorInquiryTemplate;
import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.InquiryCoordinatorAnswer;
import pt.ist.fenixedu.quc.domain.QuestionAnswer;
import pt.ist.fenixframework.Atomic;

import com.google.common.base.Strings;

public class CoordinatorInquiryBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Set<InquiryBlockDTO> coordinatorInquiryBlocks;
    private InquiryCoordinatorAnswer inquiryCoordinatorAnswer;
    private Coordinator coordinator;
    private ExecutionSemester executionSemester;
    private ExecutionDegree executionDegree;

    public CoordinatorInquiryBean(CoordinatorInquiryTemplate coordinatorInquiryTemplate, Coordinator coordinator,
            InquiryCoordinatorAnswer inquiryCoordinatorAnswer, ExecutionSemester executionSemester,
            ExecutionDegree executionDegree) {
        setCoordinator(coordinator);
        setExecutionSemester(executionSemester);
        setExecutionDegree(executionDegree);
        initCoordinatorInquiryBlocks(coordinatorInquiryTemplate, inquiryCoordinatorAnswer);
    }

    private void initCoordinatorInquiryBlocks(CoordinatorInquiryTemplate coordinatorInquiryTemplate,
            InquiryCoordinatorAnswer inquiryCoordinatorAnswer) {
        setCoordinatorInquiryBlocks(new TreeSet<InquiryBlockDTO>(Comparator.comparing(InquiryBlockDTO::getInquiryBlock)));
        setInquiryCoordinatorAnswer(inquiryCoordinatorAnswer);
        for (InquiryBlock inquiryBlock : coordinatorInquiryTemplate.getInquiryBlocksSet()) {
            getCoordinatorInquiryBlocks().add(new InquiryBlockDTO(inquiryCoordinatorAnswer, inquiryBlock));
        }
    }

    public String validateInquiry() {
        String validationResult = null;
        for (InquiryBlockDTO inquiryBlockDTO : getCoordinatorInquiryBlocks()) {
            validationResult = inquiryBlockDTO.validateMandatoryConditions(getCoordinatorInquiryBlocks());
            if (!Boolean.valueOf(validationResult)) {
                return validationResult;
            }
        }
        return Boolean.toString(true);
    }

    @Atomic
    public void saveInquiry() {
        for (InquiryBlockDTO blockDTO : getCoordinatorInquiryBlocks()) {
            for (InquiryGroupQuestionBean groupQuestionBean : blockDTO.getInquiryGroups()) {
                for (InquiryQuestionDTO questionDTO : groupQuestionBean.getInquiryQuestions()) {
                    if (!Strings.isNullOrEmpty(questionDTO.getResponseValue()) || questionDTO.getQuestionAnswer() != null) {
                        if (questionDTO.getQuestionAnswer() != null) {
                            questionDTO.getQuestionAnswer().setAnswer(questionDTO.getResponseValue());
                            questionDTO.getQuestionAnswer().getInquiryAnswer().setResponseDateTime(new DateTime());
                            getInquiryCoordinatorAnswer().setLastUpdatedBy(getCoordinator().getPerson());
                        } else {
                            if (getInquiryCoordinatorAnswer() == null) {
                                setInquiryCoordinatorAnswer(new InquiryCoordinatorAnswer(getExecutionDegree(),
                                        getExecutionSemester()));
                            }
                            new QuestionAnswer(getInquiryCoordinatorAnswer(), questionDTO.getInquiryQuestion(),
                                    questionDTO.getFinalValue());
                            getInquiryCoordinatorAnswer().setResponseDateTime(new DateTime());
                            getInquiryCoordinatorAnswer().setLastUpdatedBy(getCoordinator().getPerson());
                        }
                    }
                }
            }
        }
    }

    public void setCoordinatorInquiryBlocks(Set<InquiryBlockDTO> coordinatorInquiryBlocks) {
        this.coordinatorInquiryBlocks = coordinatorInquiryBlocks;
    }

    public Set<InquiryBlockDTO> getCoordinatorInquiryBlocks() {
        return coordinatorInquiryBlocks;
    }

    public void setInquiryCoordinatorAnswer(InquiryCoordinatorAnswer inquiryCoordinatorAnswer) {
        this.inquiryCoordinatorAnswer = inquiryCoordinatorAnswer;
    }

    public InquiryCoordinatorAnswer getInquiryCoordinatorAnswer() {
        return inquiryCoordinatorAnswer;
    }

    public void setCoordinator(Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    public Coordinator getCoordinator() {
        return coordinator;
    }

    public void setExecutionSemester(ExecutionSemester executionSemester) {
        this.executionSemester = executionSemester;
    }

    public ExecutionSemester getExecutionSemester() {
        return executionSemester;
    }

    public void setExecutionDegree(ExecutionDegree executionDegree) {
        this.executionDegree = executionDegree;
    }

    public ExecutionDegree getExecutionDegree() {
        return executionDegree;
    }

}
