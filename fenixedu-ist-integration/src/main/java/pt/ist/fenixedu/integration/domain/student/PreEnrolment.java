package pt.ist.fenixedu.integration.domain.student;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.User;

public class PreEnrolment extends PreEnrolment_Base {

    public PreEnrolment(User user, CurricularCourse curricularCourse, CourseGroup courseGroup, Degree degree,
            ExecutionSemester executionSemester) {
        super();
        checkParameters(user, curricularCourse, courseGroup, degree, executionSemester);
        validateDuplicate(user, curricularCourse, courseGroup, degree, executionSemester);
        setUser(user);
        setCurricularCourse(curricularCourse);
        setCourseGroup(courseGroup);
        setDegree(degree);
        setExecutionSemester(executionSemester);
    }

    private void checkParameters(User user, CurricularCourse curricularCourse, CourseGroup courseGroup, Degree degree,
            ExecutionSemester executionSemester) {
        if (user == null) {
            throw new DomainException("error.preEnrolment.user.cannot.be.null");
        }
        if (curricularCourse == null) {
            throw new DomainException("error.preEnrolment.curricularCourse.cannot.be.null");
        }
        if (courseGroup == null) {
            throw new DomainException("error.preEnrolment.courseGroup.cannot.be.null");
        }
        if (degree == null) {
            throw new DomainException("error.preEnrolment.degree.cannot.be.null");
        }
        if (executionSemester == null) {
            throw new DomainException("error.preEnrolment.executionSemester.cannot.be.null");
        }
    }

    private void validateDuplicate(User user, CurricularCourse curricularCourse, CourseGroup courseGroup, Degree degree,
            ExecutionSemester executionSemester) {
        if (user.getPreEnrolmentsSet().stream()
                .anyMatch(pe -> pe.getCourseGroup() == courseGroup && pe.getCurricularCourse() == curricularCourse
                        && pe.getDegree() == degree && pe.getExecutionSemester() == executionSemester)) {
            throw new DomainException("error.preEnrolment.alreadyExists");
        }
    }

    public void delete() {
        setCourseGroup(null);
        setCurricularCourse(null);
        setDegree(null);
        setExecutionSemester(null);
        setUser(null);
        super.deleteDomainObject();
    }
}
