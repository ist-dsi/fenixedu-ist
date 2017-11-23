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
package pt.ist.fenixedu.integration.api;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fenixedu.academic.FenixEduAcademicConfiguration;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.portal.api.json.SupportConfigurationViewer;
import org.fenixedu.bennu.portal.domain.MenuFunctionality;
import org.fenixedu.bennu.portal.domain.SupportConfiguration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.contracts.domain.LegacyRoleUtils;
import pt.ist.fenixframework.FenixFramework;

@Path("/fenix-ist/support-form")
public class SupportFormResource extends BennuRestResource {

    private static final Logger logger = LoggerFactory.getLogger(SupportFormResource.class);

    private static final String SEPARATOR =
            "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n";

    private static final String hostname = getHostName();

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "<Host name unknown>";
        }
    }

    private static boolean validateEmail(String email) {
        try {
            InternetAddress address = new InternetAddress(email);
            address.validate();
            return true;
        } catch (AddressException e) {
            return false;
        }
    }

    @Path("{oid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement getSupportConfigurations(@PathParam("oid") final String menuOid) {
        JsonObject jsonObject = new JsonObject();
        MenuFunctionality menuFunctionality = Strings.isNullOrEmpty(menuOid) ? null : FenixFramework.getDomainObject(menuOid);
        jsonObject.add("default", view(menuFunctionality.getSupport(), SupportConfigurationViewer.class));
        jsonObject.add("options", view(Bennu.getInstance().getSupportConfigurationSet().stream()
                .filter(support -> !support.equals(menuFunctionality.getSupport())), SupportConfigurationViewer.class));
        return jsonObject;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendSupportForm(String jsonStr, @Context HttpServletRequest request) {
        SupportBean bean = new Gson().fromJson(jsonStr, SupportBean.class);

        String email = bean.getEmail();

        String mailSubject = generateEmailSubject(bean);
        String mailBody = generateEmailBody(bean);

        if (CoreConfiguration.getConfiguration().developmentMode()) {
            logger.warn("Submitted error form from {} to {}: '{}'\n{}", email, bean.getSupportEmail(), mailSubject, mailBody);
        } else {
            sendEmail(validateEmail(email) ? email : CoreConfiguration.getConfiguration().defaultSupportEmailAddress(),
                    mailSubject, mailBody, bean);
        }
        return Response.ok().build();
    }

    private String generateEmailSubject(SupportBean bean) {
        StringBuilder builder = new StringBuilder();
        MenuFunctionality functionality = bean.getFunctionality();
        builder.append("[Fenix] [");
        builder.append(functionality != null ? functionality.getPathFromRoot().get(0).getTitle().getContent() : "").append("] ");
        builder.append('[').append(str("label.support.form.type." + bean.type)).append("] ");
        builder.append(bean.subject);
        return builder.toString();
    }

    private String generateEmailBody(SupportBean bean) {
        StringBuilder builder = new StringBuilder();
        builder.append(SEPARATOR);
        appendHeader(builder, bean);
        appendBody(builder, bean);
        return builder.toString();
    }

    private void appendBody(StringBuilder builder, SupportBean bean) {
        builder.append(SEPARATOR).append("\n");
        builder.append(bean.description).append("\n\n\n\n");
        if (!Strings.isNullOrEmpty(bean.exceptionInfo)) {
            builder.append(SEPARATOR).append(bean.exceptionInfo);
        }
    }

    private void appendHeader(StringBuilder builder, SupportBean bean) {
        generateLabel(builder, "Roles").append('[');
        Person person = AccessControl.getPerson();
        if (person != null) {
            builder.append(LegacyRoleUtils.mainRolesStr(person.getUser()));
        }
        builder.append("]\n");

        generateLabel(builder, BundleUtil.getString(Bundle.APPLICATION, "label.name"));
        if (person != null) {
            builder.append("[").append(person.getName()).append("]\n");
            generateLabel(builder, "Username");
            builder.append("[").append(person.getUsername()).append("]\n");
        } else {
            builder.append(BundleUtil.getString(Bundle.APPLICATION, "support.mail.session.error")).append('\n');
        }

        generateLabel(builder, "Email").append("[").append(bean.getEmail()).append("]\n");

        // Portal
        MenuFunctionality functionality = bean.getFunctionality();
        generateLabel(builder, "Portal").append("[");
        if (functionality != null) {
            builder.append(functionality.getPathFromRoot().stream().map(item -> item.getTitle().getContent())
                    .collect(Collectors.joining(" > ")));
        }
        builder.append("]\n");

        generateLabel(builder, "Referer").append('[').append(bean.referer).append("]\n");
        generateLabel(builder, "Browser/OS").append("[").append(bean.userAgent).append("]\n");

        generateLabel(builder, str("label.support.form.type")).append('[').append(str("label.support.form.type." + bean.type))
                .append("]\n");

        // Extra Info
        generateLabel(builder, "When").append('[').append(DateTime.now()).append("]\n");
        generateLabel(builder, "Host").append('[').append(hostname).append("]\n");
    }

    private static String str(String key) {
        return BundleUtil.getString("resources.FenixeduIstIntegrationResources", key);
    }

    private StringBuilder generateLabel(StringBuilder builder, String label) {
        builder.append(label);
        for (int i = label.length(); i <= 15; i++) {
            builder.append('_');
        }
        return builder;
    }

    private void sendEmail(String from, String subject, String body, SupportBean bean) {
        Properties props = new Properties();
        props.put("mail.smtp.host",
                MoreObjects.firstNonNull(FenixEduAcademicConfiguration.getConfiguration().getMailSmtpHost(), "localhost"));
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(bean.getSupportEmail()));
            message.setSubject(subject);
            message.setText(body);

            Multipart multipart = new MimeMultipart();
            {
                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(body);
                multipart.addBodyPart(messageBodyPart);
            }

            if (!Strings.isNullOrEmpty(bean.attachment)) {
                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(Base64.getDecoder()
                        .decode(bean.attachment), bean.mimeType)));
                messageBodyPart.setFileName(bean.fileName);
                multipart.addBodyPart(messageBodyPart);
            }

            message.setContent(multipart);

            Transport.send(message);
        } catch (Exception e) {
            logger.error("Could not send support email! Original message was: " + body, e);
        }
    }

    private static class SupportBean {
        public String mimeType;
        public String userAgent;
        public String exceptionInfo;
        public String description;
        private String functionality;
        private String support;
        private String email;
        public String subject;
        public String type = "exception";
        public String referer;
        public String attachment;
        public String fileName;

        public String getEmail() {
            if (!Strings.isNullOrEmpty(email)) {
                return email;
            } else if (Authenticate.isLogged()) {
                return Authenticate.getUser().getEmail();
            } else {
                return "noreply@fenixedu.org";
            }
        }

        public MenuFunctionality getFunctionality() {
            return Strings.isNullOrEmpty(functionality) ? null : FenixFramework.getDomainObject(functionality);
        }

        public String getSupportEmail() {
            if (Strings.isNullOrEmpty(support)) {
                return getFunctionality().getSupport() != null ? getFunctionality().getSupport()
                        .getEmailAddress() : CoreConfiguration.getConfiguration().defaultSupportEmailAddress();
            }
            SupportConfiguration supportConfiguration = FenixFramework.getDomainObject(support);
            return supportConfiguration.getEmailAddress();
        }
    }

}
