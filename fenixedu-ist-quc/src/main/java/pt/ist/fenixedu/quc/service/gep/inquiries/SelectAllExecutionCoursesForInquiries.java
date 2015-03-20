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
package pt.ist.fenixedu.quc.service.gep.inquiries;

import static org.fenixedu.academic.predicate.AccessControl.check;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.executionCourse.ExecutionCourseSearchBean;
import org.fenixedu.academic.predicate.RolePredicates;

import pt.ist.fenixedu.quc.domain.InquiriesRoot;
import pt.ist.fenixframework.Atomic;

public class SelectAllExecutionCoursesForInquiries {

    @Atomic
    public static void run(final ExecutionCourseSearchBean executionCourseSearchBean) {
        check(RolePredicates.GEP_PREDICATE);
        for (final ExecutionCourse executionCourse : executionCourseSearchBean.search()) {
            executionCourse.setAvailableForInquiries(InquiriesRoot.getInstance());
        }
    }

    @Atomic
    public static void unselectAll(final ExecutionCourseSearchBean executionCourseSearchBean) {
        check(RolePredicates.GEP_PREDICATE);
        for (final ExecutionCourse executionCourse : executionCourseSearchBean.search()) {
            executionCourse.setAvailableForInquiries(null);
        }
    }

}