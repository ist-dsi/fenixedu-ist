package pt.ist.registration.process.ui.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.fenixedu.academic.domain.util.email.Message;
import org.fenixedu.academic.domain.util.email.SystemSender;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.rest.JsonBodyReaderWriter;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import pt.ist.drive.sdk.ClientFactory;
import pt.ist.drive.sdk.DriveClient;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.registration.process.handler.CandidacySignalHandler;

@Service
public class SignCertAndStoreService {

    private final static String RESOURCE_BUNDLE = "resources.RegistrationProcessResources";
    private static final Logger logger = LoggerFactory.getLogger(SignCertAndStoreService.class);

    private final Client client;

    public SignCertAndStoreService() {
        this.client = ClientBuilder.newClient();
        this.client.register(MultiPartFeature.class);
        this.client.register(JsonBodyReaderWriter.class);
    }

    public void sendDocumentToBeSigned(String registrationExternalId, String queue, String title, String description,
            String filename, byte[] documentWithSignatureField, String uniqueIdentifier) throws Error {
        String compactJws = Jwts.builder().setSubject(RegistrationProcessConfiguration.getConfiguration().signerJwtUser())
                .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.signerJwtSecret()).compact();
        final StreamDataBodyPart streamDataBodyPart = new StreamDataBodyPart("file",
                new ByteArrayInputStream(documentWithSignatureField), filename, new MediaType("application", "pdf"));
        try (final FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            formDataMultiPart.bodyPart(streamDataBodyPart);
            formDataMultiPart.bodyPart(new FormDataBodyPart("queue", queue));
            formDataMultiPart.bodyPart(
                    new FormDataBodyPart("creator", RegistrationProcessConfiguration.getConfiguration().signerJwtUser()));
            formDataMultiPart.bodyPart(new FormDataBodyPart("filename", filename));
            formDataMultiPart.bodyPart(new FormDataBodyPart("title", title));
            formDataMultiPart.bodyPart(new FormDataBodyPart("description", description));
            formDataMultiPart.bodyPart(new FormDataBodyPart("externalIdentifier", uniqueIdentifier));
            formDataMultiPart.bodyPart(new FormDataBodyPart("signatureField", CandidacySignalHandler.SIGNATURE_FIELD));

            String nounce = Jwts.builder().setSubject(uniqueIdentifier)
                    .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.signerJwtSecret()).compact();

            formDataMultiPart.bodyPart(new FormDataBodyPart("callbackUrl", CoreConfiguration.getConfiguration().applicationUrl()
                    + "/registration-process/registration-declarations/sign/" + registrationExternalId + "?nounce=" + nounce));
            String result = client.target(RegistrationProcessConfiguration.getConfiguration().signerUrl()).path("sign-requests")
                    .request().header("Authorization", "Bearer " + compactJws)
                    .post(Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA_TYPE), String.class);
            logger.debug(result);
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    public void sendDocumentToBeCertified(String registrationExternalId, String filename, MultipartFile file,
            String uniqueIdentifier) throws IOException {
        String compactJws = Jwts.builder().setSubject(uniqueIdentifier)
                .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.certifierJwtSecret()).compact();
        final StreamDataBodyPart streamDataBodyPart =
                new StreamDataBodyPart("file", file.getInputStream(), filename, new MediaType("application", "pdf"));
        try (final FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            formDataMultiPart.bodyPart(streamDataBodyPart);
            formDataMultiPart.bodyPart(new FormDataBodyPart("filename", filename));
            formDataMultiPart.bodyPart(new FormDataBodyPart("mimeType", "application/json"));
            formDataMultiPart.bodyPart(new FormDataBodyPart("identifier", uniqueIdentifier));

            String nounce = Jwts.builder().setSubject(uniqueIdentifier)
                    .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.certifierJwtSecret()).compact();
            formDataMultiPart.bodyPart(new FormDataBodyPart("callback", CoreConfiguration.getConfiguration().applicationUrl()
                    + "/registration-process/registration-declarations/cert/" + registrationExternalId + "?nounce=" + nounce));
            String result = client.target(RegistrationProcessConfiguration.getConfiguration().certifierUrl())
                    .path("api/documents/certify").request().header("Authorization", "Bearer " + compactJws)
                    .post(Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA_TYPE), String.class);
            logger.debug(result);
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    public void sendDocumentToBeStored(String username, String email, String filename, MultipartFile file,
            String uniqueIdentifier) throws IOException {
        String driveUrl = RegistrationProcessConfiguration.getConfiguration().storeUrl();
        String appId = RegistrationProcessConfiguration.getConfiguration().storeAppId();
        String appUser = RegistrationProcessConfiguration.getConfiguration().storeAppUser();
        String refreshToken = RegistrationProcessConfiguration.getConfiguration().storeAppRefreshToken();
        JsonObject result = ClientFactory.driveCLient(driveUrl, appId, appUser, refreshToken)
                .uploadWithInfo(uploadDirectoryFor(username).get(), filename, file.getInputStream(), file.getContentType());
        logger.debug("Registration Declaration {} of student {} stored", uniqueIdentifier, username);
        logger.debug("Registration Declaration {} of student {} is being emailed.", uniqueIdentifier, username);
        sendEmailNotification(email, result.get("downloadFileLink").getAsString());
    }

    @Atomic(mode = TxMode.WRITE)
    private void sendEmailNotification(String email, String link) {
        String title = BundleUtil.getString(RESOURCE_BUNDLE, "registration.document.email.title");
        String body = BundleUtil.getString(RESOURCE_BUNDLE, "registration.document.email.body", link);
        SystemSender systemSender = Bennu.getInstance().getSystemSender();
        new Message(systemSender, systemSender.getConcreteReplyTos(), Collections.EMPTY_LIST, title, body, email);
    }

    private Optional<String> uploadDirectoryFor(String username) {
        String directoryName = RegistrationProcessConfiguration.getConfiguration().storeDirectoryName();
        String driveUrl = RegistrationProcessConfiguration.getConfiguration().storeUrl();
        String appId = RegistrationProcessConfiguration.getConfiguration().storeAppId();
        String refreshToken = RegistrationProcessConfiguration.getConfiguration().storeAppRefreshToken();
        DriveClient driveCLient = ClientFactory.driveCLient(driveUrl, appId, username, refreshToken);
        JsonArray folders = driveCLient.listDirectory("/");
        for (JsonElement el : folders) {
            if (el.getAsJsonObject().get("name").getAsString().equals(directoryName)) {
                return Optional.of(el.getAsJsonObject().get("id").getAsString());
            }
        }
        return Optional.empty();
    }
}
