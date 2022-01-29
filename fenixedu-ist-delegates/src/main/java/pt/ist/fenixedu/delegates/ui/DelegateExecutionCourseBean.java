/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Delegates.
 *
 * FenixEdu IST Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.ui;

/*
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

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;

import java.util.Comparator;
import java.util.Objects;

public class DelegateExecutionCourseBean {
    private final ExecutionCourse executionCourse;
    private int enrollmentCount;

    public static final Comparator<DelegateExecutionCourseBean> EXECUTION_COURSE_COMPARATOR_BY_EXECUTION_YEAR_AND_SEMESTER =
            Comparator.comparing(DelegateExecutionCourseBean::getExecutionYear)
                    .thenComparing(DelegateExecutionCourseBean::getSemester)
                    .thenComparing((bean) -> bean.getExecutionCourse().getNameI18N());

    public DelegateExecutionCourseBean(ExecutionCourse executionCourse) {
        this.executionCourse = executionCourse;
        calculateEnrolledStudents();
    }

    public ExecutionCourse getExecutionCourse() {
        return this.executionCourse;
    }

    public ExecutionYear getExecutionYear() {
        return this.executionCourse.getExecutionYear();
    }

    public ExecutionSemester getExecutionPeriod() {
        return this.executionCourse.getExecutionPeriod();
    }

    public int getEnrollmentCount() {
        return enrollmentCount;
    }

    private void calculateEnrolledStudents() {
        this.enrollmentCount = getExecutionCourse().getEnrolmentCount();
    }

    public int getSemester() {
        return getCurricularSemester();
    }

    public int getCurricularSemester() {
        return getExecutionPeriod().getSemester();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DelegateExecutionCourseBean && equals((DelegateExecutionCourseBean) obj);
    }

    public boolean equals(DelegateExecutionCourseBean delegateExecutionCourseBean) {
        return Objects.equals(this.getExecutionCourse(), delegateExecutionCourseBean.getExecutionCourse());
    }

    @Override
    public int hashCode() {
        return getExecutionCourse().hashCode();
    }
}