package pt.ist.fenixedu.giaf.invoices.task;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.accounting.Refund;
import org.fenixedu.academic.domain.accounting.RefundState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.sap.client.SapFinantialClient;

@Task(englishTitle = "Sync refund information (state and date) with SAP.", readOnly = true)
public class SyncRefundStateSapInfo extends CronTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getSapRoot().getSapRequestSet().stream().parallel().forEach(sr -> process(sr));
    }

    private void process(final SapRequest sr) {
        try {
            FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<Void>() {
                @Override
                public Void call() {
                    syncRefundState(sr);
                    return null;
                }
            }, new AtomicInstance(TxMode.SPECULATIVE_READ, false));
        } catch (Exception e) {
            logError(sr, e);
            e.printStackTrace();
        }
    }

    private void syncRefundState(final SapRequest sr) {
        if (!sr.isInitialization() && sr.getRequestType() == SapRequestType.REIMBURSEMENT && RefundState.CONCLUDED != sr.getRefundState()) {
            String documentNumber = sr.getDocumentNumberForType("NA");
            if (documentNumber == null) { //refund of excess only
                documentNumber = sr.getAdvancementRequest().getDocumentNumberForType("NA");
            }
            JsonObject result = SapFinantialClient.getReimbursementState(documentNumber, SapEvent.IST_VAT_NUMBER, SapEvent.PROCESS_ID);
            taskLog("%s %s %s\n", sr.getExternalId(), sr.getDocumentNumber(), result);
            final String statusCode = result.get("statusCode").getAsString();
            if (!statusCode.equals("E")) {
                RefundState refundState = RefundState.valueOf(statusCode);
                String stateDateStr = result.get("statusDate").getAsString();
                LocalDate stateDate = DateTimeFormat.forPattern("yyyy-MM-dd").parseLocalDate(stateDateStr);

                sr.setRefundState(refundState);
                sr.setRefundStateDate(stateDate);

                Refund refund = sr.getRefund();
                if (isRefundComplete(refund)) {
                    refund.setState(refundState);
                    refund.setStateDate(stateDate);
                }
            }
        }
    }

    private boolean isRefundComplete(final Refund refund) {
        return !refund.getSapRequestSet().stream().anyMatch(sr -> sr.getRefundState() != RefundState.CONCLUDED);
    }

    @Atomic(mode = TxMode.READ)
    private void logError(final SapRequest sr, final Exception e) {
        taskLog("Error processing %s for event %s -> %s\n", sr.getExternalId(), sr.getEvent().getExternalId(), e.getMessage());
    }
}
