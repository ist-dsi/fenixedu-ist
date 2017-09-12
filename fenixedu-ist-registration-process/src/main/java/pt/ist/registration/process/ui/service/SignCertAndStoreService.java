package pt.ist.registration.process.ui.service;

import static org.fenixedu.bennu.RegistrationProcessConfiguration.RESOURCE_BUNDLE;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
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
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.gson.JsonObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import pt.ist.drive.sdk.ClientFactory;
import pt.ist.drive.sdk.DriveClient;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.handler.CandidacySignalHandler;

@Service
public class SignCertAndStoreService {

    private static final Logger logger = LoggerFactory.getLogger(SignCertAndStoreService.class);

    private final Client client;
    private final String driveUrl;
    private final String appId;
    private final String refreshToken;
    private final String appUser;

    public SignCertAndStoreService() {
        this.client = ClientBuilder.newClient();
        this.client.register(MultiPartFeature.class);
        this.client.register(JsonBodyReaderWriter.class);
        driveUrl = RegistrationProcessConfiguration.getConfiguration().storeUrl();
        appId = RegistrationProcessConfiguration.getConfiguration().storeAppId();
        refreshToken = RegistrationProcessConfiguration.getConfiguration().storeAppRefreshToken();
        appUser = RegistrationProcessConfiguration.getConfiguration().storeAppUser();
    }

    public void sendDocumentToBeSigned(String registrationExternalId, String queue, String title, String description,
            String filename, InputStream contentStream, String uniqueIdentifier) throws Error {
        logger.debug("Sending document to be signed with id {}", uniqueIdentifier);
        String compactJws = Jwts.builder()
                                .setSubject(RegistrationProcessConfiguration.getConfiguration().signerJwtUser())
                                .setExpiration(DateTime.now().plusHours(6).toDate())
                                .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.signerJwtSecret()).compact();

        try (final FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            final StreamDataBodyPart streamDataBodyPart = new StreamDataBodyPart("file", contentStream, filename, new MediaType("application", "pdf"));
            formDataMultiPart.bodyPart(streamDataBodyPart);
            formDataMultiPart.bodyPart(new FormDataBodyPart("queue", queue));
            formDataMultiPart.bodyPart(
                    new FormDataBodyPart("creator", "Sistema FenixEdu"));
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
            String uniqueIdentifier, boolean alreadyCertified) throws IOException {
        String compactJws = Jwts.builder().setSubject(uniqueIdentifier)
                .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.certifierJwtSecret()).compact();

        try (final FormDataMultiPart formDataMultiPart = new FormDataMultiPart(); InputStream fileStream = file.getInputStream()) {
            final StreamDataBodyPart streamDataBodyPart =
                    new StreamDataBodyPart("file", fileStream, filename, new MediaType("application", "pdf"));
            formDataMultiPart.bodyPart(streamDataBodyPart);
            formDataMultiPart.bodyPart(new FormDataBodyPart("filename", filename));
            formDataMultiPart.bodyPart(new FormDataBodyPart("mimeType", "application/json"));
            formDataMultiPart.bodyPart(new FormDataBodyPart("identifier", uniqueIdentifier));
            formDataMultiPart.bodyPart(new FormDataBodyPart("alreadyCertified", Boolean.valueOf(alreadyCertified).toString()));

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
            RegistrationDeclarationFile declarationFile) throws IOException {
        DriveClient driveClient = ClientFactory.driveCLient(driveUrl, appId, appUser, refreshToken);
        String directory = uploadDirectoryFor(driveClient, username).get();
        try (InputStream fileStream = file.getInputStream()) {
            JsonObject result = driveClient.uploadWithInfo(directory, filename, fileStream, file.getContentType());
            logger.debug("Registration Declaration {} of student {} stored", declarationFile.getUniqueIdentifier(), username);
            logger.debug("Registration Declaration {} of student {} is being emailed.", declarationFile.getUniqueIdentifier(), username);
            sendEmailNotification(email, declarationFile.getDisplayName(), result.get("downloadFileLink").getAsString());
        }
    }

    @Atomic(mode = TxMode.WRITE)
    private void sendEmailNotification(String email, String displayName,  String link) {
        String title = BundleUtil.getString(RESOURCE_BUNDLE, "registration.document.email.title", displayName);
        String body = BundleUtil.getString(RESOURCE_BUNDLE, "registration.document.email.body", displayName, link);

        SystemSender systemSender = Bennu.getInstance().getSystemSender();
        new Message(systemSender, systemSender.getConcreteReplyTos(), Collections.EMPTY_LIST, title, body, email);
    }

    private Optional<String> uploadDirectoryFor(DriveClient driveClient, String username) {
        String directoryName = Joiner.on("/").join(username, RegistrationProcessConfiguration.getConfiguration().storeDirectoryName());
        List<String> slugParts = Splitter.on("/").splitToList(directoryName);
        JsonObject directory = driveClient.getDirectoryWithSlug(slugParts);
        return Optional.ofNullable(directory).map(o -> o.get("id").getAsString());
    }
}
