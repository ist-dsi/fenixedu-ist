package pt.ist.fenixedu.integration.ui.spring.service;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.commons.i18n.I18N;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import pt.ist.fenixedu.integration.domain.SantanderCard;
import pt.ist.fenixedu.integration.domain.cgd.CgdCard;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */

@Service
public class AuthorizePersonalDataAccessService {

    private static final String santanderCardTitle = "authorize.personal.data.access.title.santander.card";
    private static final String santanderCardMessage = "authorize.personal.data.access.description.santander.card";
    private static final String cgdCardTitle = "authorize.personal.data.access.title.cgd.card";
    private static final String cgdCardMessage = "authorize.personal.data.access.description.cgd.card";

    private static final String santanderBankTitle = "authorize.personal.data.access.title.santander.bank";
    private static final String santanderBankMessage = "authorize.personal.data.access.description.santander.bank";
    private static final String cgdBankTitle = "authorize.personal.data.access.title.cgd.bank";
    private static final String cgdBankMessage = "authorize.personal.data.access.description.cgd.bank";

    private final MessageSource messageSource;
    private final SendCgdCardService sendCgdCardService;

    @Autowired
    public AuthorizePersonalDataAccessService(final MessageSource messageSource, final SendCgdCardService sendCgdCardService) {
        this.messageSource = messageSource;
        this.sendCgdCardService = sendCgdCardService;
    }

    private String getMessage(final String key) {
        return messageSource.getMessage(key, new Object[0], I18N.getLocale());
    }

    public String getSantanderCardTitle() {
        return getMessage(santanderCardTitle);
    }

    public String getSantanderCardMessage() {
        return getMessage(santanderCardMessage);
    }

    public String getCgdCardTitle() {
        return getMessage(cgdCardTitle);
    }

    public String getCgdCardMessage() {
        return getMessage(cgdCardMessage);
    }

    public String getSantanderBankTitle() {
        return getMessage(santanderBankTitle);
    }

    public String getSantanderBankMessage() {
        return getMessage(santanderBankMessage);
    }

    public String getCgdBankTitle() {
        return getMessage(cgdBankTitle);
    }

    public String getCgdBankMessage() {
        return getMessage(cgdBankMessage);
    }

    public void setSantanderGrantBankAccess(final boolean allow, final User user) {
        SantanderCard.setGrantBankAccess(allow, user, getSantanderBankTitle(), getSantanderBankMessage());
    }

    public String setCgdGrantBankAccess(final boolean allow, final User user) {
        final CgdCard cgdCard = CgdCard.setGrantBankAccess(allow, user, getCgdBankTitle(), getCgdBankMessage());
        return sendCgdCardService.sendCgdCard(cgdCard);
    }

    public void setSantanderGrantCardAccess(final boolean allow, final User user) {
        SantanderCard.setGrantCardAccess(allow, user, getSantanderCardTitle(), getSantanderCardMessage());
    }

    public void setCgdGrantCardAccess(final boolean allow, final User user) {
        CgdCard.setGrantCardAccess(allow, user, getCgdCardTitle(), getCgdCardMessage());
    }

}
