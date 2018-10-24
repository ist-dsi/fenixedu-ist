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
package pt.ist.fenixedu.teacher.evaluation.domain.credits;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.teacher.evaluation.domain.time.calendarStructure.TeacherCreditsFillingForTeacherCE;
import pt.ist.fenixframework.Atomic;

public class AnnualCreditsState extends AnnualCreditsState_Base {

    public AnnualCreditsState(ExecutionYear executionYear) {
        super();
        setExecutionYear(executionYear);
        setOrientationsCalculationDate(new LocalDate(executionYear.getBeginCivilYear(), 12, 31));
        setIsOrientationsCalculated(false);
        setIsFinalCreditsCalculated(false);
        setIsCreditsClosed(false);
        setCreationDate(new DateTime());
        setRootDomainObject(Bennu.getInstance());
    }

    @Atomic
    public static AnnualCreditsState getAnnualCreditsState(ExecutionYear executionYear) {
        AnnualCreditsState annualCreditsState = executionYear.getAnnualCreditsState();
        if (annualCreditsState == null) {
            annualCreditsState = new AnnualCreditsState(executionYear);
        }
        return annualCreditsState;
    }

    @Atomic
    public static void closeAnnualCredits(ExecutionYear executionYear) {
        AnnualCreditsState annualCreditsState = getAnnualCreditsState(executionYear);
        if (!annualCreditsState.getIsCreditsClosed()) {
            if (!annualCreditsState.getIsFinalCreditsCalculated()) {
                calculateAnnualCredits(executionYear);
            }
            annualCreditsState.setIsCreditsClosed(true);
        }
    }

    @Atomic
    public static void calculateAnnualCredits(ExecutionYear executionYear) {
        AnnualCreditsState annualCreditsState = getAnnualCreditsState(executionYear);
        if (!annualCreditsState.getIsCreditsClosed()) {
            Set<Teacher> teachers = annualCreditsState.getThisYearTeachers();
            for (Teacher teacher : teachers) {
                AnnualTeachingCredits annualTeachingCredits = AnnualTeachingCredits.readByYearAndTeacher(executionYear, teacher);
                if (annualTeachingCredits == null) {
                    annualTeachingCredits = new AnnualTeachingCredits(teacher, annualCreditsState);
                }

                annualTeachingCredits.calculateCredits();
            }
            annualCreditsState.setIsFinalCreditsCalculated(true);
        }
    }

    private Set<Teacher> getThisYearTeachers() {
        Set<Teacher> teachers = new HashSet<Teacher>();
        Set<Teacher> allTeachers = Bennu.getInstance().getTeachersSet();
        for (ExecutionSemester executionSemester : getExecutionYear().getExecutionPeriodsSet()) {
            for (Teacher teacher : allTeachers) {
                if (teacher.hasTeacherAuthorization(executionSemester.getAcademicInterval())) {
                    teachers.add(teacher);
                }
            }
        }
        return teachers;
    }

    @Override
    public void setFinalCalculationDate(LocalDate finalCalculationDate) {
        if (finalCalculationDate != null && !finalCalculationDate.equals(getFinalCalculationDate())
                && finalCalculationDate.isBefore(new LocalDate())) {
            throw new DomainException("renderers.validator.dateTime.beforeNow");
        }
        if (finalCalculationDate == null || !finalCalculationDate.equals(getFinalCalculationDate())) {
            setIsFinalCreditsCalculated(false);
            super.setFinalCalculationDate(finalCalculationDate);
        }
    }

    public static ExecutionYear getExecutionYearForValidReductionServiceApprovalPeriod() {
        return Bennu.getInstance().getAnnualCreditsStatesSet().stream()
                .filter(acs -> acs.getReductionServiceApproval() != null && acs.getReductionServiceApproval().containsNow())
                .map(acs -> acs.getExecutionYear()).findFirst().orElse(null);
    }

    public static boolean isInValidReductionServiceApprovalPeriod(ExecutionYear executionYear) {
        AnnualCreditsState annualCreditsState = getAnnualCreditsState(executionYear);
        return annualCreditsState.getReductionServiceApproval() != null
                && annualCreditsState.getReductionServiceApproval().containsNow();
    }

    public static boolean isInValidTeacherFillingPeriod(ExecutionYear executionYear) {
        for (ExecutionSemester executionSemester : executionYear.getExecutionPeriodsSet()) {
            TeacherCreditsFillingForTeacherCE teacherCreditsFillingForTeacher =
                    TeacherCreditsFillingForTeacherCE.getTeacherCreditsFillingForTeacher(executionSemester.getAcademicInterval());
            if (teacherCreditsFillingForTeacher != null && teacherCreditsFillingForTeacher.containsNow()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInValidCreditsPeriod(ExecutionSemester executionSemester) {
        TeacherCreditsFillingForTeacherCE teacherCreditsFillingForTeacher =
                TeacherCreditsFillingForTeacherCE.getTeacherCreditsFillingForTeacher(executionSemester.getAcademicInterval());
        return teacherCreditsFillingForTeacher != null && teacherCreditsFillingForTeacher.containsNow();
    }

    public static AnnualCreditsState getLastClosedAnnualCreditsState() {
        return Bennu.getInstance().getAnnualCreditsStatesSet().stream().filter(acs -> acs.getIsCreditsClosed())
                .sorted(Comparator.comparing(AnnualCreditsState::getExecutionYear).reversed()).findFirst().orElse(null);
    }

}
