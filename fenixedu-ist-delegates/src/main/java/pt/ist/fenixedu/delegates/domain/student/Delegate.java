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

import java.util.List;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import pt.ist.fenixedu.delegates.ui.DelegateBean;

public abstract class Delegate extends Delegate_Base {

    public abstract String getTitle();

    public Delegate() {
        setBennu(Bennu.getInstance());
    }

    public abstract DelegateBean getBean();

    public abstract Boolean samePosition(Delegate delegate);

    public abstract List<CurricularCourse> getDelegateCourses();

    public abstract Boolean isDegreeOrCycleDelegate();

    public abstract Boolean isYearDelegate();

    public Boolean isActive() {
        return isActive(DateTime.now());
    }

    public Interval getInterval() {
        return new Interval(getStart(), getEnd());
    }

    public Boolean isActive(DateTime when) {
        return getInterval().contains(when);
    }

    public Boolean isAfter(Delegate delegate) {
        return getEnd().isAfter(delegate.getEnd());
    }

    public Registration getRegistration() {
        return getUser().getPerson().getStudent().getActiveRegistrationFor(getDegree());
    }

    public CurricularYear getCurricularYear() {
        return null;
    }

    public CycleType getCycleType() {
        return null;
    }
}
