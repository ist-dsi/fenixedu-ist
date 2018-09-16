package pt.ist.registration.process.domain;

import java.util.Locale;

import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixWebFramework.rendererExtensions.util.IPresentableEnum;

public enum RegistrationDeclarationFileState implements IPresentableEnum {

    CREATED,

    PENDING,

    SIGNED,

    CERTIFIED,

    STORED;

    @Override
    public String getLocalizedName() {
        return getLocalizedNameI18N().getContent();
    }

    public String getLocalizedName(final Locale locale) {
        return getLocalizedNameI18N().getContent(locale);
    }

    public LocalizedString getLocalizedNameI18N() {
        return BundleUtil.getLocalizedString("resources.RegistrationProcessResources", getClass().getName() + "." + name());
    }

}
