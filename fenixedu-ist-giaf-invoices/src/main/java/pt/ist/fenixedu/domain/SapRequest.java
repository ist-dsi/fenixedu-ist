package pt.ist.fenixedu.domain;

import java.util.Comparator;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class SapRequest extends SapRequest_Base {
    
    public static final Comparator<SapRequest> COMPARATOR_BY_DATE = new Comparator<SapRequest>() {
        @Override
        public int compare(SapRequest r1, SapRequest r2) {
            final int i = r1.getWhenCreated().compareTo(r2.getWhenCreated());
            return i == 0 ? r1.getExternalId().compareTo(r2.getExternalId()) : i;
        }
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

    public SapRequest(Event event, String clientId, Money amount, String documentNumber, SapRequestType requestType,
            Money advancement,
            JsonObject request) {
        setEvent(event);
        setClientId(clientId);
        setValue(amount);
        setDocumentNumber(documentNumber);
        setRequestType(requestType);
        setAdvancement(advancement);
        setRequest(request.toString());
        setSent(false);
        setWhenCreated(new DateTime());
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
        deleteDomainObject();
    }
}
