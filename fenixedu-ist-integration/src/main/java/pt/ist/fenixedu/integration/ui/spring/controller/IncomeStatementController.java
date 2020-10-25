/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.ui.spring.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.SignatureAlgorithm;
import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.DriveAPIStorage;
import org.fenixedu.bennu.io.domain.FileSupport;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.jwt.Tools;
import org.fenixedu.messaging.core.domain.Message;
import org.joda.time.DateTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@SpringApplication(group = "logged", path = "income-statement", title = "title.income.statement")
@SpringFunctionality(app = IncomeStatementController.class, title = "title.income.statement")
@Controller
@RequestMapping("/income-statement")
public class IncomeStatementController {

    @RequestMapping(method = RequestMethod.GET)
    public String home(final Model model) {
        final User user = Authenticate.getUser();
        final List<JsonObject> documents = new ArrayList<>();
        consumeDirectory(user, "1414452989674240", json -> {
            final DateTime created = new DateTime(json.get("created").getAsLong());
            final DateTime modified = new DateTime(json.get("modified").getAsLong());
            final String year = json.get("parent").getAsJsonObject().get("name").getAsString();
            final JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("year", year);
            jsonObject.addProperty("created", created.toString("yyyy-MM-dd HH:mm"));
            jsonObject.addProperty("modified", modified.toString("yyyy-MM-dd HH:mm"));
            jsonObject.addProperty("checksum", json.get("checksum").getAsString());
            documents.add(jsonObject);
        });
        model.addAttribute("documents", documents);
        return "fenixedu-ist-integration/incomeStatement";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String submit(final @RequestParam("year") int year, final @RequestParam("file") MultipartFile file,
                         final Model model) {
        if (file.getSize() > 0l) {
            final User user = Authenticate.getUser();
            final String fileprefix = user.getUsername();
            try {
                final String instante = new DateTime().toString("yyyy-MM-dd HH:mm");
                final String declaration = BundleUtil.getString("resources.FenixeduIstIntegrationResources",
                        "title.income.statement.declaration", user.getPerson().getName())
                        .replaceAll("<br/>", "\n")
                        + "\n\n" + instante;
                upload(Integer.toString(year), fileprefix + ".txt", declaration.getBytes());
                upload(Integer.toString(year), fileprefix + ".pdf", file.getBytes());

                Message.fromSystem().singleTos(user.getEmail())
                        .subject(BundleUtil.getString("resources.FenixeduIstIntegrationResources",
                                "title.income.statement.declaration.subject", Integer.toString(year)))
                        .textBody(BundleUtil.getString("resources.FenixeduIstIntegrationResources",
                                "title.income.statement.declaration.body", file.getOriginalFilename(), Long.toString(file.getSize()),
                                        user.getPerson().getName(), instante))
                        .send();
            } catch (final IOException e) {
                throw new Error(e);
            }
            return "fenixedu-ist-integration/incomeStatementSubmitted";
        } else {
            model.addAttribute("errorMessage", BundleUtil.getString("resources.FenixeduIstIntegrationResources",
                    "error.file.cannot.be.empty"));
            return "fenixedu-ist-integration/incomeStatement";
        }
    }

    private void upload(final String year, final String filename, final byte[] content) {
        final FileSupport fileSupport = FileSupport.getInstance();
        final DriveAPIStorage driveAPIStorage = fileSupport.getFileStorageSet().stream()
                .filter(DriveAPIStorage.class::isInstance)
                .map(DriveAPIStorage.class::cast)
                .findAny().orElseThrow(() -> new Error());

        final MultipartBody request = Unirest.post(driveAPIStorage.getDriveUrl() + "/api/drive/directory/" + "1414452989674240")
                .header("Authorization", "Bearer " + getAccessToken(driveAPIStorage))
                .header("X-Requested-With", "XMLHttpRequest")
                .field("path", year);
        final Function<MultipartBody, MultipartBody> fileSetter = b ->  b.field("file", content, filename);
        final HttpResponse<String> response = fileSetter.apply(request).asString();
        final JsonObject result = new JsonParser().parse(response.getBody()).getAsJsonObject();
        final JsonElement id = result.get("id");
        if (id == null || id.isJsonNull()) {
            throw new Error(result.toString());
        }
    }

    private transient String accessToken = null;
    private transient long accessTokenValidUnit = System.currentTimeMillis() - 1;

    private String getAccessToken(final DriveAPIStorage driveAPIStorage) {
        if (accessToken == null || System.currentTimeMillis() >= accessTokenValidUnit) {
            synchronized (this) {
                if (accessToken == null || System.currentTimeMillis() >= accessTokenValidUnit) {
                    final JsonObject claim = new JsonObject();
                    claim.addProperty("username", driveAPIStorage.getRemoteUsername());
                    accessToken = Tools.sign(SignatureAlgorithm.RS256, CoreConfiguration.getConfiguration().jwtPrivateKeyPath(), claim);
                }
            }
        }
        return accessToken;
    }

    private void consumeDirectory(final User user, final String remoteDirId, final Consumer<JsonObject> consumer) {
        final FileSupport fileSupport = FileSupport.getInstance();
        final DriveAPIStorage driveAPIStorage = fileSupport.getFileStorageSet().stream()
                .filter(DriveAPIStorage.class::isInstance)
                .map(DriveAPIStorage.class::cast)
                .findAny().orElseThrow(() -> new Error());

        final GetRequest request = Unirest.get(driveAPIStorage.getDriveUrl() + "/api/drive/directory/" + remoteDirId)
                .header("Authorization", "Bearer " + getAccessToken(driveAPIStorage))
                .header("X-Requested-With", "XMLHttpRequest");
        final JsonObject directory = new JsonParser().parse(request.asString().getBody()).getAsJsonObject();
        for (final JsonElement element : directory.get("items").getAsJsonArray()) {
            final JsonObject node = element.getAsJsonObject();
            final String name = node.get("name").getAsString();
            final String type = node.get("type").getAsString();
            if ("application/vnd.fenixedu.drive.directory".equals(type)) {
                consumeDirectory(user, node.get("id").getAsString(), consumer);
            } else if (name.equals(user.getUsername() + ".pdf")) {
                consumer.accept(node);
            }
        }
    }

}
