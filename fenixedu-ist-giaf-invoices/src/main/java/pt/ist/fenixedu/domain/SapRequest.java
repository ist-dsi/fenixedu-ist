package pt.ist.fenixedu.domain;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Refund;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

import java.util.Comparator;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

public class SapRequest extends SapRequest_Base {

    public class ClientData {

        private final JsonObject clientData = getRequestAsJson().get("clientData").getAsJsonObject();

        public String getAccountId() {
            return clientData.get("accountId").getAsString();
        }
        public String getCompanyName() {
            return clientData.get("companyName").getAsString();
        }
        public String getClientId() {
            return clientData.get("clientId").getAsString();
        }
        public String getCountry() {
            return clientData.get("country").getAsString();
        }
        public String getStreet() {
            return clientData.get("street").getAsString();
        }
        public String getCity() {
            return clientData.get("city").getAsString();
        }
        public String getRegion() {
            return clientData.get("region").getAsString();
        }
        public String getPostalCode() {
            return clientData.get("postalCode").getAsString();
        }
        public String getVatNumber() {
            return clientData.get("vatNumber").getAsString();
        }
        public String getFiscalCountry() {
            return clientData.get("fiscalCountry").getAsString();
        }
        public String getNationality() {
            return clientData.get("nationality").getAsString();
        }
        public String getBillingIndicator() {
            return clientData.get("billingIndicator").getAsString();
        }
    }

    public class DocumentData {

        private final JsonObject json = getRequestAsJson();

        public String getCurrencyCode() {
            return json.get("currencyCode").getAsString();
        }
        public String getProductCode() {
            return json.get("productCode").getAsString();
        }
        public String getProductDescription() {
            return json.get("productDescription").getAsString();
        }
        public String getWorkingDocumentNumber() {
            final JsonElement result;
            final JsonElement workingDocument = json.get("workingDocument");
            if (workingDocument != null && !workingDocument.isJsonNull()) {
                result = workingDocument;
            } else {
                final JsonElement paymentDocument = json.get("paymentDocument");
                if (paymentDocument != null && !paymentDocument.isJsonNull()) {
                    result = paymentDocument;
                } else {
                    return null;
                }
            }
            return result.getAsJsonObject().get("workingDocumentNumber").getAsString();
        }
    }

    public static final Comparator<SapRequest> COMPARATOR_BY_DATE = new Comparator<SapRequest>() {
        @Override
        public int compare(SapRequest r1, SapRequest r2) {
            final int i = r1.getWhenCreated().compareTo(r2.getWhenCreated());
            return i == 0 ? r1.getExternalId().compareTo(r2.getExternalId()) : i;
        }
    };

    public static final Comparator<? super SapRequest> COMPARATOR_BY_EVENT_AND_ORDER = (r1, r2) -> {
        final int e = Event.COMPARATOR_BY_DATE.compare(r1.getEvent(), r2.getEvent());
        final int o = e == 0 ? r1.getOrder().compareTo(r2.getOrder()) : e;
        return o == 0 ? COMPARATOR_BY_DATE.compare(r1, r2) : o;
    };

    public static final Comparator<SapRequest> DOCUMENT_NUMBER_COMPARATOR = new Comparator<SapRequest>() {
        @Override
        public int compare(final SapRequest sr1, final SapRequest sr2) {
            final Integer i1 =
                    sr1.getDocumentNumber() != null ? Integer.valueOf(sr1.getDocumentNumber().substring(2)) : Integer.valueOf(-1);
            final Integer i2 = sr2.getDocumentNumber() != null ? Integer.valueOf(sr2.getDocumentNumber().substring(2)) : Integer
                    .valueOf(-1);
            return i1.compareTo(i2);
        }
    };

    public static final Comparator<SapRequest> COMPARATOR_BY_ORDER = new Comparator<SapRequest>() {
        @Override
        public int compare(SapRequest r1, SapRequest r2) {
            return r1.getOrder().compareTo(r2.getOrder());
        }
    };

