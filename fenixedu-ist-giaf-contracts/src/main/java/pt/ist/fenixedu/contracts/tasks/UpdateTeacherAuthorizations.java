/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Contracts.
 *
 * FenixEdu IST GIAF Contracts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Contracts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Contracts.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.contracts.tasks;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.TeacherAuthorization;
import org.fenixedu.academic.domain.TeacherCategory;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.DepartmentUnit;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.PeriodType;

import pt.ist.fenixedu.contracts.domain.organizationalStructure.EmployeeContract;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.GiafProfessionalData;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.PersonContractSituation;
import pt.ist.fenixedu.contracts.domain.personnelSection.contracts.ProfessionalCategory;
import pt.ist.fenixedu.contracts.domain.util.CategoryType;

@Task(englishTitle = "UpdateTeacherAuthorizations")
public class UpdateTeacherAuthorizations extends CronTask {

    private static final int minimumDaysForActivity = 90;

    @Override
    public void runTask() {
        for (ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear(); executionYear != null; executionYear =
                executionYear.getNextExecutionYear()) {
            taskLog(executionYear.getQualifiedName());
            executionYear
                    .getExecutionPeriodsSet()
                    .stream()
                    .sorted(ExecutionSemester.COMPARATOR_BY_SEMESTER_AND_YEAR)
                    .filter(executionSemester -> executionSemester.getTeacherAuthorizationStream().findAny().isPresent())
                    .forEach(
                            executionSemester -> {
                                int countNew = 0;
                                int countRevoked = 0;
                                int countEdited = 0;
                                Interval semesterInterval = executionSemester.getAcademicInterval().toInterval();
                                for (GiafProfessionalData giafProfessionalData : Bennu.getInstance().getGiafProfessionalDataSet()) {
                                    Person person = giafProfessionalData.getPersonProfessionalData().getPerson();
                                    if (person != null) {
                                        Department department = getDominantDepartment(person, executionSemester);
                                        TeacherAuthorization teacherAuthorization = null;
                                        if (department != null) {
                                            SortedSet<PersonContractSituation> validPersonContractSituations =
                                                    new TreeSet<PersonContractSituation>(
                                                            new Comparator<PersonContractSituation>() {
                                                                @Override
                                                                public int compare(PersonContractSituation c1,
                                                                        PersonContractSituation c2) {
                                                                    int compare = c1.getBeginDate().compareTo(c2.getBeginDate());
                                                                    return compare == 0 ? c1.getExternalId().compareTo(
                                                                            c2.getExternalId()) : compare;
                                                                }
                                                            });
                                            validPersonContractSituations.addAll(giafProfessionalData
                                                    .getValidPersonContractSituations()
                                                    .stream()
                                                    .filter(pcs -> pcs.overlaps(semesterInterval))
                                                    .filter(pcs -> {
                                                        ProfessionalCategory professionalCategory = pcs.getProfessionalCategory();
                                                        return professionalCategory != null
                                                                && professionalCategory.getCategoryType() != null
                                                                && professionalCategory.getCategoryType().equals(
                                                                        CategoryType.TEACHER);
                                                    }).filter(Objects::nonNull).collect(Collectors.toSet()));
                                            int activeDays =
                                                    validPersonContractSituations.stream()
                                                            .mapToInt(s -> getActiveDays(s, semesterInterval)).sum();

                                            if (activeDays >= minimumDaysForActivity) {
                                                PersonContractSituation situation =
                                                        getDominantSituation(validPersonContractSituations, semesterInterval);
                                                if (situation != null) {
                                                    Teacher teacher = person.getTeacher();
                                                    if (person.getTeacher() == null) {
                                                        teacher = new Teacher(person);
                                                    }
                                                    TeacherCategory teacherCategory =
                                                            situation.getProfessionalCategory().getTeacherCategory();
                                                    Double lessonHours = situation.getWeeklyLessonHours(semesterInterval);
                                                    TeacherAuthorization existing =
                                                            teacher.getTeacherAuthorization(
                                                                    executionSemester.getAcademicInterval()).orElse(null);
                                                    if (existing != null) {
                                                        if (existing.getDepartment().equals(department)
                                                                && existing.isContracted()
                                                                && existing.getLessonHours().equals(lessonHours)
                                                                && existing.getTeacherCategory().equals(teacherCategory)) {
                                                            teacherAuthorization = existing;
                                                        } else {
                                                            countEdited++;
                                                            existing.revoke();
                                                        }
                                                    } else {
                                                        countNew++;
                                                    }
                                                    if (teacherAuthorization == null) {
                                                        teacherAuthorization =
                                                                TeacherAuthorization.createOrUpdate(teacher, department,
                                                                        executionSemester, teacherCategory, true, lessonHours);
                                                    }
                                                }
                                            }
                                        }
                                        if (teacherAuthorization == null && person.getTeacher() != null) {
                                            teacherAuthorization =
                                                    person.getTeacher()
                                                            .getTeacherAuthorization(executionSemester.getAcademicInterval())
                                                            .orElse(null);
                                            if (teacherAuthorization != null && teacherAuthorization.isContracted()) {
                                                teacherAuthorization.revoke();
                                                countRevoked++;
                                            }
                                        }

                                    }

                                }
                                taskLog(countNew + " authorizations created for semester " + executionSemester.getQualifiedName());
                                taskLog(countEdited + " authorizations edited for semester "
                                        + executionSemester.getQualifiedName());
                                taskLog(countRevoked + " authorizations revoked for semester "
                                        + executionSemester.getQualifiedName());
                            });

        }
    }

