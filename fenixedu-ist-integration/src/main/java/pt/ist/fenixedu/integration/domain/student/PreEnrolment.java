package pt.ist.fenixedu.integration.domain.student;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.bennu.core.domain.User;

public class PreEnrolment extends PreEnrolment_Base {

    public PreEnrolment(User user, CurricularCourse curricularCourse, CourseGroup courseGroup, Degree degree,
            ExecutionSemester executionSemester) {
        super();
        setUser(user);
        setCurricularCourse(curricularCourse);
        setCourseGroup(courseGroup);
        setDegree(degree);
        setExecutionSemester(executionSemester);
    }
}
