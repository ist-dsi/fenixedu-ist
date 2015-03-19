package pt.ist.fenixedu.quc.dto;

import java.io.Serializable;
import java.util.Set;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionCourse;

import pt.ist.fenixedu.quc.domain.ExecutionCourseAudit;
import pt.ist.fenixedu.quc.domain.InquiriesRoot;
import pt.ist.fenixframework.Atomic;

public class ExecutionCourseBean implements Serializable {

    private static final long serialVersionUID = 1L;
    ExecutionCourse executionCourse;

    public ExecutionCourseBean(ExecutionCourse executionCourse) {
        super();
        this.executionCourse = executionCourse;
    }

    public Set<Department> getDepartments() {
        return executionCourse.getDepartments();
    }

    public String getNome() {
        return executionCourse.getNome();
    }

    public String getDegreePresentationString() {
        return executionCourse.getDegreePresentationString();
    }

    public boolean isExecutionCourseUnderAudition() {
        return executionCourse.getExecutionCourseAudit() != null;
    }

    public String getExternalId() {
        return executionCourse.getExternalId();
    }

    public ExecutionCourseAudit getExecutionCourseAudit() {
        return executionCourse.getExecutionCourseAudit();
    }

    public boolean isAvailableForInquiries() {
        return executionCourse.getAvailableForInquiries() != null;
    }

    @Atomic
    public void setAvailableForInquiries(boolean availableForInquiries) {
        if (!availableForInquiries) {
            executionCourse.setAvailableForInquiries(null);
        } else {
            executionCourse.setAvailableForInquiries(InquiriesRoot.getInstance());
        }
    }

}
