package pt.ist.fenixedu.sap.servlet;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.*;
import org.fenixedu.academic.domain.accounting.EventState.ChangeStateEvent;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixframework.FenixFramework;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.time.Year;
import java.util.Calendar;
import java.util.Collection;
import java.util.Optional;

@WebListener
public class FenixEduSapInvoiceContextListener implements ServletContextListener {


    private static final String BUNDLE = "resources.GiafInvoicesResources";

    public static boolean allowCloseToOpen = false;

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        Event.canBeRefunded = (event) -> {
            final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
            return calculator.getPayments().count() > 0 && calculator.getPayments()
                    .allMatch(p -> p.getDate().getYear() >= 2018);
//                .map(p -> (AccountingTransaction) FenixFramework.getDomainObject(p.getId()))
//                .allMatch(t -> event.getSapRequestSet().stream().anyMatch(sr -> sr.getPayment() == t && sr.getCanBeRefunded()));
        };

        Signal.register(AccountingTransactionDetail.SIGNAL_TRANSACTION_DETAIL_INIT, this::checkAllowedPaymentDate);

        if (GiafInvoiceConfiguration.getConfiguration().sapSyncActive()) {
            Signal.register(AccountingTransaction.SIGNAL_ANNUL, this::handlerAccountingTransactionAnnulment);
            Signal.register(EventState.EVENT_STATE_CHANGED, this::handlerEventStateChange);
            Signal.register(EventState.EVENT_STATE_CHANGED, this::calculateSapRequestsForCanceledEvent);
//            Signal.registerWithoutTransaction(EventState.EVENT_STATE_CHANGED, this::processEvent);

            FenixFramework.getDomainModel().registerDeletionBlockerListener(Exemption.class, this::blockExemption);
            FenixFramework.getDomainModel().registerDeletionBlockerListener(Discount.class, this::blockDiscount);
            FenixFramework.getDomainModel().registerDeletionBlockerListener(Refund.class, this::blockRefund);
        }

