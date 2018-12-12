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
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventState;
import org.fenixedu.academic.domain.accounting.Refund;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.dto.accounting.DepositAmountBean;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.ui.spring.controller.AccountingEventsPaymentManagerController;
import org.fenixedu.academic.ui.spring.service.AccountingManagementAccessControlService;
import org.fenixedu.academic.ui.spring.service.AccountingManagementService;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
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
import pt.ist.fenixframework.Atomic;

@SpringFunctionality(app = InvoiceDownloadController.class, title = "title.sap.invoice.viewer")
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

    public static boolean isSapIntegrationManager() {
        return Group.dynamic("sapIntegrationManager").isMember(Authenticate.getUser());
    }

    private String homeRedirect(final String username) {
        return "redirect:/sap-invoice-viewer?username=" + username;
    }

    private String eventRedirect(final Event event) {
        return "redirect:" + eventDetails(event);
    }

    private String eventDetails(final Event event) {
        return AccountingEventsPaymentManagerController.REQUEST_MAPPING + "/" + event.getExternalId() + "/details";
    }
    
    private String sapDocumentsRedirect(final Event event) {
        return "redirect:/sap-invoice-viewer/" + event.getExternalId();
    }

    @RequestMapping(method = RequestMethod.GET)
    public String home(@RequestParam(required = false) String username, final Model model,
            @RequestParam(required = false) String exception, @RequestParam(required = false) String errors) {
        final User user = InvoiceDownloadController.getUser(username);
        if (InvoiceDownloadController.isAllowedToAccess(user)) {
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

    @RequestMapping(value = "/{event}", method = RequestMethod.GET)
    public String eventDetails(final @PathVariable Event event, final Model model,
            @RequestParam(required = false) String exception, @RequestParam(required = false) String errors) {
        model.addAttribute("eventDetailsUrl", eventDetails(event));
        model.addAttribute("event", event);
        final Set<SapRequest> sapRequests = new TreeSet<>(SapRequest.COMPARATOR_BY_EVENT_AND_ORDER);
        sapRequests.addAll(event.getSapRequestSet());
        model.addAttribute("sapRequests", sapRequests);
        if (!Strings.isNullOrEmpty(exception)) {
            model.addAttribute("exception", exception);
        }
        if (!Strings.isNullOrEmpty(errors)) {
            model.addAttribute("errors", errors);
        }
        model.addAttribute("isSapIntegrator", Group.dynamic("sapIntegrationManager").isMember(Authenticate.getUser()));
        model.addAttribute("isPaymentManager", SapInvoiceController.isAdvancedPaymentManager());
        return "sap-invoice-viewer/eventDetails";
    }

    @RequestMapping(value = "/{event}/calculateRequests", method = RequestMethod.POST)
    public String calculateRequests(final @PathVariable Event event, final Model model) {
        return processEvent(model, event, (c, l, e) -> EventProcessor.registerEventSapRequests(c, l, e));
    }

    @RequestMapping(value = "/{event}/sync", method = RequestMethod.POST)
    public String syncEvent(final @PathVariable Event event, final Model model) {
        return processEvent(model, event, (c, l, e) -> EventProcessor.syncEventWithSap(c, l, e));
    }

    @RequestMapping(value = "{event}/refundEvent", method = RequestMethod.POST)
    public String refundEvent(final @PathVariable Event event, final User user, final Model model, @RequestParam(required = false) final ExternalClient client,
            final @RequestParam EventExemptionJustificationType justificationType, final @RequestParam String reason) {
        return doRefund(event, user, model, () -> doRefundToExternalClient(event, user, client, justificationType, reason));
    }

    @Atomic
    private Refund doRefundToExternalClient(final Event event, final User user, final ExternalClient client,
            final EventExemptionJustificationType justificationType, final String reason) {
        final Refund refund = new AccountingManagementService().refundEvent(event, user, justificationType, reason);
        refund.setExternalClient(client);
        return refund;
    }

    @RequestMapping(value = "{event}/refundExcessPayment", method = RequestMethod.POST)
    public String refundExcessPayment(final @PathVariable Event event, final User user, final Model model, @RequestParam(required = false) final ExternalClient client){
        return doRefund(event, user, model, () -> doRefundExcessToExternalClient(event, user, client));
    }

    @Atomic
    private Refund doRefundExcessToExternalClient(final Event event, final User user, final ExternalClient client) {
        final Refund refund = new AccountingManagementService().refundExcessPayment(event, user, null);
        refund.setExternalClient(client);
        return refund;
    }

    private String doRefund(final @PathVariable Event event, final User user, final Model model, Supplier<?> supplier) {
        new AccountingManagementAccessControlService().checkAdvancedPaymentManager(event, user);
        try {
            supplier.get();
        } catch (final DomainException e) {
            model.addAttribute("error", e.getLocalizedMessage());
            model.addAttribute("eventDetailsUrl", eventDetails(event));
            return "redirect:/accounting-management/" + event.getExternalId() + "/refund";
        }
        return eventRedirect(event);        
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
        if (Group.dynamic("managers").isMember(Authenticate.getUser())
                || Group.dynamic("sapIntegrationManager").isMember(Authenticate.getUser())) {
            processor.process(errorLogConsumer, elogger, event);
        }

        model.addAttribute("errors", errors.toString());

        return sapDocumentsRedirect(event);
    }

    @RequestMapping(value = "/{sapRequest}/transfer", method = RequestMethod.GET)
    public String prepareTransfer(final @PathVariable SapRequest sapRequest, final Model model) {
        model.addAttribute("sapRequest", sapRequest);
        model.addAttribute("clientData", sapRequest.getClientData());
        model.addAttribute("documentData", sapRequest.getDocumentData());
        model.addAttribute("isPaymentManager", SapInvoiceController.isAdvancedPaymentManager());
        return "sap-invoice-viewer/invoiceDetails";
    }

    @RequestMapping(value = "/{sapRequest}/transfer", method = RequestMethod.POST)
    public String transfer(final @PathVariable SapRequest sapRequest, final Model model,
            @RequestParam final ExternalClient client, @RequestParam final String valueToTransfer,
            @RequestParam final String pledgeNumber) {
        if (Group.dynamic("managers").isMember(Authenticate.getUser()) || isAdvancedPaymentManager() || Group.dynamic("sapIntegrationManager").isMember(Authenticate.getUser())) {
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
        return eventRedirect(sapRequest.getEvent());
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
        if (Group.dynamic("managers").isMember(Authenticate.getUser()) || Group.dynamic("sapIntegrationManager").isMember(Authenticate.getUser())) {
            sapRequest.delete();
        }
        return sapDocumentsRedirect(event);
    }

    @RequestMapping(value = "/{sapRequest}/close", method = RequestMethod.POST)
    public String close(final @PathVariable SapRequest sapRequest, final Model model) {
        final Event event = sapRequest.getEvent();
        if (Group.dynamic("managers").isMember(Authenticate.getUser()) || Group.dynamic("sapIntegrationManager").isMember(Authenticate.getUser())) {
            final SapEvent sapEvent = new SapEvent(event);
            try {
                sapEvent.closeDocument(sapRequest);
            } catch (final Exception | Error e) {
                model.addAttribute("exception", e.getMessage());
            }
        }
        return sapDocumentsRedirect(event);
    }

    @RequestMapping(value = "/{sapRequest}/cancel", method = RequestMethod.POST)
    public String cancel(final @PathVariable SapRequest sapRequest, final Model model) {
        final Event event = sapRequest.getEvent();
        if (Group.dynamic("managers").isMember(Authenticate.getUser()) || Group.dynamic("sapIntegrationManager").isMember(Authenticate.getUser())) {
            final SapEvent sapEvent = new SapEvent(event);
            try {
                sapEvent.cancelDocument(sapRequest);
            } catch (final Exception | Error e) {
                model.addAttribute("exception", e.getMessage());
            }
        }
        return sapDocumentsRedirect(event);
    }

    @RequestMapping(value = "/{event}/cancelDebt", method = RequestMethod.POST)
    public String cancelDebt(final @PathVariable Event event, final Model model) {
        if (Group.dynamic("managers").isMember(Authenticate.getUser()) || Group.dynamic("sapIntegrationManager").isMember(Authenticate.getUser())) {
            final SapEvent sapEvent = new SapEvent(event);
            try {
                sapEvent.cancelDebt();
            } catch (final Exception | Error e) {
                model.addAttribute("exception", e.getMessage());
            }
        }
        return sapDocumentsRedirect(event);
    }

    @RequestMapping(value = "/{event}/createNewInvoice", method = RequestMethod.GET)
    public String prepareCreateNewInvoice(final @PathVariable Event event, final Model model) {
        model.addAttribute("event", event);
        model.addAttribute("eventDetailsUrl", eventDetails(event));
        return "sap-invoice-viewer/createNewInvoice";
    }

    @RequestMapping(value = "/{event}/createNewInvoice", method = RequestMethod.POST)
    public String createNewInvoice(final @PathVariable Event event, final Model model,
            @RequestParam final ExternalClient client, @RequestParam final String valueToTransfer,
            @RequestParam final String pledgeNumber) {
        if (Group.dynamic("managers").isMember(Authenticate.getUser()) || Group.dynamic("sapIntegrationManager").isMember(Authenticate.getUser())) {
            try {
                final Money value = toMoney(valueToTransfer);
                if (value.isZero() || value.isNegative()) {
                    model.addAttribute("error", "error.value.to.transfer.must.be.positive");
                    return prepareCreateNewInvoice(event, model);
                }
                final SapEvent sapEvent = new SapEvent(event);
                try {
                    sapEvent.registerInvoice(value, event.isGratuity(), true, client, pledgeNumber);
                } catch (final Exception | Error e) {
                    model.addAttribute("exception", e.getMessage());
                    return prepareCreateNewInvoice(event, model);
                }
            } catch (final NumberFormatException ex) {
                model.addAttribute("error", "error.value.to.transfer.must.be.positive");
                return prepareCreateNewInvoice(event, model);
            }
        }
        return sapDocumentsRedirect(event);
    }

    @RequestMapping(value = "/{event}/registerInternalPayment", method = RequestMethod.GET)
    public String prepareRegisterInternalPayment(final @PathVariable Event event, final Model model) {
        model.addAttribute("event", event);
        model.addAttribute("eventDetailsUrl", eventDetails(event));
        return "sap-invoice-viewer/registerInternalPayment";
    }

    @RequestMapping(value = "/{event}/registerInternalPayment", method = RequestMethod.POST)
    public String registerInternalPayment(final @PathVariable Event event, final Model model,
            @RequestParam final String unit, @RequestParam final String valueToTransfer,
            @RequestParam final DateTime whenRegistered, @RequestParam final String reason) {
        final User user = Authenticate.getUser();
        if (Group.dynamic("managers").isMember(user) || Group.dynamic("sapIntegrationManager").isMember(user)) {
            try {
                final Money value = toMoney(valueToTransfer);
                if (value.isZero() || value.isNegative()) {
                    model.addAttribute("error", "error.value.to.transfer.must.be.positive");
                    return prepareRegisterInternalPayment(event, model);
                }
                final DebtInterestCalculator calculatorNow = event.getDebtInterestCalculator(new DateTime());
                final Money dueAmount = new Money(calculatorNow.getDueAmount());
                if (value.greaterThan(dueAmount)) {
                    model.addAttribute("error", "error.value.exeeds.due.amount");
                    return prepareRegisterInternalPayment(event, model);                    
                }
                final DebtInterestCalculator calculatorWhen = event.getDebtInterestCalculator(whenRegistered);
                final Money dueInterestOrFine = new Money(calculatorWhen.getDueFineAmount().add(calculatorWhen.getDueInterestAmount()));
                if (dueInterestOrFine.isPositive()) {
                    model.addAttribute("error", "error.cannot.register.internal.payment.with.pending.interes.or.fines");
                    return prepareRegisterInternalPayment(event, model);                    
                }

                final DepositAmountBean bean = new DepositAmountBean();
                bean.setAmount(value);
                bean.setEntryType(event.getEntryType());
                bean.setPaymentMethod(Bennu.getInstance().getInternalPaymentMethod());
                bean.setPaymentReference(unit);
                bean.setReason(reason);
                bean.setWhenRegistered(whenRegistered);
                new AccountingManagementService().depositAmount(event, user, bean);
            } catch (final NumberFormatException ex) {
                model.addAttribute("error", "error.value.to.transfer.must.be.positive");
                return prepareRegisterInternalPayment(event, model);
            }
        }
        return eventRedirect(event);
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
        result.addProperty("isAvailableForTransfer", sapRequest.isAvailableForTransfer());
        result.addProperty("valuevalueAvailableForTransfer", sapRequest.getValue() == null ? null : sapRequest.getValue().subtract(sapRequest.consumedAmount()).toPlainString());
        return result;
    }

    public static <T extends JsonElement> Collector<T, JsonArray, JsonArray> toJsonArray() {
        return Collector.of(JsonArray::new, (array, element) -> array.add(element), (one, other) -> {
            one.addAll(other);
            return one;
        } , Characteristics.IDENTITY_FINISH);
    }

}
