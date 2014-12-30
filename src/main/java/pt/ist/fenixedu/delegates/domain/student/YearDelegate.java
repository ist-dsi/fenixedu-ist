/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Core.
 *
 * FenixEdu Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Core.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.domain.student;

import static org.fenixedu.bennu.FenixEduDelegatesConfiguration.BUNDLE;

import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.UserGroup;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixedu.delegates.domain.accessControl.DegreeDelegatesGroup;
import pt.ist.fenixedu.delegates.domain.util.email.DelegateSender;
import pt.ist.fenixedu.delegates.ui.DelegateBean;

public class YearDelegate extends YearDelegate_Base {

    public YearDelegate(User user, Degree degree, CurricularYear curricularYear) {
        super();
        setUser(user);
        setDegree(degree);
        setCurricularYear(curricularYear);
    }

    @Override
    public DelegateBean getBean() {
        return new DelegateBean(this);
    }

    @Override
    public void setSender(DelegateSender sender) {
        super.setSender(sender);
        getSender().setMembers(UserGroup.of(getUser()));
        getSender().addRecipients(getRecipientFromGroup(DegreeDelegatesGroup.get(getDegree())));
    }

    @Override
    public String getTitle() {
        String delegate = BundleUtil.getString(BUNDLE, "delegate");
        String of = BundleUtil.getString(BUNDLE, "delegate.of");
        String year = BundleUtil.getString(BUNDLE, "delegate.year");
        return delegate + " " + of + " " + getCurricularYear().getYear() + " " + year;
    }

    @Override
    public Boolean samePosition(Delegate delegate) {
        YearDelegate yearDelegate = (YearDelegate) delegate;
        if (getDegree().equals(yearDelegate.getDegree()) && getCurricularYear().equals(yearDelegate.getCurricularYear())) {
            return true;
        }
        return false;
    }

    @Override
    public List<CurricularCourse> getDelegateCourses() {

        ExecutionYear execYear = ExecutionYear.getExecutionYearByDate(getStart().toYearMonthDay());

        return getDegree()
                .getDegreeCurricularPlansForYear(execYear)
                .stream()
                .flatMap(
                        p -> p.getCurricularCoursesByExecutionYearAndCurricularYear(execYear, getCurricularYear().getYear())
                                .stream()).collect(Collectors.toList());
    }

    @Override
    public Boolean isDegreeOrCycleDelegate() {
        return false;
    }

    @Override
    public Boolean isYearDelegate() {
        return true;
    }

}