    public SapRequest(Event event, String clientId, Money amount, String documentNumber, SapRequestType requestType,
            Money advancement, JsonObject request) {
        Optional<SapRequest> maxRequest = event.getSapRequestSet().stream().filter(sr -> sr != this).max(COMPARATOR_BY_ORDER);
        Integer order = maxRequest.isPresent() ? maxRequest.get().getOrder() : 0;
        setSapRoot(SapRoot.getInstance());
        setEvent(event);
        setClientId(clientId);
        setValue(amount);
        setDocumentNumber(documentNumber);
        setRequestType(requestType);
        setAdvancement(advancement);
        setRequest(request.toString());
        setSent(false);
        setWhenCreated(new DateTime());
        setOrder(order + 1);
        setIgnore(false);
    }

    public JsonObject getIntegrationMessageAsJson() {
        final String message = getIntegrationMessage();
        return message == null || message.isEmpty() ? new JsonObject() : new JsonParser().parse(message).getAsJsonObject();
    }

    public void addIntegrationMessage(final String key, final JsonElement message) {
        final JsonObject messages = getIntegrationMessageAsJson();
        messages.add(key, message);
        setIntegrationMessage(messages.toString());
    }

    public void removeIntegrationMessage(final String key) {
        final JsonObject messages = getIntegrationMessageAsJson();
        messages.remove(key);
        setIntegrationMessage(messages.toString());
    }
  
    public SortedSet<String> getErrorMessages() {
        final SortedSet<String> errors = new TreeSet<>();
        final JsonObject integrationMessage = getIntegrationMessageAsJson();
        final JsonElement client = integrationMessage.get("Cliente");
        if (client != null && !client.isJsonNull()) {
            errors.add(client.getAsJsonObject().get("Mensagem").getAsString());
        }
        final JsonElement document = integrationMessage.get("Documento");
        if (document != null && !document.isJsonNull()) {
            if (document.isJsonObject()) {
                errors.add(document.getAsJsonObject().get("Mensagem").getAsString());
            } else {
                for (JsonElement element : document.getAsJsonArray()) {
                    errors.add(element.getAsJsonObject().get("Mensagem").getAsString());
                }
            }
        }
        return errors;
    }

    @Atomic(mode = TxMode.WRITE)
    public void delete() {
        setAnulledRequest(null);
        setEvent(null);
        setOriginalRequest(null);
        setPayment(null);
        final Refund refund = getRefund();
        if (refund != null) {
            setRefund(null);
            refund.getSapRequestSet().stream().
                filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
                .forEach(sr -> sr.delete());
        }
        setAdvancementRequest(null);
        setReimbursementRequest(null);
        setSapRoot(null);
        deleteDomainObject();
    }

    public boolean isReferencedByOtherRequest() {
        return getEvent().getSapRequestSet().stream()
                .filter(r -> !r.getIgnore())
                .anyMatch(r -> r != this && r.refersToDocument(getDocumentNumber()));
    }

    public boolean refersToDocument(final String documentNumber) {
        if (getDocumentNumber().equals(documentNumber)) {
            return true;
        }
        final JsonObject o = new JsonParser().parse(getRequest()).getAsJsonObject();
        final JsonElement paymentDocument = o.get("paymentDocument");
        if (paymentDocument != null && !paymentDocument.isJsonNull()) {
            final JsonObject paymentJson = paymentDocument.getAsJsonObject();
            if (hasValue(paymentJson, "workingDocumentNumber", documentNumber)
                    || hasValue(paymentJson, "originatingOnDocumentNumber", documentNumber)
                    || hasValue(paymentJson, "paymentOriginDocNumber", documentNumber)) {
                return true;
            }
        }
        final JsonElement workingDocument = o.get("workingDocument");
        if (workingDocument != null && !workingDocument.isJsonNull()) {
            final JsonObject workingJson = workingDocument.getAsJsonObject();
            if (hasValue(workingJson, "workOriginDocNumber", documentNumber)
                    || hasValue(workingJson, "paymentDocumentNumber", documentNumber)) {
                return true;
            }
        }
        return false;
    }

