package pt.ist.fenixedu.sap.servlet;

import java.util.Collection;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Discount;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventState;
import org.fenixedu.academic.domain.accounting.EventState.ChangeStateEvent;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;

import pt.ist.fenixedu.giaf.invoices.ui.SapInvoiceController;
import pt.ist.fenixframework.FenixFramework;

@WebListener
public class FenixEduSapInvoiceContextListener implements ServletContextListener {

    public static boolean allowCloseToOpen = false;

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        if (GiafInvoiceConfiguration.getConfiguration().sapSyncActive()) {
            Signal.register(AccountingTransaction.SIGNAL_ANNUL, this::handlerAccountingTransactionAnnulment);
            Signal.register(EventState.EVENT_STATE_CHANGED, this::handlerEventStateChange);

            FenixFramework.getDomainModel().registerDeletionBlockerListener(Exemption.class, this::blockExemption);
            FenixFramework.getDomainModel().registerDeletionBlockerListener(Discount.class, this::blockDiscount);
        }
    }
    
    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
    }

    private void handlerAccountingTransactionAnnulment(final DomainObjectEvent<AccountingTransaction> domainEvent) {
        final AccountingTransaction transaction = domainEvent.getInstance();
        final Event event = transaction.getEvent();
        syncEvent(event);
        throw new Error("This transaction must be first canceled / undone in SAP"); // TODO
    }

    private void handlerEventStateChange(final ChangeStateEvent eventStateChange) {
        final Event event = eventStateChange.getEvent();
        final EventState oldtState = event.getEventState();
        final EventState newState = eventStateChange.getNewState();

        /*
         *  NewValue > |  null  | OPEN | CLOSED | CANCELED
         *  OldValue   |________|______|________|__________
         *     V       |  
         *    null     |   OK   |  Ok  |   Ex   |   Ex
         *    OPEN     |   Ex   |  OK  |   Ok   |   SAP
         *   CLOSED    |   Ex   |  Ex  |   OK   |   Ex
         *  CANCELED   |   Ex   |  Ex  |   Ex   |   OK
         */

        if (oldtState == newState) {
            // Not really a state change... nothing to be done.
        } else if (oldtState == null && newState == EventState.OPEN) {
            // Ack, normal SAP integration will be fine.
        } else if (oldtState == EventState.OPEN && newState == EventState.CLOSED) {
            // Ack, normal SAP integration will be fine.
        } else if (oldtState == EventState.OPEN && newState == EventState.CANCELLED) {
            syncEvent(event);
            throw new Error("Event state change must first be canceled in SAP"); // TODO
        } else if (allowCloseToOpen && oldtState == EventState.CLOSED && newState == EventState.OPEN) {
            // Ack. Fuck it...
        } else {
            throw new Error("New event state change that must be handled: "
                    + (oldtState == null ? "" : oldtState.name()) + " -> "
                    + (newState == null ? "null" : newState.name())
                    + " on event: " + event.getExternalId());
        }
    }

    private void blockExemption(final Exemption exemption, final Collection<String> blockers) {
        final Event event = exemption.getEvent();
        syncEvent(event);
        blockers.add("Exemption must be first undone in SAP"); // TODO
    }

    private void blockDiscount(final Discount discount, final Collection<String> blockers) {
        final Event event = discount.getEvent();
        syncEvent(event);
        blockers.add("Discount must first be removed from SAP"); // TODO
    }

    private void syncEvent(final Event event) {
//        final String errors = SapInvoiceController.syncEvent(event);
//        if (!errors.isEmpty()) {
//            throw new Error("Unable to sync event: " + errors);
//        }
    }

}
