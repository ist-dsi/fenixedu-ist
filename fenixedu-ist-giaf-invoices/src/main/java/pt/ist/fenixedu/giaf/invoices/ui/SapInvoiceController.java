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

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventState;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.joda.time.DateTime;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.domain.ExternalClient;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.SapEvent;

@SpringFunctionality(app = InvoiceDownlaodController.class, title = "title.sap.invoice.viewer")
@RequestMapping("/sap-invoice-viewer")
public class SapInvoiceController {

    static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static interface EventProcessorInterface {
        public void process(final ErrorLogConsumer consumer, final EventLogger logger, final Event event);
    }

    public static boolean isAdvancedPaymentManager() {
        return Optional.ofNullable(Authenticate.getUser())
                .map(AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_STUDENT_PAYMENTS_ADV, Collections.emptySet(), Collections.emptySet(), null)::isMember)
                .orElse(false);
    }

    private String homeRedirect(final String username) {
        return "redirect:/sap-invoice-viewer?username=" + username;
    }

    @RequestMapping(method = RequestMethod.GET)
    public String home(@RequestParam(required = false) String username, final Model model,
            @RequestParam(required = false) String exception, @RequestParam(required = false) String errors) {
        final User user = InvoiceDownlaodController.getUser(username);
        if (InvoiceDownlaodController.isAllowedToAccess(user)) {
            final Person person = user.getPerson();
            final DateTime now = new DateTime();
            final JsonArray events = person.getEventsSet().stream()
                    .sorted(Event.COMPARATOR_BY_DATE)
                    .map(e -> toJsonObject(e, now))
                    .collect(toJsonArray());
            model.addAttribute("events", events);
        }
        if (!Strings.isNullOrEmpty(exception)) {
            model.addAttribute("exception", exception);
        }
        if (!Strings.isNullOrEmpty(errors)) {
            model.addAttribute("errors", errors);
        }
        return "sap-invoice-viewer/home";
    }

    @RequestMapping(value = "/{event}/calculateRequests", method = RequestMethod.POST)
    public String calculateRequests(final @PathVariable Event event, final Model model) {
        return processEvent(model, event, (c, l, e) -> EventProcessor.registerEventSapRequests(c, l, e));
    }

    @RequestMapping(value = "/{event}/sync", method = RequestMethod.POST)
    public String syncEvent(final @PathVariable Event event, final Model model) {
        return processEvent(model, event, (c, l, e) -> EventProcessor.syncEventWithSap(c, l, e));
    }

    private String processEvent(final Model model, final Event event, final EventProcessorInterface processor) {
        final StringBuilder errors = new StringBuilder();
        final ErrorLogConsumer errorLogConsumer = new ErrorLogConsumer() {

            @Override
            public void accept(final String oid, final String user, final String name, final String amount,
                    final String cycleType, final String error, final String args, final String type,
                    final String countryOfVatNumber, final String vatNumber, final String address, final String locality,
                    final String postCode, final String countryOfAddress, final String paymentMethod, final String documentNumber,
                    final String action) {

                if (errors.length() > 0) {
                    errors.append("<br/>");
                }
                errors.append(error);
            }
        };
        final EventLogger elogger = (msg, args) -> {
            if (errors.length() > 0) {
                errors.append("<br/>");
            }
            errors.append(String.format(msg.replace("%n", ""), args));
        };
        if (Group.dynamic("managers").isMember(Authenticate.getUser())) {
            processor.process(errorLogConsumer, elogger, event);
        }

        model.addAttribute("errors", errors.toString());

        return homeRedirect(event.getPerson().getUsername());
    }

    @RequestMapping(value = "/{sapRequest}/transfer", method = RequestMethod.GET)
    public String prepareTransfer(final @PathVariable SapRequest sapRequest, final Model model) {
        model.addAttribute("sapRequest", toJsonObject(sapRequest));
        final DateTime now = new DateTime();
        model.addAttribute("event", toJsonObject(sapRequest.getEvent(), now));
        return "sap-invoice-viewer/transferInvoice";
    }

    @RequestMapping(value = "/{sapRequest}/transfer", method = RequestMethod.POST)
    public String transfer(final @PathVariable SapRequest sapRequest, final Model model,
            @RequestParam final ExternalClient client, @RequestParam final String valueToTransfer,
            @RequestParam final String pledgeNumber) {
        if (Group.dynamic("managers").isMember(Authenticate.getUser()) || isAdvancedPaymentManager()) {
            try {
                final Money value = toMoney(valueToTransfer);
                if (value.isZero() || value.isNegative()) {
                    model.addAttribute("error", "error.value.to.transfer.must.be.positive");
                    return prepareTransfer(sapRequest, model);
                }
                if (value.greaterThan(sapRequest.getValue())) {
                    model.addAttribute("error", "error.value.to.transfer.cannot.exceed.invouce.value");
                    return prepareTransfer(sapRequest, model);                
                }
                if (client == null) {
                    model.addAttribute("error", "error.destination.client.not.found");
                    return prepareTransfer(sapRequest, model);                
                }
                final SapEvent sapEvent = new SapEvent(sapRequest.getEvent());
                try {
                    sapEvent.transferInvoice(sapRequest, client, value, pledgeNumber);
                } catch (final Exception | Error e) {
                    model.addAttribute("exception", e.getMessage());
                    return prepareTransfer(sapRequest, model);
                }
            } catch (final NumberFormatException ex) {
                model.addAttribute("error", "error.value.to.transfer.must.be.positive");
                return prepareTransfer(sapRequest, model);                
            }
        }
        return homeRedirect(sapRequest.getEvent().getPerson().getUsername());
    }

    static Money toMoney(final String s) {
        if (s == null || s.isEmpty()) {
            return Money.ZERO;
        }
        final String v = s.matches("-?\\d+(\\,\\d+)?") ? s.replace(',', '.') : s;
        return new Money(v);
    }

    @RequestMapping(value = "/{sapRequest}/delete", method = RequestMethod.POST)
    public String delete(final @PathVariable SapRequest sapRequest, final Model model) {
        final Event event = sapRequest.getEvent();
        if (Group.dynamic("managers").isMember(Authenticate.getUser())) {
            sapRequest.delete();
        }
        return homeRedirect(event.getPerson().getUsername());
    }

    @RequestMapping(value = "/{sapRequest}/cancel", method = RequestMethod.POST)
    public String cancel(final @PathVariable SapRequest sapRequest, final Model model) {
        final Event event = sapRequest.getEvent();
        if (Group.dynamic("managers").isMember(Authenticate.getUser())) {
            final SapEvent sapEvent = new SapEvent(event);
            try {
                sapEvent.cancelDocument(sapRequest);
            } catch (final Exception | Error e) {
                model.addAttribute("exception", e.getMessage());
            }
        }
        return homeRedirect(event.getPerson().getUsername());
    }

    @RequestMapping(value = "/{event}/cancelDebt", method = RequestMethod.POST)
    public String cancelDebt(final @PathVariable Event event, final Model model) {
        if (Group.dynamic("managers").isMember(Authenticate.getUser())) {
            final SapEvent sapEvent = new SapEvent(event);
            try {
                sapEvent.cancelDebt();
            } catch (final Exception | Error e) {
                model.addAttribute("exception", e.getMessage());
            }
        }
        return homeRedirect(event.getPerson().getUsername());
    }

    private JsonObject toJsonObject(final Event event, final DateTime when) {
        final JsonObject result = new JsonObject();

        final SapEvent sapEvent = new SapEvent(event);

        result.addProperty("eventId", event.getExternalId());
        result.addProperty("eventDescription", event.getDescription().toString());
        result.addProperty("isCanceled", event.isInState(EventState.CANCELLED));
        result.addProperty("canCancelDebt", !sapEvent.getFilteredSapRequestStream().anyMatch(r -> !r.isDebtDocument()) && sapEvent.calculateDebtValue().isPositive());
        result.addProperty("hasAnyPendingSapRequests", event.getSapRequestSet().stream().anyMatch(r -> !r.getIntegrated()));

        final DebtInterestCalculator calculator = event.getDebtInterestCalculator(when);

        result.addProperty("debtAmount", calculator.getDebtAmount());
        result.addProperty("debtExemptionAmount", calculator.getDebtExemptionAmount());

        result.addProperty("dueAmount", calculator.getDueAmount());
        result.addProperty("dueFineAmount", calculator.getDueFineAmount());
        result.addProperty("dueInterestAmount", calculator.getDueInterestAmount());

        result.addProperty("fineAmount", calculator.getFineAmount());
        result.addProperty("fineExemptionAmount", calculator.getFineExemptionAmount());

        result.addProperty("interestAmount", calculator.getInterestAmount());
        result.addProperty("interestExemptionAmount", calculator.getInterestExemptionAmount());

        result.addProperty("paidDebtAmount", calculator.getPaidDebtAmount());
        result.addProperty("paidFineAmount", calculator.getPaidFineAmount());
        result.addProperty("paidInterestAmount", calculator.getPaidInterestAmount());

        result.addProperty("totalAmount", calculator.getTotalAmount());
        result.addProperty("totalDueAmount", calculator.getTotalDueAmount());
        result.addProperty("totalPaidAmount", calculator.getTotalPaidAmount());

        final JsonArray sapRequests = event.getSapRequestSet().stream()
            .sorted(SapRequest.COMPARATOR_BY_EVENT_AND_ORDER)
            .map(sr -> toJsonObject(sr))
            .collect(toJsonArray());
        result.add("sapRequests", sapRequests);

        return result;
    }

    private JsonObject toJsonObject(final SapRequest sapRequest) {
        final JsonObject result = new JsonObject();
        result.addProperty("id", sapRequest.getExternalId());
        result.addProperty("advancement", sapRequest.getAdvancement() == null ? null : sapRequest.getAdvancement().toPlainString());
        result.addProperty("integrationMessage", sapRequest.getIntegrationMessage());
        //result.addProperty("anulledRequest", sapRequest.getAnulledRequest());
        result.addProperty("clientId", sapRequest.getClientId());
        result.addProperty("documentNumber", sapRequest.getDocumentNumber());
        result.addProperty("integrated", sapRequest.getIntegrated());
        result.addProperty("integrationMessage", sapRequest.getIntegrationMessage());
        final SapRequest originalRequest = sapRequest.getOriginalRequest();
        if (originalRequest != null) {
            result.addProperty("originalRequest", originalRequest.getExternalId());
        }
        final SapRequest anulledRequest = sapRequest.getAnulledRequest();
        if (anulledRequest != null) {
            result.addProperty("anulledRequest", anulledRequest.getExternalId());
        }
        //result.addProperty("payment", sapRequest.getPayment());
        result.addProperty("request", sapRequest.getRequest());
        result.addProperty("requestType", sapRequest.getRequestType() == null ? null : sapRequest.getRequestType().name());
        result.addProperty("sapDocumentNumber", sapRequest.getSapDocumentNumber());
        result.addProperty("hasDocument", sapRequest.getSapDocumentFile() != null);
        result.addProperty("sent", sapRequest.getSent());
        result.addProperty("value", sapRequest.getValue() == null ? null : sapRequest.getValue().toPlainString());
        result.addProperty("whenCreated", sapRequest.getWhenCreated() == null ? null : sapRequest.getWhenCreated().toString(DATE_TIME_FORMAT));
        result.addProperty("whenSent", sapRequest.getWhenSent() == null ? null : sapRequest.getWhenSent().toString(DATE_TIME_FORMAT));
        result.addProperty("ignore", sapRequest.getIgnore());
        result.addProperty("referenced", sapRequest.getReferenced());
        return result;
    }

    public static <T extends JsonElement> Collector<T, JsonArray, JsonArray> toJsonArray() {
        return Collector.of(JsonArray::new, (array, element) -> array.add(element), (one, other) -> {
            one.addAll(other);
            return one;
        } , Characteristics.IDENTITY_FINISH);
    }

}
