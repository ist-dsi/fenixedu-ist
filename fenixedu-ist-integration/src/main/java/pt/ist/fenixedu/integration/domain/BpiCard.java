package pt.ist.fenixedu.integration.domain;

import java.time.Year;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;

public class BpiCard extends BpiCard_Base {

    public BpiCard(User user, boolean allow) {
        super();
        setUser(user);
        setAllowSendDetails(allow);
    }

    @Atomic
    public static void setGrantAccess(final boolean allowAccess, final User user) {
        if (user != null) {
            final BpiCard card = user.getBpiCard();
            if (card != null) {
                card.setAllowSendDetails(allowAccess);
            } else {
                new BpiCard(user, allowAccess);
            }
        }
    }

    @Override
    public void setAllowSendDetails(boolean allow) {
        super.setAllowSendDetails(allow);
        setWhenAllowChanged(new DateTime());
    }

    public static boolean hasAccessResponse() {
        final User user = Authenticate.getUser();
        final int year = Year.now().getValue();
        return user.getBpiCard() != null && user.getBpiCard().getWhenAllowChanged().getYear() == year;
    }
}
