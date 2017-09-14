package pt.ist.registration.process.ui;

import java.io.IOException;
import java.util.Optional;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.fenixedu.bennu.core.security.SkipCSRF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Joiner;

import io.jsonwebtoken.Jwts;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.ui.exception.UnauthorizedException;
import pt.ist.registration.process.ui.service.SignCertAndStoreService;

@RestController
@RequestMapping("/registration-process/registration-declarations")
public class SignedDeclarationsController {
    
    
    private static final Logger logger = LoggerFactory.getLogger(SignedDeclarationsController.class);

    private final SignCertAndStoreService signCertAndStoreService;

    @Autowired
    public SignedDeclarationsController(SignCertAndStoreService signCertAndStoreService) {
        this.signCertAndStoreService = signCertAndStoreService;
    }

    @SkipCSRF
    @RequestMapping(method = RequestMethod.POST, value = "sign/{registration}")
    public String signCallback(@PathVariable Registration registration, @RequestParam MultipartFile file,
            @RequestParam String nounce) {
        String uniqueIdentifier = Jwts.parser().setSigningKey(RegistrationProcessConfiguration.signerJwtSecret())
                .parseClaimsJws(nounce).getBody().getSubject();
        Optional<RegistrationDeclarationFile> registrationDeclarationFile =
                RegistrationDeclarationFile.getRegistrationDeclarationFile(registration, uniqueIdentifier);
        return registrationDeclarationFile.map(declarationFile -> {
            try {
                logger.debug("Registration Declaration {} of student {} was signed.",uniqueIdentifier, registration.getNumber());
                logger.debug("Registration Declaration {} of student {} sent to be certified", uniqueIdentifier, registration.getNumber());
                signCertAndStoreService.sendDocumentToBeCertified(registration.getExternalId(), declarationFile.getFilename(), file,
                        uniqueIdentifier, true);
            } catch (IOException e) {
                throw new Error(e);
            }
            return "ok";
        }).orElseThrow(UnauthorizedException::new);
    }

    @SkipCSRF
    @RequestMapping(method = RequestMethod.POST, value = "cert/{registration}")
    public String certifierCallback(@PathVariable Registration registration, @RequestParam MultipartFile file,
            @RequestParam String nounce) {
        String uniqueIdentifier = Jwts.parser().setSigningKey(RegistrationProcessConfiguration.certifierJwtSecret())
                .parseClaimsJws(nounce).getBody().getSubject();
        Optional<RegistrationDeclarationFile> registrationDeclarationFile =
                RegistrationDeclarationFile.getRegistrationDeclarationFile(registration, uniqueIdentifier);
        return registrationDeclarationFile.map(declarationFile -> {
            try {
                logger.debug("Registration Declaration {} of student {} was certified.", uniqueIdentifier, registration.getNumber());
                logger.debug("Registration Declaration {} of student {} sent out to be stored.", uniqueIdentifier, registration.getNumber());
                signCertAndStoreService.sendDocumentToBeStored(registration.getPerson().getUsername(), registration.getPerson().getEmailForSendingEmails(), declarationFile, file);
                return "ok";
            } catch (IOException e) {
                throw new Error(e);
            }
        }).orElseThrow(UnauthorizedException::new);
    }

}
