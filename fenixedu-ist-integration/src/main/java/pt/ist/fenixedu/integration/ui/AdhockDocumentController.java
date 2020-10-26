package pt.ist.fenixedu.integration.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.rest.JsonBodyReaderWriter;
import org.fenixedu.bennu.core.security.SkipCSRF;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.DriveAPIStorage;
import org.fenixedu.bennu.io.domain.FileSupport;
import org.fenixedu.jwt.Tools;
import org.fenixedu.messaging.core.domain.Message;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pt.ist.fenixframework.FenixFramework;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

@RestController
@RequestMapping("/adhock-document")
public class AdhockDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(AdhockDocumentController.class);

    @SkipCSRF
    @RequestMapping(method = RequestMethod.POST, value = "store/{user}")
    public String storeCallback(@PathVariable User user, @RequestParam MultipartFile file,
                                @RequestParam String nounce) {
        if (logger.isDebugEnabled()) {
            logger.debug("Running AdhockDocumentController store callback.");
        }
        final String uuid = Jwts.parser().setSigningKey(RegistrationProcessConfiguration.signerJwtSecret())
                .parseClaimsJws(nounce).getBody().getSubject();
        if (logger.isDebugEnabled()) {
            logger.debug("Processing UUID: " + uuid);
        }
        final String filename = file.getOriginalFilename();
        if (logger.isDebugEnabled()) {
            logger.debug("   filename: " + filename);
        }
        try {
            final byte[] document = file.getBytes();
            if (logger.isDebugEnabled()) {
                logger.debug("   read " + document.length + " bytes.");
            }
            sendDocumentToBeCertified(filename, new ByteArrayInputStream(document), uuid);
            if (logger.isDebugEnabled()) {
                logger.debug("   sent to certifier.");
            }
            final String downloadFileLink = upload(user.getUsername(), filename, document);
            if (logger.isDebugEnabled()) {
                logger.debug("   sent to drive: " + downloadFileLink);
            }
            sendEmailNotification(user.getEmail(), filename, downloadFileLink);
            if (logger.isDebugEnabled()) {
                logger.debug("   email sent");
            }
            return "ok";
        } catch (final IOException ex) {
            throw new Error(ex);
        }
    }

    public void sendDocumentToBeCertified(final String filename, final InputStream inputStream, final String uuid) {
        String compactJws = Jwts.builder().setSubject(uuid)
                .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.certifierJwtSecret()).compact();

        try (final FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            final StreamDataBodyPart streamDataBodyPart =
                    new StreamDataBodyPart("file", inputStream, filename, new MediaType("application", "pdf"));
            formDataMultiPart.bodyPart(streamDataBodyPart);
            formDataMultiPart.bodyPart(new FormDataBodyPart("filename", filename));
            formDataMultiPart.bodyPart(new FormDataBodyPart("mimeType", "application/json"));
            formDataMultiPart.bodyPart(new FormDataBodyPart("identifier", uuid));
            formDataMultiPart.bodyPart(new FormDataBodyPart("alreadyCertified", Boolean.FALSE.toString()));

            final String nounce = Jwts.builder().setSubject(uuid)
                    .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.certifierJwtSecret()).compact();
            formDataMultiPart.bodyPart(new FormDataBodyPart("callback", CoreConfiguration.getConfiguration().applicationUrl()
                    + "/registration-process/registration-declarations/cert/" + "registrationExternalId" + "?nounce=" + nounce));
            final Client client = ClientBuilder.newClient();
            client.register(MultiPartFeature.class);
            client.register(JsonBodyReaderWriter.class);
            client.target(RegistrationProcessConfiguration.getConfiguration().certifierUrl())
                    .path("api/documents/certify").request().header("Authorization", "Bearer " + compactJws)
                    .post(Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA_TYPE), String.class);
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    private String upload(final String username, final String filename, final byte[] content) {
        final FileSupport fileSupport = FileSupport.getInstance();
        final DriveAPIStorage driveAPIStorage = fileSupport.getFileStorageSet().stream()
                .filter(DriveAPIStorage.class::isInstance)
                .map(DriveAPIStorage.class::cast)
                .findAny().orElseThrow(() -> new Error());

        final String directory = dirFor(username);
        final MultipartBody request = Unirest.post(driveAPIStorage.getDriveUrl() + "/api/drive/directory/" + directory)
                .header("Authorization", "Bearer " + getAccessToken("fenix"))
                .header("X-Requested-With", "XMLHttpRequest")
                .field("path", "");
        final Function<MultipartBody, MultipartBody> fileSetter = b -> b.field("file", content, filename);
        final HttpResponse<String> response = fileSetter.apply(request).asString();
        final JsonObject result = new JsonParser().parse(response.getBody()).getAsJsonObject();
        final JsonElement id = result.get("id");
        if (id == null || id.isJsonNull()) {
            throw new Error(result.toString());
        }
        return result.get("downloadFileLink").getAsString();
    }

    private String dirFor(final String username) {
        final FileSupport fileSupport = FileSupport.getInstance();
        final DriveAPIStorage driveAPIStorage = fileSupport.getFileStorageSet().stream()
                .filter(DriveAPIStorage.class::isInstance)
                .map(DriveAPIStorage.class::cast)
                .findAny().orElseThrow(() -> new Error());
        final GetRequest getRequest = Unirest.get(driveAPIStorage.getDriveUrl() + "/api/drive/directory/")
                .queryString("slug", username + "/documentos-oficiais/declaracoes")
                .header("Authorization", "Bearer " + getAccessToken(username))
                .header("X-Requested-With", "XMLHttpRequest");
        final HttpResponse<String> response = getRequest.asString();
        final JsonObject result = new JsonParser().parse(response.getBody()).getAsJsonObject();
        final JsonElement id = result.get("id");
        if (id == null || id.isJsonNull()) {
            throw new Error(result.toString());
        }
        return id.getAsString();
    }

    private String getAccessToken(final String username) {
        final JsonObject claim = new JsonObject();
        claim.addProperty("username", username);
        return Tools.sign(SignatureAlgorithm.RS256, CoreConfiguration.getConfiguration().jwtPrivateKeyPath(), claim);
    }

    private void sendEmailNotification(final String email, final String displayName, final String link) {
        final String title = "Disponibilização de Documento";
        final String body = "Ficou agora disponível o documento " + displayName + " ao qual pode aceder "
                + "acedendo ao drive do técnico ou por meio do seguinte link: " + link;
        FenixFramework.atomic(() -> {
            Message.fromSystem()
                    .replyToSender()
                    .singleBcc(email)
                    .subject(title)
                    .textBody(body)
                    .send();
        });
    }

}
