/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Invoices.
 *
 * FenixEdu IST GIAF Invoices is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Invoices is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Invoices.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.giaf.invoices.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventState;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.domain.SapDocumentFile;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.giaf.invoices.GiafEvent;

@SpringFunctionality(app = InvoiceController.class, title = "title.giaf.invoice.viewer")
@RequestMapping("/invoice-downloader")
public class InvoiceDownlaodController {

    @RequestMapping(method = RequestMethod.GET)
    public String list(@RequestParam(required = false) String username, final Model model) {
        final User user = InvoiceController.getUser(username);
        if (InvoiceController.isAllowedToAccess(user)) {
            final Person person = user.getPerson();
            final JsonArray sapRequests = person.getEventsSet().stream()
                    .flatMap(e -> e.getSapRequestSet().stream())
                    .filter(r -> r.getRequest().length() > 2)
                    .sorted(SapRequest.COMPARATOR_BY_EVENT_AND_ORDER)
                    .map(r -> toJsonObject(r))
                    .collect(SapInvoiceController.toJsonArray());
            model.addAttribute("sapRequests", sapRequests);

            final JsonArray giafDocuments = new JsonArray();
            for (final Event event : person.getEventsSet()) {
                final JsonArray jsonEvent = GiafEvent.readEventFile(event);
                for (final JsonElement je : jsonEvent) {
                    final JsonObject jo = je.getAsJsonObject();
                    final JsonElement receiptId = jo.get("receiptId");
                    if (receiptId != null && !receiptId.isJsonNull()) {
                        giafDocuments.add(jo);
                        jo.addProperty("description", event.getDescription().toString());
                    }
                }
            }
            model.addAttribute("giafDocuments", giafDocuments);
        }
        return "invoice-viewer/home";
    }

    private JsonObject toJsonObject(final SapRequest sapRequest) {
        final JsonObject result = new JsonObject();
        result.addProperty("id", sapRequest.getExternalId());
        result.addProperty("advancement", sapRequest.getAdvancement() == null ? null : sapRequest.getAdvancement().toPlainString());
        result.addProperty("integrationMessage", sapRequest.getIntegrationMessage());
        result.addProperty("clientId", sapRequest.getClientId());
        result.addProperty("documentNumber", sapRequest.getDocumentNumber());
        result.addProperty("integrated", sapRequest.getIntegrated());
        result.addProperty("integrationMessage", sapRequest.getIntegrationMessage());
        result.addProperty("request", sapRequest.getRequest());
        result.addProperty("requestType", sapRequest.getRequestType() == null ? null : sapRequest.getRequestType().name());
        result.addProperty("sapDocumentNumber", sapRequest.getSapDocumentNumber());
        result.addProperty("sent", sapRequest.getSent());
        result.addProperty("value", sapRequest.getValue() == null ? null : sapRequest.getValue().toPlainString());
        result.addProperty("whenCreated", sapRequest.getWhenCreated() == null ? null : sapRequest.getWhenCreated().toString(SapInvoiceController.DATE_TIME_FORMAT));
        result.addProperty("whenSent", sapRequest.getWhenSent() == null ? null : sapRequest.getWhenSent().toString(SapInvoiceController.DATE_TIME_FORMAT));
        result.addProperty("ignore", sapRequest.getIgnore());
        result.addProperty("referenced", sapRequest.getReferenced());
        result.addProperty("description", sapRequest.getEvent().getDescription().toString());
        result.addProperty("isCanceled", sapRequest.getEvent().isInState(EventState.CANCELLED));
        return result;
    }

    @RequestMapping(value = "/giaf/{event}/{filename}", method = RequestMethod.GET, produces = "application/pdf")
    public void giafInvoice(@PathVariable Event event, @PathVariable String filename, final HttpServletResponse response) {
        if (sAllowedToAccess(event)) {
            final File file =  GiafEvent.receiptFile(event, filename);
            if (file != null && file.exists()) {
                try (final FileInputStream inputStream = new FileInputStream(file)) {
                    response.setHeader("Content-Disposition", "attachment; filename=" + fullFilename(filename));
                    ByteStreams.copy(inputStream, response.getOutputStream());
                    response.flushBuffer();
                } catch (final IOException e) {
                    throw new Error(e);
                }
            }
        } else {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getClass();
        }
    }

    @RequestMapping(value = "/sap/{sapRequest}/{filename}", method = RequestMethod.GET, produces = "application/pdf")
    public void sapInvoice(@PathVariable SapRequest sapRequest, @PathVariable String filename, final HttpServletResponse response) {
        if (sAllowedToAccess(sapRequest.getEvent())) {
            final SapDocumentFile sapDocumentFile = sapRequest.getSapDocumentFile();
            if (sapDocumentFile == null) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            } else {
                try (final InputStream inputStream = sapDocumentFile.getStream()) {
                    response.setHeader("Content-Disposition", "attachment; filename=" + fullFilename(filename));
                    ByteStreams.copy(inputStream, response.getOutputStream());
                } catch (final IOException e) {
                    throw new Error(e);
                }
            }
        } else {
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }
        try {
            response.flushBuffer();
        } catch (final IOException e) {
            throw new Error(e);
        };
    }

    private String fullFilename(final String filename) {
        return filename.endsWith(".pdf") ? filename : filename + ".pdf";
    }

    private boolean sAllowedToAccess(final Event event) {
        final User user = Authenticate.getUser();
        return isOwner(event, user) || isAcademicServiceStaff(user);
    }

    private boolean isOwner(final Event event, final User user) {
        final Person person = event == null ? null : event.getPerson();
        return user != null && user == person.getUser();
    }

    private boolean isAcademicServiceStaff(final User user) {
        return AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_STUDENT_PAYMENTS).isMember(user)
                || AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_STUDENT_PAYMENTS_ADV).isMember(user);
    }

}
