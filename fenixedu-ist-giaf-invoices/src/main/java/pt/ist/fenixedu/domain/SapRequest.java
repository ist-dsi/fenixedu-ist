package pt.ist.fenixedu.domain;

import java.util.Comparator;
import java.util.Optional;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

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
                    .valueOf(-1);;
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
            Money advancement,
            JsonObject request) {
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

    public void addIntegrationMessage(final String key, final JsonObject message) {
        final JsonObject messages = getIntegrationMessageAsJson();
        messages.add(key, message);
        setIntegrationMessage(messages.toString());
    }

    public void removeIntegrationMessage(final String key) {
        final JsonObject messages = getIntegrationMessageAsJson();
        messages.remove(key);
        setIntegrationMessage(messages.toString());
    }
  
    @Atomic(mode = TxMode.WRITE)
    public void delete() {
        setAnulledRequest(null);
        setEvent(null);
        setOriginalRequest(null);
        setPayment(null);
        final SapDocumentFile documentFile = getSapDocumentFile();
        if (documentFile != null) {
            documentFile.delete();
        }
        setSapRoot(null);
        deleteDomainObject();
    }

    public boolean refersToDocument(final String documentNumber) {
        if (getDocumentNumber().equals(documentNumber)) {
            return true;
        }
        final JsonObject o = new JsonParser().parse(getRequest()).getAsJsonObject();
        final JsonElement paymentDocument = o.get("paymentDocument");
        if (paymentDocument != null && !paymentDocument.isJsonNull()) {
            final JsonObject paymentDocumentO = paymentDocument.getAsJsonObject();
            if (hasValue(paymentDocumentO, "workingDocumentNumber", documentNumber)
                    || hasValue(paymentDocumentO, "originatingOnDocumentNumber", documentNumber)
                    || hasValue(paymentDocumentO, "paymentOriginDocNumber", documentNumber)) {
                return true;
            }
        }
        final JsonElement workingDocument = o.get("workingDocument");
        if (workingDocument != null && !workingDocument.isJsonNull()) {
            final JsonObject workingDocumentO = workingDocument.getAsJsonObject();
            if (hasValue(workingDocumentO, "workOriginDocNumber", documentNumber)
                    || hasValue(workingDocumentO, "paymentDocumentNumber", documentNumber)) {
                return true;
            }
        }
        return false;
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

    private JsonObject getRequestAsJson() {
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
            .filter(r -> r != sapRequest && r.refersToDocument(sapRequest.getDocumentNumber()))
            .map(r -> r.getValue())
            .reduce(Money.ZERO, Money::add);
    }

    public Money getValueAvailableForTransfer() {
        return getValue().subtract(consumedAmount());
    }

    public boolean isAvailableForTransfer() {
        return getValueAvailableForTransfer().isPositive();
    }

}
