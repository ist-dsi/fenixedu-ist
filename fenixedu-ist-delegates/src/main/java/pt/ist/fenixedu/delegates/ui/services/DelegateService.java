/**
 * Copyright © 2013 Instituto Superior Técnico
 * <p>
 * This file is part of FenixEdu IST Delegates.
 * <p>
 * FenixEdu IST Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * FenixEdu IST Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.ui.services;

import org.fenixedu.academic.domain.*;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.domain.student.YearDelegate;
import pt.ist.fenixedu.delegates.ui.DelegateBean;
import pt.ist.fenixedu.delegates.ui.DelegateExecutionCourseBean;
import pt.ist.fenixedu.delegates.ui.DelegatePositionBean;
import pt.ist.fenixedu.delegates.ui.DelegateSearchBean;
import pt.ist.fenixframework.Atomic;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fenixedu.bennu.FenixEduDelegatesConfiguration.BUNDLE;

@Service
public class DelegateService {

    public DelegateSearchBean generateNewBean(DelegateSearchBean delegateSearchBean) {
        ExecutionYear executionYear = delegateSearchBean.getExecutionYear();
        Degree degree = delegateSearchBean.getDegree();
        DegreeType degreeType = delegateSearchBean.getDegreeType();
        delegateSearchBean.setExecutionYears(Bennu.getInstance().getExecutionYearsSet().stream()
                .sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).collect(Collectors.toList()));
        delegateSearchBean.setDegreeTypes(executionYear.getExecutionDegreesSet().stream().map(d -> d.getDegree().getDegreeType())
                .distinct().sorted().collect(Collectors.toList()));
        if (degreeType == null && (degree == null || degree instanceof EmptyDegree)) {
            delegateSearchBean.setDegrees(executionYear.getExecutionDegreesSet().stream().map(ExecutionDegree::getDegree).distinct()
                    .sorted(Degree.COMPARATOR_BY_DEGREE_TYPE_DEGREE_NAME_AND_ID).collect(Collectors.toList()));
            return delegateSearchBean;
        }
        if (degree == null || degree instanceof EmptyDegree) {
            delegateSearchBean.setDegrees(executionYear.getExecutionDegreesSet().stream()
                    .filter(d -> d.getDegree().getDegreeType().equals(degreeType)).map(ExecutionDegree::getDegree).distinct()
                    .sorted(Degree.COMPARATOR_BY_DEGREE_TYPE_DEGREE_NAME_AND_ID).collect(Collectors.toList()));
            return delegateSearchBean;
        }
        delegateSearchBean.setDegrees(executionYear.getExecutionDegreesSet().stream()
                .filter(d -> d.getDegree().getDegreeType().equals(degreeType)).map(ExecutionDegree::getDegree).distinct()
                .sorted(Degree.COMPARATOR_BY_DEGREE_TYPE_DEGREE_NAME_AND_ID).collect(Collectors.toList()));
        return delegateSearchBean;
    }

    public List<DelegateBean> searchDelegates(DelegateSearchBean delegateSearchBean) {
        DateTime activeWhen;
        if (delegateSearchBean.getExecutionYear().equals(ExecutionYear.readCurrentExecutionYear())) {
            activeWhen = DateTime.now();
        } else {
            activeWhen = delegateSearchBean.getExecutionYear().getAcademicInterval().toInterval().getEnd();
        }

        Stream<Delegate> stream;
        if (delegateSearchBean.getDegree() == null) {
            stream = Bennu.getInstance().getDelegatesSet().stream();
        } else {
            stream = delegateSearchBean.getDegree().getDelegateSet().stream();
        }

        List<Delegate> withDuplicates = stream.filter(d -> d.isActive(activeWhen)).collect(Collectors.toList());

        List<Delegate> toRemove = new ArrayList<>();
        for (Delegate delegate : withDuplicates) {
            withDuplicates.stream().filter(delegate2 -> delegate.getClass().isInstance(delegate2))
                    .filter(delegate2 -> delegate.samePosition(delegate2) && delegate.getStart().isBefore(delegate2.getStart()))
                    .map(delegate2 -> delegate).forEach(toRemove::add);
        }
        withDuplicates.removeAll(toRemove);

        return withDuplicates.stream().map(Delegate::getBean).collect(Collectors.toList());
    }

    public Stream<DelegateBean> search(DelegateSearchBean delegateSearchBean, DateTime when) {
        Stream<Delegate> delegateStream;
        if (delegateSearchBean.getDegree() != null) {
            delegateStream = delegateSearchBean.getDegree().getDelegateSet().stream();
        } else if (delegateSearchBean.getDegreeType() != null) {
            delegateStream = delegateSearchBean.getDegrees().stream().flatMap(d -> d.getDelegateSet().stream());
        } else {
            delegateStream = Bennu.getInstance().getDelegatesSet().stream();
        }
        return delegateStream.filter(d -> d.isActive(when)).distinct().map(Delegate::getBean);
    }

    public List<DelegateExecutionCourseBean> getExecutionCourses(Delegate delegate) {
        return delegate.getDelegateExecutionCourses()
                .stream()
                .map(executionCourse -> new DelegateExecutionCourseBean(executionCourse, delegate.getDegree()))
                .sorted(DelegateExecutionCourseBean.EXECUTION_COURSE_COMPARATOR_BY_EXECUTION_YEAR_AND_SEMESTER)
                .collect(Collectors.toList());
    }

    @Atomic
    public void terminateDelegatePosition(Delegate delegate) {
        delegate.setEnd(new DateTime());
    }

    public List<DelegateBean> getDegreePositions(Degree degree) {
        DegreeType degreeType = degree.getDegreeType();
        List<DelegateBean> ldpb = new ArrayList<>();
        ldpb.add(new DelegatePositionBean(null, null, CurricularYear.readByYear(1), degree));
        ldpb.add(new DelegatePositionBean(null, null, CurricularYear.readByYear(2), degree));
        if (degreeType.isBolonhaMasterDegree()) {
            ldpb.add(new DelegatePositionBean(null, CycleType.SECOND_CYCLE, null, degree));
            return ldpb;
        }
        if (degreeType.isIntegratedMasterDegree()) {
            ldpb.add(new DelegatePositionBean(null, null, CurricularYear.readByYear(3), degree));
            ldpb.add(new DelegatePositionBean(null, null, CurricularYear.readByYear(4), degree));
            ldpb.add(new DelegatePositionBean(null, null, CurricularYear.readByYear(5), degree));
            ldpb.add(new DelegatePositionBean(null, CycleType.FIRST_CYCLE, null, degree));
            ldpb.add(new DelegatePositionBean(null, null, null, degree));
            return ldpb;
        }
        if (degreeType.isBolonhaDegree()) {
            ldpb.add(new DelegatePositionBean(null, null, CurricularYear.readByYear(3), degree));
            ldpb.add(new DelegatePositionBean(null, CycleType.FIRST_CYCLE, null, degree));
            return ldpb;
        }
        return ldpb;
    }

    @Atomic
    public boolean attributeDelegatePosition(DelegatePositionBean delegatePositionBean) {
        User user = User.findByUsername(delegatePositionBean.getName());
        if (user == null) {
            delegatePositionBean.setErrorMessage(
                    String.format("%s %s", BundleUtil.getString(BUNDLE, "user.not.found"), delegatePositionBean.getName()));
            return false;
        }
        Delegate oldDelegate = delegatePositionBean.getDelegate();
        if (oldDelegate != null) {
            terminateDelegatePosition(oldDelegate);
        }
        delegatePositionBean.getDelegateFromPositionBean(user);
        return true;
    }
}