    private PersonContractSituation getDominantSituation(SortedSet<PersonContractSituation> personContractSituations,
            Interval semesterInterval) {
        for (PersonContractSituation situation : personContractSituations) {
            int activeDays = getActiveDays(situation, semesterInterval);
            if (activeDays > minimumDaysForActivity) {
                return situation;
            }
        }
        return personContractSituations.first();
    }

    private int getActiveDays(PersonContractSituation situation, Interval semesterInterval) {
        LocalDate beginDate =
                situation.getBeginDate().isBefore(semesterInterval.getStart().toLocalDate()) ? semesterInterval.getStart()
                        .toLocalDate() : situation.getBeginDate();
        LocalDate endDate =
                situation.getEndDate() == null || situation.getEndDate().isAfter(semesterInterval.getEnd().toLocalDate()) ? semesterInterval
                        .getEnd().toLocalDate() : situation.getEndDate();

        int activeDays =
                new Interval(beginDate.toDateTimeAtStartOfDay(), endDate.toDateTimeAtStartOfDay()).toPeriod(PeriodType.days())
                        .getDays() + 1;
        return activeDays;
    }

    private Department getDominantDepartment(Person person, ExecutionSemester semester) {
        SortedSet<EmployeeContract> contracts = new TreeSet<EmployeeContract>(new Comparator<EmployeeContract>() {
            @Override
            public int compare(EmployeeContract ec1, EmployeeContract ec2) {
                int compare = ec1.getBeginDate().compareTo(ec2.getBeginDate());
                return compare == 0 ? ec1.getExternalId().compareTo(ec2.getExternalId()) : compare;
            }
        });
        Interval semesterInterval = semester.getAcademicInterval().toInterval();
        contracts.addAll(((Collection<EmployeeContract>) person.getParentAccountabilities(
                AccountabilityTypeEnum.WORKING_CONTRACT, EmployeeContract.class))
                .stream()
                .filter(ec -> ec.belongsToPeriod(semesterInterval.getStart().toYearMonthDay(), semesterInterval.getEnd()
                        .toYearMonthDay())).filter(Objects::nonNull).collect(Collectors.toSet()));

        Department firstDepartmentUnit = null;
        for (EmployeeContract employeeContract : contracts) {
            Department employeeDepartmentUnit = getEmployeeDepartmentUnit(employeeContract.getUnit());
            if (employeeDepartmentUnit != null) {
                Interval contractInterval =
                        new Interval(employeeContract.getBeginDate().toLocalDate().toDateTimeAtStartOfDay(),
                                employeeContract.getEndDate() == null ? new DateTime(Long.MAX_VALUE) : employeeContract
                                        .getEndDate().toLocalDate().toDateTimeAtStartOfDay().plusMillis(1));
                Interval overlap = semesterInterval.overlap(contractInterval);
                if (overlap != null) {
                    int days = overlap.toPeriod(PeriodType.days()).getDays() + 1;
                    if (days > minimumDaysForActivity) {
                        return employeeDepartmentUnit;
                    }
                    if (firstDepartmentUnit == null) {
                        firstDepartmentUnit = employeeDepartmentUnit;
                    }
                }
            }
        }
        return firstDepartmentUnit;
    }

    private Department getEmployeeDepartmentUnit(Unit unit) {
        Collection<Unit> parentUnits = unit.getParentUnits();
        if (unitDepartment(unit)) {
            return ((DepartmentUnit) unit).getDepartment();
        } else if (!parentUnits.isEmpty()) {
            for (Unit parentUnit : parentUnits) {
                if (unitDepartment(parentUnit)) {
                    return ((DepartmentUnit) parentUnit).getDepartment();
                } else if (parentUnit.hasAnyParentUnits()) {
                    Department department = getEmployeeDepartmentUnit(parentUnit);
                    if (department != null) {
                        return department;
                    }
                }
            }
        }
        return null;
    }

    private boolean unitDepartment(Unit unit) {
        return unit.isDepartmentUnit() && ((DepartmentUnit) unit).getDepartment() != null;
    }
}
