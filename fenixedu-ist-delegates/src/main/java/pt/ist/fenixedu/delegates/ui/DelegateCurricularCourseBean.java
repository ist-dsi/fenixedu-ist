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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Student;

public class DelegateCurricularCourseBean {
    private CurricularCourse curricularCourse;

    private ExecutionYear executionYear;

    private ExecutionSemester executionSemester;

    private List<Student> enrolledStudents;

    private Integer curricularYear;

    static final public Comparator<DelegateCurricularCourseBean> CURRICULAR_COURSE_COMPARATOR_BY_CURRICULAR_YEAR_AND_CURRICULAR_SEMESTER =
            Comparator.comparing(DelegateCurricularCourseBean::getCurricularYear).thenComparing(
                    DelegateCurricularCourseBean::getCurricularSemester);

    public DelegateCurricularCourseBean(CurricularCourse curricularCourse, ExecutionYear executionYear, Integer curricularYear,
            ExecutionSemester executionSemester) {
        setCurricularCourse(curricularCourse);
        setExecutionYear(executionYear);
        setCurricularYear(curricularYear);
        setExecutionPeriod(executionSemester);
    }

    public CurricularCourse getCurricularCourse() {
        return (curricularCourse);
    }

    public void setCurricularCourse(CurricularCourse curricularCourse) {
        this.curricularCourse = curricularCourse;
    }

    public ExecutionYear getExecutionYear() {
        return (executionYear);
    }

    public void setExecutionYear(ExecutionYear executionYear) {
        this.executionYear = executionYear;
    }

    public ExecutionSemester getExecutionPeriod() {
        return (executionSemester);
    }

    public void setExecutionPeriod(ExecutionSemester executionSemester) {
        this.executionSemester = executionSemester;
    }

    public List<Student> getEnrolledStudents() {
        return new ArrayList<>(this.enrolledStudents);
    }

    public void setEnrolledStudents(List<Student> students) {
        this.enrolledStudents = new ArrayList<>(students);
    }

    public void calculateEnrolledStudents() {
        List<Student> enrolledStudents = getCurricularCourse()
                .getEnrolmentsByAcademicInterval(getExecutionPeriod().getAcademicInterval()).stream()
                .map(enrolment -> enrolment.getRegistration().getStudent())
                .sorted(Student.NUMBER_COMPARATOR).collect(Collectors.toList());
        setEnrolledStudents(enrolledStudents);
    }

    public Integer getSemester() {
        return getCurricularSemester();
    }

    public Integer getCurricularSemester() {
        return getExecutionPeriod().getSemester();
    }

    public Integer getCurricularYear() {
        return curricularYear;
    }

    public void setCurricularYear(Integer curricularYear) {
        this.curricularYear = curricularYear;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DelegateCurricularCourseBean && equals((DelegateCurricularCourseBean) obj);
    }

    public boolean equals(DelegateCurricularCourseBean delegateCurricularCourseBean) {
        return getCurricularCourse().equals(delegateCurricularCourseBean.getCurricularCourse())
                && getCurricularYear().equals(delegateCurricularCourseBean.getCurricularYear())
                && getExecutionPeriod().equals(delegateCurricularCourseBean.getExecutionPeriod())
                && getExecutionYear().equals(delegateCurricularCourseBean.getExecutionYear());
    }

    @Override
    public int hashCode() {
        return getCurricularCourse().hashCode() + getCurricularYear().hashCode() + getExecutionPeriod().hashCode() + getExecutionYear().hashCode();
    }
}