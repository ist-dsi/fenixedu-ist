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
package pt.ist.fenixedu.delegates.domain.student;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accessControl.StudentGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import pt.ist.fenixedu.delegates.domain.accessControl.DelegateGroup;
import pt.ist.fenixedu.delegates.domain.util.email.DelegateSender;
import pt.ist.fenixedu.delegates.ui.DelegateBean;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.fenixedu.bennu.FenixEduDelegatesConfiguration.BUNDLE;

public class CycleDelegate extends CycleDelegate_Base {

    public CycleDelegate(User user, Degree degree, CycleType cycle) {
        super();
        setUser(user);
        setDegree(degree);
        setCycle(cycle);
    }

    @Override
    public DelegateBean getBean() {
        return new DelegateBean(this);
    }

    private void setupRecipients() {
        getSender().addRecipient(DelegateGroup.get());
        getSender().addRecipient(DelegateGroup.get(true));
        getSender().addRecipient(DelegateGroup.get(getDegree()));
    }

    @Override
    public String getTitle() {
        return BundleUtil.getString(BUNDLE, "delegate.title.cycle-delegate",
                getCycle().getDescription(), getDegree().getSigla());
    }

    @Override
    public Boolean samePosition(Delegate delegate) {
        CycleDelegate cycleDelegate = (CycleDelegate) delegate;
        return getDegree().equals(cycleDelegate.getDegree()) && getCycle().equals(cycleDelegate.getCycle());
    }

    @Override
    public List<ExecutionCourse> getDelegateExecutionCourses() {
        final List<ExecutionYear> execYears = getMandateExecutionYears();

        return execYears.stream().flatMap(execYear ->
                        getDegree()
                                .getDegreeCurricularPlansForYear(execYear)
                                .stream()
                                .flatMap(plan ->
                                        plan.getCycleCourseGroup(getCycle())
                                                .getAllCurricularCourses()
                                                .stream()
                                )
                                .filter(curricularCourse -> curricularCourse
                                        .getDegreeModuleScopes()
                                        .stream()
                                        .anyMatch(scope -> scope.isActiveForExecutionYear(execYear))
                                )
                                .flatMap(curricularCourse -> curricularCourse.getExecutionCoursesByExecutionYear(execYear).stream())
                )
                .distinct().collect(Collectors.toList());
    }

    @Override
    public Boolean isDegreeOrCycleDelegate() {
        return true;
    }

    @Override
    public void setSender(DelegateSender sender) {
        super.setSender(sender);
        getSender().setMembers(getUser().groupOf());
        setupRecipients();
    }

    @Override
    public Boolean isYearDelegate() {
        return false;
    }

    @Override
    public CycleType getCycleType() {
        return getCycle();
    }

    @Override
    public StudentGroup getStudentGroupForExecutionYear(ExecutionYear year) {
        return StudentGroup.get(null, this.getDegree(), this.getCycleType(), null, null, null, year);
    }
}
