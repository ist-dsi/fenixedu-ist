package pt.ist.fenixedu.delegates.domain.util.email;

import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.util.email.ReplyTo;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.UserGroup;

import pt.ist.fenixedu.delegates.domain.student.Delegate;

public class DelegateSender extends DelegateSender_Base {

    public DelegateSender() {
        super();
    }

    public DelegateSender(Delegate delegate) {
        setFromName(delegate.getUser().getPerson().getName() + " (" + delegate.getTitle() + ")");
        setFromAddress(getNoreplyMail());
        setMembers(UserGroup.of(delegate.getUser()));
        setRootDomainObject(Bennu.getInstance());
    }

    @Override
    public Set<ReplyTo> getReplyTosSet() {
        return getDelegateSet().stream().map(d -> d.getUser().getPerson().getReplyTo()).collect(Collectors.toSet());
    }

    @Override
    public Set<ReplyTo> getReplyTos() {
        return getReplyTosSet();
    }

}
