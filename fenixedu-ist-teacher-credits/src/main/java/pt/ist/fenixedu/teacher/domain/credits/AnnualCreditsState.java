/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Credits.
 *
 * FenixEdu IST Teacher Credits is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Credits is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Credits.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.teacher.domain.credits;

import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonProfessionalData;
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
                boolean isContractedTeacher = PersonProfessionalData.isTeacherActiveForSemester(teacher, executionSemester);
                if (isContractedTeacher || teacher.hasTeacherAuthorization(executionSemester.getAcademicInterval())) {
                    teachers.add(teacher);
                }
            }
        }
        return teachers;
    }

    @Override
    public void setFinalCalculationDate(LocalDate finalCalculationDate) {
        if (finalCalculationDate != null && finalCalculationDate.isBefore(new LocalDate())) {
            throw new DomainException("renderers.validator.dateTime.beforeNow");
        }
        if (finalCalculationDate == null || !finalCalculationDate.equals(getFinalCalculationDate())) {
            setIsFinalCreditsCalculated(false);
            super.setFinalCalculationDate(finalCalculationDate);
        }
    }
}
