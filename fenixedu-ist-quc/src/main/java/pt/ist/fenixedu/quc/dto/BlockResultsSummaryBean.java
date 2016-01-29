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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.fenixedu.academic.domain.Person;

import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.InquiryGroupQuestion;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.ResultClassification;
import pt.ist.fenixedu.quc.domain.ResultPersonCategory;

public class BlockResultsSummaryBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private InquiryBlock inquiryBlock;
    private ResultClassification blockResultClassification;
    private List<GroupResultsSummaryBean> groupsResults = new ArrayList<GroupResultsSummaryBean>();

    public BlockResultsSummaryBean(InquiryBlock inquiryBlock, List<InquiryResult> inquiryResults, Person person,
            ResultPersonCategory personCategory) {
        setInquiryBlock(inquiryBlock);
        setBlockResultClassification(getInquiryResultQuestion(inquiryResults));
        for (InquiryGroupQuestion inquiryGroupQuestion : inquiryBlock.getInquiryGroupsQuestionsSet()) {
            if (inquiryGroupQuestion.isToPresentStandardResults()) {
                getGroupsResults().add(new GroupResultsSummaryBean(inquiryGroupQuestion, inquiryResults, person, personCategory));
            }
        }
        Collections.sort(getGroupsResults(), Comparator.comparing(GroupResultsSummaryBean::getInquiryGroupQuestion));
        setLeftRightGroups();
    }

    private void setLeftRightGroups() {
        GroupResultsSummaryBean currentGroup = null;
        GroupResultsSummaryBean previousGroup = null;
        for (Iterator<GroupResultsSummaryBean> iterator = getGroupsResults().iterator(); iterator.hasNext();) {
            previousGroup = currentGroup;
            currentGroup = iterator.next();
            if (currentGroup.getInquiryGroupQuestion().isCheckbox()) {
                if (previousGroup != null && previousGroup.getInquiryGroupQuestion().isCheckbox()) {
                    currentGroup.setLeft(false);
                }
            }
        }
    }

    public boolean isMandatoryComments() {
        for (GroupResultsSummaryBean groupResultsSummaryBean : getGroupsResults()) {
            for (QuestionResultsSummaryBean questionResultsSummaryBean : groupResultsSummaryBean.getQuestionsResults()) {
                if (questionResultsSummaryBean.getResultClassification() != null
                        && questionResultsSummaryBean.getResultClassification().isMandatoryComment()) {
                    return true;
                }
            }
        }
        return false;
    }

    private InquiryResult getInquiryResultQuestion(List<InquiryResult> inquiryResults) {
        for (InquiryResult inquiryResult : inquiryResults) {
            if (inquiryResult.getInquiryQuestion() == getInquiryBlock().getResultQuestion()) {
                return inquiryResult;
            }
        }
        return null;
    }

    public InquiryBlock getInquiryBlock() {
        return inquiryBlock;
    }

    public void setInquiryBlock(InquiryBlock inquiryBlock) {
        this.inquiryBlock = inquiryBlock;
    }

    public List<GroupResultsSummaryBean> getGroupsResults() {
        return groupsResults;
    }

    public void setGroupsResults(List<GroupResultsSummaryBean> groupsResults) {
        this.groupsResults = groupsResults;
    }

    private void setBlockResultClassification(InquiryResult inquiryResultQuestion) {
        if (getInquiryBlock().getResultQuestion() != null && inquiryResultQuestion != null) {
            setBlockResultClassification(inquiryResultQuestion.getResultClassification());
        }
    }

    public void setBlockResultClassification(ResultClassification resultClassification) {
        this.blockResultClassification = resultClassification;
    }

    public ResultClassification getBlockResultClassification() {
        return blockResultClassification;
    }
}
