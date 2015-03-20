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

import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

public class Student1rstCycleInquiryTemplate extends Student1rstCycleInquiryTemplate_Base {

    public Student1rstCycleInquiryTemplate(DateTime begin, DateTime end) {
        super();
        init(begin, end);
    }

    public static Student1rstCycleInquiryTemplate getCurrentTemplate() {
        final Collection<InquiryTemplate> inquiryTemplates = Bennu.getInstance().getInquiryTemplatesSet();
        for (final InquiryTemplate inquiryTemplate : inquiryTemplates) {
            if (inquiryTemplate instanceof Student1rstCycleInquiryTemplate && inquiryTemplate.isOpen()) {
                return (Student1rstCycleInquiryTemplate) inquiryTemplate;
            }
        }
        return null;
    }
}
