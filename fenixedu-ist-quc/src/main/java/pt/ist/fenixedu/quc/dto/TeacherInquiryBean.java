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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.ShiftType;
import org.joda.time.DateTime;

import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.InquiryQuestion;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.InquiryResultComment;
import pt.ist.fenixedu.quc.domain.InquiryTeacherAnswer;
import pt.ist.fenixedu.quc.domain.MandatoryCondition;
import pt.ist.fenixedu.quc.domain.QuestionAnswer;
import pt.ist.fenixedu.quc.domain.QuestionCondition;
import pt.ist.fenixedu.quc.domain.ResultPersonCategory;
import pt.ist.fenixedu.quc.domain.TeacherInquiryTemplate;
import pt.ist.fenixframework.Atomic;

import com.google.common.base.Strings;

public class TeacherInquiryBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<TeacherShiftTypeResultsBean> teachersResults;
    private Set<InquiryBlockDTO> teacherInquiryBlocks;
    private Professorship professorship;

    public TeacherInquiryBean(TeacherInquiryTemplate teacherInquiryTemplate, Professorship professorship) {
        setProfessorship(professorship);
        initTeachersResults(professorship, professorship.getPerson());
        initTeacherInquiry(teacherInquiryTemplate, professorship);
        setGroupsVisibility();
    }

    private void initTeacherInquiry(TeacherInquiryTemplate teacherInquiryTemplate, Professorship professorship) {
        setTeacherInquiryBlocks(new TreeSet<InquiryBlockDTO>());
        for (InquiryBlock inquiryBlock : teacherInquiryTemplate.getInquiryBlocksSet()) {
            getTeacherInquiryBlocks().add(new InquiryBlockDTO(getInquiryTeacherAnswer(), inquiryBlock));
        }

    }

    private void initTeachersResults(Professorship professorship, Person person) {
        setTeachersResults(new ArrayList<TeacherShiftTypeResultsBean>());
        Collection<InquiryResult> professorshipResults = professorship.getInquiryResultsSet();
        if (!professorshipResults.isEmpty()) {
            for (ShiftType shiftType : getShiftTypes(professorshipResults)) {
                List<InquiryResult> teacherShiftResults = InquiryResult.getInquiryResults(professorship, shiftType);
                if (!teacherShiftResults.isEmpty()) {
                    getTeachersResults().add(
                            new TeacherShiftTypeResultsBean(professorship, shiftType, professorship.getExecutionCourse()
                                    .getExecutionPeriod(), teacherShiftResults, person, ResultPersonCategory.TEACHER));
                }
            }
        }
        Collections.sort(
                getTeachersResults(),
                Comparator.comparing(TeacherShiftTypeResultsBean::getProfessorship,
                        Comparator.comparing(Professorship::getPerson, Comparator.comparing(Person::getName))).thenComparing(
                        TeacherShiftTypeResultsBean::getShiftType));
    }

    private Set<ShiftType> getShiftTypes(Collection<InquiryResult> professorshipResults) {
        Set<ShiftType> shiftTypes = new HashSet<ShiftType>();
        for (InquiryResult inquiryResult : professorshipResults) {
            shiftTypes.add(inquiryResult.getShiftType());
        }
        return shiftTypes;
    }

    public void setGroupsVisibility() {
        for (InquiryBlockDTO inquiryBlockDTO : getTeacherInquiryBlocks()) {
            Set<InquiryGroupQuestionBean> groups = inquiryBlockDTO.getInquiryGroups();
            for (InquiryGroupQuestionBean group : groups) {
                setGroupVisibility(getTeacherInquiryBlocks(), group);
            }
        }
    }

    private void setGroupVisibility(Set<InquiryBlockDTO> inquiryBlocks, InquiryGroupQuestionBean groupQuestionBean) {
        for (QuestionCondition questionCondition : groupQuestionBean.getInquiryGroupQuestion().getQuestionConditionsSet()) {
            if (questionCondition instanceof MandatoryCondition) {
                MandatoryCondition condition = (MandatoryCondition) questionCondition;
                InquiryQuestionDTO inquiryDependentQuestionBean =
                        getInquiryQuestionBean(condition.getInquiryDependentQuestion(), inquiryBlocks);
                boolean isMandatory =
                        inquiryDependentQuestionBean.getFinalValue() == null ? false : condition.getConditionValuesAsList()
                                .contains(inquiryDependentQuestionBean.getFinalValue());
                if (isMandatory) {
                    groupQuestionBean.setVisible(true);
                } else {
                    groupQuestionBean.setVisible(false);
                    for (InquiryQuestionDTO questionDTO : groupQuestionBean.getInquiryQuestions()) {
                        questionDTO.setResponseValue(null);
                    }
                }
            }
        }
    }

    private InquiryQuestionDTO getInquiryQuestionBean(InquiryQuestion inquiryQuestion, Set<InquiryBlockDTO> inquiryBlocks) {
        for (InquiryBlockDTO blockDTO : inquiryBlocks) {
            for (InquiryGroupQuestionBean groupQuestionBean : blockDTO.getInquiryGroups()) {
                for (InquiryQuestionDTO inquiryQuestionDTO : groupQuestionBean.getInquiryQuestions()) {
                    if (inquiryQuestionDTO.getInquiryQuestion() == inquiryQuestion) {
                        return inquiryQuestionDTO;
                    }
                }
            }
        }
        return null;
    }

    public List<TeacherShiftTypeResultsBean> getTeachersResults() {
        return teachersResults;
    }

    public void setTeachersResults(List<TeacherShiftTypeResultsBean> teachersResults) {
        this.teachersResults = teachersResults;
    }

    public String validateInquiry() {
        String validationResult = null;
        for (InquiryBlockDTO inquiryBlockDTO : getTeacherInquiryBlocks()) {
            validationResult = inquiryBlockDTO.validateMandatoryConditions(getTeacherInquiryBlocks());
            if (!Boolean.valueOf(validationResult)) {
                return validationResult;
            }
        }
        return Boolean.toString(true);
    }

    @Atomic
    public void saveChanges(Person person, ResultPersonCategory teacher) {
        for (TeacherShiftTypeResultsBean teacherShiftTypeResultsBean : getTeachersResults()) {
            saveComments(person, teacher, teacherShiftTypeResultsBean.getBlockResults());
        }
        for (InquiryBlockDTO blockDTO : getTeacherInquiryBlocks()) {
            for (InquiryGroupQuestionBean groupQuestionBean : blockDTO.getInquiryGroups()) {
                for (InquiryQuestionDTO questionDTO : groupQuestionBean.getInquiryQuestions()) {
                    if (!Strings.isNullOrEmpty(questionDTO.getResponseValue()) || questionDTO.getQuestionAnswer() != null) {
                        if (questionDTO.getQuestionAnswer() != null) {
                            questionDTO.getQuestionAnswer().setAnswer(questionDTO.getResponseValue());
                            questionDTO.getQuestionAnswer().getInquiryAnswer().setResponseDateTime(new DateTime());
                        } else {
                            if (getInquiryTeacherAnswer() == null) {
                                new InquiryTeacherAnswer(getProfessorship());
                            }
                            new QuestionAnswer(getInquiryTeacherAnswer(), questionDTO.getInquiryQuestion(),
                                    questionDTO.getFinalValue());
                            getInquiryTeacherAnswer().setResponseDateTime(new DateTime());
                        }
                    }
                }
            }
        }
    }

    private void saveComments(Person person, ResultPersonCategory teacher, List<BlockResultsSummaryBean> blocksResults) {
        for (BlockResultsSummaryBean blockResultsSummaryBean : blocksResults) {
            for (GroupResultsSummaryBean groupResultsSummaryBean : blockResultsSummaryBean.getGroupsResults()) {
                for (QuestionResultsSummaryBean questionResultsSummaryBean : groupResultsSummaryBean.getQuestionsResults()) {
                    InquiryResult questionResult = questionResultsSummaryBean.getQuestionResult();
                    if (questionResult != null) {
                        InquiryResultComment inquiryResultComment =
                                questionResultsSummaryBean.getQuestionResult().getInquiryResultComment(person, teacher);
                        if (!Strings.isNullOrEmpty(questionResultsSummaryBean.getEditableComment())
                                || inquiryResultComment != null) {
                            if (inquiryResultComment == null) {
                                inquiryResultComment =
                                        new InquiryResultComment(questionResult, person, teacher, questionResultsSummaryBean
                                                .getQuestionResult().getInquiryResultCommentsSet().size() + 1);
                            }
                            inquiryResultComment.setComment(questionResultsSummaryBean.getEditableComment());
                        }
                    }
                }
            }
        }
    }

    public void setProfessorship(Professorship professorship) {
        this.professorship = professorship;
    }

    public Professorship getProfessorship() {
        return professorship;
    }

    public void setTeacherInquiryBlocks(Set<InquiryBlockDTO> teacherInquiryBlocks) {
        this.teacherInquiryBlocks = teacherInquiryBlocks;
    }

    public Set<InquiryBlockDTO> getTeacherInquiryBlocks() {
        return teacherInquiryBlocks;
    }

    public InquiryTeacherAnswer getInquiryTeacherAnswer() {
        return getProfessorship().getInquiryTeacherAnswer();
    }
}
