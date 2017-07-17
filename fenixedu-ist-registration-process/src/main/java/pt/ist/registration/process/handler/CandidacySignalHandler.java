package pt.ist.registration.process.handler;

import java.util.function.Consumer;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.workflow.RegistrationOperation.RegistrationCreatedByCandidacy;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.ui.service.RegistrationDeclarationCreatorService;
import pt.ist.registration.process.ui.service.SignCertAndStoreService;
import pt.ist.registration.process.ui.service.exception.ProblemsGeneratingDocumentException;

@Component
public class CandidacySignalHandler implements Consumer<RegistrationCreatedByCandidacy> {

    private static final Logger logger = LoggerFactory.getLogger(CandidacySignalHandler.class);
    public static final String SIGNATURE_FIELD = "signatureField";

    private final RegistrationDeclarationCreatorService documentService;
    private final SignCertAndStoreService signCertAndStoreService;

    @Autowired
    public CandidacySignalHandler(RegistrationDeclarationCreatorService documentService,
            SignCertAndStoreService signCertAndStoreService) {
        this.documentService = documentService;
        this.signCertAndStoreService = signCertAndStoreService;
    }

    @Override
    public void accept(RegistrationCreatedByCandidacy registrationCreatedByCandidacy) {
        sendDocumentsToBeSigned(registrationCreatedByCandidacy);
    }

    @Atomic(mode = TxMode.READ)
    private void sendDocumentsToBeSigned(RegistrationCreatedByCandidacy registrationCreatedByCandidacy) {
        Registration registration = registrationCreatedByCandidacy.getInstance();
        CoreConfiguration.supportedLocales().forEach(locale -> {
            try {
                ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
                RegistrationDeclarationFile registrationDeclarationFile = documentService
                    .generateAndSaveDocumentsForRegistration(registration, executionYear, locale);

                String filename = registrationDeclarationFile.getFilename();
                byte[] documentWithSignatureField = documentService.generateDocumentWithSignatureField(
                        registrationDeclarationFile, SIGNATURE_FIELD);
                String queue = getQueue(registration);

                String username = registration.getPerson().getUsername();
                String language = registrationDeclarationFile.getLocale().getLanguage();
                String degreeName = registration.getDegree().getSigla();
                String executionYearName = executionYear.getName();
                String title =  String.format("%s - Declaração de Matrícula %s %s %s", username, language, degreeName, executionYearName);
                String description = "Declaração de Matrícula do ano letivo " + executionYearName;
                String externalIdentifier = registrationDeclarationFile.getUniqueIdentifier();

                signCertAndStoreService.sendDocumentToBeSigned(registration.getExternalId(), queue, title, description, filename,
                        documentWithSignatureField, externalIdentifier);
            } catch (ProblemsGeneratingDocumentException e) {
                logger.error("Error generating registration declaration document", e);
            }
        });
    }

    private String getQueue(Registration registration) {
        String campusName = registration.getCampus().getName().toLowerCase();

        if (campusName.contains("alameda")) {
            return RegistrationProcessConfiguration.getConfiguration().signerAlamedaQueue();
        }

        if (campusName.contains("taguspark")) {
            return RegistrationProcessConfiguration.getConfiguration().signerTagusparkQueue();
        }

        logger.error("Can't select campus for registration id " + registration.getExternalId());
        return null;
    }

}
