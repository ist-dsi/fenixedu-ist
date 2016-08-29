package pt.ist.fenixedu.integration.domain;

import java.time.Year;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;

public class SantanderCard extends SantanderCard_Base {
    
    public SantanderCard(User user, boolean allow) {
        super();
        setUser(user);
        setAllowSendDetails(allow);
    }

    @Atomic
    public static void setGrantAccess(final boolean allowAccess, final User user) {
        if (user != null) {
            final SantanderCard card = user.getSantanderCard();
            if (card != null) {
                card.setAllowSendDetails(allowAccess);
            } else {
                new SantanderCard(user, allowAccess);
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
        return user.getSantanderCard() != null && user.getSantanderCard().getWhenAllowChanged().getYear() == year;
    }
    
}
