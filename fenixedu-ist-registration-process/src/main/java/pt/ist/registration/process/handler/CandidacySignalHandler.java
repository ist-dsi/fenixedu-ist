package pt.ist.registration.process.handler;

import java.util.function.Consumer;

import org.fenixedu.academic.domain.candidacy.workflow.RegistrationOperation.RegistrationCreatedByCandidacy;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.ui.service.RegistrationDeclarationCreatorService;
import pt.ist.registration.process.ui.service.SignCertAndStoreService;
import pt.ist.registration.process.ui.service.exception.ProblemsGeneratingDocumentException;

public class CandidacySignalHandler implements Consumer<RegistrationCreatedByCandidacy> {

    private static final Logger logger = LoggerFactory.getLogger(CandidacySignalHandler.class);
    public static final String SIGNATURE_FIELD = "signatureField";

    private final RegistrationDeclarationCreatorService documentService;
    private final SignCertAndStoreService signCertAndStoreService;

    public CandidacySignalHandler(RegistrationDeclarationCreatorService documentService,
            SignCertAndStoreService signCertAndStoreService) {
        this.documentService = documentService;
        this.signCertAndStoreService = signCertAndStoreService;
    }

    @Override
    public void accept(RegistrationCreatedByCandidacy registrationCreatedByCandidacy) {
        Registration registration = registrationCreatedByCandidacy.getInstance();
        try {
            documentService.generateAndSaveDocumentsForRegistration(registration);
            sendDocumentToBeSigned(registration);
        } catch (ProblemsGeneratingDocumentException e) {
            logger.error("Error generating registration declaration document", e);
        }
    }
    
    @Atomic(mode = TxMode.READ)
    private void sendDocumentToBeSigned(Registration registration)
            throws ProblemsGeneratingDocumentException, Error {
        String studentNumber = registration.getNumber().toString();
        RegistrationDeclarationFile declarationFile = registration.getRegistrationDeclarationFile();
        String filename = registration.getPerson().getDocumentIdNumber() + "_sign_request.pdf";
        byte[] documentWithSignatureField = documentService.generateDocumentWithSignatureField(registration, SIGNATURE_FIELD);
        String queue = getQueue(registration);
        String title = studentNumber + ": Declaração de Matrícula";
        String description = "Declaração de Matrícula do ano letivo " + registration.getStartExecutionYear().getName();
        String externalIdentifier = declarationFile.getUniqueIdentifier();
        logger.debug("Sending document to be signed with id {}", declarationFile.getUniqueIdentifier());
        signCertAndStoreService.sendDocumentToBeSigned(registration.getExternalId(), queue, title, description, filename,
                documentWithSignatureField, externalIdentifier);
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
