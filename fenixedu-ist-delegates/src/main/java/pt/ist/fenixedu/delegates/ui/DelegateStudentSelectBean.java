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

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeModuleScope;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accessControl.StudentGroup;
import org.fenixedu.academic.domain.accessControl.TeacherResponsibleOfExecutionCourseGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.springframework.util.CollectionUtils;
import pt.ist.fenixedu.delegates.domain.student.CycleDelegate;
import pt.ist.fenixedu.delegates.domain.student.DegreeDelegate;
import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.domain.student.YearDelegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DelegateStudentSelectBean {

    private List<CurricularCourse> selectedCurricularCourses;
    private Delegate selectedPosition;
    private Set<Delegate> positions;
    private Boolean selectedYearStudents;
    private Boolean selectedDegreeOrCycleStudents;
    private Boolean selectedResponsibleTeachers;

    public DelegateStudentSelectBean(Set<Delegate> delegates) {
        selectedCurricularCourses = new ArrayList<>();
        positions = null;
        selectedPosition = null;
        setInfo(delegates);
        selectedYearStudents = false;
        selectedDegreeOrCycleStudents = false;
        selectedResponsibleTeachers = false;
    }

    public DelegateStudentSelectBean() {
        selectedCurricularCourses = new ArrayList<>();
        selectedPosition = null;
        positions = null;
        selectedYearStudents = false;
        selectedDegreeOrCycleStudents = false;
        selectedResponsibleTeachers = false;
    }

    public void setInfo(Set<Delegate> delegates) {
        positions = delegates;
        setInfo();
    }

    private void setInfo() {
        if (selectedPosition == null) {
            selectedPosition = positions.iterator().next();
            return;
        }
        if (positions == null) {
            positions = selectedPosition.getUser().getDelegatesSet();
        }
    }

    public List<CurricularCourse> getSelectedCurricularCourses() {
        return selectedCurricularCourses;
    }

    public Delegate getSelectedPosition() {
        return selectedPosition;
    }

    public Set<Delegate> getPositions() {
        return positions;
    }

    public Boolean getSelectedYearStudents() {
        return selectedYearStudents;
    }

    public Boolean getSelectedDegreeOrCycleStudents() {
        return selectedDegreeOrCycleStudents;
    }

    public Boolean getSelectedResponsibleTeachers() {
        return selectedResponsibleTeachers;
    }

    public void setSelectedCurricularCourses(List<CurricularCourse> selectedCurricularCourses) {
        this.selectedCurricularCourses = selectedCurricularCourses;
    }

    public void setSelectedPosition(Delegate selectedPosition) {
        this.selectedPosition = selectedPosition;
        if (selectedPosition != null) {
            setInfo();
        }
    }

    public void setPositions(Set<Delegate> positions) {
        this.positions = positions;
    }

    public void setSelectedYearStudents(Boolean selectedYearStudents) {
        this.selectedYearStudents = selectedYearStudents;
    }

    public void setSelectedDegreeOrCycleStudents(Boolean selectedDegreeOrCycleStudents) {
        this.selectedDegreeOrCycleStudents = selectedDegreeOrCycleStudents;
    }

    public void setSelectedResponsibleTeachers(Boolean selectedResponsibleTeachers) {
        this.selectedResponsibleTeachers = selectedResponsibleTeachers;
    }

    public List<Group> getRecipients() {
        List<Group> toRet = new ArrayList<>();
        if (!CollectionUtils.isEmpty(selectedCurricularCourses)) {
            List<ExecutionCourse> selectedStudentCourses =
                    getCurricularCoursesBeans(selectedPosition, selectedCurricularCourses).stream()
                            .flatMap(ccb -> ccb.getCurricularCourse()
                                            .getExecutionCoursesByExecutionPeriod(ccb.getExecutionPeriod()).stream()
                                            .filter(ec -> ec.getDegreesSortedByDegreeName().contains(selectedPosition.getDegree())))
                            .collect(Collectors.toList());

            if (selectedResponsibleTeachers){
                selectedStudentCourses.stream().map(TeacherResponsibleOfExecutionCourseGroup::get).forEach(toRet::add);
            }
            else {
                selectedStudentCourses.stream().map(StudentGroup::get).forEach(toRet::add);
            }
        }
        if (selectedYearStudents && selectedPosition instanceof YearDelegate) {
            YearDelegate yearDelegate = (YearDelegate) selectedPosition;
            toRet.add(StudentGroup.get(selectedPosition.getDegree(), yearDelegate.getCurricularYear(),
                    ExecutionYear.getExecutionYearByDate(yearDelegate.getStart().toYearMonthDay())));
        }
        if (selectedDegreeOrCycleStudents) {
            if (selectedPosition instanceof CycleDelegate) {
                CycleDelegate cycleDelegate = (CycleDelegate) selectedPosition;
                toRet.add(StudentGroup.get(selectedPosition.getDegree(), cycleDelegate.getCycle()));
            }
            if (selectedPosition instanceof DegreeDelegate) {
                DegreeDelegate degreeDelegate = (DegreeDelegate) selectedPosition;
                toRet.add(StudentGroup.get(degreeDelegate.getDegree(), null));
            }
        }
        return toRet;
    }


    private List<DelegateCurricularCourseBean> getCurricularCoursesBeans(Delegate delegate,
            Collection<CurricularCourse> curricularCourses) {
        final ExecutionYear executionYear = ExecutionYear.getExecutionYearByDate(delegate.getStart().toYearMonthDay());
        List<DelegateCurricularCourseBean> result = new ArrayList<>();

        for (CurricularCourse curricularCourse : curricularCourses) {
            for (ExecutionSemester executionSemester : executionYear.getExecutionPeriodsSet()) {
                if (curricularCourse.hasAnyExecutionCourseIn(executionSemester)) {
                    for (DegreeModuleScope scope : curricularCourse.getDegreeModuleScopes()) {
                        if (!scope.isActiveForExecutionPeriod(executionSemester)) {
                            continue;
                        }

                        if (delegate.getClass().equals(YearDelegate.class)) {
                            YearDelegate yearDelegate = (YearDelegate) delegate;
                            if (!scopeBelongsToDelegateCurricularYear(scope, yearDelegate.getCurricularYear().getYear())) {
                                continue;
                            }
                        }

                        DelegateCurricularCourseBean bean =
                                new DelegateCurricularCourseBean(curricularCourse, executionYear, scope.getCurricularYear(),
                                        executionSemester);
                        if (!result.contains(bean)) {
                            bean.calculateEnrolledStudents();
                            result.add(bean);
                        }
                    }
                }
            }
        }
        return result;
    }

    private boolean scopeBelongsToDelegateCurricularYear(DegreeModuleScope scope, Integer curricularYear) {
        return scope.getCurricularYear().equals(curricularYear);
    }

}