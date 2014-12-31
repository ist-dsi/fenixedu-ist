package pt.ist.fenixedu.delegates.domain.student;

import java.util.List;

import org.fenixedu.academic.domain.CurricularCourse;
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
}
