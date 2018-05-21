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
package pt.ist.fenixedu.integration.task;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.google.common.base.MoreObjects;
import org.fenixedu.academic.FenixEduAcademicConfiguration;
import org.fenixedu.bennu.io.domain.FileStorage;
import org.fenixedu.bennu.io.domain.FileSupport;
import org.fenixedu.bennu.io.domain.LocalFileSystemStorage;
import org.fenixedu.bennu.portal.domain.PortalConfiguration;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;

import org.fenixedu.messaging.core.domain.MessagingSystem;
import pt.ist.fenixedu.integration.FenixEduIstIntegrationConfiguration;

import com.google.common.io.ByteStreams;

@Task(englishTitle = "Checks the AFS store's quota", readOnly = true)
public class CheckStoreQuota extends CronTask {

    @Override
    public void runTask() throws Exception {
        List<String> messages = new ArrayList<>();
        for (FileStorage store : FileSupport.getInstance().getFileStorageSet().stream()
                .filter(store -> store instanceof LocalFileSystemStorage).collect(Collectors.toList())) {
            LocalFileSystemStorage localStore = (LocalFileSystemStorage) store;
            String path = localStore.getAbsolutePath();
            taskLog("Path: %s\n", path);
            if (path.startsWith("/tmp/")) {
                continue;
            }
            Process process = Runtime.getRuntime().exec("fs listquota " + path);
            String[] lines = new String(ByteStreams.toByteArray(process.getInputStream())).split("\n");
            List<String> fragments =
                    Stream.of(lines[1].split(" ")).filter(str -> !str.trim().isEmpty()).collect(Collectors.toList());
            long total = Integer.parseInt(fragments.get(1));
            long current = Integer.parseInt(fragments.get(2));
            BigDecimal occupation =
                    BigDecimal.valueOf(current).divide(BigDecimal.valueOf(total)).multiply(BigDecimal.valueOf(100));
            taskLog("\tTotal: %s Current: %s, Occupied: %s\n", total, current, occupation);
            if (occupation.longValue() > 90 && hasConfigurations(store)) {
                messages.add("- " + store.getName() + ". Total: " + total + ", Current: " + current + ". Occupation: "
                        + occupation);
            }
        }
        if (!messages.isEmpty()) {
            String subject =
                    "[" + PortalConfiguration.getInstance().getApplicationTitle().getContent() + "] File storage space alert";
            String body = "File Storage Space alert!\n\n" + messages.stream().collect(Collectors.joining("\n"));
            sendEmail(subject, body, FenixEduIstIntegrationConfiguration.getConfiguration().getStoreQuotaWarningEmail());
            taskLog("Notification sent!");
        } else {
            taskLog("Everything is ok!");
        }
    }

    private void sendEmail(String subject, String body, String to) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.host",
                MoreObjects.firstNonNull(FenixEduAcademicConfiguration.getConfiguration().getMailSmtpHost(), "localhost"));
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(MessagingSystem.systemSender().getAddress()));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(body);

        Multipart multipart = new MimeMultipart();
        {
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);
            multipart.addBodyPart(messageBodyPart);
        }

        message.setContent(multipart);

        Transport.send(message);
    }

    private boolean hasConfigurations(FileStorage store) {
        return FileSupport.getInstance().getConfigurationSet().stream().anyMatch(config -> config.getStorage().equals(store));
    }

}
