/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Evaluation.
 *
 * FenixEdu IST Teacher Evaluation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Evaluation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Evaluation.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.teacher.evaluation.dto.teacherCredits;

import java.io.Serializable;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.teacher.evaluation.domain.credits.AnnualCreditsState;
import pt.ist.fenixedu.teacher.evaluation.domain.time.calendarStructure.TeacherCreditsFillingForDepartmentAdmOfficeCE;
import pt.ist.fenixedu.teacher.evaluation.domain.time.calendarStructure.TeacherCreditsFillingForTeacherCE;
import pt.ist.fenixframework.Atomic;

public class TeacherCreditsPeriodBean implements Serializable {

    private DateTime beginForTeacher;
    private DateTime endForTeacher;

    private DateTime beginForDepartmentAdmOffice;
    private DateTime endForDepartmentAdmOffice;

    private ExecutionSemester executionPeriodReference;

    private AnnualCreditsState annualCreditsState;
    private DateTime sharedUnitCreditsBeginDate;
    private DateTime sharedUnitCreditsEndDate;
    private DateTime unitCreditsBeginDate;
    private DateTime unitCreditsEndDate;
    private DateTime reductionServiceApprovalBeginDate;
    private DateTime reductionServiceApprovalEndDate;

    private LocalDate finalCalculationDate;
    private LocalDate closeCreditsDate;

    private boolean teacher;

    public TeacherCreditsPeriodBean(ExecutionSemester executionSemester) {
        setExecutionPeriod(executionSemester);
        refreshDates();
    }

    public TeacherCreditsPeriodBean(ExecutionSemester executionSemester, boolean teacher) {
        setExecutionPeriod(executionSemester);
        setTeacher(teacher);
        refreshDates();
    }

    public ExecutionSemester getExecutionPeriod() {
        return executionPeriodReference;
    }

    public void setExecutionPeriod(ExecutionSemester executionSemester) {
        executionPeriodReference = executionSemester;
    }

    public DateTime getBeginForTeacher() {
        return beginForTeacher;
    }

    public void setBeginForTeacher(DateTime begin) {
        this.beginForTeacher = begin;
    }

    public DateTime getEndForTeacher() {
        return endForTeacher;
    }

    public void setEndForTeacher(DateTime end) {
        this.endForTeacher = end;
    }

    public DateTime getBeginForDepartmentAdmOffice() {
        return beginForDepartmentAdmOffice;
    }

    public void setBeginForDepartmentAdmOffice(DateTime beginForDepartmentAdmOffice) {
        this.beginForDepartmentAdmOffice = beginForDepartmentAdmOffice;
    }

    public DateTime getEndForDepartmentAdmOffice() {
        return endForDepartmentAdmOffice;
    }

    public void setEndForDepartmentAdmOffice(DateTime endForDepartmentAdmOffice) {
        this.endForDepartmentAdmOffice = endForDepartmentAdmOffice;
    }

    public void refreshDates() {

        ExecutionSemester executionSemester = getExecutionPeriod();

        TeacherCreditsFillingForDepartmentAdmOfficeCE departmentAdmOffice =
                TeacherCreditsFillingForDepartmentAdmOfficeCE.getTeacherCreditsFillingForDepartmentAdmOffice(executionSemester
                        .getAcademicInterval());
        setBeginForDepartmentAdmOffice(departmentAdmOffice != null ? departmentAdmOffice.getBegin() : null);
        setEndForDepartmentAdmOffice(departmentAdmOffice != null ? departmentAdmOffice.getEnd() : null);

        TeacherCreditsFillingForTeacherCE teacherCE =
                TeacherCreditsFillingForTeacherCE.getTeacherCreditsFillingForTeacher(executionSemester.getAcademicInterval());
        setBeginForTeacher(teacherCE != null ? teacherCE.getBegin() : null);
        setEndForTeacher(teacherCE != null ? teacherCE.getEnd() : null);
        setAnnualCreditsState();
    }

    public boolean isTeacher() {
        return teacher;
    }

    public void setTeacher(boolean teacher) {
        this.teacher = teacher;
    }

    public AnnualCreditsState getAnnualCreditsState() {
        return annualCreditsState;
    }

