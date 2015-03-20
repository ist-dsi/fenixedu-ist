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

import java.util.Collection;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

public class StudentTeacherInquiryTemplate extends StudentTeacherInquiryTemplate_Base {

    public StudentTeacherInquiryTemplate(DateTime begin, DateTime end) {
        super();
        init(begin, end);
    }

    public static StudentTeacherInquiryTemplate getCurrentTemplate() {
        final Collection<InquiryTemplate> inquiryTemplates = Bennu.getInstance().getInquiryTemplatesSet();
        for (final InquiryTemplate inquiryTemplate : inquiryTemplates) {
            if (inquiryTemplate instanceof StudentTeacherInquiryTemplate && inquiryTemplate.isOpen()) {
                return (StudentTeacherInquiryTemplate) inquiryTemplate;
            }
        }
        return null;
    }

    public static StudentTeacherInquiryTemplate getTemplateByExecutionPeriod(ExecutionSemester executionSemester) {
        final Collection<InquiryTemplate> inquiryTemplates = Bennu.getInstance().getInquiryTemplatesSet();
        for (final InquiryTemplate inquiryTemplate : inquiryTemplates) {
            if (inquiryTemplate instanceof StudentTeacherInquiryTemplate
                    && executionSemester == inquiryTemplate.getExecutionPeriod()) {
                return (StudentTeacherInquiryTemplate) inquiryTemplate;
            }
        }
        return null;
    }
}
