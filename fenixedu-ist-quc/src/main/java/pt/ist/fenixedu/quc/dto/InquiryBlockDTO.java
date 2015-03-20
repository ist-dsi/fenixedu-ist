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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections.comparators.ComparatorChain;

import pt.ist.fenixedu.quc.domain.InquiryAnswer;
import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.InquiryGroupQuestion;
import pt.ist.fenixedu.quc.domain.StudentInquiryRegistry;

public class InquiryBlockDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private StudentInquiryRegistry inquiryRegistry;
    private InquiryBlock inquiryBlock;

    private SortedSet<InquiryGroupQuestionBean> inquiryGroups;

    public InquiryBlockDTO(InquiryBlock inquiryBlock, StudentInquiryRegistry inquiryRegistry) {
        initBlock(inquiryBlock);
        for (InquiryGroupQuestion inquiryGroupQuestion : inquiryBlock.getInquiryGroupsQuestionsSet()) {
            getInquiryGroups().add(new InquiryGroupQuestionBean(inquiryGroupQuestion, inquiryRegistry));
        }
    }

    public InquiryBlockDTO(InquiryAnswer inquiryAnswer, InquiryBlock inquiryBlock) {
        initBlock(inquiryBlock);
        for (InquiryGroupQuestion inquiryGroupQuestion : inquiryBlock.getInquiryGroupsQuestionsSet()) {
            getInquiryGroups().add(new InquiryGroupQuestionBean(inquiryGroupQuestion, inquiryAnswer));
        }
    }

    public InquiryBlockDTO(InquiryBlock inquiryBlock) {
        initBlock(inquiryBlock);
        for (InquiryGroupQuestion inquiryGroupQuestion : inquiryBlock.getInquiryGroupsQuestionsSet()) {
            getInquiryGroups().add(new InquiryGroupQuestionBean(inquiryGroupQuestion));
        }
    }

    private void initBlock(InquiryBlock inquiryBlock) {
        setInquiryBlock(inquiryBlock);
        ComparatorChain comparatorChain = new ComparatorChain();
        comparatorChain.addComparator(new BeanComparator("inquiryGroupQuestion.groupOrder"));
        comparatorChain.addComparator(new BeanComparator("order"));
        setInquiryGroups(new TreeSet<InquiryGroupQuestionBean>(comparatorChain));
    }

    public InquiryBlock getInquiryBlock() {
        return inquiryBlock;
    }

    public void setInquiryBlock(InquiryBlock inquiryBlock) {
        this.inquiryBlock = inquiryBlock;
    }

    public void setInquiryGroups(SortedSet<InquiryGroupQuestionBean> inquiryGroups) {
        this.inquiryGroups = inquiryGroups;
    }

    public SortedSet<InquiryGroupQuestionBean> getInquiryGroups() {
        return inquiryGroups;
    }

    public void setInquiryRegistry(StudentInquiryRegistry inquiryRegistry) {
        this.inquiryRegistry = inquiryRegistry;
    }

    public StudentInquiryRegistry getInquiryRegistry() {
        return inquiryRegistry;
    }

    public String validate(Set<InquiryBlockDTO> inquiryBlocks) {
        Set<InquiryGroupQuestionBean> groups = getInquiryGroups();
        String validationResult = null;
        for (InquiryGroupQuestionBean group : groups) {
            validationResult = group.validate(inquiryBlocks);
            if (!Boolean.valueOf(validationResult)) {
                return validationResult;
            }
        }
        return Boolean.toString(true);
    }

    public String validateMandatoryConditions(Set<InquiryBlockDTO> inquiryBlocks) {
        Set<InquiryGroupQuestionBean> groups = getInquiryGroups();
        String validationResult = null;
        for (InquiryGroupQuestionBean group : groups) {
            validationResult = group.validateMandatoryConditions(inquiryBlocks);
            if (!Boolean.valueOf(validationResult)) {
                return validationResult;
            }
        }
        return Boolean.toString(true);
    }
}
