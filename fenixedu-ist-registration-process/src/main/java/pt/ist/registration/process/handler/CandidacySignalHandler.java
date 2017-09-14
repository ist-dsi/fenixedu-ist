package pt.ist.registration.process.handler;

import java.util.function.Consumer;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.workflow.RegistrationOperation.RegistrationCreatedByCandidacy;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.registration.process.domain.DeclarationTemplate;
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
        ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();

        Bennu.getInstance().getFirstTimeRegistrationTemplateSet().forEach(declarationTemplate -> {
            sendDocumentToBeSigned(registration, executionYear, declarationTemplate);
        });
    }

    public void sendDocumentToBeSigned(Registration registration, ExecutionYear executionYear, DeclarationTemplate declarationTemplate) {
        try {

            RegistrationDeclarationFile registrationDeclarationFile = documentService.generateAndSaveFile(registration, executionYear, declarationTemplate);

            String filename = registrationDeclarationFile.getFilename();
            String title = registrationDeclarationFile.getDisplayName();
            String queue = getQueue(registration);
            
            String externalIdentifier = registrationDeclarationFile.getUniqueIdentifier();
            signCertAndStoreService.sendDocumentToBeSigned(registration.getExternalId(), queue, title, title, filename,
                    registrationDeclarationFile.getStream(), externalIdentifier);
        } catch (ProblemsGeneratingDocumentException e) {
            logger.error("Error generating registration declaration document", e);
        }
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