    public void setAnnualCreditsState() {
        annualCreditsState = AnnualCreditsState.getAnnualCreditsState(getExecutionPeriod().getExecutionYear());
        setSharedUnitCreditsBeginDate(annualCreditsState.getSharedUnitCreditsInterval() != null ? annualCreditsState
                .getSharedUnitCreditsInterval().getStart() : null);
        setSharedUnitCreditsEndDate(annualCreditsState.getSharedUnitCreditsInterval() != null ? annualCreditsState
                .getSharedUnitCreditsInterval().getEnd() : null);
        setUnitCreditsBeginDate(annualCreditsState.getUnitCreditsInterval() != null ? annualCreditsState.getUnitCreditsInterval()
                .getStart() : null);
        setUnitCreditsEndDate(annualCreditsState.getUnitCreditsInterval() != null ? annualCreditsState.getUnitCreditsInterval()
                .getEnd() : null);
        setReductionServiceApprovalBeginDate(annualCreditsState.getReductionServiceApproval() != null ? annualCreditsState
                .getReductionServiceApproval().getStart() : null);
        setReductionServiceApprovalEndDate(annualCreditsState.getReductionServiceApproval() != null ? annualCreditsState
                .getReductionServiceApproval().getEnd() : null);
        setFinalCalculationDate(annualCreditsState.getFinalCalculationDate());
        setCloseCreditsDate(annualCreditsState.getCloseCreditsDate());
    }

    public DateTime getSharedUnitCreditsBeginDate() {
        return sharedUnitCreditsBeginDate;
    }

    public void setSharedUnitCreditsBeginDate(DateTime sharedUnitCreditsBeginDate) {
        this.sharedUnitCreditsBeginDate = sharedUnitCreditsBeginDate;
    }

    public DateTime getSharedUnitCreditsEndDate() {
        return sharedUnitCreditsEndDate;
    }

    public void setSharedUnitCreditsEndDate(DateTime sharedUnitCreditsEndDate) {
        this.sharedUnitCreditsEndDate = sharedUnitCreditsEndDate;
    }

    public DateTime getUnitCreditsBeginDate() {
        return unitCreditsBeginDate;
    }

    public void setUnitCreditsBeginDate(DateTime unitCreditsBeginDate) {
        this.unitCreditsBeginDate = unitCreditsBeginDate;
    }

    public DateTime getUnitCreditsEndDate() {
        return unitCreditsEndDate;
    }

    public void setUnitCreditsEndDate(DateTime unitCreditsEndDate) {
        this.unitCreditsEndDate = unitCreditsEndDate;
    }

    public DateTime getReductionServiceApprovalBeginDate() {
        return reductionServiceApprovalBeginDate;
    }

    public void setReductionServiceApprovalBeginDate(DateTime reductionServiceApprovalBeginDate) {
        this.reductionServiceApprovalBeginDate = reductionServiceApprovalBeginDate;
    }

    public DateTime getReductionServiceApprovalEndDate() {
        return reductionServiceApprovalEndDate;
    }

    public void setReductionServiceApprovalEndDate(DateTime reductionServiceApprovalEndDate) {
        this.reductionServiceApprovalEndDate = reductionServiceApprovalEndDate;
    }

    private Interval getSharedUnitCreditsInterval() {
        return getInterval(getSharedUnitCreditsBeginDate(), getSharedUnitCreditsEndDate());
    }

    private Interval getUnitCreditsInterval() {
        return getInterval(getUnitCreditsBeginDate(), getUnitCreditsEndDate());
    }

    private Interval getReductionServiceApprovalInterval() {
        return getInterval(getReductionServiceApprovalBeginDate(), getReductionServiceApprovalEndDate());
    }

    private Interval getInterval(DateTime start, DateTime end) {
        if (start == null && end == null) {
            return null;
        }
        try {
            return new Interval(start, end);
        } catch (IllegalArgumentException e) {
            throw new DomainException(e.getMessage());
        }
    }

    public LocalDate getFinalCalculationDate() {
        return finalCalculationDate;
    }

    public void setFinalCalculationDate(LocalDate finalCalculationDate) {
        this.finalCalculationDate = finalCalculationDate;
    }

    public LocalDate getCloseCreditsDate() {
        return closeCreditsDate;
    }

    public void setCloseCreditsDate(LocalDate closeCreditsDate) {
        this.closeCreditsDate = closeCreditsDate;
    }

    @Atomic
    public void editIntervals() {
        annualCreditsState.setSharedUnitCreditsInterval(getSharedUnitCreditsInterval());
        annualCreditsState.setUnitCreditsInterval(getUnitCreditsInterval());
        annualCreditsState.setReductionServiceApproval(getReductionServiceApprovalInterval());
        annualCreditsState.setCloseCreditsDate(getCloseCreditsDate());
        annualCreditsState.setFinalCalculationDate(getFinalCalculationDate());
        setAnnualCreditsState();
    }
}
