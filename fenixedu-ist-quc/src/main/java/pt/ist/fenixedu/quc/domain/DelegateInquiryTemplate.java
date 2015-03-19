/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Core.
 *
 * FenixEdu Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Core.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.quc.domain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.domain.student.YearDelegate;
import pt.ist.fenixedu.quc.util.DelegateUtils;

public class DelegateInquiryTemplate extends DelegateInquiryTemplate_Base {

    public DelegateInquiryTemplate(DateTime begin, DateTime end) {
        super();
        init(begin, end);
    }

    public static DelegateInquiryTemplate getCurrentTemplate() {
        final Collection<InquiryTemplate> inquiryTemplates = Bennu.getInstance().getInquiryTemplatesSet();
        for (final InquiryTemplate inquiryTemplate : inquiryTemplates) {
            if (inquiryTemplate instanceof DelegateInquiryTemplate && inquiryTemplate.isOpen()) {
                return (DelegateInquiryTemplate) inquiryTemplate;
            }
        }
        return null;
    }

    public static boolean hasYearDelegateInquiriesToAnswer(Student student) {
        DelegateInquiryTemplate currentTemplate = getCurrentTemplate();
        if (currentTemplate == null) {
            return false;
        }
        final ExecutionSemester executionSemester = currentTemplate.getExecutionPeriod();

        for (Delegate delegate : student.getPerson().getUser().getDelegatesSet()) {
            if (delegate instanceof YearDelegate) {
                if (DelegateUtils.DelegateIsActiveForFirstExecutionYear(delegate, executionSemester.getExecutionYear())) {
                    YearDelegate lastYearDelegatePersonFunction =
                            DelegateUtils.getLastYearDelegateByExecutionYearAndCurricularYear(delegate.getDegree(),
                                    executionSemester.getExecutionYear(), ((YearDelegate) delegate).getCurricularYear());
                    if (lastYearDelegatePersonFunction == (YearDelegate) delegate) {
                        if (hasInquiriesToAnswer(((YearDelegate) delegate), executionSemester)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static Boolean hasMandatoryCommentsToMake(YearDelegate yearDelegate, ExecutionCourse executionCourse,
            ExecutionDegree executionDegree) {
        Collection<InquiryResult> inquiryResults = executionCourse.getInquiryResultsSet();
        for (InquiryResult inquiryResult : inquiryResults) {
            if (inquiryResult.getResultClassification() != null
                    && (inquiryResult.getExecutionDegree() == executionDegree || inquiryResult.getExecutionDegree() == null)) {
                if (inquiryResult.getResultClassification().isMandatoryComment()
                        && !inquiryResult.getInquiryQuestion().isResultQuestion(executionCourse.getExecutionPeriod())) {
                    InquiryResultComment inquiryResultComment =
                            inquiryResult.getInquiryResultComment(yearDelegate.getUser().getPerson(),
                                    ResultPersonCategory.DELEGATE);
                    if (inquiryResultComment == null || StringUtils.isEmpty(inquiryResultComment.getComment())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasInquiriesToAnswer(YearDelegate yearDelegate, ExecutionSemester executionSemester) {
        if (yearDelegate.getInquiryDelegateAnswersSet().isEmpty()) {
            return true;
        }

        List<Boolean> asd =
                yearDelegate
                        .getDegree()
                        .getExecutionDegrees(executionSemester.getAcademicInterval())
                        .stream()
                        .flatMap(
                                ed -> DelegateUtils.getExecutionCoursesToInquiries(yearDelegate, executionSemester, ed).stream()
                                        .map(ec -> hasMandatoryCommentsToMake(yearDelegate, ec, ed)).collect(Collectors.toList())
                                        .stream()).collect(Collectors.toList());
        return false;
    }

    public static DelegateInquiryTemplate getTemplateByExecutionPeriod(ExecutionSemester executionSemester) {
        final Collection<InquiryTemplate> inquiryTemplates = Bennu.getInstance().getInquiryTemplatesSet();
        for (final InquiryTemplate inquiryTemplate : inquiryTemplates) {
            if (inquiryTemplate instanceof DelegateInquiryTemplate && executionSemester == inquiryTemplate.getExecutionPeriod()) {
                return (DelegateInquiryTemplate) inquiryTemplate;
            }
        }
        return null;
    }
}
