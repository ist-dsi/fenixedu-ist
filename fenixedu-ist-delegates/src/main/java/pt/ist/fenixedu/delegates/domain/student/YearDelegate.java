/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Delegates.
 *
 * FenixEdu IST Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.domain.student;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import pt.ist.fenixedu.delegates.domain.accessControl.DelegateGroup;
import pt.ist.fenixedu.delegates.domain.util.email.DelegateSender;
import pt.ist.fenixedu.delegates.ui.DelegateBean;

import java.util.List;
import java.util.stream.Collectors;

import static org.fenixedu.bennu.FenixEduDelegatesConfiguration.BUNDLE;

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
        getSender().setMembers(getUser().groupOf());
        getSender().addRecipient(DelegateGroup.get(getDegree()));
    }

    @Override
    public String getTitle() {
        String delegate = BundleUtil.getString(BUNDLE, "delegate");
        String of = BundleUtil.getString(BUNDLE, "delegate.of");
        String year = BundleUtil.getString(BUNDLE, "delegate.year");
        return String.format("%s %s %d %s", delegate, of, getCurricularYear().getYear(), year);
    }

    @Override
    public Boolean samePosition(Delegate delegate) {
        YearDelegate yearDelegate = (YearDelegate) delegate;
        return getDegree().equals(yearDelegate.getDegree()) && getCurricularYear().equals(yearDelegate.getCurricularYear());
    }

    @Override
    public List<CurricularCourse> getDelegateCourses() {

        ExecutionYear execYear = ExecutionYear.getExecutionYearByDate(getStart().toYearMonthDay());

        return getDegree().getDegreeCurricularPlansForYear(execYear).stream()
                .flatMap(p -> p.getCurricularCoursesByExecutionYearAndCurricularYear(execYear, getCurricularYear().getYear())
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