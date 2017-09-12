package pt.ist.registration.process.domain;

import java.util.Locale;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.gson.Gson;

public class DeclarationTemplate extends DeclarationTemplate_Base {
    
    private DeclarationTemplate() {
        super();
    }

    public DeclarationTemplate(String name, LocalizedString displayName, String templateHtml, Locale locale, String
    filenameFormat, String displayNameFormat, pt.ist.registration.process.domain.SignatureFieldSettings
    signatureFieldSettings, Group accessGroup) {
        setBennuTemplates(Bennu.getInstance());
        setName(name);
        setDisplayName(displayName);
        setTemplateHtml(templateHtml);
        setLocale(locale);
        setFilenameFormat(filenameFormat);
        setDisplayNameFormat(displayNameFormat);
        setSignatureFieldsSettings(signatureFieldSettings);
        setPersistentAccessGroup(accessGroup.toPersistentGroup());
    }

    public void setSignatureFieldsSettings(pt.ist.registration.process.domain.SignatureFieldSettings settings) {
        setSignatureFieldSettingsElement(new Gson().toJsonTree(settings));
    }

    public pt.ist.registration.process.domain.SignatureFieldSettings getSignatureFieldsSettings() {
        return new Gson().fromJson(getSignatureFieldSettingsElement(), pt.ist.registration.process.domain
        .SignatureFieldSettings.class);
    }


    public Group getAccessGroup() {
        return getPersistentAccessGroup() == null ? null : getPersistentAccessGroup().toGroup();
    }

    public void setAccessGroup(Group group) {
        setPersistentAccessGroup(group == null ? null : group.toPersistentGroup());
    }

    public void delete() {
        setAccessGroup(null);
        setBennuTemplates(null);
        setBennuFirstTimeRegistrationTemplate(null);
        super.deleteDomainObject();
    }
}
