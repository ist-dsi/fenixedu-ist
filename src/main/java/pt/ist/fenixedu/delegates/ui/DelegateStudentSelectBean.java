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
package pt.ist.fenixedu.delegates.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeModuleScope;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accessControl.StudentGroup;
import org.fenixedu.academic.domain.util.email.Recipient;

import pt.ist.fenixedu.delegates.domain.student.CycleDelegate;
import pt.ist.fenixedu.delegates.domain.student.DegreeDelegate;
import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.domain.student.YearDelegate;

public class DelegateStudentSelectBean {

    List<CurricularCourse> selectedExecutionCourses;
    Delegate selectedPosition;
    Set<Delegate> positions;
    Boolean selectedYearStudents;
    Boolean selectedDegreeOrCycleStudents;

    public DelegateStudentSelectBean(Set<Delegate> delegates) {
        selectedYearStudents = false;
        selectedDegreeOrCycleStudents = false;
        positions = null;
        selectedPosition = null;
        setInfo(delegates);
        selectedExecutionCourses = new ArrayList<CurricularCourse>();
    }

    public DelegateStudentSelectBean() {
        selectedExecutionCourses = new ArrayList<CurricularCourse>();
        selectedYearStudents = false;
        positions = null;
        selectedPosition = null;
        selectedDegreeOrCycleStudents = false;
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

    public List<CurricularCourse> getSelectedExecutionCourses() {
        return selectedExecutionCourses;
    }

    public void setSelectedExecutionCourses(List<CurricularCourse> selectedExecutionCourses) {
        this.selectedExecutionCourses = selectedExecutionCourses;
    }

    public Boolean getSelectedYearStudents() {
        return selectedYearStudents;
    }

    public void setSelectedYearStudents(Boolean selectedYearStudents) {
        this.selectedYearStudents = selectedYearStudents;
    }

    public Boolean getSelectedDegreeOrCycleStudents() {
        return selectedDegreeOrCycleStudents;
    }

    public void setSelectedDegreeOrCycleStudents(Boolean selectedDegreeOrCycleStudents) {
        this.selectedDegreeOrCycleStudents = selectedDegreeOrCycleStudents;
    }

    public Delegate getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedPosition(Delegate selectedPosition) {
        this.selectedPosition = selectedPosition;
        if (selectedPosition != null) {
            setInfo();
        }
    }

    public List<Recipient> getRecipients() {
        List<Recipient> toRet = new ArrayList<Recipient>();
        if (selectedExecutionCourses != null && selectedExecutionCourses.size() > 0) {

            List<DelegateCurricularCourseBean> lccb =
                    getCurricularCoursesBeans(selectedPosition, selectedExecutionCourses.stream().collect(Collectors.toSet()));

            List<ExecutionCourse> selectedStudentCourses =
                    lccb.stream()
                            .flatMap(
                                    ccb -> ccb
                                            .getCurricularCourse()
                                            .getExecutionCoursesByExecutionPeriod(ccb.getExecutionPeriod())
                                            .stream()
                                            .filter(ec -> (ec.getDegreesSortedByDegreeName().contains(selectedPosition
                                                    .getDegree())))).collect(Collectors.toList());

            selectedStudentCourses.stream().map(ec -> StudentGroup.get(ec)).forEach(sg -> toRet.add(Recipient.newInstance(sg)));

        }
        if (selectedYearStudents && selectedPosition instanceof YearDelegate) {
            YearDelegate yearDelegate = (YearDelegate) selectedPosition;
            StudentGroup sg =
                    StudentGroup.get(selectedPosition.getDegree(), yearDelegate.getCurricularYear(),
                            ExecutionYear.getExecutionYearByDate(yearDelegate.getStart().toYearMonthDay()));
            toRet.add(Recipient.newInstance(sg));
        }
        if (selectedDegreeOrCycleStudents) {
            if (selectedPosition instanceof CycleDelegate) {
                CycleDelegate cycleDelegate = (CycleDelegate) selectedPosition;
                StudentGroup sg = StudentGroup.get(selectedPosition.getDegree(), cycleDelegate.getCycle());
                toRet.add(Recipient.newInstance(sg));
            }
            if (selectedPosition instanceof DegreeDelegate) {
                DegreeDelegate degreeDelegate = (DegreeDelegate) selectedPosition;
                StudentGroup sg = StudentGroup.get(degreeDelegate.getDegree(), null);
                toRet.add(Recipient.newInstance(sg));
            }
        }

        return toRet;
    }

    public Set<Delegate> getPositions() {
        return positions;
    }

    public void setPositions(Set<Delegate> positions) {
        this.positions = positions;
    }

    private List<DelegateCurricularCourseBean> getCurricularCoursesBeans(Delegate delegate,
            Set<CurricularCourse> curricularCourses) {
        final Class delegateFunctionType = delegate.getClass();
        final ExecutionYear executionYear = ExecutionYear.getExecutionYearByDate(delegate.getStart().toYearMonthDay());

        List<DelegateCurricularCourseBean> result = new ArrayList<DelegateCurricularCourseBean>();

        for (CurricularCourse curricularCourse : curricularCourses) {
            for (ExecutionSemester executionSemester : executionYear.getExecutionPeriodsSet()) {
                if (curricularCourse.hasAnyExecutionCourseIn(executionSemester)) {
                    for (DegreeModuleScope scope : curricularCourse.getDegreeModuleScopes()) {
                        if (!scope.isActiveForExecutionPeriod(executionSemester)) {
                            continue;
                        }

                        if (delegateFunctionType.equals(YearDelegate.class)) {
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
        if (scope.getCurricularYear().equals(curricularYear)) {
            return true;
        }
        return false;
    }

}