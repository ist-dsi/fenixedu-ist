package pt.ist.fenixedu.integration.service.services.externalServices;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.bennu.core.domain.User;

import pt.ist.fenixedu.integration.domain.student.PreEnrolment;
import pt.ist.fenixframework.Atomic;

public class CreatePreEnrolment {

    @Atomic
    public static void create(ExecutionSemester executionSemester, Degree degree, User user, CurricularCourse curricularCourse,
            CourseGroup courseGroup) {
        new PreEnrolment(user, curricularCourse, courseGroup, degree, executionSemester);
    }
}
