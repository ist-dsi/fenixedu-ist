package pt.ist.fenixedu.delegates.domain.student;

import static org.fenixedu.bennu.FenixEduDelegatesConfiguration.BUNDLE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.UserGroup;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixedu.delegates.domain.accessControl.DegreeDelegatesGroup;
import pt.ist.fenixedu.delegates.domain.accessControl.DelegatesOfDegreeGroup;
import pt.ist.fenixedu.delegates.domain.accessControl.YearDelegatesGroup;
import pt.ist.fenixedu.delegates.domain.util.email.DelegateSender;
import pt.ist.fenixedu.delegates.ui.DelegateBean;

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
        DelegatesOfDegreeGroup delegatesOfDegreeGroup = new DelegatesOfDegreeGroup();
        getSender().addRecipients(getRecipientFromGroup(delegatesOfDegreeGroup));
        YearDelegatesGroup yearDelegatesGroup = new YearDelegatesGroup();
        getSender().addRecipients(getRecipientFromGroup(yearDelegatesGroup));
        DegreeDelegatesGroup degreeDelegatesGroup = DegreeDelegatesGroup.get(getDegree());
        getSender().addRecipients(getRecipientFromGroup(degreeDelegatesGroup));
    }

    @Override
    public String getTitle() {
        String delegate = BundleUtil.getString(BUNDLE, "delegate");
        String of = BundleUtil.getString(BUNDLE, "delegate.of");
        return delegate + " " + of + " " + getCycle().getDescription();
    }

    @Override
    public Boolean samePosition(Delegate delegate) {
        CycleDelegate cycleDelegate = (CycleDelegate) delegate;
        if (getDegree().equals(cycleDelegate.getDegree()) && getCycle().equals(cycleDelegate.getCycle())) {
            return true;
        }
        return false;
    }

    @Override
    public List<CurricularCourse> getDelegateCourses() {
        List<CurricularCourse> toRet = new ArrayList<CurricularCourse>();
        ExecutionYear executionYearByDate = ExecutionYear.getExecutionYearByDate(getStart().toYearMonthDay());
        for (DegreeCurricularPlan curricularPlan : getDegree().getActiveDegreeCurricularPlans()) {
            for (ExecutionSemester execSem : executionYearByDate.getExecutionPeriodsSet()) {
                toRet.addAll(curricularPlan.getCycleCourseGroup(getCycle()).getAllCurricularCourses(execSem));
            }
        }
        return toRet.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public Boolean isDegreeOrCycleDelegate() {
        return true;
    }

    @Override
    public void setSender(DelegateSender sender) {
        super.setSender(sender);
        getSender().setMembers(UserGroup.of(getUser()));
        setupRecipients();
    }

    @Override
    public Boolean isYearDelegate() {
        return false;
    }

}
