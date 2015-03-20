/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Tutorship.
 *
 * FenixEdu IST Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.service.coordinator;

import java.util.ArrayList;
import java.util.List;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.service.services.exceptions.NotAuthorizedException;

import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixedu.tutorship.dto.coordinator.tutor.StudentsByEntryYearBean;
import pt.ist.fenixedu.tutorship.dto.coordinator.tutor.TutorshipErrorBean;
import pt.ist.fenixedu.tutorship.dto.coordinator.tutor.TutorshipManagementBean;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class InsertTutorship extends TutorshipManagement {

    protected void run(String executionDegreeID, TutorshipManagementBean bean) throws FenixServiceException {

        final Integer studentNumber = bean.getStudentNumber();
        final Teacher teacher = bean.getTeacher();
        final ExecutionDegree executionDegree = FenixFramework.getDomainObject(executionDegreeID);
        final DegreeCurricularPlan degreeCurricularPlan = executionDegree.getDegreeCurricularPlan();

        validateTeacher(teacher, executionDegree);

        Registration registration =
                Registration.readRegisteredRegistrationByNumberAndDegreeType(studentNumber, executionDegree
                        .getDegreeCurricularPlan().getDegreeType());

        validateStudentRegistration(registration, executionDegree, degreeCurricularPlan, studentNumber);

        validateTutorship(registration);

        Tutorship.createTutorship(teacher, registration.getActiveStudentCurricularPlan(), bean.getTutorshipEndMonth()
                .getNumberOfMonth(), bean.getTutorshipEndYear());
    }

    public List<TutorshipErrorBean> run(String executionDegreeID, StudentsByEntryYearBean bean) throws FenixServiceException {

        final List<StudentCurricularPlan> students = bean.getStudentsToCreateTutorshipList();
        final ExecutionDegree executionDegree = FenixFramework.getDomainObject(executionDegreeID);
        final DegreeCurricularPlan degreeCurricularPlan = executionDegree.getDegreeCurricularPlan();
        final Teacher teacher = bean.getTeacher();

        validateTeacher(teacher, executionDegree);

        List<TutorshipErrorBean> studentsWithErrors = new ArrayList<TutorshipErrorBean>();

        for (StudentCurricularPlan studentCurricularPlan : students) {
            Registration registration = studentCurricularPlan.getRegistration();
            Integer studentNumber = registration.getNumber();

            if (!registration.isActive() || !studentCurricularPlan.isLastStudentCurricularPlanFromRegistration()) {
                studentsWithErrors.add(new TutorshipErrorBean("error.tutor.notActiveRegistration", new String[] { Integer
                        .toString(studentNumber) }));
                continue;
            }

            try {
                validateStudentRegistration(registration, executionDegree, degreeCurricularPlan, studentNumber);

                validateTutorship(registration);

                Tutorship.createTutorship(teacher, studentCurricularPlan, bean.getTutorshipEndMonth().getNumberOfMonth(),
                        bean.getTutorshipEndYear());

            } catch (FenixServiceException ex) {
                studentsWithErrors.add(new TutorshipErrorBean(ex.getMessage(), ex.getArgs()));
            } catch (DomainException ex) {
                studentsWithErrors.add(new TutorshipErrorBean(ex.getMessage(), ex.getArgs()));
            }
        }

        return studentsWithErrors;
    }

    // Service Invokers migrated from Berserk

    private static final InsertTutorship serviceInstance = new InsertTutorship();

    @Atomic
    public static List<TutorshipErrorBean> runInsertTutorship(String executionDegreeID, StudentsByEntryYearBean bean)
            throws FenixServiceException, NotAuthorizedException {
        return serviceInstance.run(executionDegreeID, bean);
    }

}
