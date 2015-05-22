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
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;

public class InquiryQuestion extends InquiryQuestion_Base {

    public InquiryQuestion() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setRequired(false);
        setAutofit(false);
        setNewRow(false);
        setShowRequiredMark(false);
        setHasClassification(false);
        setPresentResults(true);
        long code = InquiriesRoot.getInstance().getLastInquiryQuestionCode() + 1;
        InquiriesRoot.getInstance().setLastInquiryQuestionCode(code);
        setCode(code);
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

    public void delete() {
        if (!getInquiryResultsSet().isEmpty()) {
            throw new DomainException("error.inquiryQuestion.can.not.delete.hasAssociatedResults");
        }
        if (!getQuestionAnswersSet().isEmpty()) {
            throw new DomainException("error.inquiryQuestion.can.not.delete.hasAssociatedAnswers");
        }
        for (; !getQuestionConditionsSet().isEmpty(); getQuestionConditionsSet().iterator().next().delete()) {
            ;
        }
        if (getInquiryQuestionHeader() != null) {
            getInquiryQuestionHeader().delete();
        }
        for (InquiryBlock inquiryBlock : getAssociatedBlocksSet()) {
            removeAssociatedBlocks(inquiryBlock);
        }
        for (InquiryBlock inquiryBlock : getAssociatedResultBlocksSet()) {
            removeAssociatedResultBlocks(inquiryBlock);
        }
        setCheckboxGroupQuestion(null);
        setDependentQuestionCondition(null);
        setInquiryGroupQuestion(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    public boolean isScaleQuestion() {
        return ((getInquiryGroupQuestion().getInquiryQuestionHeader() != null && getInquiryGroupQuestion()
                .getInquiryQuestionHeader().getScaleHeaders() != null) || (getInquiryQuestionHeader() != null && getInquiryQuestionHeader()
                .getScaleHeaders() != null));
    }

    public boolean isResultQuestion(ExecutionSemester executionSemester) {
        return getInquiryGroupQuestion().getInquiryBlock().isResultBlock(executionSemester);
    }

    public boolean hasGroupDependentQuestionCondition() {
        return getDependentQuestionCondition() != null && getDependentQuestionCondition().getInquiryGroupQuestion() != null;
    }

}
