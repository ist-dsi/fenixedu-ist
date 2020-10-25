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
import org.fenixedu.bennu.core.security.SkipCSRF;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.DriveAPIStorage;
import org.fenixedu.bennu.io.domain.FileSupport;
import org.fenixedu.jwt.Tools;
import org.fenixedu.messaging.core.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pt.ist.fenixframework.FenixFramework;

import java.io.IOException;
import java.util.function.Function;

@RestController
@RequestMapping("/adhock-document")
public class AdhockDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(AdhockDocumentController.class);

    @SkipCSRF
    @RequestMapping(method = RequestMethod.POST, value = "store/{user}")
    public String storeCallback(@PathVariable User user, @RequestParam MultipartFile file,
                                @RequestParam String nounce) {
        final String uniqueIdentifier = Jwts.parser().setSigningKey(RegistrationProcessConfiguration.signerJwtSecret())
                .parseClaimsJws(nounce).getBody().getSubject();
        final String filename = file.getOriginalFilename();
        try {
            final String downloadFileLink = upload(user.getUsername(), filename, file.getBytes());
            sendEmailNotification(user.getEmail(), filename, downloadFileLink);
            return "ok";
        } catch (final IOException ex) {
            throw new Error(ex);
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
