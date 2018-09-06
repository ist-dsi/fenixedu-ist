package pt.ist.fenixedu.integration.domain;

import java.time.Year;

import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;

public class SantanderCard extends SantanderCard_Base {

    public SantanderCard(User user) {
        super();
        setUser(user);
    }
    
    public SantanderCard(User user, boolean allow) {
        super();
        setUser(user);
        setAllowSendDetails(allow);
    }

    @Atomic
    public static void setGrantAccess(final boolean allowAccess, String title, String body, final User user) {
        if (user != null) {
            final SantanderCard card = user.getSantanderCard();
            if (card != null) {
                card.setAllowSendDetails(allowAccess);
            } else {
                new SantanderCard(user, allowAccess);
            }
        }
    }

    @Atomic
    public static void setGrantCardAccess(final boolean allowCardAccess, final User user, final String title, final String body) {
        if (user != null) {
            final SantanderCard card = user.getSantanderCard();
            if (card != null) {
                card.setAllowSendCardDetails(allowCardAccess);
            } else {
                SantanderCard santanderCard = new SantanderCard(user);
                santanderCard.setAllowSendCardDetails(allowCardAccess);
            }

            new CardDataAuthorizationLog(title, body, BundleUtil.getString("resources.FenixeduIstIntegrationResources", "label.take.consent"));
        }
    }

    @Atomic
    public static void setGrantBankAccess(final boolean allowBankAccess, final User user, final String title, final String body) {
        if (user != null) {
            final SantanderCard card = user.getSantanderCard();
            if (card != null) {
                card.setAllowSendBankDetails(allowBankAccess);
            } else {
                SantanderCard santanderCard = new SantanderCard(user);
                santanderCard.setAllowSendBankDetails(allowBankAccess);
            }
            new CardDataAuthorizationLog(title, body, BundleUtil.getString(Bundle.ACADEMIC, allowBankAccess ? "label.yes" : "label.no"));
        }
    }

    private static SantanderCard getUserCard(User user) {
        if (user != null) {
            return user.getSantanderCard();
        }
        return null;
    }

    public static boolean getAllowSendBankDetails(User user) {
        SantanderCard card = SantanderCard.getUserCard(user);
        if (card != null) {
            return card.getAllowSendBankDetails() != null && card.getAllowSendBankDetails();
        }
        return false;
    }

    public static boolean finishedCardDataAuthorization(User user) {
        SantanderCard card = SantanderCard.getUserCard(user);
        if (card != null) {
            return card.getAllowSendCardDetails() != null && card.getAllowSendBankDetails() != null;
        }
        return false;
    }

    @Override
    public void setAllowSendDetails(boolean allow) {
        super.setAllowSendDetails(allow);
        setWhenAllowChanged(new DateTime());
    }

    @Override
    public void setAllowSendCardDetails(Boolean allow) {
        super.setAllowSendCardDetails(allow);
        setWhenCardAllowChanged(new DateTime());
    }

    @Override
    public void setAllowSendBankDetails(Boolean allow) {
        super.setAllowSendBankDetails(allow);
        setWhenBankAllowChanged(new DateTime());
    }

    public static boolean hasAccessResponse() {
        final User user = Authenticate.getUser();
        final int year = Year.now().getValue();
        return user.getSantanderCard() != null && user.getSantanderCard().getWhenAllowChanged().getYear() == year;
    }
    
}
