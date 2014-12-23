package pt.ist.fenixedu.delegates.domain.accessControl;

import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.GroupStrategy;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;

import pt.ist.fenixedu.delegates.domain.student.YearDelegate;

public class YearDelegatesGroup extends GroupStrategy {

    private static final long serialVersionUID = -7017075302622415050L;

    @Override
    public Set<User> getMembers(DateTime when) {
        return Bennu.getInstance().getDelegatesSet().stream().filter(d -> d instanceof YearDelegate && d.isActive(when))
                .map(d -> d.getUser()).collect(Collectors.toSet());
    }

    @Override
    public Set<User> getMembers() {
        return getMembers(DateTime.now());
    }

    @Override
    public boolean isMember(User user) {
        return isMember(user, DateTime.now());
    }

    @Override
    public boolean isMember(User user, DateTime when) {
        if (user.getDelegatesSet().stream().filter(d -> d instanceof YearDelegate).map(d -> d.isActive(when))
                .reduce(false, (x, y) -> x || y)) {
            return true;
        }
        return false;
    }

    public String getPresentationNameKey() {
        return "label.name." + getClass().getSimpleName();
    }

    @Override
    public String getPresentationName() {
        return BundleUtil.getString(Bundle.DELEGATE, getPresentationNameKey());
    }

}
