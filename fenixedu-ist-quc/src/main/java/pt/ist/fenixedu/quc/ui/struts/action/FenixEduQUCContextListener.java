package pt.ist.fenixedu.quc.ui.struts.action;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.service.filter.enrollment.ClassEnrollmentAuthorizationFilter;
import org.fenixedu.academic.service.services.manager.MergeExecutionCourses;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixedu.quc.domain.InquiryCourseAnswer;
import pt.ist.fenixedu.quc.domain.InquiryDelegateAnswer;
import pt.ist.fenixedu.quc.domain.InquiryGlobalComment;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.StudentInquiryRegistry;
import pt.ist.fenixedu.quc.domain.exceptions.FenixEduQucDomainException;
import pt.ist.fenixframework.FenixFramework;

@WebListener
public class FenixEduQUCContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        MergeExecutionCourses.registerMergeHandler(FenixEduQUCContextListener::copyInquiries);
        ClassEnrollmentAuthorizationFilter.registerCondition(registration -> {
            if (StudentInquiryRegistry.hasInquiriesToRespond(registration.getStudent())) {
                throw FenixEduQucDomainException.inquiriesNotAnswered();
            }
        });
        FenixFramework.getDomainModel()
                .registerDeletionBlockerListener(
                        Professorship.class,
                        (professorship, blockers) -> {
                            if (!professorship.getInquiryStudentTeacherAnswersSet().isEmpty()) {
                                blockers.add(BundleUtil.getString(Bundle.APPLICATION,
                                        "error.remove.professorship.hasAnyInquiryStudentTeacherAnswers"));
                            }
                            if (!professorship.getInquiryResultsSet().isEmpty()) {
                                blockers.add(BundleUtil.getString(Bundle.APPLICATION,
                                        "error.remove.professorship.hasAnyInquiryResults"));
                            }
                            if (professorship.getInquiryTeacherAnswer() != null) {
                                blockers.add(BundleUtil.getString(Bundle.APPLICATION,
                                        "error.remove.professorship.hasInquiryTeacherAnswer"));
                            }
                            if (professorship.getInquiryRegentAnswer() != null) {
                                blockers.add(BundleUtil.getString(Bundle.APPLICATION,
                                        "error.remove.professorship.hasInquiryRegentAnswer"));
                            }
                        });
        FenixFramework.getDomainModel().registerDeletionListener(ExecutionCourse.class, executionCourse -> {
            executionCourse.setAvailableForInquiries(null);
            if (executionCourse.getExecutionCourseAudit() != null) {
                executionCourse.getExecutionCourseAudit().delete();
            }
        });
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    private static void copyInquiries(final ExecutionCourse executionCourseFrom, final ExecutionCourse executionCourseTo) {
        for (final StudentInquiryRegistry studentInquiryRegistry : executionCourseFrom.getStudentsInquiryRegistriesSet()) {
            studentInquiryRegistry.setExecutionCourse(executionCourseTo);
        }
        for (final InquiryResult inquiryResult : executionCourseFrom.getInquiryResultsSet()) {
            inquiryResult.setExecutionCourse(executionCourseTo);
        }
        for (final InquiryCourseAnswer inquiryCourseAnswer : executionCourseFrom.getInquiryCourseAnswersSet()) {
            inquiryCourseAnswer.setExecutionCourse(executionCourseTo);
        }
        for (final InquiryDelegateAnswer inquiryDelegateAnswer : executionCourseFrom.getInquiryDelegatesAnswersSet()) {
            inquiryDelegateAnswer.setExecutionCourse(executionCourseTo);
        }
        for (final InquiryGlobalComment inquiryGlobalComment : executionCourseFrom.getInquiryGlobalCommentsSet()) {
            inquiryGlobalComment.setExecutionCourse(executionCourseTo);
        }
        if (executionCourseFrom.getExecutionCourseAudit() != null && executionCourseTo.getExecutionCourseAudit() == null) {
            executionCourseTo.setExecutionCourseAudit(executionCourseFrom.getExecutionCourseAudit());
        }
    }
}
