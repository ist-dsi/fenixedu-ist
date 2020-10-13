/**
 * Copyright © 2013 Instituto Superior Técnico
 * <p>
 * This file is part of FenixEdu IST Integration.
 * <p>
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.domain.student;

import java.util.function.Predicate;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.OptionalEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.candidacy.StudentCandidacy;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.degreeStructure.OptionalCurricularCourse;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.PersonalIngressionData;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.Credits;
import org.fenixedu.academic.domain.studentCurriculum.CreditsDismissal;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroupFactory;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.studentCurriculum.EnrolmentWrapper;
import org.fenixedu.academic.domain.studentCurriculum.Equivalence;
import org.fenixedu.academic.domain.studentCurriculum.OptionalDismissal;
import org.fenixedu.academic.domain.studentCurriculum.Substitution;
import org.fenixedu.academic.domain.studentCurriculum.TemporarySubstitution;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

public class SeparationCyclesManagement {

    private static final Predicate<DegreeType> ACCEPTED_DEGREE_TYPES = DegreeType.oneOf(DegreeType::isBolonhaDegree,
            DegreeType::isIntegratedMasterDegree);

    public SeparationCyclesManagement() {
    }

    public Registration separateSecondCycle(final StudentCurricularPlan studentCurricularPlan) {
        checkIfCanSeparateSecondCycle(studentCurricularPlan);
        return createNewSecondCycle(studentCurricularPlan);
    }

    protected void checkIfCanSeparateSecondCycle(final StudentCurricularPlan studentCurricularPlan) {

        if (!studentCurricularPlan.isBolonhaDegree()) {
            throw new DomainException("error.SeparationCyclesManagement.not.bolonha.degree");
        }

        if (!studentCurricularPlan.isActive() && !studentCurricularPlan.getRegistration().isConcluded()) {
            throw new DomainException("error.SeparationCyclesManagement.not.active.or.concluded", studentCurricularPlan.getName());
        }

        if (!ACCEPTED_DEGREE_TYPES.test(studentCurricularPlan.getDegreeType())) {
            throw new DomainException("error.SeparationCyclesManagement.invalid.degreeType");
        }

        final CycleCurriculumGroup secondCycle = studentCurricularPlan.getSecondCycle();
        if (secondCycle == null || !secondCycle.isExternal()) {
            throw new DomainException("error.SeparationCyclesManagement.invalid.secondCycle");
        }

        final CycleCurriculumGroup firstCycle = studentCurricularPlan.getFirstCycle();
        if (firstCycle == null || !firstCycle.isConcluded()) {
            throw new DomainException("error.SeparationCyclesManagement.invalid.firstCycle");
        }

        if (studentAlreadyHasNewRegistration(studentCurricularPlan)) {
            final DegreeCurricularPlan degreeCurricularPlan = secondCycle.getDegreeCurricularPlanOfDegreeModule();
            throw new DomainException("error.SeparationCyclesManagement.already.has.registration", degreeCurricularPlan.getName());
        }
    }

    private boolean studentAlreadyHasNewRegistration(final StudentCurricularPlan studentCurricularPlan) {
        final Student student = studentCurricularPlan.getRegistration().getStudent();
        return student.getActiveRegistrationFor(studentCurricularPlan.getSecondCycle().getDegreeCurricularPlanOfDegreeModule())
                != null;
    }

    protected Registration createNewSecondCycle(final StudentCurricularPlan oldStudentCurricularPlan) {
        final Student student = oldStudentCurricularPlan.getRegistration().getStudent();
        final CycleCurriculumGroup oldSecondCycle = oldStudentCurricularPlan.getSecondCycle();
        final DegreeCurricularPlan degreeCurricularPlan = oldSecondCycle.getDegreeCurricularPlanOfDegreeModule();

        final Registration newRegistration = createRegistration(student, oldStudentCurricularPlan);
        final StudentCurricularPlan newStudentCurricularPlan =
                createStudentCurricularPlan(newRegistration, degreeCurricularPlan, oldSecondCycle.getCycleType());
        final CycleCurriculumGroup newSecondCycle = newStudentCurricularPlan.getSecondCycle();

        boolean isToMoveEnrolments = isToMove2ndCycle(oldStudentCurricularPlan);
        copyCycleCurriculumGroupsInformation(oldSecondCycle, newSecondCycle, isToMoveEnrolments);

        if (isToMoveEnrolments) {
            moveAttends(oldStudentCurricularPlan, newStudentCurricularPlan);
            tryRemoveOldSecondCycle(oldSecondCycle);
        }

        if (oldStudentCurricularPlan.getDegreeCurricularPlan().getDegreeType().isIntegratedMasterDegree()) {
            markOldRegistrationWithState(oldStudentCurricularPlan, RegistrationStateType.EXTERNAL_ABANDON);
        } else {
            markOldRegistrationWithState(oldStudentCurricularPlan, RegistrationStateType.CONCLUDED);
        }

        return newRegistration;
    }

    private boolean isToMove2ndCycle(final StudentCurricularPlan oldSCP) {        
        CycleCurriculumGroup secondCycle = oldSCP.getSecondCycle();
        if (secondCycle.hasEnrolment(ExecutionSemester.readActualExecutionSemester())
                && secondCycle.getNumberOfAllApprovedEnrolments(getCurrentExecutionPeriod()) == 0
                && secondCycle.getEnrolmentsBy(getCurrentExecutionPeriod()).size() == secondCycle.getEnrolments().size()) {
            return true;
        } else {
            return false;
        }           
    }
    
    private void moveAttends(final StudentCurricularPlan oldStudentCurricularPlan,
            final StudentCurricularPlan newStudentCurricularPlan) {
        oldStudentCurricularPlan.getRegistration().getAssociatedAttendsSet().stream()
            .filter(attend -> !belongsTo(oldStudentCurricularPlan, attend))
            .filter(attend -> isToMoveAttendsFrom(oldStudentCurricularPlan, newStudentCurricularPlan, attend))
            .filter(attend -> !newStudentCurricularPlan.getRegistration().attends(attend.getExecutionCourse()))
            .forEach(attend -> attend.setRegistration(newStudentCurricularPlan.getRegistration()));
        }
        
    private boolean belongsTo(final StudentCurricularPlan studentCurricularPlan, final Attends attend) {
        return attend.getExecutionCourse().getAssociatedCurricularCoursesSet().stream()
                .anyMatch(curricularCourse -> studentCurricularPlan.getDegreeCurricularPlan().hasDegreeModule(curricularCourse));
    }
    
    private boolean isToMoveAttendsFrom(final StudentCurricularPlan oldStudentCurricularPlan,
                      final StudentCurricularPlan newStudentCurricularPlan, final Attends attend) {
    
        if (attend.getEnrolment() != null) {
            return !oldStudentCurricularPlan.hasEnrolments(attend.getEnrolment())
                    && newStudentCurricularPlan.hasEnrolments(attend.getEnrolment());
        }        
        return !attend.getExecutionPeriod().isBefore(newStudentCurricularPlan.getStartExecutionPeriod());
    }

    private void moveEnrolment(final Enrolment enrolment, final CurriculumGroup parent) {
        final CurriculumModule child = parent.getChildCurriculumModule(enrolment.getDegreeModule());
        if (child != null && child.isEnrolment()) {
            final Enrolment childEnrolment = (Enrolment) child;
            if (childEnrolment.getExecutionPeriod() == enrolment.getExecutionPeriod()) {
                throw new DomainException("error.SeparationCyclesManagement.enrolment.should.not.exist.for.same.executionPeriod");
            }
        }

        final Registration registration = parent.getStudentCurricularPlan().getRegistration();
        enrolment.setCurriculumGroup(parent);

        for (final Attends attend : enrolment.getAttendsSet()) {
            if (!registration.attends(attend.getExecutionCourse())) {
                attend.setRegistration(registration);
            }
        }
    }
    
    private Registration createRegistration(final Student student, final StudentCurricularPlan sourceStudentCurricularPlan) {

        final CycleCurriculumGroup oldSecondCycle = sourceStudentCurricularPlan.getSecondCycle();
        Registration registration = student.getActiveRegistrationFor(oldSecondCycle.getDegreeCurricularPlanOfDegreeModule());

        if (registration != null) {
            return registration;
        }

        Degree degree = oldSecondCycle.getDegreeCurricularPlanOfDegreeModule().getDegree();
        registration = new Registration(student.getPerson(), student.getNumber(), degree);
        StudentCandidacy studentCandidacy = createStudentCandidacy(student, oldSecondCycle);
        registration.setStudentCandidacy(studentCandidacy);
        PersonalIngressionData personalIngressionData =
                student.getPersonalIngressionDataByExecutionYear(registration.getRegistrationYear());
        if (personalIngressionData == null) {
            new PersonalIngressionData(student, registration.getRegistrationYear(),
                    studentCandidacy.getPrecedentDegreeInformation());
        } else {
            personalIngressionData.addPrecedentDegreesInformations(studentCandidacy.getPrecedentDegreeInformation());
        }
        registration.addPrecedentDegreesInformations(studentCandidacy.getPrecedentDegreeInformation());

        registration.setStartDate(getBeginDate(sourceStudentCurricularPlan, getCurrentExecutionPeriod()));
        RegistrationState activeState = registration.getActiveState();
        activeState.setStateDate(getBeginDate(sourceStudentCurricularPlan, getCurrentExecutionPeriod()));
        activeState.setResponsiblePerson(null);
        registration.setSourceRegistration(sourceStudentCurricularPlan.getRegistration());
        registration.setRegistrationProtocol(sourceStudentCurricularPlan.getRegistration().getRegistrationProtocol());

        return registration;
    }

    private YearMonthDay getBeginDate(final StudentCurricularPlan sourceStudentCurricularPlan,
                                      final ExecutionSemester executionSemester) {

        if (!sourceStudentCurricularPlan.getFirstCycle().isConcluded()) {
            throw new DomainException("error.SeparationCyclesManagement.source.studentCurricularPlan.is.not.concluded");
        }

        final YearMonthDay conclusionDate = sourceStudentCurricularPlan.getFirstCycle().calculateConclusionDate();
        final YearMonthDay stateDate = conclusionDate != null ? conclusionDate.plusDays(1) : new YearMonthDay().plusDays(1);

        return executionSemester.getBeginDateYearMonthDay().isBefore(stateDate) ? stateDate : executionSemester
                .getBeginDateYearMonthDay();
    }

    private StudentCandidacy createStudentCandidacy(final Student student, final CycleCurriculumGroup oldSecondCycle) {
        final DegreeCurricularPlan dcp = oldSecondCycle.getDegreeCurricularPlanOfDegreeModule();
        return StudentCandidacy.createStudentCandidacy(dcp.getExecutionDegreeByYear(getCurrentExecutionYear()), student.getPerson());
    }

    private StudentCurricularPlan createStudentCurricularPlan(final Registration registration,
                                                              final DegreeCurricularPlan degreeCurricularPlan, CycleType cycleType) {

        StudentCurricularPlan result = registration.getStudentCurricularPlan(degreeCurricularPlan);
        if (result != null) {
            return result;
        }

        result =
                StudentCurricularPlan.createWithEmptyStructure(registration, degreeCurricularPlan, cycleType,
                        registration.getStartDate());

        // set ingression after create studentcurricularPlan
        registration.setIngressionType(IngressionType.findByPredicate(IngressionType::isDirectAccessFrom1stCycle).orElse(null));

        return result;
    }

    private void copyCycleCurriculumGroupsInformation(final CycleCurriculumGroup oldSecondCycle,
                                                      final CycleCurriculumGroup newSecondCycle, boolean isToMoveEnrolments) {
        for (final CurriculumModule curriculumModule : oldSecondCycle.getCurriculumModulesSet()) {
            if (curriculumModule.isLeaf()) {
                copyCurricumLineInformation((CurriculumLine) curriculumModule, newSecondCycle, isToMoveEnrolments);
            } else {
                copyCurriculumGroupsInformation((CurriculumGroup) curriculumModule, newSecondCycle, isToMoveEnrolments);
            }
        }
    }

    private void copyCurriculumGroupsInformation(final CurriculumGroup source, final CurriculumGroup parent, boolean isToMoveEnrolments) {
        final CurriculumGroup destination;
        //test if source group still exists as part of destination DCP
        if (!groupIsStillValid(source)) {
            return;
        }
        if (parent.hasChildDegreeModule(source.getDegreeModule())) {
            destination = (CurriculumGroup) parent.getChildCurriculumModule(source.getDegreeModule());
        } else {
            destination = CurriculumGroupFactory.createGroup(parent, source.getDegreeModule());
        }

        for (final CurriculumModule curriculumModule : source.getCurriculumModulesSet()) {
            if (curriculumModule.isLeaf()) {
                copyCurricumLineInformation((CurriculumLine) curriculumModule, destination, isToMoveEnrolments);
            } else {
                copyCurriculumGroupsInformation((CurriculumGroup) curriculumModule, destination, isToMoveEnrolments);
            }
        }
    }

    private boolean groupIsStillValid(CurriculumGroup source) {
        ExecutionYear nowadays = ExecutionYear.readCurrentExecutionYear();
        if (source.getDegreeModule().getValidChildContexts(nowadays).size() > 0) {
            return true;
        }
        return source.getChildCurriculumGroups().stream().anyMatch(this::groupIsStillValid);
    }

    private void copyCurricumLineInformation(final CurriculumLine curriculumLine, final CurriculumGroup parent, boolean isToMoveEnrolments) {
        if (curriculumLine.isEnrolment()) {
            final Enrolment enrolment = (Enrolment) curriculumLine;
            if (enrolment.isApproved()) {
                createSubstitutionForEnrolment((Enrolment) curriculumLine, parent);
            } else if (isToMoveEnrolments) {
                moveEnrolment((Enrolment) curriculumLine, parent);
            }
        } else if (curriculumLine.isDismissal()) {
            createDismissal((Dismissal) curriculumLine, parent);
        } else {
            throw new DomainException("error.unknown.curriculumLine");
        }
    }

    private void tryRemoveOldSecondCycle(final CycleCurriculumGroup oldSecondCycle) {
        if (canRemoveOldSecondCycle(oldSecondCycle)) {
            deleteCurriculumModules(oldSecondCycle);
        }
    }

    protected void deleteCurriculumModules(final CurriculumModule curriculumModule) {
        if (curriculumModule == null) {
            return;
        }
        if (!curriculumModule.isLeaf()) {
            final CurriculumGroup curriculumGroup = (CurriculumGroup) curriculumModule;
            for (; !curriculumGroup.getCurriculumModulesSet().isEmpty();) {
                deleteCurriculumModules(curriculumGroup.getCurriculumModulesSet().iterator().next());
            }
            curriculumGroup.delete();
        } else if (curriculumModule.isDismissal()) {
            curriculumModule.delete();
        } else {
            throw new DomainException("error.can.only.remove.groups.and.dismissals");
        }
    }

    private boolean canRemoveOldSecondCycle(final CycleCurriculumGroup oldSecondCycle) {
        for (final CurriculumLine curriculumLine : oldSecondCycle.getAllCurriculumLines()) {
            if (curriculumLine.isEnrolment()) {
                return false;
            } else if (!curriculumLine.isDismissal()) {
                throw new DomainException("error.unknown.curriculum.line");
            }
        }
        return true;
    }
    
    private void createSubstitutionForEnrolment(final Enrolment enrolment, final CurriculumGroup parent) {
        if (enrolment.getUsedInSeparationCycle() || parent.hasChildDegreeModule(enrolment.getDegreeModule())) {
            // TODO: temporary
            enrolment.setUsedInSeparationCycle(true);
            return;
        }

        enrolment.setUsedInSeparationCycle(true);

        if (enrolment.isOptional()) {
            final OptionalEnrolment optional = (OptionalEnrolment) enrolment;
            if (parent.hasChildDegreeModule(optional.getOptionalCurricularCourse())) {
                return;
            }
            final Substitution substitution = createSubstitution(enrolment, parent);
            createNewOptionalDismissal(substitution, parent, enrolment, optional.getOptionalCurricularCourse(),
                    optional.getEctsCredits());
        } else {
            createNewDismissal(createSubstitution(enrolment, parent), parent, enrolment);
        }
    }

    private Substitution createSubstitution(final Enrolment enrolment, final CurriculumGroup parent) {
        final Substitution substitution = new Substitution();
        substitution.setStudentCurricularPlan(parent.getStudentCurricularPlan());
        substitution.setExecutionPeriod(getCurrentExecutionPeriod());
        EnrolmentWrapper.create(substitution, enrolment);
        return substitution;
    }

    private Dismissal createNewDismissal(final Credits credits, final CurriculumGroup parent, final CurriculumLine curriculumLine) {

        final CurricularCourse curricularCourse = curriculumLine.getCurricularCourse();

        if (!hasCurricularCourseToDismissal(parent, curricularCourse) && !hasResponsibleForCreation(curriculumLine)) {
            throw new DomainException("error.SeparationCyclesManagement.parent.doesnot.have.curricularCourse.to.dismissal");
        }

        final Dismissal dismissal = new Dismissal();
        dismissal.setCredits(credits);
        dismissal.setCurriculumGroup(parent);
        dismissal.setCurricularCourse(curricularCourse);

        return dismissal;
    }

    private OptionalDismissal createNewOptionalDismissal(final Credits credits, final CurriculumGroup parent,
                                                         final CurriculumLine curriculumLine, final OptionalCurricularCourse curricularCourse, final Double ectsCredits) {

        if (ectsCredits == null || ectsCredits == 0) {
            throw new DomainException("error.OptionalDismissal.invalid.credits");
        }

        if (!hasCurricularCourseToDismissal(parent, curricularCourse) && !hasResponsibleForCreation(curriculumLine)) {
            throw new DomainException("error.SeparationCyclesManagement.parent.doesnot.have.curricularCourse.to.dismissal");
        }

        final OptionalDismissal dismissal = new OptionalDismissal();
        dismissal.setCredits(credits);
        dismissal.setCurriculumGroup(parent);
        dismissal.setCurricularCourse(curricularCourse);
        dismissal.setEctsCredits(ectsCredits);

        return dismissal;
    }

    private boolean hasResponsibleForCreation(final CurriculumLine line) {
        return line.hasCreatedBy();
    }

    private boolean hasCurricularCourseToDismissal(final CurriculumGroup curriculumGroup, final CurricularCourse curricularCourse) {
        final CourseGroup degreeModule = curriculumGroup.getDegreeModule();
        return degreeModule.getChildContexts(CurricularCourse.class).stream()
                .map(context -> (CurricularCourse) context.getChildDegreeModule())
                .anyMatch(each -> each.isEquivalent(curricularCourse) && !curriculumGroup.hasChildDegreeModule(degreeModule));
    }

    private void createDismissal(final Dismissal dismissal, final CurriculumGroup parent) {
        if (dismissal.getUsedInSeparationCycle() || curriculumGroupHasSimilarDismissal(parent, dismissal)) {
            // TODO: temporary
            dismissal.setUsedInSeparationCycle(true);
            return;
        }

        dismissal.setUsedInSeparationCycle(true);
        final Credits credits = dismissal.getCredits();

        final Credits newCredits;
        if (credits.isTemporary()) {
            newCredits = new TemporarySubstitution();

        } else if (credits.isSubstitution()) {
            newCredits = new Substitution();

        } else if (credits.isEquivalence()) {
            final Equivalence equivalence = (Equivalence) credits;
            final Equivalence newEquivalence = new Equivalence();
            newEquivalence.setGrade(equivalence.getGrade());
            newCredits = newEquivalence;

        } else {
            newCredits = new Credits();
        }

        newCredits.setStudentCurricularPlan(parent.getStudentCurricularPlan());
        newCredits.setExecutionPeriod(getCurrentExecutionPeriod());
        newCredits.setGivenCredits(credits.getGivenCredits());

        for (final IEnrolment enrolment : credits.getIEnrolments()) {
            EnrolmentWrapper.create(newCredits, enrolment);
        }

        if (dismissal.hasCurricularCourse()) {
            if (dismissal instanceof OptionalDismissal) {
                final OptionalDismissal optionalDismissal = (OptionalDismissal) dismissal;
                createNewOptionalDismissal(newCredits, parent, dismissal, optionalDismissal.getCurricularCourse(),
                        optionalDismissal.getEctsCredits());

            } else {
                createNewDismissal(newCredits, parent, dismissal);
            }
        } else if (dismissal.isCreditsDismissal()) {
            final CreditsDismissal creditsDismissal = (CreditsDismissal) dismissal;
            new CreditsDismissal(newCredits, parent, creditsDismissal.getNoEnrolCurricularCoursesSet());
        } else {
            throw new DomainException("error.unknown.dismissal.type");
        }
    }

    private boolean curriculumGroupHasSimilarDismissal(final CurriculumGroup curriculumGroup, final Dismissal dismissal) {
        return curriculumGroup.getChildDismissals().stream().anyMatch(each -> each.isSimilar(dismissal));
    }

    private void markOldRegistrationWithState(final StudentCurricularPlan oldStudentCurricularPlan, RegistrationStateType
            stateType) {
        if (oldStudentCurricularPlan.getRegistration().hasState(stateType)) {
            return;
        }

        LocalDate stateDate = new LocalDate();
        if (stateDate.isAfter(getCurrentExecutionYear().getEndDateYearMonthDay())) {
            stateDate = getCurrentExecutionYear().getEndDateYearMonthDay().toLocalDate();
        }

        final RegistrationState state =
                RegistrationState.createRegistrationState(oldStudentCurricularPlan.getRegistration(), null,
                        stateDate.toDateTimeAtStartOfDay(), stateType);
        state.setResponsiblePerson(null);
    }

    protected ExecutionSemester getCurrentExecutionPeriod() {
        return ExecutionSemester.readActualExecutionSemester();
    }

    private ExecutionYear getCurrentExecutionYear() {
        return getCurrentExecutionPeriod().getExecutionYear();
    }

}
