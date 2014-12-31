package pt.ist.fenixedu.delegates.domain.student;

import static org.fenixedu.bennu.FenixEduDelegatesConfiguration.BUNDLE;

import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.util.email.Recipient;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.UserGroup;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixedu.delegates.domain.accessControl.DelegateGroup;
import pt.ist.fenixedu.delegates.domain.util.email.DelegateSender;
import pt.ist.fenixedu.delegates.ui.DelegateBean;

public class DegreeDelegate extends DegreeDelegate_Base {

    public DegreeDelegate(User user, Degree degree) {
        super();
        setUser(user);
        setDegree(degree);
    }

    @Override
    public DelegateBean getBean() {
        return new DelegateBean(this);
    }

    private void setupRecipients() {
        getSender().addRecipients(Recipient.getRecipientFromGroup(DelegateGroup.get()));
        getSender().addRecipients(Recipient.getRecipientFromGroup(DelegateGroup.get(true)));
        getSender().addRecipients(Recipient.getRecipientFromGroup(DelegateGroup.get(getDegree())));
    }

    @Override
    public void setSender(DelegateSender sender) {
        super.setSender(sender);
        getSender().setMembers(UserGroup.of(getUser()));
        setupRecipients();
    }

    @Override
    public String getTitle() {
        String delegate = BundleUtil.getString(BUNDLE, "delegate");
        String of = BundleUtil.getString(BUNDLE, "delegate.of");
        return delegate + " " + of + " " + getDegree().getDegreeType().getLocalizedName();
    }

    @Override
    public Boolean samePosition(Delegate delegate) {
        DegreeDelegate degreeDelegate = (DegreeDelegate) delegate;
        if (getDegree().equals(degreeDelegate.getDegree())) {
            return true;
        }
        return false;
    }

    @Override
    public List<CurricularCourse> getDelegateCourses() {
        ExecutionCourse ec;
        ExecutionYear executionYearByDate = ExecutionYear.getExecutionYearByDate(getStart().toYearMonthDay());

        return getDegree().getDegreeCurricularPlansForYear(executionYearByDate).stream()
                .flatMap(p -> p.getCurricularCoursesSet().stream()).distinct().collect(Collectors.toList());

    }

    @Override
    public Boolean isDegreeOrCycleDelegate() {
        return true;
    }

    @Override
    public Boolean isYearDelegate() {
        return false;
    }

}
