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

import org.apache.commons.lang.StringUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

public class InquiryAnswer extends InquiryAnswer_Base {

    public InquiryAnswer() {
        super();
        setRootDomainObject(Bennu.getInstance());
        setResponseDateTime(new DateTime());
        long code = InquiriesRoot.getInstance().getLastInquiryAnswerCode() + 1;
        InquiriesRoot.getInstance().setLastInquiryAnswerCode(code);
        setCode(code);
    }

    public QuestionAnswer getQuestionAnswer(InquiryQuestion inquiryQuestion) {
        for (QuestionAnswer questionAnswer : getQuestionAnswersSet()) {
            if (questionAnswer.getInquiryQuestion() == inquiryQuestion) {
                return questionAnswer;
            }
        }
        return null;
    }

    public int getNumberOfAnsweredRequiredQuestions() {
        int count = 0;
        for (QuestionAnswer questionAnswer : getQuestionAnswersSet()) {
            if (!StringUtils.isEmpty(questionAnswer.getAnswer()) && questionAnswer.getInquiryQuestion().getRequired()) {
                count++;
            }
        }
        return count;
    }

    public int getNumberOfAnsweredQuestions() {
        int count = 0;
        for (QuestionAnswer questionAnswer : getQuestionAnswersSet()) {
            if (!StringUtils.isEmpty(questionAnswer.getAnswer())) {
                count++;
            }
        }
        return count;
    }

    public boolean hasRequiredQuestionsToAnswer(InquiryTemplate inquiryTemplate) {
        return getNumberOfAnsweredRequiredQuestions() < inquiryTemplate.getNumberOfRequiredQuestions();
    }

    @Deprecated
    public java.util.Date getResponse() {
        org.joda.time.DateTime dt = getResponseDateTime();
        return (dt == null) ? null : new java.util.Date(dt.getMillis());
    }

    @Deprecated
    public void setResponse(java.util.Date date) {
        if (date == null) {
            setResponseDateTime(null);
        } else {
            setResponseDateTime(new org.joda.time.DateTime(date.getTime()));
        }
    }

}
