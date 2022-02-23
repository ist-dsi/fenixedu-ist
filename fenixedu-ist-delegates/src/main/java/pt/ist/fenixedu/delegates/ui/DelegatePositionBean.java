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
package pt.ist.fenixedu.delegates.ui;

import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.bennu.core.domain.User;
import org.joda.time.DateTime;

import pt.ist.fenixedu.delegates.domain.student.CycleDelegate;
import pt.ist.fenixedu.delegates.domain.student.DegreeDelegate;
import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.domain.student.YearDelegate;
import pt.ist.fenixedu.delegates.domain.util.email.DelegateSender;

public class DelegatePositionBean extends DelegateBean {

    String errorMessage;

    public DelegatePositionBean(Delegate delegate) {
        this.curricularYear = delegate.getCurricularYear();
        this.delegate = delegate;
        this.degree = delegate.getDegree();
        this.delegateTitle = delegate.getTitle();
    }

    public DelegatePositionBean(Delegate delegate, CycleType cycleType, CurricularYear curricularYear, Degree degree) {
        this.delegate = delegate;
        this.curricularYear = curricularYear;
        this.cycleType = cycleType;
        this.degree = degree;
        this.delegateTitle = delegate.getTitle();
    }

    public DelegatePositionBean() {

    }

    public void setErrorMessage(String message) {
        errorMessage = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Delegate getDelegateFromPositionBean(User user) {
        Delegate newDelegate = null;
        if (cycleType != null) {
            newDelegate = new CycleDelegate(user, degree, cycleType);
        }
        if (curricularYear != null) {
            newDelegate = new YearDelegate(user, degree, curricularYear);
        }
        if (newDelegate == null) {
            newDelegate = new DegreeDelegate(user, degree);
        }
        newDelegate.setStart(new DateTime());
        newDelegate.setEnd(ExecutionYear.readCurrentExecutionYear().getEndDateYearMonthDay().toDateTimeAtMidnight().plusYears(1));
        newDelegate.setSender(new DelegateSender(newDelegate));
        return newDelegate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getEmail() {
        return "";
    }

    @Override
    public String getInterval() {
        return "";
    }

    @Override
    public String getPicture() {
        return picture;
    }

    @Override
    public Degree getDegree() {
        return degree;
    }

    @Override
    public void setDegree(Degree degree) {
        this.degree = degree;
    }

    @Override
    public CurricularYear getCurricularYear() {
        return curricularYear;
    }

    @Override
    public void setCurricularYear(CurricularYear curricularYear) {
        this.curricularYear = curricularYear;
    }

    @Override
    public CycleType getCycleType() {
        return cycleType;
    }

    @Override
    public void setCycleType(CycleType cycleType) {
        this.cycleType = cycleType;
    }

    @Override
    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public Delegate getDelegate() {
        return delegate;
    }

}