    public String getDocumentNumberForType(String typeCode){
        final JsonObject json = new JsonParser().parse(getRequest()).getAsJsonObject();
        final JsonElement paymentDocument = json.get("paymentDocument");
        if (paymentDocument != null && !paymentDocument.isJsonNull()) {
            final JsonObject paymentJson = paymentDocument.getAsJsonObject();
            final String paymentDocumentNumber = getDocumentNumber(paymentJson, "paymentDocumentNumber", typeCode);
            if(paymentDocumentNumber != null) {
                return paymentDocumentNumber;
            }
            final String workingDocumentNumber = getDocumentNumber(paymentJson, "workingDocumentNumber", typeCode);
            if (workingDocumentNumber != null) {
                return workingDocumentNumber;
            }
            final String originatingOnDocumentNumber = getDocumentNumber(paymentJson, "originatingOnDocumentNumber", typeCode);
            if (originatingOnDocumentNumber != null) {
                return originatingOnDocumentNumber;
            }
            final String paymentOriginDocNumber = getDocumentNumber(paymentJson, "paymentOriginDocNumber", typeCode);
            if (paymentOriginDocNumber != null) {
                return paymentOriginDocNumber;
            }
        }
        final JsonElement workingDocument = json.get("workingDocument");
        if (workingDocument != null && !workingDocument.isJsonNull()) {
            final JsonObject workingJson = workingDocument.getAsJsonObject();
            final String workOriginDocNumber = getDocumentNumber(workingJson, "paymentOriginDocNumber", typeCode);
            if (workOriginDocNumber != null) {
                return workOriginDocNumber;
            }
            final String workingDocumentNumber = getDocumentNumber(workingJson, "workingDocumentNumber", typeCode);
            if (workingDocumentNumber != null) {
                return workingDocumentNumber;
            }
            final String paymentDocumentNumber = getDocumentNumber(workingJson, "paymentDocumentNumber", typeCode);
            if(paymentDocumentNumber != null) {
                return paymentDocumentNumber;
            }
        }
        return null;
    }

    private String getDocumentNumber(final JsonObject json, final String key, final String value){
        final JsonElement jsonElement = json.get(key);
        if(jsonElement != null && !jsonElement.isJsonNull() && jsonElement.getAsString().startsWith(value)){
            return jsonElement.getAsString();
        }
        return null;
    }

    private boolean hasValue(final JsonObject o, final String key, final String value) {
        final JsonElement e = o.get(key);
        return e != null && !e.isJsonNull() && e.getAsString().equals(value);
    }

    public JsonObject getClientJson() {
        final JsonObject o = getRequestAsJson();
        return o == null ? null : o.get("clientData").getAsJsonObject();
    }

    public String getUVat() {
        final JsonObject o = getRequestAsJson();
        final JsonObject clientData = o == null ? null : o.get("clientData").getAsJsonObject();
        return clientData == null || clientData.isJsonNull() ? null : clientData.get("vatNumber").getAsString();
    }

    public boolean getReferenced() {
        final String documentNumber = getDocumentNumber();
        return documentNumber.length() == 3 || getEvent().getSapRequestSet().stream()
            .anyMatch(r -> r != this && r.getRequest().indexOf(documentNumber) > 0);
    }

    public boolean isDebtDocument() {
        final SapRequestType requestType = getRequestType();
        return requestType == SapRequestType.DEBT || requestType == SapRequestType.DEBT_CREDIT;
    }

    public JsonObject getRequestAsJson() {
        final String request = getRequest();
        return request == null || request.isEmpty() ? null : new JsonParser().parse(getRequest()).getAsJsonObject();
    }

    public ClientData getClientData() {
        return new ClientData();
    }

    public DocumentData getDocumentData() {
        return new DocumentData();
    }

    public Money consumedAmount() {
        final SapRequest sapRequest = this;
        return getEvent().getSapRequestSet().stream()
            .filter(r -> r != sapRequest && !r.getIgnore() && r.refersToDocument(sapRequest.getDocumentNumber()) && r.isConsumer())
            .map(r -> r.getValue())
            .reduce(Money.ZERO, Money::add);
    }

    private boolean isConsumer() {
        final SapRequestType type = getRequestType();
        return type == SapRequestType.PAYMENT || type == SapRequestType.ADVANCEMENT || type == SapRequestType.CREDIT;
    }

