package pt.ist.fenixedu.contracts.domain.accessControl;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.GroupStrategy;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
import pt.ist.fenixedu.contracts.domain.organizationalStructure.Invitation;

import java.util.stream.Stream;

@GroupOperator("activeInvitations")
public class ActiveInvitationsGroup extends GroupStrategy {

    private static final long serialVersionUID = -2985536595609345377L;

    @Override
    public String getPresentationName() {
        return BundleUtil.getString("resources.GiafContractResources", "label.name.ActiveInvitationsGroup");
    }

    @Override
    public Stream<User> getMembers() {
        final DateTime today = new DateTime();
        return getMembers(today);
    }

    private boolean hasActiveInvitation(final User user, final YearMonthDay when) {
        final Person person = user.getPerson();
        return person != null && person.getParentsSet().stream()
                .filter(a -> a instanceof Invitation)
                .map(a -> (Invitation) a)
                .anyMatch(i -> i.isActive(when));
    }

    @Override
    public Stream<User> getMembers(final DateTime when) {
        return Bennu.getInstance().getUserSet().stream()
                .filter(u -> hasActiveInvitation(u, when.toYearMonthDay()));
    }

    @Override
    public boolean isMember(final User user) {
        return hasActiveInvitation(user, new YearMonthDay());
    }

    @Override
    public boolean isMember(final User user, final DateTime when) {
        return hasActiveInvitation(user, when.toYearMonthDay());
    }

}
