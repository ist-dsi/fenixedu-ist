package pt.ist.fenixedu.sap.servlet;

import java.util.Collection;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Discount;
import org.fenixedu.academic.domain.accounting.EnrolmentBlocker;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventState;
import org.fenixedu.academic.domain.accounting.EventState.ChangeStateEvent;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;

import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

@WebListener
public class FenixEduSapInvoiceContextListener implements ServletContextListener {

    public static boolean allowCloseToOpen = false;

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        Event.canBeRefunded = (event) -> event.getSapRequestSet().stream().anyMatch(sr -> sr.getCanBeRefunded());

        if (GiafInvoiceConfiguration.getConfiguration().sapSyncActive()) {
            Signal.register(AccountingTransaction.SIGNAL_ANNUL, this::handlerAccountingTransactionAnnulment);
            Signal.register(EventState.EVENT_STATE_CHANGED, this::handlerEventStateChange);

            FenixFramework.getDomainModel().registerDeletionBlockerListener(Exemption.class, this::blockExemption);
            FenixFramework.getDomainModel().registerDeletionBlockerListener(Discount.class, this::blockDiscount);
        }

        EnrolmentBlocker.enrolmentBlocker = new EnrolmentBlocker() {

            @Override
            public boolean isAnyGratuityOrAdministrativeOfficeFeeAndInsuranceInDebt(final StudentCurricularPlan scp, final ExecutionYear executionYear) {
                return isAnyTuitionInDebt(scp.getStudent().getStudent(), executionYear) || isAnyAdministrativeOfficeFeeAndInsuranceInDebtUntil(scp, executionYear);
            }

            private boolean isAnyAdministrativeOfficeFeeAndInsuranceInDebtUntil(final StudentCurricularPlan scp, final ExecutionYear executionYear) {
                for (final Event event : scp.getPerson().getEventsSet()) {
                    if (event.getSapRoot() == null
                            && event instanceof AdministrativeOfficeFeeAndInsuranceEvent
                            && ((AdministrativeOfficeFeeAndInsuranceEvent) event).getExecutionYear().isBefore(executionYear)
                            && event.isOpen()) {
                        return true;
                    }
                }

                return false;
            }

            private boolean isAnyTuitionInDebt(final Student student, final ExecutionYear executionYear) {
                return student.getRegistrationStream().anyMatch(r -> hasAnyNotPayedGratuityEventsForPreviousYears(r, executionYear));
            }

            public boolean hasAnyNotPayedGratuityEventsForPreviousYears(final Registration registration, final ExecutionYear limitExecutionYear) {
                for (final StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
                    if (hasAnyNotPayedGratuityEventsForPreviousYears(studentCurricularPlan, limitExecutionYear)) {
                        return true;
                    }
                }
                return false;
            }

            final public boolean hasAnyNotPayedGratuityEventsForPreviousYears(final StudentCurricularPlan scp, final ExecutionYear limitExecutionYear) {
                for (final GratuityEvent gratuityEvent : scp.getGratuityEventsSet()) {
                    if (gratuityEvent.getSapRoot() == null && gratuityEvent.getExecutionYear().isBefore(limitExecutionYear) && gratuityEvent.isInDebt()) {
                        return true;
                    }
                }
                return false;
            }

        };
    }
    
    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
    }

    private void handlerAccountingTransactionAnnulment(final DomainObjectEvent<AccountingTransaction> domainEvent) {
        final AccountingTransaction transaction = domainEvent.getInstance();
        final Event event = transaction.getEvent();
        if (new SapEvent(event).hasPayment(transaction.getExternalId())) {
            throw new Error("This transaction must be first canceled / undone in SAP");
        }
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
            if (new SapEvent(event).canCancel()) {
                throw new Error("Event state change must first be canceled in SAP");
            }
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
        if (new SapEvent(event).hasCredit(exemption.getExternalId())) {
            blockers.add("Exemption must be first undone in SAP");
        }
    }

    private void blockDiscount(final Discount discount, final Collection<String> blockers) {
        final Event event = discount.getEvent();
        if (new SapEvent(event).hasCredit(discount.getExternalId())) {
            blockers.add("Discount must first be removed from SAP");
        }
    }

}
