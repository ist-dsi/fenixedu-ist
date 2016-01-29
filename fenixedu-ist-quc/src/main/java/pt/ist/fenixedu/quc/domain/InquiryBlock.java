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

public class InquiryBlock extends InquiryBlock_Base implements Comparable<InquiryBlock> {

    public InquiryBlock() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public boolean isResultBlock(ExecutionSemester executionSemester) {
        return getInquiry(executionSemester) instanceof ResultsInquiryTemplate;
    }

    public InquiryTemplate getInquiry(ExecutionSemester executionSemester) {
        for (InquiryTemplate inquiryTemplate : getInquiriesSet()) {
            if (inquiryTemplate.getExecutionPeriod() == executionSemester) {
                return inquiryTemplate;
            }
        }
        return null;
    }

    public void delete() {
        for (; !getInquiryGroupsQuestionsSet().isEmpty(); getInquiryGroupsQuestionsSet().iterator().next().delete()) {
            setRootDomainObject(null);
        }
        if (getInquiryQuestionHeader() != null) {
            getInquiryQuestionHeader().delete();
        }
        setResultQuestion(null);
        setGroupResultQuestion(null);
        for (InquiryTemplate inquiryTemplate : getInquiriesSet()) {
            removeInquiries(inquiryTemplate);
        }
        super.deleteDomainObject();
    }

    @Override
    public int compareTo(InquiryBlock o) {
        return getBlockOrder().compareTo(o.getBlockOrder());
    }

}
