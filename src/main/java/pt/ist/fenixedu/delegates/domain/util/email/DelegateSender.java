package pt.ist.fenixedu.delegates.domain.util.email;

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
        addReplyTos(delegate.getUser().getPerson().getReplyTo());
        setMembers(UserGroup.of(delegate.getUser()));
        setRootDomainObject(Bennu.getInstance());
    }

}
