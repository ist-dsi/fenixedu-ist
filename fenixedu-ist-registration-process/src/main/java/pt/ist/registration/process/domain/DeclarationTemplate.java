package pt.ist.registration.process.domain;

import java.util.Locale;
import java.util.Optional;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.papyrus.domain.PapyrusTemplate;
import org.fenixedu.bennu.papyrus.domain.SignatureFieldSettings;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.gson.Gson;

import pt.ist.papyrus.PapyrusSettings;

public class DeclarationTemplate extends DeclarationTemplate_Base {

    private DeclarationTemplate() {
        super();
    }

    public DeclarationTemplate(String name, LocalizedString displayName, String templateHtml, Locale locale,
                                  SignatureFieldSettings signatureFieldSettings, PapyrusSettings printSettings,
                                  String filenameFormat, String displayNameFormat, Group accessGroup) {
        init(name, displayName, templateHtml, locale, signatureFieldSettings, printSettings);
        setFilenameFormat(filenameFormat);
        setDisplayNameFormat(displayNameFormat);
        setPersistentAccessGroup(accessGroup.toPersistentGroup());
    }

    public void setSignatureFieldsSettings(SignatureFieldSettings settings) {
        setSignatureFieldSettingsElement(new Gson().toJsonTree(settings));
    }

    public SignatureFieldSettings getSignatureFieldsSettings() {
        return new Gson().fromJson(getSignatureFieldSettingsElement(), SignatureFieldSettings.class);
    }

    public void setPrintSettings(PapyrusSettings settings) {
        setPrintSettingsElement(new Gson().toJsonTree(settings));
    }

    public PapyrusSettings getPrintSettings() {
        return new Gson().fromJson(getPrintSettingsElement(), PapyrusSettings.class);
    }

    public Group getAccessGroup() {
        return getPersistentAccessGroup() == null ? null : getPersistentAccessGroup().toGroup();
    }

    public void setAccessGroup(Group group) {
        setPersistentAccessGroup(group == null ? null : group.toPersistentGroup());
    }

    @Override
    protected void disconnect() {
        super.disconnect();
        setAccessGroup(null);
        setBennuFirstTimeRegistrationTemplate(null);
    }

    public static Optional<? extends DeclarationTemplate> findByNameAndLocale(String name, Locale locale) {
        return PapyrusTemplate.findByNameAndLocale(name,locale).filter(DeclarationTemplate.class
        ::isInstance).map(DeclarationTemplate.class::cast);
    }

}
