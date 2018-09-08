package pt.ist.fenixedu.integration.domain;

import java.time.Year;

import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;

public class BpiCard extends BpiCard_Base {

    public BpiCard(User user) {
        super();
        setUser(user);
    }

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

    @Atomic
    public static void setGrantCardAccess(final boolean allowCardAccess, final User user, String title, String body) {
        if (user != null) {
            final BpiCard card = user.getBpiCard();
            if (card != null) {
                card.setAllowSendCardDetails(allowCardAccess);
            } else {
                BpiCard bpiCard = new BpiCard(user, allowCardAccess);
                bpiCard.setAllowSendCardDetails(allowCardAccess);
            }
            new CardDataAuthorizationLog(title, body, BundleUtil.getString(Bundle.ACADEMIC,allowCardAccess ? "label.yes" :
                    "label.no"), user.getPerson());
        }
    }

    @Atomic
    public static void setGrantBankAccess(final boolean allowBankAccess, final User user, String title, String body) {
        if (user != null) {
            final BpiCard card = user.getBpiCard();
            if (card != null) {
                card.setAllowSendBankDetails(allowBankAccess);
            } else {
                BpiCard bpiCard = new BpiCard(user, allowBankAccess);
                bpiCard.setAllowSendBankDetails(allowBankAccess);
            }
            new CardDataAuthorizationLog(title, body, BundleUtil.getString(Bundle.ACADEMIC,allowBankAccess ? "label.yes" :
                    "label.no"), user.getPerson());
        }
    }

    private static BpiCard getUserCard(User user) {
        if (user != null) {
            return user.getBpiCard();
        }
        return null;
    }

    public static boolean getAllowSendBankDetails(User user) {
        BpiCard card = BpiCard.getUserCard(user);
        if (card != null) {
            return card.getAllowSendBankDetails() != null && card.getAllowSendBankDetails();
        }
        return false;
    }

    public static boolean finishedCardDataAuthorization(User user) {
        if (user != null) {
            final BpiCard card = user.getBpiCard();
            if (card != null) {
                return card.getAllowSendBankDetails() != null;
            }
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
        return user.getBpiCard() != null && user.getBpiCard().getWhenBankAllowChanged().getYear() == year;
    }
}