        EnrolmentBlocker.enrolmentBlocker = new EnrolmentBlocker() {

            @Override
            public boolean isAnyGratuityOrAdministrativeOfficeFeeAndInsuranceInDebt(final StudentCurricularPlan scp, final ExecutionYear executionYear) {
                return isAnyTuitionInDebt(scp.getStudent().getStudent(), executionYear) || isAnyAdministrativeOfficeFeeAndInsuranceInDebtUntil(scp, executionYear);
            }

            private boolean isAnyAdministrativeOfficeFeeAndInsuranceInDebtUntil(final StudentCurricularPlan scp, final ExecutionYear executionYear) {
                final boolean hasInsuranceDebt = scp.getPerson().getInsuranceEventsUntil(executionYear.getPreviousExecutionYear())
                        .anyMatch(event -> (!event.getLapsed()) && Utils.isOverDue(event));

                final boolean hasAdminDebt = scp.getPerson().getAdministrativeOfficeFeeEventsUntil(executionYear.getPreviousExecutionYear())
                        .anyMatch(event -> (!event.getLapsed()) && Utils.isOverDue(event));

                return hasInsuranceDebt || hasAdminDebt;
            }

            private boolean isAnyTuitionInDebt(final Student student, final ExecutionYear executionYear) {
                return student.getRegistrationStream().anyMatch(r -> hasAnyNotPayedGratuityEventsForPreviousYears(r, executionYear));
            }

            public boolean hasAnyNotPayedGratuityEventsForPreviousYears(final Registration registration, final ExecutionYear limitExecutionYear) {
                return registration.getGratuityEventsUntil(limitExecutionYear.getPreviousExecutionYear())
                    .filter(event -> event.getSapRoot() == null)
                    .anyMatch(event -> (!event.getLapsed()) && Utils.isOverDue(event));
            }
        };
    }

    private void checkAllowedPaymentDate(final DomainObjectEvent<AccountingTransactionDetail> doEvent) {
        final AccountingTransactionDetail detail = doEvent.getInstance();
        final DateTime dateTime = detail.getWhenRegistered();
        final int year = dateTime.getYear();
        if (year != Year.now().getValue() && year != SapRoot.getInstance().getOpenYear().intValue()) {
            throw new DomainException(Optional.of(BUNDLE), "error.not.allowed.to.register.payments.for.year");
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
    }

    private void handlerAccountingTransactionAnnulment(final DomainObjectEvent<AccountingTransaction> domainEvent) {
        final AccountingTransaction transaction = domainEvent.getInstance();
        final Event event = transaction.getEvent();
        if (new SapEvent(event).hasPayment(transaction.getExternalId())) {
            throw new DomainException(Optional.of(BUNDLE), "error.first.undo.transaction.in.sap");
        }
    }

    private void handlerEventStateChange(final ChangeStateEvent eventStateChange) {
        final Event event = eventStateChange.getEvent();
        final EventState oldState = eventStateChange.getOldState();
        final EventState newState = eventStateChange.getNewState();

        /*
         *  NewValue > |  null  | OPEN | CLOSED | CANCELED
         *  OldValue   |________|______|________|__________
         *     V       |  
         *    null     |   OK   |  OK  |   Ex   |   Ex
         *    OPEN     |   Ex   |  OK  |   OK   |   SAP
         *   CLOSED    |   Ex   |  Ok  |   OK   |   OK
         *  CANCELED   |   Ex   |  Ex  |   Ex   |   OK
         */

        if (oldState == newState) {
            // Not really a state change... nothing to be done.
        } else if (oldState == null && newState == EventState.OPEN) {
            // Ack, normal SAP integration will be fine.
        } else if (oldState == EventState.OPEN && newState == EventState.CLOSED) {
            // Ack, normal SAP integration will be fine.
        } else if (oldState == EventState.CLOSED && newState == EventState.OPEN) {
            // Ack, normal SAP integration will be fine.
        } else if (oldState == EventState.CLOSED && newState == EventState.CANCELLED) {
            // Ack, normal SAP integration will be fine.
        } else if (oldState == EventState.OPEN && newState == EventState.CANCELLED) {
            if (!new SapEvent(event).canCancel()) {
                throw new DomainException(Optional.of(BUNDLE), "error.event.state.change.first.in.sap");
            }
        } else if (allowCloseToOpen && oldState == EventState.CLOSED && newState == EventState.OPEN) {
            // Ack. 
        } else {
            throw new DomainException(Optional.of(BUNDLE), "error.new.event.state.change.must.be.handled", (oldState == null ?
                    "null" : oldState.name()), (newState == null ? "null" : newState.name()), event.getExternalId());
        }
    }

    private void blockExemption(final Exemption exemption, final Collection<String> blockers) {
        final Event event = exemption.getEvent();
        if (new SapEvent(event).hasCredit(exemption.getExternalId())) {
            blockers.add(BundleUtil.getString(BUNDLE, "error.first.undo.exemption.in.sap"));
        }
    }

    private void blockDiscount(final Discount discount, final Collection<String> blockers) {
        final Event event = discount.getEvent();
        if (new SapEvent(event).hasCredit(discount.getExternalId())) {
            blockers.add(BundleUtil.getString(BUNDLE, "error.first.undo.discount.in.sap"));
        }
    }

    private void blockRefund(final Refund refund, final Collection<String> blockers) {
        final Event event = refund.getEvent();
        if (new SapEvent(event).hasRefund(refund.getExternalId())) {
            blockers.add(BundleUtil.getString(BUNDLE, "error.first.undo.refund.in.sap"));
        }
    }

    private void calculateSapRequestsForCanceledEvent(final ChangeStateEvent eventStateChange) {
        final Event event = eventStateChange.getEvent();
        final EventState oldState = eventStateChange.getOldState();
        final EventState newState = eventStateChange.getNewState();

        if (newState == EventState.CANCELLED && oldState != newState && event.getSapRequestSet().isEmpty()) {
            final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
            final Money debtAmount = new Money(calculator.getDebtAmount());
            if (debtAmount.isPositive()) {
                final DebtExemption debtExemption = calculator.getAccountingEntries().stream()
                        .filter(e -> e instanceof DebtExemption)
                        .map(e -> (DebtExemption) e)
                        .filter(e -> new Money(e.getAmount()).equals(debtAmount))
                        .findAny().orElse(null);
                if (debtExemption == null) {
                    throw new Error("inconsistent data, event is canceled but the the exempt value does not match the orginal debt value");
                }
                final SapEvent sapEvent = new SapEvent(event);
                sapEvent.fakeSapRequest(SapRequestType.INVOICE, "ND0", debtAmount, null);
                sapEvent.fakeSapRequest(SapRequestType.CREDIT, "NA0", debtAmount, debtExemption.getId());
            }
        }
    }

    private void processEvent(final ChangeStateEvent eventStateChange) {
        if (!isScheduler()) {
            Thread thread = new FenixEduSapInvoiceContextListener.ProcessEvent(eventStateChange);
            thread.start();
        }
    }

    private boolean isScheduler() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int iter = 0; iter < stackTrace.length; iter++) {
            if (stackTrace[iter].getClassName().contains("org.fenixedu.bennu.scheduler")) {
                return true;
            }
        }
        return false;
    }

    private static class ProcessEvent extends Thread {

        private ChangeStateEvent eventStateChange;

        public ProcessEvent(final ChangeStateEvent eventStateChange) {
            setName(this.getClass().getSimpleName());
            this.eventStateChange = eventStateChange;
        }

        @Override
        public void run() {
            EventProcessor.calculate(() -> eventStateChange.getEvent());
            EventProcessor.sync(() -> eventStateChange.getEvent());
        }
    }
    
}
