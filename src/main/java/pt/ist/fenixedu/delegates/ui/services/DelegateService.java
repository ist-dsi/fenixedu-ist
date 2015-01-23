package pt.ist.fenixedu.delegates.ui.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeModuleScope;
import org.fenixedu.academic.domain.EmptyDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.springframework.stereotype.Service;

import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.domain.student.YearDelegate;
import pt.ist.fenixedu.delegates.ui.DelegateBean;
import pt.ist.fenixedu.delegates.ui.DelegateCurricularCourseBean;
import pt.ist.fenixedu.delegates.ui.DelegatePositionBean;
import pt.ist.fenixedu.delegates.ui.DelegateSearchBean;
import pt.ist.fenixedu.delegates.ui.DelegateStudentSelectBean;
import pt.ist.fenixframework.Atomic;

@Service
public class DelegateService {

    public DelegateSearchBean generateNewBean(DelegateSearchBean delegateSearchBean) {
        ExecutionYear executionYear = delegateSearchBean.getExecutionYear();
        Degree degree = delegateSearchBean.getDegree();
        DegreeType degreeType = delegateSearchBean.getDegreeType();
        delegateSearchBean.setExecutionYears(Bennu.getInstance().getExecutionYearsSet().stream()
                .sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).collect(Collectors.toList()));
        Set<DegreeType> aux =
                executionYear.getExecutionDegreesSet().stream().map(d -> d.getDegree().getDegreeType())
                        .collect(Collectors.toSet());
        delegateSearchBean.setDegreeTypes(aux.stream().collect(Collectors.toList()));
        if (degreeType == null && (degree == null || EmptyDegree.class.isInstance(degree))) {
            delegateSearchBean.setDegrees(executionYear.getExecutionDegreesSet().stream().map(d -> d.getDegree())
                    .sorted(Degree.COMPARATOR_BY_NAME).collect(Collectors.toList()));
            delegateSearchBean.setDegree(delegateSearchBean.getDegrees().iterator().next());
            delegateSearchBean.setDegreeType(delegateSearchBean.getDegree().getDegreeType());
            return delegateSearchBean;
        }
        if (degree == null || EmptyDegree.class.isInstance(degree)) {
            delegateSearchBean.setDegrees(executionYear.getExecutionDegreesSet().stream()
                    .filter(d -> d.getDegree().getDegreeType().equals(degreeType)).map(d -> d.getDegree())
                    .sorted(Degree.COMPARATOR_BY_NAME).collect(Collectors.toList()));
            delegateSearchBean.setDegree(delegateSearchBean.getDegrees().iterator().next());
            return delegateSearchBean;
        }
        delegateSearchBean.setDegrees(executionYear.getExecutionDegreesSet().stream()
                .filter(d -> d.getDegree().getDegreeType().equals(degreeType)).map(d -> d.getDegree())
                .sorted(Degree.COMPARATOR_BY_NAME).collect(Collectors.toList()));
        return delegateSearchBean;
    }

    public List<DelegateBean> searchDelegates(DelegateSearchBean delegateSearchBean) {
        DateTime beginDate = delegateSearchBean.getExecutionYear().getBeginDateYearMonthDay().toDateTimeAtMidnight();
        DateTime endDate = delegateSearchBean.getExecutionYear().getEndDateYearMonthDay().toDateTimeAtMidnight();
        Interval executionYearSpan = new Interval(beginDate, endDate);
        List<Delegate> toRemove = new ArrayList<Delegate>();
        List<Delegate> withDuplicates = delegateSearchBean.getDegree().getDelegateSet().stream().filter(d -> {
            DateTime start = d.getStart();
            DateTime end = d.getEnd();
            Interval activity = new Interval(start, end);
            if (executionYearSpan.overlaps(activity)) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());

        for (Delegate delegate : withDuplicates) {
            for (Delegate delegate2 : withDuplicates) {
                if (delegate.getClass().isInstance(delegate2)) {
                    if (delegate.samePosition(delegate2) && delegate.getStart().isBefore(delegate2.getStart())) {
                        toRemove.add(delegate);
                    }
                }
            }
        }
        withDuplicates.removeAll(toRemove);

        return withDuplicates.stream().map(p -> p.getBean()).collect(Collectors.toList());
    }

    public List<DelegateBean> searchDelegates(DelegateSearchBean delegateSearchBean, DateTime when) {
        List<Delegate> toRemove = new ArrayList<Delegate>();
        Stream<Delegate> delegateStream = delegateSearchBean.getDegree().getDelegateSet().stream();
        return delegateStream.filter(d -> d.isActive(when)).map(d -> d.getBean()).collect(Collectors.toList());
    }

    public List<DelegateCurricularCourseBean> getCurricularCourses(Delegate delegate) {
        return getCurricularCoursesBeans(delegate, delegate.getDelegateCourses().stream().collect(Collectors.toSet()));
    }

    public List<DelegateCurricularCourseBean> getCurricularCoursesBeans(Delegate delegate, Set<CurricularCourse> curricularCourses) {
        final Class delegateFunctionType = delegate.getClass();
        final ExecutionYear executionYear = ExecutionYear.getExecutionYearByDate(delegate.getStart().toYearMonthDay());

        List<DelegateCurricularCourseBean> result = new ArrayList<DelegateCurricularCourseBean>();

        for (CurricularCourse curricularCourse : curricularCourses) {
            for (ExecutionSemester executionSemester : executionYear.getExecutionPeriodsSet()) {
                if (curricularCourse.hasAnyExecutionCourseIn(executionSemester)) {
                    for (DegreeModuleScope scope : curricularCourse.getDegreeModuleScopes()) {
                        if (!scope.isActiveForExecutionPeriod(executionSemester)) {
                            continue;
                        }

                        if (delegateFunctionType.equals(YearDelegate.class)) {
                            YearDelegate yearDelegate = (YearDelegate) delegate;
                            if (!scopeBelongsToDelegateCurricularYear(scope, yearDelegate.getCurricularYear().getYear())) {
                                continue;
                            }
                        }

                        DelegateCurricularCourseBean bean =
                                new DelegateCurricularCourseBean(curricularCourse, executionYear, scope.getCurricularYear(),
                                        executionSemester);
                        if (!result.contains(bean)) {
                            bean.calculateEnrolledStudents();
                            result.add(bean);
                        }
                    }
                }
            }
        }
        Collections.sort(result,
                DelegateCurricularCourseBean.CURRICULAR_COURSE_COMPARATOR_BY_CURRICULAR_YEAR_AND_CURRICULAR_SEMESTER);

        return result;
    }

    private boolean scopeBelongsToDelegateCurricularYear(DegreeModuleScope scope, Integer curricularYear) {
        if (scope.getCurricularYear().equals(curricularYear)) {
            return true;
        }
        return false;
    }

    public List<User> getSelectedUsers(DelegateStudentSelectBean delegateStudentSelectBean) {
        Delegate selectedSender = delegateStudentSelectBean.getSelectedPosition();
        if (delegateStudentSelectBean.getSelectedExecutionCourses() != null
                && delegateStudentSelectBean.getSelectedExecutionCourses().size() > 0) {
            List<DelegateCurricularCourseBean> courseBeans =
                    getCurricularCoursesBeans(selectedSender, delegateStudentSelectBean.getSelectedExecutionCourses().stream()
                            .collect(Collectors.toSet()));
            return courseBeans.stream().flatMap(dcb -> dcb.getEnrolledStudents().stream()).map(s -> s.getPerson().getUser())
                    .collect(Collectors.toList());
        } else {
            if (delegateStudentSelectBean.getSelectedDegreeOrCycleStudents() == true) {
                return getCurricularCourses(selectedSender).stream().flatMap(dcb -> dcb.getEnrolledStudents().stream())
                        .map(s -> s.getPerson().getUser()).collect(Collectors.toList());
            } else {
                if (delegateStudentSelectBean.getSelectedYearStudents() == true) {
                    return getCurricularCourses(selectedSender).stream().flatMap(dcb -> dcb.getEnrolledStudents().stream())
                            .map(s -> s.getPerson().getUser()).collect(Collectors.toList());
                }
            }
        }
        return new ArrayList<User>();
    }

    @Atomic
    public void terminateDelegatePosition(Delegate delegate) {
        delegate.setEnd(new DateTime());
    }

    public List<DelegateBean> getDegreePositions(Degree degree) {
        DegreeType degreeType = degree.getDegreeType();
        List<DelegateBean> ldpb = new ArrayList<DelegateBean>();
        ldpb.add(new DelegatePositionBean(null, null, CurricularYear.readByYear(1), degree));
        ldpb.add(new DelegatePositionBean(null, null, CurricularYear.readByYear(2), degree));
        if (degreeType == DegreeType.BOLONHA_MASTER_DEGREE) {
            ldpb.add(new DelegatePositionBean(null, CycleType.SECOND_CYCLE, null, degree));
            return ldpb;
        }
        if (degreeType == DegreeType.BOLONHA_INTEGRATED_MASTER_DEGREE) {
            ldpb.add(new DelegatePositionBean(null, null, CurricularYear.readByYear(3), degree));
            ldpb.add(new DelegatePositionBean(null, null, CurricularYear.readByYear(4), degree));
            ldpb.add(new DelegatePositionBean(null, null, CurricularYear.readByYear(5), degree));
            ldpb.add(new DelegatePositionBean(null, CycleType.FIRST_CYCLE, null, degree));
            ldpb.add(new DelegatePositionBean(null, null, null, degree));
            return ldpb;
        }
        if (degreeType == DegreeType.BOLONHA_DEGREE) {
            ldpb.add(new DelegatePositionBean(null, null, CurricularYear.readByYear(3), degree));
            ldpb.add(new DelegatePositionBean(null, CycleType.FIRST_CYCLE, null, degree));
        }
        return new ArrayList<DelegateBean>();
    }

    @Atomic
    public void attributeDelegatePosition(DelegatePositionBean delegatePositionBean) {
        User user = User.findByUsername(delegatePositionBean.getName());
        Delegate oldDelegate = delegatePositionBean.getDelegate();
        if (oldDelegate != null) {
            terminateDelegatePosition(oldDelegate);
        }
        delegatePositionBean.getDelegateFromPositionBean(user);
    }
}
