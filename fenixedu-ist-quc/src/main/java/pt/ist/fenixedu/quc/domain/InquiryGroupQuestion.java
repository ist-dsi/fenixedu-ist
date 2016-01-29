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
package pt.ist.fenixedu.quc.domain;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.core.domain.Bennu;

public class InquiryGroupQuestion extends InquiryGroupQuestion_Base implements Comparable<InquiryGroupQuestion> {

    public InquiryGroupQuestion() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setRequired(false);
    }

    public boolean isVisible(StudentInquiryRegistry studentInquiryRegistry) {
        for (QuestionCondition questionCondition : getQuestionConditionsSet()) {
            if (questionCondition instanceof ECTSVisibleCondition) {
                return ((ECTSVisibleCondition) questionCondition).isVisible(studentInquiryRegistry);
            }
        }
        return true;
    }

    public String[] getConditionValues(StudentInquiryRegistry studentInquiryRegistry) {
        for (QuestionCondition questionCondition : getQuestionConditionsSet()) {
            if (questionCondition instanceof ECTSVisibleCondition) {
                return ((ECTSVisibleCondition) questionCondition).getConditionValues(studentInquiryRegistry);
            }
        }
        return null;
    }

    public boolean isScaleGroup() {
        if (getInquiryQuestionHeader() != null && getInquiryQuestionHeader().getScaleHeaders() != null) {
            return true;
        }
        for (InquiryQuestion inquiryQuestion : getInquiryQuestionsSet()) {
            if (inquiryQuestion.isScaleQuestion()) {
                return true;
            }
        }
        return false;
    }

    public boolean isResultGroup(ExecutionSemester executionSemester) {
        return getInquiryBlock().isResultBlock(executionSemester);
    }

    public boolean isCheckbox() {
        for (InquiryQuestion inquiryQuestion : getInquiryQuestionsSet()) {
            if (inquiryQuestion instanceof InquiryCheckBoxQuestion) {
                return true;
            }
        }
        return false;
    }

    public boolean isToPresentStandardResults() {
        for (InquiryQuestion inquiryQuestion : getInquiryQuestionsSet()) {
            if (inquiryQuestion.getPresentResults()) {
                return true;
            }
        }
        return false;
    }

    public int getNumberOfMandatoryQuestions() {
        int count = 0;
        for (InquiryQuestion inquiryQuestion : getInquiryQuestionsSet()) {
            if (inquiryQuestion.getRequired()) {
                count++;
            }
        }
        return count;
    }

    public void delete() {
        for (; !getInquiryQuestionsSet().isEmpty(); getInquiryQuestionsSet().iterator().next().delete()) {
            ;
        }
        for (; !getQuestionConditionsSet().isEmpty(); getQuestionConditionsSet().iterator().next().delete()) {
            ;
        }
        if (getInquiryQuestionHeader() != null) {
            getInquiryQuestionHeader().delete();
        }
        if (getResultQuestionHeader() != null) {
            getResultQuestionHeader().delete();
        }
        setInquiryBlock(null);
        setResultQuestion(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    @Override
    public int compareTo(InquiryGroupQuestion o) {
        return getGroupOrder().compareTo(o.getGroupOrder());
    }

}
