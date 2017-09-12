package pt.ist.registration.process.domain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Locale;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;

import com.google.common.io.Files;

import pt.ist.fenixframework.Atomic.TxMode;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
public class CreateDeclarationTemplateTask extends CustomTask{
    @Override
    public TxMode getTxMode() {
        return TxMode.WRITE;
    }

    //private static final String TEMPLATES_PATH = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/papyrus_templates/";
    private static final String TEMPLATES_PATH = "/tmp";

    @Override
    public void runTask() throws Exception {
        deleteAndCreate();
    }

    protected void deleteAndCreate() {
        Locale pt = Locale.forLanguageTag("pt-PT");
        Locale en = Locale.forLanguageTag("en-GB");
        Group studentGroup = Group.parse("activeStudents");
        Group academicRequestsGroup = Group.parse("academic(MANAGE_REGISTRATIONS)");

        Bennu.getInstance().getDeclarationTemplateSet().forEach(DeclarationTemplate::delete);

        if (Bennu.getInstance().getDeclarationTemplateSet().isEmpty()) {
            String titleFormat = "%s - Declaração de Matrícula %s %s %s";

            String filenameFormat = "%s_declaracao_%s_%s_%s.pdf";
            SignatureFieldSettings
                    signatureFieldSetttings = new SignatureFieldSettings(100, 350, 500, 250,
                    "signatureField", 1);


            String name = "declaracao-matricula-pt";
            String filename = "declaracao-matricula-pt.html";
            LocalizedString displayName = new LocalizedString.Builder().with(pt, "Declaração de Inscrição (Português)").with
                    (en, "Enrolment Declaration (Portuguese)").build();

            DeclarationTemplate declarationTemplate = new DeclarationTemplate(name, displayName, resolveHtmlFromFile(filename),
                    pt, filenameFormat, titleFormat, signatureFieldSetttings, studentGroup.or(academicRequestsGroup));
            declarationTemplate.setBennuFirstTimeRegistrationTemplate(Bennu.getInstance());

            name = "declaracao-matricula-en";
            filename = "declaracao-matricula-en.html";
            displayName = new LocalizedString.Builder().with(pt, "Declaração de Inscrição (Inglês)").with
                    (en, "Enrolment Declaration (English)").build();

            declarationTemplate = new DeclarationTemplate(name, displayName, resolveHtmlFromFile(filename), en, filenameFormat,
                    titleFormat, signatureFieldSetttings,studentGroup.or(academicRequestsGroup));
            declarationTemplate.setBennuFirstTimeRegistrationTemplate(Bennu.getInstance());

            signatureFieldSetttings = new SignatureFieldSettings(100, 220, 500, 120, "signatureField", 2);

            filenameFormat = "%s_declaracao_sub23_sembolsa_%s_%s_%s.pdf";
            name = "declaracao-transportes-sub23-sembolsa";
            filename = "declaracao-transportes-sub23-sem-bolsa.html";
            titleFormat = "%1$s - SUB23@SUPERIOR.TP - Sem Bolsa - %3$s %4$s";
            displayName = new LocalizedString.Builder().with(pt, "Declaração SUB23@SUPERIOR.TP - Sem Bolsa").with(en,
                    "SUB23@SUPERIOR.TP Declaration - Without grant").build();
            new DeclarationTemplate(name, displayName, resolveHtmlFromFile(filename), pt,filenameFormat,
                    titleFormat, signatureFieldSetttings , academicRequestsGroup);

            filenameFormat = "%s_declaracao_sub23_renovacao_bolsa_%s_%s_%s.pdf";
            name = "declaracao-transportes-sub23-renovacao-bolsa";
            filename = "declaracao-transportes-sub23-renovacao-bolsa.html";
            titleFormat = "%1$s - SUB23@SUPERIOR.TP - Renovação Bolsa - %3$s %4$s";
            displayName = new LocalizedString.Builder().with(pt, "Declaração SUB23@SUPERIOR.TP - Renovação Bolsa Pedida")
                    .with(en, "SUB23@SUPERIOR.TP Declaration - With renewal request").build();
            new DeclarationTemplate(name, displayName, resolveHtmlFromFile(filename), pt,filenameFormat,
                    titleFormat, signatureFieldSetttings , academicRequestsGroup);

            filenameFormat = "%s_declaracao_sub23_com_bolsa_%s_%s_%s.pdf";
            name = "declaracao-transportes-sub23-com-bolsa";
            filename = "declaracao-transportes-sub23-com-bolsa.html";
            titleFormat = "%1$s - SUB23@SUPERIOR.TP - Com Bolsa - %3$s %4$s";
            displayName = new LocalizedString.Builder().with(pt, "Declaração SUB23@SUPERIOR.TP - Bolsa Atribuída").with(en,
                    "SUB23@SUPERIOR.TP Declaration - With grant").build();
            new DeclarationTemplate(name, displayName, resolveHtmlFromFile(filename), pt,filenameFormat, titleFormat, signatureFieldSetttings , academicRequestsGroup);
        }
    }

    private String resolveHtmlFromFile(String name) {
        try {
            return Files.toString(Paths.get(TEMPLATES_PATH).resolve(name).toFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}