    public Money getValueAvailableForTransfer() {
        return getValue().subtract(consumedAmount());
    }

    public boolean isAvailableForTransfer() {
        return getValueAvailableForTransfer().isPositive();
    }

    public boolean getIsAvailableForTransfer() {
        return isAvailableForTransfer();
    }

    public boolean getCanBeCanceled() {
        return getIntegrated() && !getIgnore() && !isInitialization() && getAnulledRequest() == null
                && getRequestType() != SapRequestType.DEBT && getRequestType() != SapRequestType.DEBT_CREDIT
                && getRequestType() != SapRequestType.CREDIT && !isReferencedByOtherRequest();
    }

    public boolean getCanBeClosed() {
        return getIntegrated() && !getIgnore() && !isInitialization() && getAnulledRequest() == null
                && (getRequestType() == SapRequestType.DEBT || getRequestType() == SapRequestType.INVOICE
                    || getRequestType() == SapRequestType.CREDIT) && !isReferencedByOtherRequest();
    }

    public boolean getCanBeRefunded() {
        return getIntegrated() && !getIgnore() && !isInitialization() && (getRequestType() == SapRequestType.PAYMENT || getRequestType() == SapRequestType.ADVANCEMENT);
    }

    public Money openInvoiceValue() {
        return isInitialization() ? Money.ZERO : getValue().subtract(consumedAmount());
    }

    @Atomic
    public void toggleIgnore() {
        setIgnore(!getIgnore());
    }

    public boolean isInitialization() {
        return getRequest().equals("{}");
    }

    @Atomic
    public void consolidate() {
        setSapRootFromConsolidated(SapRoot.getInstance());
    }

    @Atomic
    public void revertConsolidation() {
        setSapRootFromConsolidated(null);
    }

    public DateTime getDocumentDate() {
        final SapRequestType sapRequestType = getRequestType();
        final DateTime documentDate;
        if (sapRequestType == SapRequestType.DEBT || sapRequestType == SapRequestType.DEBT_CREDIT
                || sapRequestType == SapRequestType.INVOICE || sapRequestType == SapRequestType.INVOICE_INTEREST
                || sapRequestType == SapRequestType.CREDIT) {
            documentDate = documentDateFor("workingDocument", "documentDate");
        } else if (sapRequestType == SapRequestType.PAYMENT || sapRequestType == SapRequestType.PAYMENT_INTEREST
                || sapRequestType == SapRequestType.ADVANCEMENT || sapRequestType == SapRequestType.CLOSE_INVOICE
                || sapRequestType == SapRequestType.REIMBURSEMENT) {
            documentDate = documentDateFor("paymentDocument", "paymentDate");
        } else {
            throw new Error("unreachable code");
        }
        return documentDate;
    }

    private DateTime documentDateFor(final String document, final String dateField) {
        final String s = getRequestAsJson().get(document).getAsJsonObject().get(dateField).getAsString();
        return DateTime.parse(s, DateTimeFormat.forPattern(GiafInvoiceConfiguration.DT_FORMAT));
    }

    public void hackDocumentDate(final DateTime dateTime) {
        if (getIntegrated()) {
            throw new Error("Cannot change document date of integrated document.");
        }
        final JsonObject request = getRequestAsJson();
        if (request.get("workingDocument") != null && !request.get("workingDocument").isJsonNull()) {
            final JsonObject workingDocument = request.get("workingDocument").getAsJsonObject();
            workingDocument.addProperty("documentDate", dateTime.toString(GiafInvoiceConfiguration.DT_FORMAT));
        }
        if (request.get("paymentDocument") != null && !request.get("paymentDocument").isJsonNull()) {
            final JsonObject paymentDocument = request.get("paymentDocument").getAsJsonObject();
            paymentDocument.addProperty("paymentDate", dateTime.toString(GiafInvoiceConfiguration.DT_FORMAT));
        }
        setRequest(request.toString());
    }

    public boolean allowedToSend() {
        return SapRoot.getInstance().yearIsOpen(getDocumentDate().getYear());
    }

}
