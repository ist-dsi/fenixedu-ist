package pt.ist.fenixedu.giaf.invoices;

import java.math.BigDecimal;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.PostalCodeValidator;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.PaymentMode;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeEvent;
import org.fenixedu.academic.domain.accounting.events.EnrolmentEvaluationEvent;
import org.fenixedu.academic.domain.accounting.events.ImprovementOfApprovedEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.SpecialSeasonEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.dfa.DFACandidacyEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.phd.debts.ExternalScholarshipPhdGratuityContribuitionEvent;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.generated.sources.saft.sap.SAFTPTPaymentType;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSettlementType;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSourceBilling;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSourcePayment;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixedu.domain.ExternalClient;
import pt.ist.fenixedu.domain.SapDocumentFile;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.sap.client.SapFinantialClient;

public class SapEvent {

    private static final String DT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String MORADA_DESCONHECIDO = "Desconhecido";
    private static final String EMPTY_JSON = "{}";
    private static final int MAX_SIZE_ADDRESS = 100;
    private static final int MAX_SIZE_CITY = 50;
    private static final int MAX_SIZE_REGION = 50;
    private static final int MAX_SIZE_POSTAL_CODE = 20;
    private static final int MAX_SIZE_VAT_NUMBER = 20;
    public LocalDate currentDate = new LocalDate();
    public static Function<LocalDate, DateTime> documentDatePreviousYear =
            (currentDate) -> new DateTime(currentDate.getYear() - 1, 12, 31, 23, 59);

    public Event event = null;

    public SapEvent(final Event event) {
        this.event = event;
    }

    public Stream<SapRequest> getFilteredSapRequestStream() {
        return event.getSapRequestSet().stream().filter(r -> !r.getIgnore());
    }

    public void registerInvoice(Money debtFenix, Event event, boolean isGratuity, boolean isNewDate) throws Exception {

        if (isToProcessDebt(event, isGratuity)) {
            //if debt is greater than invoice, then there was a debt registered and the correspondent invoice failed, don't register the debt again
            if (!getDebtAmount().greaterThan(getInvoiceAmount())) {
                registerDebt(debtFenix, event, isNewDate);
            }
        }

        String clientId = ClientMap.uVATNumberFor(event.getParty());
        JsonObject data = toJsonInvoice(event, debtFenix, getDocumentDate(event.getWhenOccured(), isNewDate), new DateTime(),
                clientId, false, false);

        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest =
                new SapRequest(event, clientId, debtFenix, documentNumber, SapRequestType.INVOICE, Money.ZERO, data);

//        if (checkAndRegisterIntegration(event, errorLog, elogger, data, documentNumber, sapRequest, result,
//                SapRequestType.INVOICE.name(), true)) {
        // if there are amounts in advancement we need to register them in the new invoice
        //TODO if we do this the value registered in the advancement will be counted 2 times in different categories
//            Money advancementAmount = getAdvancementAmount();
//            if (advancementAmount.isPositive()) {
//                return registerPaymentFromAdvancement(event, clientId, advancementAmount, sapRequest, errorLog, elogger);
//            } else {
//                return true;
//            }
//            return true;
//        } else {
//            return false;
//        }
    }

    @Atomic
    public void transferInvoice(final SapRequest sapRequest, final ExternalClient externalClient, final Money amountToTransfer) throws Exception {
        final SapRequestType requestType = sapRequest.getRequestType();
        if (requestType != SapRequestType.INVOICE) {
            throw new Error("label.error.document.is.not.an.invoice");
        }
        if (amountToTransfer.isZero()) {
            throw new Error("label.error.value.to.transfer.must.be.posituve");
        }
        event.getSapRequestSet().stream()
            .filter(r -> r != sapRequest && r.refersToDocument(sapRequest.getDocumentNumber()))
            .findAny().ifPresent(r -> {
                throw new Error("label.error.invoice.already.used");
            });
        final Money invoiceValue = sapRequest.getValue();
        final Money remainder = invoiceValue.subtract(amountToTransfer);
        if (remainder.isNegative()) {
            throw new Error("label.error.amount.exceeds.invoice.value");
        } else {
            final SapRequest creditRequest = registerCredit(event, EventProcessor.getCreditEntry(invoiceValue), invoiceValue, sapRequest);
            sapRequest.setIgnore(true);
            creditRequest.setIgnore(true);

            final JsonObject data = toJsonInvoice(sapRequest, externalClient, amountToTransfer, getDocumentDate(event.getWhenOccured(), true), new DateTime(), false, false);
            final String documentNumber = getDocumentNumber(data, false);
            new SapRequest(event, externalClient.getClientId(), amountToTransfer, documentNumber, SapRequestType.INVOICE, Money.ZERO, data);
        }
        if (remainder.isPositive()) {
            final JsonObject data = toJsonInvoice(event, remainder, getDocumentDate(event.getWhenOccured(), true), new DateTime(), sapRequest.getClientId(), false, false);
            final String documentNumber = getDocumentNumber(data, false);
            new SapRequest(event, sapRequest.getClientId(), remainder, documentNumber, SapRequestType.INVOICE, Money.ZERO, data);
        }
    }

    @Atomic
    public void cancelDocument(final SapRequest sapRequest) {
        final SapRequestType requestType = sapRequest.getRequestType();
        if (requestType != SapRequestType.INVOICE
                && requestType != SapRequestType.INVOICE_INTEREST
                && requestType != SapRequestType.CREDIT
                && requestType != SapRequestType.PAYMENT
                && requestType != SapRequestType.PAYMENT_INTEREST) {
            throw new Error("label.document.type.cannot.be.canceled");
        }
        event.getSapRequestSet().stream()
                .filter(r -> !r.getIgnore())
                .filter(r -> r != sapRequest && r.refersToDocument(sapRequest.getDocumentNumber()))
                .findAny().ifPresent(r -> {
                    throw new Error("label.error.invoice.already.used");
                });

        JsonObject jsonAnnulled = new JsonParser().parse(sapRequest.getRequest()).getAsJsonObject();
        if (requestType == SapRequestType.INVOICE
                || requestType == SapRequestType.INVOICE_INTEREST
                || requestType == SapRequestType.CREDIT) {
            final JsonObject workDocument = jsonAnnulled.get("workingDocument").getAsJsonObject();
            workDocument.addProperty("workStatus", "A");
            workDocument.addProperty("documentDate", new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
        }
        if (requestType == SapRequestType.PAYMENT
                || requestType == SapRequestType.PAYMENT_INTEREST
                || requestType == SapRequestType.CREDIT) {
            final JsonObject workDocument = jsonAnnulled.get("paymentDocument").getAsJsonObject();
            workDocument.addProperty("paymentStatus", "A");
            workDocument.addProperty("paymentDate", new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
        }
        if (requestType == SapRequestType.CREDIT) {
            registerDebt(sapRequest.getValue(), event, true);
        }
            
        final SapRequest sapRequestAnnulled = new SapRequest(sapRequest.getEvent(), sapRequest.getClientId(), sapRequest.getValue(),
                sapRequest.getDocumentNumber(), sapRequest.getRequestType(), sapRequest.getAdvancement(), jsonAnnulled);
        sapRequest.setAnulledRequest(sapRequestAnnulled);
        sapRequestAnnulled.setIgnore(true);
    }

    @Atomic
    public void cancelDebt() throws Exception {
        if (getFilteredSapRequestStream().anyMatch(r -> !r.isDebtDocument())) {
            throw new Error("label.error.cannot.cancel.debt.active.documents");
        }
        final Money valueToCredit = calculateDebtValue();
        if (valueToCredit.isZero()) {
            throw new Error("label.error.debt.already.cancelled");
        }
        if (valueToCredit.isNegative()) {
            throw new Error("label.error.negative.debt");
        }
        registerDebtCredit(EventProcessor.getCreditEntry(valueToCredit), event, true);
        getFilteredSapRequestStream().forEach(r -> r.setIgnore(true));
    }

    public void updateInvoiceWithNewClientData() throws Exception {
        for (final SapRequest sapRequest : getFilteredSapRequestStream().collect(Collectors.toSet())) {
            updateInvoiceWithNewClientData(sapRequest);
        }
    }

    private void updateInvoiceWithNewClientData(final SapRequest sapRequest) throws Exception {
        final SapRequestType requestType = sapRequest.getRequestType();
        if (requestType != SapRequestType.INVOICE) {
            return;
        }
        if (event.getSapRequestSet().stream().anyMatch(r -> r != sapRequest && r.refersToDocument(sapRequest.getDocumentNumber()))) {
            return;
        }
        final String clientId = ClientMap.uVATNumberFor(event.getParty());
        final String sapRequestClientId = sapRequest.getClientId();
        if (clientId.equals(sapRequestClientId)) {
            return;
        }
        if (SapRoot.getInstance().getExternalClientSet().stream().map(c -> c.getClientId()).anyMatch(s -> s.equals(sapRequestClientId))) {
            return;
        }

        final Money invoiceValue = sapRequest.getValue();
        final SapRequest creditRequest = registerCredit(event, EventProcessor.getCreditEntry(invoiceValue), invoiceValue, sapRequest);
        sapRequest.setIgnore(true);
        creditRequest.setIgnore(true);

        final JsonObject data = toJsonInvoice(event, invoiceValue, getDocumentDate(event.getWhenOccured(), true), new DateTime(), clientId, false, false);
        final String documentNumber = getDocumentNumber(data, false);
        new SapRequest(event, clientId, invoiceValue, documentNumber, SapRequestType.INVOICE, Money.ZERO, data);
    }

    private void registerPaymentFromAdvancement(Event event, String clientId, Money advancementAmount, SapRequest invoiceRequest,
            ErrorLogConsumer errorLog, EventLogger elogger) throws Exception {

        checkValidDocumentNumber(invoiceRequest.getDocumentNumber(), event);

        // ver se o valor da divida é superior ou igual ao advancement, se for tudo ok, caso contrário é registar o pagamento no valor da factura
        // e abater esse valor ao advancement
        Money amountToRegister = advancementAmount;
        if (advancementAmount.greaterThan(invoiceRequest.getValue())) {
            amountToRegister = invoiceRequest.getValue();
        }

        JsonObject data = toJsonPaymentFromAdvancement(event, invoiceRequest, amountToRegister);
        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest = new SapRequest(event, clientId, amountToRegister, documentNumber, SapRequestType.PAYMENT,
                amountToRegister.negate(), data);
    }

    private SapRequest registerDebt(Money debtFenix, Event event, boolean isNewDate) {
        String clientId = ClientMap.uVATNumberFor(event.getParty());
        JsonObject data = toJsonDebt(event, debtFenix, clientId, getDocumentDate(event.getWhenOccured(), isNewDate),
                new DateTime(), true, "NG", true, isNewDate, null);

        String documentNumber = getDocumentNumber(data, false);
        return new SapRequest(event, clientId, debtFenix, documentNumber, SapRequestType.DEBT, Money.ZERO, data);
    }

    private void registerDebtCredit(CreditEntry creditEntry, Event event, boolean isNewDate) throws Exception {

        String clientId = ClientMap.uVATNumberFor(event.getParty());
        SimpleImmutableEntry<List<SapRequest>, Money> openDebtsAndRemainingValue = getOpenDebtsAndRemainingValue();
        List<SapRequest> openDebts = openDebtsAndRemainingValue.getKey();
        Money remainingAmount = openDebtsAndRemainingValue.getValue();
        if (creditEntry.getAmount().compareTo(remainingAmount.getAmount()) == 1) {
            if (openDebts.size() > 1) {
                // dividir o valor da isenção pelas várias dívidas
                registerDebtCreditList(event, openDebts, new Money(creditEntry.getAmount()), creditEntry, remainingAmount,
                        clientId, isNewDate);
            } else {
                // o valor da isenção é superior ao valor em dívida
                SapRequest debt;
                if (openDebts.size() == 1) { // mas só existe uma dívida abertura
                    debt = openDebts.get(0);
                } else { // não existe nenhuma dívida aberta, ir buscar a última
                    debt = getLastDebt();
                }
                registerDebtCredit(clientId, event, new Money(creditEntry.getAmount()), creditEntry, debt, isNewDate);
            }
        } else {
            //tudo normal
            registerDebtCredit(clientId, event, new Money(creditEntry.getAmount()), creditEntry, openDebts.get(0), isNewDate);
        }
    }

    private void registerDebtCreditList(Event event, List<SapRequest> openDebts, Money amountToRegister, CreditEntry creditEntry,
            Money remainingAmount, String clientId, boolean isNewDate)
            throws Exception {
        if (amountToRegister.greaterThan(remainingAmount)) {
            if (openDebts.size() > 1) {
                registerDebtCredit(clientId, event, remainingAmount, creditEntry, openDebts.get(0), isNewDate);
                registerDebtCreditList(event, openDebts.subList(1, openDebts.size()), amountToRegister.subtract(remainingAmount),
                        creditEntry, openDebts.get(1).getValue(), clientId, isNewDate);
            } else {
                registerDebtCredit(clientId, event, amountToRegister, creditEntry, openDebts.get(0), isNewDate);
            }
        } else {
            registerDebtCredit(clientId, event, amountToRegister, creditEntry, openDebts.get(0), isNewDate);
        }
    }

    private void registerDebtCredit(String clientId, Event event, Money amountToRegister, CreditEntry creditEntry,
            SapRequest debtRequest, boolean isNewDate) throws Exception {
        checkValidDocumentNumber(debtRequest.getDocumentNumber(), event);

        JsonObject data = toJsonDebtCredit(event, amountToRegister, clientId,
                getDocumentDate(creditEntry.getCreated(), isNewDate), new DateTime(), true, "NJ", false, isNewDate, debtRequest);
        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest =
                new SapRequest(event, clientId, amountToRegister, documentNumber, SapRequestType.DEBT_CREDIT, Money.ZERO, data);
        sapRequest.setCreditId(creditEntry.getId());
    }

    public void registerCredit(Event event, CreditEntry creditEntry, boolean isGratuity, ErrorLogConsumer errorLog,
            EventLogger elogger) throws Exception {
        // diminuir divida no sap (se for propina diminuir dívida) e credit note na última factura existente
        // se o valor pago nesta factura for superior à nova dívida, o que fazer? terá que existir nota crédito no fenix -> sim

        if (isToProcessDebt(event, isGratuity)) {
            //if the debt credit amount is greater than the credit amount it means that a credit debt was registered but the correspondent invoice credit failed
            //we don't register the credit debt again
            if (!getDebtCreditAmount().greaterThan(getCreditAmount())) {
                registerDebtCredit(creditEntry, event, true);
            }
        }

        String clientId = ClientMap.uVATNumberFor(event.getParty());

        SimpleImmutableEntry<List<SapRequest>, Money> openInvoicesAndRemainingValue = getOpenInvoicesAndRemainingValue();
        List<SapRequest> openInvoices = openInvoicesAndRemainingValue.getKey();
        Money remainingAmount = openInvoicesAndRemainingValue.getValue();
        if (creditEntry.getAmount().compareTo(remainingAmount.getAmount()) == 1) {
            if (openInvoices.size() > 1) {
                // dividir o valor da isenção pelas várias facturas....
                registerCreditList(event, openInvoices, creditEntry, new Money(creditEntry.getAmount()), remainingAmount,
                        clientId, errorLog, elogger);
            } else {
                // o valor da isenção é superior ao valor em dívida
                SapRequest invoiceNumber = null;
                if (openInvoices.size() == 1) { // mas só existe uma factura abertura
                    invoiceNumber = openInvoices.get(0);
                } else { // não existe nenhuma factura aberta, ir buscar a última
                    invoiceNumber = getLastInvoiceNumber();
                }
                registerCredit(event, creditEntry, new Money(creditEntry.getAmount()), invoiceNumber);
            }
        } else {
            //tudo normal
            registerCredit(event, creditEntry, new Money(creditEntry.getAmount()), openInvoices.get(0));
        }
    }

    private void registerCreditList(Event event, List<SapRequest> openInvoices, CreditEntry creditEntry, Money amountToRegister,
            Money remainingAmount, String clientId, ErrorLogConsumer errorLog, EventLogger elogger) throws Exception {
        if (amountToRegister.greaterThan(remainingAmount)) {
            if (openInvoices.size() > 1) {
                registerCredit(event, creditEntry, remainingAmount, openInvoices.get(0));
                registerCreditList(event, openInvoices.subList(1, openInvoices.size()), creditEntry,
                        amountToRegister.subtract(remainingAmount), openInvoices.get(1).getValue(), clientId, errorLog, elogger);
            } else {
                registerCredit(event, creditEntry, amountToRegister, openInvoices.get(0));
            }
        } else {
            registerCredit(event, creditEntry, amountToRegister, openInvoices.get(0));
        }
    }

    private SapRequest registerCredit(Event event, CreditEntry creditEntry, Money creditAmount, SapRequest sapInvoiceRequest) throws Exception {
        checkValidDocumentNumber(sapInvoiceRequest.getDocumentNumber(), event);

        JsonObject data = toJsonCredit(event, getDocumentDate(creditEntry.getCreated(), false), creditAmount, sapInvoiceRequest, false, true);
        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest =
                new SapRequest(event, sapInvoiceRequest.getClientId(), creditAmount, documentNumber, SapRequestType.CREDIT, Money.ZERO, data);
        sapRequest.setCreditId(creditEntry.getId());
        return sapRequest;
    }

    public void registerReimbursement(Event event, Money amount, ErrorLogConsumer errorLog, EventLogger elogger)
            throws Exception {

        SapRequest sapInvoiceRequest = getLastInvoiceNumber();
        checkValidDocumentNumber(sapInvoiceRequest.getDocumentNumber(), event);

        String clientId = ClientMap.uVATNumberFor(event.getParty());
        JsonObject data = toJsonReimbursement(event, amount, sapInvoiceRequest, false, true);

        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest =
                new SapRequest(event, clientId, amount, documentNumber, SapRequestType.REIMBURSEMENT, Money.ZERO, data);
    }

    private void registerInterest(final Money payedInterest, final String clientId,
            final AccountingTransactionDetail transactionDetail, final ErrorLogConsumer errorLog, final EventLogger elogger)
            throws Exception {

        if (hasInterestPayment(transactionDetail.getTransaction())) {
            return; //both invoice and payment requests have been integrated
        }

        SapRequest interestInvoiceRequest = getInterestInvoiceRequest(transactionDetail.getTransaction());
        if (interestInvoiceRequest == null) {
            // registering the invoice
            interestInvoiceRequest = registerInterestInvoice(payedInterest, clientId, transactionDetail,
                    getDocumentDate(transactionDetail.getWhenProcessed(), false), errorLog, elogger);
        }
//        else {
//            // the interest invoice was integrated but for some reason the interest payment request was not created
//        }

        // even if the invoice fails (in the integration with SAP) we want to register the request
        // so that when the invoice interest is integrated the pending interest payment is resent and integrated also 
        // registering the payment
        registerInterestPayment(payedInterest, interestInvoiceRequest, transactionDetail, errorLog, elogger);
    }

    private SapRequest registerInterestInvoice(final Money payedInterest, final String clientId,
            final AccountingTransactionDetail transactionDetail, DateTime paymentDate, final ErrorLogConsumer errorLog,
            final EventLogger elogger) throws Exception {
        JsonObject data = toJsonInvoice(event, payedInterest, paymentDate, new DateTime(), clientId, false,
                true);

        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest = new SapRequest(event, clientId, payedInterest, documentNumber,
                SapRequestType.INVOICE_INTEREST, Money.ZERO, data);
        sapRequest.setPayment(transactionDetail.getTransaction());
        return sapRequest;
    }

    private void registerInterestPayment(final Money payedInterest, SapRequest sapInvoiceRequest,
            final AccountingTransactionDetail transactionDetail, final ErrorLogConsumer errorLog, final EventLogger elogger) throws Exception {
        JsonObject data = toJsonPayment(transactionDetail, payedInterest, sapInvoiceRequest, true);

        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest = new SapRequest(event, sapInvoiceRequest.getClientId(), payedInterest, documentNumber,
                SapRequestType.PAYMENT_INTEREST, Money.ZERO, data);
        sapRequest.setPayment(transactionDetail.getTransaction());
    }

    public void registerPayment(Payment payment, ErrorLogConsumer errorLog, EventLogger elogger) throws Exception {

        AccountingTransactionDetail transactionDetail =
                ((AccountingTransaction) FenixFramework.getDomainObject(payment.getId())).getTransactionDetail();
        String clientId = ClientMap.uVATNumberFor(event.getParty());

        // ir buscar a ultima factura aberta e verificar se o pagamento ultrapassa o valor da factura
        // e associar o restante à(s) factura(s) seguinte(s)
        SimpleImmutableEntry<List<SapRequest>, Money> openInvoicesAndRemainingAmount = getOpenInvoicesAndRemainingValue();

        Money payedAmount = new Money(payment.getUsedAmountInDebts());
        Money firstRemainingAmount = openInvoicesAndRemainingAmount.getValue();
        List<SapRequest> openInvoices = openInvoicesAndRemainingAmount.getKey();

        final Money payedInterest = new Money(payment.getUsedAmountInInterests().add(payment.getUsedAmountInFines()));
        if (payedInterest.isPositive()) {
            registerInterest(payedInterest, clientId, transactionDetail, errorLog, elogger);
        }

        if (payedAmount.isZero()) {
            if (payedInterest.isZero()) {
                //there was a payment made and the debt is already closed, it's an extra payment (advancement)
                payedAmount = new Money(payment.getAmount());
            } else {
                //it was all used for interests, there is nothing more to register
                return;
            }
        }

        if (firstRemainingAmount.isZero()) {
            // não há facturas abertas, fazer adiantamento, sobre a última factura!!
            registerAdvancement(Money.ZERO, payedAmount, getLastInvoiceNumber(), transactionDetail, errorLog, elogger);
        }

        if (firstRemainingAmount.lessThan(payedAmount)) {
            // quer dizer que ou há outra factura aberta ou é um pagamento em excesso
            // dividir o valor pago pela facturas e registar n pagamentos ou registar um pagamento adiamento

            if (openInvoices.size() == 1) {
                // só há uma factura aberta -> fazer adiantamento
                registerAdvancement(firstRemainingAmount, payedAmount.subtract(firstRemainingAmount), openInvoices.get(0),
                        transactionDetail, errorLog, elogger);
            } else {
                // vai distribuir o pagamento pelas restantes facturas abertas
                registerPaymentList(openInvoices, payedAmount, firstRemainingAmount, clientId, transactionDetail, errorLog,
                        elogger);
            }
        } else {
            // tudo ok, é só registar o pagamento
            registerPayment(transactionDetail, payedAmount, openInvoices.get(0), errorLog, elogger);
        }
    }

    private void registerPaymentList(List<SapRequest> openInvoices, Money amountToRegister, Money remainingAmount,
            String clientId, AccountingTransactionDetail transactionDetail, ErrorLogConsumer errorLog, EventLogger elogger)
            throws Exception {
        if (amountToRegister.greaterThan(remainingAmount)) {
            if (openInvoices.size() > 1) {
                registerPayment(transactionDetail, remainingAmount, openInvoices.get(0), errorLog,
                        elogger);
                registerPaymentList(openInvoices.subList(1, openInvoices.size()), amountToRegister.subtract(remainingAmount),
                        openInvoices.get(1).getValue(), clientId, transactionDetail, errorLog, elogger);
            } else {
                // neste ponto sabemos sempre que existe pelo menos uma factura em aberto e que o remaining amout nunca é zero
                // portanto se não entrou no if de cima quer dizer que só existe uma factura aberta
                // registar adiantamento
                registerAdvancement(remainingAmount, amountToRegister.subtract(remainingAmount), openInvoices.get(0),
                        transactionDetail, errorLog, elogger);
            }
        } else {
            registerPayment(transactionDetail, amountToRegister, openInvoices.get(0), errorLog,
                    elogger);
        }
    }

    private void registerPayment(AccountingTransactionDetail transactionDetail, Money payedAmount, SapRequest sapInvoiceRequest,
            ErrorLogConsumer errorLog, EventLogger elogger) throws Exception {

        checkValidDocumentNumber(sapInvoiceRequest.getDocumentNumber(), event);

        JsonObject data = toJsonPayment(transactionDetail, payedAmount, sapInvoiceRequest, false);
        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest = new SapRequest(event, sapInvoiceRequest.getClientId(), payedAmount, documentNumber,
                SapRequestType.PAYMENT, Money.ZERO, data);
        sapRequest.setPayment(transactionDetail.getTransaction());
    }

    private void registerAdvancement(Money amount, Money advancement, SapRequest sapInvoiceRequest,
            AccountingTransactionDetail transactionDetail, ErrorLogConsumer errorLog, EventLogger elogger) throws Exception {
        checkValidDocumentNumber(sapInvoiceRequest.getDocumentNumber(), event);

        JsonObject data = toJsonAdvancement(amount, advancement, sapInvoiceRequest, transactionDetail);
        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest = new SapRequest(event, sapInvoiceRequest.getClientId(), amount, documentNumber,
                SapRequestType.ADVANCEMENT, advancement, data);
        sapRequest.setPayment(transactionDetail.getTransaction());
    }

    public boolean processPendingRequests(Event event, ErrorLogConsumer errorLog, EventLogger elogger) {
        Set<SapRequest> requests = new TreeSet<>(SapRequest.COMPARATOR_BY_ORDER);
        requests.addAll(event.getSapRequestSet());
        for (SapRequest sr : requests) {
            if (!sr.getIntegrated()) {
                JsonParser jsonParser = new JsonParser();
                JsonObject data = (JsonObject) jsonParser.parse(sr.getRequest());

                JsonObject result = sendDataToSap(sr, data);

                boolean isIntegrated = checkAndRegisterIntegration(event, errorLog, elogger, data, sr.getDocumentNumber(), sr,
                        result, sr.getRequestType().toString(), sr.getRequestType().isToGetDocument());
                if (!isIntegrated) {
                    return isIntegrated;
                }

                final SapRequest originalRequest = sr.getOriginalRequest();
                if (originalRequest != null) {
                    originalRequest.setIgnore(true);
                }
            }
        }
        return true;
    }

    private boolean checkAndRegisterIntegration(Event event, ErrorLogConsumer errorLog, EventLogger elogger, JsonObject data,
            String documentNumber, SapRequest sapRequest, JsonObject result, String action, boolean getDocument) {
        if (result.get("exception") == null) {
            boolean docIsIntregrated = checkDocumentsStatus(result, sapRequest, event, errorLog, elogger, action);
            boolean clientStatus = checkClientStatus(result, event, errorLog, elogger, action, data, sapRequest);
            if (docIsIntregrated) {
                String sapDocumentNumber = getSapDocumentNumber(result, documentNumber);

                if (getDocument) {
                    JsonObject docResult =
                            SapFinantialClient.getDocument(sapDocumentNumber, data.get("taxRegistrationNumber").getAsString());
                    if (docResult.get("status").getAsString().equalsIgnoreCase("S")) {
                        sapRequest.setSapDocumentFile(new SapDocumentFile(sanitize(sapDocumentNumber) + ".pdf",
                                Base64.getDecoder().decode(docResult.get("documentBase64").getAsString())));
                    }
                }

                sapRequest.setSapDocumentNumber(sapDocumentNumber);
                sapRequest.setIntegrated(true);
                if (clientStatus) {
                    sapRequest.setIntegrationMessage(EMPTY_JSON);
                } else {
                    //there are still error messages regarding the client
                    sapRequest.removeIntegrationMessage("Documento");
                }
                return true;
            } else {
                return false;
            }
        } else {
            logError(event, errorLog, elogger, result.get("exception").getAsString(), documentNumber, action, sapRequest);
            return false;
        }
    }

    private DateTime getDocumentDate(DateTime documentDate, boolean isNewDate) {
        if (isNewDate) {
            return new DateTime();
        }
        if (documentDate.getYear() < currentDate.getYear()) {
            return documentDatePreviousYear.apply(currentDate);
        }
        return documentDate;
    }

    private boolean isToProcessDebt(Event event, boolean isGratuity) {
        return (isGratuity || (event instanceof ExternalScholarshipPhdGratuityContribuitionEvent))
                && event.getWhenOccured().isAfter(EventWrapper.LIMIT);
    }

    /**
     * Sends the data to SAP
     * 
     * @param sapRequest - the domain representation of the request
     * @param data - the necessary data to invoke the service for the specified operation
     * @return The result of the SAP service invocation, with the status of the documents and clients and also the xml request
     *         sent. In case of an unexpected exception returns the exception message
     */
    private JsonObject sendDataToSap(SapRequest sapRequest, JsonObject data) {
        JsonObject result = null;
        sapRequest.setWhenSent(new DateTime());
        sapRequest.setSent(true);
        try {
            result = SapFinantialClient.comunicate(data);
        } catch (Exception e) {
            e.printStackTrace();
            result = new JsonObject();
            result.addProperty("exception", responseFromException(e));
            return result;
        }
        return result;
    }

    private String responseFromException(final Throwable t) {
        final Throwable cause = t.getCause();
        final String message = t.getMessage();
        return cause == null ? message : message + '\n' + responseFromException(cause);
    }

    private String getSapDocumentNumber(JsonObject result, String docNumber) {
        JsonArray jsonArray = result.getAsJsonArray("documents");
        for (int iter = 0; iter < jsonArray.size(); iter++) {
            JsonObject json = jsonArray.get(iter).getAsJsonObject();
            if (json.get("documentNumber").getAsString().equals(docNumber) && "S".equals(json.get("status").getAsString())) {
                return json.get("sapDocumentNumber").getAsString();
            }
        }
        return null;
    }

    private JsonObject toJsonPayment(AccountingTransactionDetail transactionDetail, Money amount, SapRequest sapInvoiceRequest,
            boolean isInterest) throws Exception {
        JsonObject data =
                toJson(event, sapInvoiceRequest.getClientJson(), transactionDetail.getWhenRegistered(), false, false, isInterest);
        JsonObject paymentDocument = toJsonPaymentDocument(amount, "NP", sapInvoiceRequest.getDocumentNumber(), transactionDetail.getWhenRegistered(),
                getPaymentMechanism(transactionDetail), getPaymentMethodReference(transactionDetail),
                SAFTPTSettlementType.NL.toString(), true);

        data.add("paymentDocument", paymentDocument);
        return data;
    }

    private JsonObject toJsonPaymentFromAdvancement(Event event, SapRequest sapInvoiceRequest, Money amount)
            throws Exception {
        JsonObject data = toJson(event, sapInvoiceRequest.getClientJson(), new DateTime(), false, false, false);
        JsonObject paymentDocument = toJsonPaymentDocument(amount, "NP", sapInvoiceRequest.getDocumentNumber(), new DateTime(), "OU", "",
                SAFTPTSettlementType.NN.toString(), true);
        paymentDocument.addProperty("excessPayment", amount.negate().toPlainString());//the payment amount must be zero

        data.add("paymentDocument", paymentDocument);
        return data;
    }

    private JsonObject toJsonAdvancement(Money amount, Money excess, SapRequest sapInvoiceRequest,
            AccountingTransactionDetail transactionDetail) throws Exception {
        JsonObject data =
                toJson(event, sapInvoiceRequest.getClientJson(), transactionDetail.getWhenRegistered(), false, false, false);
        JsonObject paymentDocument = toJsonPaymentDocument(amount, "NP", sapInvoiceRequest.getDocumentNumber(), transactionDetail.getWhenRegistered(),
                getPaymentMechanism(transactionDetail), getPaymentMethodReference(transactionDetail),
                SAFTPTSettlementType.NL.toString(), true);
        paymentDocument.addProperty("excessPayment", excess.toPlainString());
        paymentDocument.addProperty("isAdvancedPayment", true);

        JsonObject workingDocument = toJsonWorkDocument(getDocumentDate(transactionDetail.getWhenRegistered(), false),
                new DateTime(), excess, "NA", false, transactionDetail.getWhenRegistered());
        workingDocument.addProperty("isAdvancedPayment", true);
        workingDocument.addProperty("paymentDocumentNumber", paymentDocument.get("paymentDocumentNumber").getAsString());

        paymentDocument.addProperty("originatingOnDocumentNumber", workingDocument.get("workingDocumentNumber").getAsString());

        data.add("workingDocument", workingDocument);
        data.add("paymentDocument", paymentDocument);
        return data;
    }

    private JsonObject toJsonCredit(Event event, DateTime documentDate, Money creditAmount, SapRequest sapInvoiceRequest,
            boolean isDebtRegistration, boolean isNewDate) throws Exception {
        JsonObject json = toJson(event, sapInvoiceRequest.getClientJson(), new DateTime(), isDebtRegistration, isNewDate, false);
        JsonObject workDocument = toJsonWorkDocument(documentDate, new DateTime(), creditAmount, "NA", false, new DateTime());
        workDocument.addProperty("workOriginDocNumber", sapInvoiceRequest.getDocumentNumber());
        json.add("workingDocument", workDocument);

        String workingDocumentNumber = workDocument.get("workingDocumentNumber").getAsString();
        JsonObject paymentDocument = toJsonPaymentDocument(creditAmount, "NP", workingDocumentNumber, documentDate, "OU", "",
                SAFTPTSettlementType.NN.toString(), false);
        paymentDocument.addProperty("isCreditNote", true);
        paymentDocument.addProperty("paymentOriginDocNumber", sapInvoiceRequest.getDocumentNumber());
        paymentDocument.addProperty("excessPayment", creditAmount.negate().toPlainString());//the payment amount must be zero
        json.add("paymentDocument", paymentDocument);

        return json;
    }

    private JsonObject toJsonReimbursement(Event event, Money amount, SapRequest sapInvoiceRequest,
            boolean isDebtRegistration, boolean isNewDate) throws Exception {
        JsonObject json = toJson(event, sapInvoiceRequest.getClientJson(), new DateTime(), isDebtRegistration, isNewDate, false);
        JsonObject workDocument = toJsonWorkDocument(new DateTime(), new DateTime(), amount, "NA", false, new DateTime());
        workDocument.addProperty("workOriginDocNumber", sapInvoiceRequest.getDocumentNumber());
        json.add("workingDocument", workDocument);

        String workingDocumentNumber = workDocument.get("workingDocumentNumber").getAsString();
        JsonObject paymentDocument = toJsonPaymentDocument(amount, "NR", workingDocumentNumber, new DateTime(), "OU", "",
                SAFTPTSettlementType.NR.toString(), false);
        paymentDocument.addProperty("isReimbursment", true);
        paymentDocument.addProperty("reimbursementStatus", "PENDING");
        paymentDocument.addProperty("excessPayment", amount.negate().toPlainString());//the payment amount must be zero
        json.add("paymentDocument", paymentDocument);

        return json;
    }

    private JsonObject toJsonPaymentDocument(Money amount, String documentType, String workingDocumentNumber,
            DateTime paymentDate, String paymentMechanism, String paymentMethodReference, String settlementType, boolean isDebit)
            throws Exception {
        JsonObject paymentDocument = new JsonObject();
        paymentDocument.addProperty("paymentDocumentNumber", documentType + getDocumentNumber());
        paymentDocument.addProperty("paymentDate", paymentDate.toString(DT_FORMAT));
        paymentDocument.addProperty("paymentType", SAFTPTPaymentType.RG.toString());
        paymentDocument.addProperty("paymentStatus", "N");
        paymentDocument.addProperty("sourcePayment", SAFTPTSourcePayment.P.toString());
        paymentDocument.addProperty("paymentAmount", amount.getAmountAsString());
        paymentDocument.addProperty("paymentMechanism", paymentMechanism);
        paymentDocument.addProperty("paymentMethodReference", paymentMethodReference);
        paymentDocument.addProperty("settlementType", settlementType);

        paymentDocument.addProperty("isToDebit", isDebit);
        paymentDocument.addProperty("workingDocumentNumber", workingDocumentNumber);

        paymentDocument.addProperty("paymentGrossTotal", BigDecimal.ZERO);
        paymentDocument.addProperty("paymentNetTotal", BigDecimal.ZERO);
        paymentDocument.addProperty("paymentTaxPayable", BigDecimal.ZERO);
        return paymentDocument;
    }

    private JsonObject toJsonInvoice(Event event, Money debtFenix, DateTime documentDate, DateTime entryDate, String clientId,
            boolean isDebtRegistration, boolean isInterest) throws Exception {
        final JsonObject clientData = toJsonClient(event.getParty(), clientId);
        JsonObject json = toJson(event, clientData, documentDate, isDebtRegistration, true, isInterest);
        JsonObject workDocument =
                toJsonWorkDocument(documentDate, entryDate, debtFenix, "ND", true, new DateTime(Utils.getDueDate(event)));

        json.add("workingDocument", workDocument);
        return json;
    }

    private JsonObject toJsonInvoice(final SapRequest sapRequest, final ExternalClient externalClient, final Money debtFenix,
            final DateTime documentDate, final DateTime entryDate, final boolean isDebtRegistration,
            final boolean isInterest) throws Exception {
        final JsonObject clientData = toJsonClient(externalClient);
        JsonObject json = toJson(event, clientData, documentDate, isDebtRegistration, true, isInterest);
        JsonObject workDocument =
                toJsonWorkDocument(documentDate, entryDate, debtFenix, "ND", true, new DateTime(Utils.getDueDate(event)));

        json.add("workingDocument", workDocument);
        return json;
    }

    private JsonObject toJsonDebt(Event event, Money debtFenix, String clientId, DateTime documentDate, DateTime entryDate,
            boolean isDebtRegistration, String docType, boolean isToDebit, boolean isNewDate, String originalMetadata) {
        final JsonObject clientData = toJsonClient(event.getParty(), clientId);
        JsonObject json = toJson(event, clientData, documentDate, isDebtRegistration, isNewDate, false);
        JsonObject workDocument =
                toJsonWorkDocument(documentDate, entryDate, debtFenix, docType, isToDebit, new DateTime(Utils.getDueDate(event)));

        if (originalMetadata == null) {
            LocalDate startDate = isNewDate ? currentDate : documentDate.toLocalDate();
            ExecutionYear executionYear = Utils.executionYearOf(event);
            if (startDate.isBefore(executionYear.getBeginLocalDate())) {
                startDate = executionYear.getBeginLocalDate();
            }
            LocalDate endDate = executionYear.getEndDateYearMonthDay().toLocalDate();

            //If it is a Phd the dates are not regulated by the execution year
            if (event instanceof PhdGratuityEvent) {
                PhdGratuityEvent phdEvent = (PhdGratuityEvent) event;
                startDate = phdEvent.getPhdGratuityDate().getYear() == phdEvent.getYear() ? phdEvent.getPhdGratuityDate()
                        .toLocalDate() : phdEvent.getWhenOccured().toLocalDate();
                endDate = startDate.plusYears(1);
            }

            String metadata = String.format("{\"ANO_LECTIVO\":\"%s\", \"START_DATE\":\"%s\", \"END_DATE\":\"%s\"}",
                    executionYear.getName(), startDate.toString("yyyy-MM-dd"), endDate.toString("yyyy-MM-dd"));
            workDocument.addProperty("debtMetadata", metadata);
        } else {
            workDocument.addProperty("debtMetadata", originalMetadata);
        }

        json.add("workingDocument", workDocument);
        return json;
    }

    private JsonObject toJsonDebtCredit(Event event, Money debtFenix, String clientId, DateTime documentDate, DateTime entryDate,
            boolean isDebtRegistration, String docType, boolean isToDebit, boolean isNewDate, SapRequest debtRequest)
            throws Exception {
        JsonObject request = new JsonParser().parse(debtRequest.getRequest()).getAsJsonObject();
        String originalMetadata = request.get("workingDocument").getAsJsonObject().get("debtMetadata").getAsString();
        JsonObject json = toJsonDebt(event, debtFenix, clientId, documentDate, entryDate, isDebtRegistration, docType, isToDebit,
                isNewDate, originalMetadata);
        JsonObject workingDocument = json.get("workingDocument").getAsJsonObject();
        workingDocument.addProperty("workOriginDocNumber", debtRequest.getDocumentNumber());
        return json;
    }

    private JsonObject toJsonWorkDocument(DateTime documentDate, DateTime entryDate, Money amount, String documentType,
            boolean isToDebit, DateTime dueDate) {
        JsonObject workDocument = new JsonObject();
        workDocument.addProperty("documentDate", documentDate.toString(DT_FORMAT));
        workDocument.addProperty("entryDate", entryDate.toString(DT_FORMAT));
        workDocument.addProperty("dueDate", dueDate.toString(DT_FORMAT));
        workDocument.addProperty("workingDocumentNumber", documentType + getDocumentNumber());
        workDocument.addProperty("sourceBilling", SAFTPTSourceBilling.P.toString());
        workDocument.addProperty("workingAmount", amount.getAmountAsString());
        workDocument.addProperty("taxPayable", BigDecimal.ZERO);
        workDocument.addProperty("workType", "DC");
        workDocument.addProperty("workStatus", "N");

        workDocument.addProperty("isToDebit", isToDebit);
        workDocument.addProperty("isToCredit", !isToDebit);

//        workDocument.addProperty("compromiseMetadata", "");

        workDocument.addProperty("taxExemptionReason", "M99");
        workDocument.addProperty("unitOfMeasure", "UNID");

        return workDocument;
    }

    public JsonObject toJson(final Event event, final JsonObject clientData, DateTime documentDate, boolean isDebtRegistration,
            boolean isNewDate, boolean isInterest) {
        final JsonObject json = toJsonCommon(documentDate, isNewDate);

        final String description = event.getDescription().toString();
        final SimpleImmutableEntry<String, String> product = mapToProduct(event, description, isDebtRegistration, isInterest);
        json.addProperty("productCode", product.getKey());
        json.addProperty("productDescription", detailedDescription(product.getValue(), event));

        json.add("clientData", clientData);

        return json;
    }

    private String detailedDescription(final String description, final Event event) {
        final Party party = event.getParty();
        return party == null ? description : description + " : " + party.getName();
    }

    private JsonObject toJsonCommon(DateTime documentDate, boolean isNewDate) {
        final JsonObject json = new JsonObject();
        json.addProperty("finantialInstitution", "IST");
        json.addProperty("taxType", "IVA");
        json.addProperty("taxCode", "ISE");
        json.addProperty("taxCountry", "PT");
        json.addProperty("taxPercentage", "0");
        json.addProperty("auditFileVersion", "1.0.3");
        json.addProperty("processId", "006");
        json.addProperty("businessName", "Técnico Lisboa");
        json.addProperty("companyName", "Instituto Superior Técnico");
        json.addProperty("companyId", "256241256");
        json.addProperty("currencyCode", "EUR");
        json.addProperty("country", "PT");
        json.addProperty("addressDetail", "Avenida Rovisco Pais, 1");
        json.addProperty("city", "Lisboa");
        json.addProperty("postalCode", "1049-001");
        json.addProperty("region", "Lisboa");
        json.addProperty("street", "Avenida Rovisco Pais, 1");
        json.addProperty("fromDate", isNewDate ? new DateTime().toString(DT_FORMAT) : documentDate.toString(DT_FORMAT));
        json.addProperty("toDate", new DateTime().toString(DT_FORMAT)); //tem impacto no ano fiscal!!!
        json.addProperty("productCompanyTaxId", "999999999");
        json.addProperty("productId", "FenixEdu/FenixEdu");
        json.addProperty("productVersion", "5.0.0.0");
        json.addProperty("softwareCertificateNumber", 0);
        json.addProperty("taxAccountingBasis", "P");
        json.addProperty("taxEntity", "Global");
        json.addProperty("taxRegistrationNumber", "501507930");
        return json;
    }

    private JsonObject toJsonClient(final Party party, final String clientId) {
        final JsonObject clientData = new JsonObject();
        clientData.addProperty("accountId", "STUDENT");
        clientData.addProperty("companyName", party.getName());
        clientData.addProperty("clientId", clientId);
        //country must be the same as the fiscal country
        final String countryCode = clientId.substring(0, 2);
        clientData.addProperty("country", countryCode);

        PhysicalAddress physicalAddress = Utils.toAddress(party, countryCode);
        clientData.addProperty("street",
                physicalAddress != null && !Strings.isNullOrEmpty(physicalAddress.getAddress().trim()) ? Utils
                        .limitFormat(MAX_SIZE_ADDRESS, physicalAddress.getAddress()) : MORADA_DESCONHECIDO);

        String city = Utils.limitFormat(MAX_SIZE_CITY, party.getDistrictSubdivisionOfResidence()).trim();
        clientData.addProperty("city", !Strings.isNullOrEmpty(city) ? city : MORADA_DESCONHECIDO);

        String region = Utils.limitFormat(MAX_SIZE_REGION, party.getDistrictOfResidence()).trim();
        clientData.addProperty("region", !Strings.isNullOrEmpty(region) ? region : MORADA_DESCONHECIDO);

        String postalCode =
                physicalAddress == null ? null : Utils.limitFormat(MAX_SIZE_POSTAL_CODE, physicalAddress.getAreaCode()).trim();
        //sometimes the address is correct but the vatNumber doesn't exists and a random one was generated from the birth country
        //in that case we must send a valid postal code for that country, even if it is not the address country
        if (physicalAddress.getCountryOfResidence() != null
                && !physicalAddress.getCountryOfResidence().getCode().equals(countryCode)) {
            postalCode = PostalCodeValidator.examplePostCodeFor(countryCode);
        }
        if (!PostalCodeValidator.isValidAreaCode(countryCode, postalCode)) {
            postalCode = PostalCodeValidator.examplePostCodeFor(countryCode);
        }
        clientData.addProperty("postalCode",
                !Strings.isNullOrEmpty(postalCode) ? postalCode : PostalCodeValidator.examplePostCodeFor(countryCode));

        clientData.addProperty("vatNumber", Utils.limitFormat(MAX_SIZE_VAT_NUMBER, clientId));
        clientData.addProperty("fiscalCountry", countryCode);
        clientData.addProperty("nationality", party.getCountry().getCode());
        clientData.addProperty("billingIndicator", 0);

        return clientData;
    }

    private JsonObject toJsonClient(final ExternalClient externalClient) {
        final JsonObject clientData = new JsonObject();
        clientData.addProperty("accountId", externalClient.getAccountId());
        clientData.addProperty("companyName", externalClient.getCompanyName());
        clientData.addProperty("clientId", externalClient.getClientId());
        clientData.addProperty("country", externalClient.getCountry());
        clientData.addProperty("street", externalClient.getStreet());
        clientData.addProperty("city", externalClient.getCity());
        clientData.addProperty("region", externalClient.getRegion());
        clientData.addProperty("postalCode", externalClient.getPostalCode());
        clientData.addProperty("vatNumber", externalClient.getVatNumber());
        clientData.addProperty("fiscalCountry", externalClient.getFiscalCountry());
        clientData.addProperty("nationality", externalClient.getNationality());
        clientData.addProperty("billingIndicator", externalClient.getBillingIndicator());
        return clientData;
    }

    private String getDocumentNumber(JsonObject data, boolean paymentDocument) {
        if (paymentDocument) {
            return data.get("paymentDocument").getAsJsonObject().get("paymentDocumentNumber").getAsString();
        } else {
            return data.get("workingDocument").getAsJsonObject().get("workingDocumentNumber").getAsString();
        }
    }

    private void checkValidDocumentNumber(String documentNumber, Event event) throws Exception {
        if ('0' == documentNumber.charAt(2)) {
            throw new Exception("Houve uma tentativa de efectuar uma operação sobre o documento: " + documentNumber
                    + " - evento: " + event.getExternalId());
        }
    }

    private Long getDocumentNumber() {
        return SapRoot.getInstance().getAndSetNextDocumentNumber();
    }

    private String getPaymentMethodReference(AccountingTransactionDetail transactionDetail) {
        if (transactionDetail.getPaymentMode().equals(PaymentMode.ATM)) {
            return ((SibsTransactionDetail) transactionDetail).getSibsCode();
        }
        return "";
    }

    private String getPaymentMechanism(AccountingTransactionDetail transactionDetail) {
//            "NU" - numerário
//            "SI" - sibs
//            "OU" - outros        
        switch (transactionDetail.getPaymentMode()) {
        case CASH:
            return "NU";
        case ATM:
            return "SI";
        default:
            throw new Error();
        }
    }

    /**
     * Returns the open invoices and the remaining value of the first open invoice
     * The list is ordered, the first open invoice is the first of the list
     * 
     * @return
     */
    private SimpleImmutableEntry<List<SapRequest>, Money> getOpenInvoicesAndRemainingValue() {
        List<SapRequest> invoiceEntries =
                getInvoiceEntries().sorted(SapRequest.DOCUMENT_NUMBER_COMPARATOR).collect(Collectors.toList());
        Money invoiceAmount = Money.ZERO;
        Money firstRemainingValue = Money.ZERO;
        Money totalAmount = getPayedAmount().add(getCreditAmount());
        List<SapRequest> openInvoiceEntries = new ArrayList<SapRequest>();
        for (SapRequest invoiceEntry : invoiceEntries) {
            invoiceAmount = invoiceAmount.add(invoiceEntry.getValue());
            if (invoiceAmount.greaterThan(totalAmount)) {
                if (firstRemainingValue.isZero()) {
                    firstRemainingValue = invoiceAmount.subtract(totalAmount);
                }
                openInvoiceEntries.add(0, invoiceEntry);
            }
        }
        return new SimpleImmutableEntry<List<SapRequest>, Money>(openInvoiceEntries, firstRemainingValue);
    }

    private Stream<SapRequest> getInvoiceEntries() {
        return getFilteredSapRequestStream().filter(sr -> sr.getRequestType() == SapRequestType.INVOICE);
    }

    private SapRequest getLastInvoiceNumber() {
        return getInvoiceEntries().max(SapRequest.DOCUMENT_NUMBER_COMPARATOR).orElse(null);
    }

    /**
     * Returns the open debts and the remaining value of the first open debt
     * The list is ordered, the first open debt is the first of the list
     * 
     * @return
     */
    private SimpleImmutableEntry<List<SapRequest>, Money> getOpenDebtsAndRemainingValue() {
        List<SapRequest> debtEntries =
                getDebtEntries().sorted(SapRequest.DOCUMENT_NUMBER_COMPARATOR).collect(Collectors.toList());
        Money debtAmount = Money.ZERO;
        Money firstRemainingValue = Money.ZERO;
        Money totalAmount = getDebtCreditAmount();
        List<SapRequest> openDebtEntries = new ArrayList<SapRequest>();
        for (SapRequest debtEntry : debtEntries) {
            debtAmount = debtAmount.add(debtEntry.getValue());
            if (debtAmount.greaterThan(totalAmount)) {
                if (firstRemainingValue.isZero()) {
                    firstRemainingValue = debtAmount.subtract(totalAmount);
                }
                openDebtEntries.add(0, debtEntry);
            }
        }
        return new SimpleImmutableEntry<List<SapRequest>, Money>(openDebtEntries, firstRemainingValue);
    }

    private Stream<SapRequest> getDebtEntries() {
        return getFilteredSapRequestStream().filter(sr -> sr.getRequestType() == SapRequestType.DEBT)
                .filter(sr -> sr.getValue().isPositive());
    }

    private SapRequest getLastDebt() {
        Optional<SapRequest> findFirst = getDebtEntries().sorted(SapRequest.DOCUMENT_NUMBER_COMPARATOR.reversed()).findFirst();
        return findFirst.isPresent() ? findFirst.get() : null;
    }

    private boolean checkClientStatus(JsonObject result, Event event, ErrorLogConsumer errorLog, EventLogger elogger,
            String action, JsonObject sentData, SapRequest sr) {
        JsonArray jsonArray = result.getAsJsonArray("customers");
        for (int iter = 0; iter < jsonArray.size(); iter++) {
            JsonObject json = jsonArray.get(iter).getAsJsonObject();
            if (!"S".equals(json.get("status").getAsString())) {
                logError(event, json.get("customerId").getAsString(), errorLog, elogger, json.get("returnMessage").getAsString(),
                        action, sentData, sr);
                return false;
            }
        }
        return true;
    }

    private boolean checkDocumentsStatus(JsonObject result, SapRequest sapRequest, Event event, ErrorLogConsumer errorLog,
            EventLogger elogger, String action) {
        JsonArray jsonArray = result.getAsJsonArray("documents");
        boolean checkStatus = true;
        for (int iter = 0; iter < jsonArray.size(); iter++) {
            JsonObject json = jsonArray.get(iter).getAsJsonObject();
            if (!"S".equals(json.get("status").getAsString())) {
                checkStatus = false;
                String errorMessage = json.get("errorDescription").getAsString();
                logError(event, errorLog, elogger, errorMessage, json.get("documentNumber").getAsString(), action, sapRequest);
            }
        }
        return checkStatus;
    }

    private Money addAll(final SapRequestType sapRequestType) {
        return getFilteredSapRequestStream().filter(sr -> sr.getRequestType().equals(sapRequestType))
                .map(SapRequest::getValue).reduce(Money.ZERO, Money::add);
    }

    public Money getDebtAmount() {
        return addAll(SapRequestType.DEBT);
    }

    public Money getDebtCreditAmount() {
        return addAll(SapRequestType.DEBT_CREDIT);
    }

    public Money getInvoiceAmount() {
        return addAll(SapRequestType.INVOICE);
    }

    public Money getPayedAmount() {
        return addAll(SapRequestType.PAYMENT).add(addAll(SapRequestType.ADVANCEMENT));
    }

    public Money getCreditAmount() {
        return addAll(SapRequestType.CREDIT);
    }

    public Money getAdvancementAmount() {
        return getFilteredSapRequestStream().filter(sr -> sr.getRequestType().equals(SapRequestType.ADVANCEMENT))
                .map(SapRequest::getAdvancement).reduce(Money.ZERO, Money::add);
    }

    public Money getReimbursementsAmount() {
        return addAll(SapRequestType.REIMBURSEMENT);
    }

    public boolean hasPayment(final String transactionId) {
        return getPaymentsFor(transactionId).findAny().isPresent();
    }

    public boolean hasPayment(final AccountingTransaction transaction, SapRequest sapRequest) {
        return getPaymentsFor(transaction.getTransactionDetail().getExternalId()).filter(sr -> transaction == sr.getPayment())
                .anyMatch(sr -> sr != sapRequest);
    }

    private Stream<SapRequest> getPaymentsFor(final String transactionDetailId) {
        return getFilteredSapRequestStream()
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.ADVANCEMENT)
                .filter(sr -> sr.getPayment() != null).filter(sr -> transactionDetailId.equals(sr.getPayment().getExternalId()));
    }

    public boolean hasInterestPayment(final AccountingTransaction transaction) {
        return getFilteredSapRequestStream().filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                .filter(sr -> transaction == sr.getPayment()).findAny().isPresent();
    }

    public SapRequest getInterestInvoiceRequest(final AccountingTransaction transaction) {
        return getFilteredSapRequestStream().filter(sr -> sr.getRequestType() == SapRequestType.INVOICE_INTEREST)
                .filter(sr -> transaction == sr.getPayment()).findAny().orElse(null);
    }

    public boolean hasCredit(String creditId) {
        return getFilteredSapRequestStream().filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
                .anyMatch(sr -> creditId.equals(sr.getCreditId()));
    }

    public static SimpleImmutableEntry<String, String> mapToProduct(Event event, String eventDescription,
            boolean isDebtRegistration, boolean isInterest) {
        if (isInterest) {
            return new SimpleImmutableEntry<String, String>("0036", "MULTAS");
        }
        if (event.isGratuity() && !(event instanceof PhdGratuityEvent)) {
            final GratuityEvent gratuityEvent = (GratuityEvent) event;
            final StudentCurricularPlan scp = gratuityEvent.getStudentCurricularPlan();
            final Degree degree = scp.getDegree();
            if (scp.getRegistration().getRegistrationProtocol().isAlien()) {
                if (isDebtRegistration) {
                    return new SimpleImmutableEntry<String, String>("E0075", "ESP PROPINAS INTERNACIONAL");
                } else {
                    return new SimpleImmutableEntry<String, String>("0075", "PROPINAS INTERNACIONAL");
                }
            }
            if (degree.isFirstCycle() && degree.isSecondCycle()) {
                if (isDebtRegistration) {
                    return new SimpleImmutableEntry<String, String>("E0030", "ESP PROPINAS MESTRADO INTEGRADO");
                } else {
                    return new SimpleImmutableEntry<String, String>("0030", "PROPINAS MESTRADO INTEGRADO");
                }
            }
            if (degree.isFirstCycle()) {
                if (isDebtRegistration) {
                    return new SimpleImmutableEntry<String, String>("E0027", "ESP PROPINAS 1 CICLO");
                } else {
                    return new SimpleImmutableEntry<String, String>("0027", "PROPINAS 1 CICLO");
                }
            }
            if (degree.isSecondCycle()) {
                if (isDebtRegistration) {
                    return new SimpleImmutableEntry<String, String>("E0028", "ESP PROPINAS 2 CICLO");
                } else {
                    return new SimpleImmutableEntry<String, String>("0028", "PROPINAS 2 CICLO");
                }
            }
            if (degree.isThirdCycle()) {
                if (isDebtRegistration) {
                    return new SimpleImmutableEntry<String, String>("E0029", "ESP PROPINAS 3 CICLO");
                } else {
                    return new SimpleImmutableEntry<String, String>("0029", "PROPINAS 3 CICLO");
                }
            }
            if (isDebtRegistration) {
                return new SimpleImmutableEntry<String, String>("E0076", "ESP PROPINAS OUTROS");
            } else {
                return new SimpleImmutableEntry<String, String>("0076", "PROPINAS OUTROS");
            }
        }
        if (event instanceof PhdGratuityEvent) {
            if (isDebtRegistration) {
                return new SimpleImmutableEntry<String, String>("E0029", "ESP PROPINAS 3 CICLO");
            } else {
                return new SimpleImmutableEntry<String, String>("0029", "PROPINAS 3 CICLO");
            }
        }
        if (event instanceof ExternalScholarshipPhdGratuityContribuitionEvent) {
            return new SimpleImmutableEntry<String, String>("0029", "PROPINAS 3 CICLO");
        }
        if (event.isResidenceEvent()) {
            return null;
        }
        if (event.isFctScholarshipPhdGratuityContribuitionEvent()) {
            return null;
        }
        if (event.isAcademicServiceRequestEvent()) {
            if (eventDescription.indexOf(" Reingresso") >= 0) {
                return new SimpleImmutableEntry<String, String>("0035", "OUTRAS TAXAS");
            }
            return new SimpleImmutableEntry<String, String>("0037", "EMOLUMENTOS");
        }
        if (event.isDfaRegistrationEvent()) {
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event.isIndividualCandidacyEvent()) {
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event.isEnrolmentOutOfPeriod()) {
            return new SimpleImmutableEntry<String, String>("0035", "OUTRAS TAXAS");
        }
        if (event instanceof AdministrativeOfficeFeeAndInsuranceEvent) {
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event instanceof AdministrativeOfficeFeeEvent) {
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event instanceof InsuranceEvent) {
            return new SimpleImmutableEntry<String, String>("0034", "SEGURO ESCOLAR");
        }
        if (event.isSpecializationDegreeRegistrationEvent()) {
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event instanceof ImprovementOfApprovedEnrolmentEvent || (event instanceof EnrolmentEvaluationEvent
                && event.getEventType() == EventType.IMPROVEMENT_OF_APPROVED_ENROLMENT)) {
            return new SimpleImmutableEntry<String, String>("0033", "TAXAS DE MELHORIAS DE NOTAS");
        }
        if (event instanceof DFACandidacyEvent) {
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event instanceof SpecialSeasonEnrolmentEvent
                || (event instanceof EnrolmentEvaluationEvent && event.getEventType() == EventType.SPECIAL_SEASON_ENROLMENT)) {
            return new SimpleImmutableEntry<String, String>("0032", "TAXAS  DE EXAMES");
        }
        if (event.isPhdEvent()) {
            if (eventDescription.indexOf("Taxa de Inscri") >= 0) {
                return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
            }
            if (eventDescription.indexOf("Requerimento de provas") >= 0) {
                return new SimpleImmutableEntry<String, String>("0032", "TAXAS  DE EXAMES");
            }
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        throw new Error("not.supported: " + event.getExternalId());
    }

    private String sanitize(final String s) {
        return s.replace('/', '_').replace('\\', '_');
    }

    private void logError(Event event, String clientId, ErrorLogConsumer errorLog, EventLogger elogger, String returnMessage,
            String action, JsonObject sentData, SapRequest sr) {
        final Party party = event.getParty();
        errorLog.accept(event.getExternalId(), clientId, party.getName(), "", "", returnMessage, "", "",
                sentData.get("clientData").getAsJsonObject().get("fiscalCountry").getAsString(), clientId,
                sentData.get("clientData").getAsJsonObject().get("street").getAsString(), "",
                sentData.get("clientData").getAsJsonObject().get("postalCode").getAsString(), "", "", "", action);
        elogger.log("Pessoa %s (%s): evento: %s %s %s %s %n", party.getExternalId(), Utils.getUserIdentifier(party),
                event.getExternalId(), clientId, returnMessage, action);

        //Write to SapRequest in json format
        JsonObject errorMessage = new JsonObject();
        errorMessage.addProperty("ID Evento", event.getExternalId());
        errorMessage.addProperty("Utilizador", Utils.getUserIdentifier(party));
        errorMessage.addProperty("Nº Contribuinte", clientId);
        errorMessage.addProperty("Nome", party.getName());
        errorMessage.addProperty("Mensagem", returnMessage);
        errorMessage.addProperty("País Fiscal", sentData.get("clientData").getAsJsonObject().get("fiscalCountry").getAsString());
        errorMessage.addProperty("Morada", sentData.get("clientData").getAsJsonObject().get("street").getAsString());
        errorMessage.addProperty("Código Postal", sentData.get("clientData").getAsJsonObject().get("postalCode").getAsString());
        errorMessage.addProperty("Tipo Documento", action);

        sr.addIntegrationMessage("Cliente", errorMessage);
    }

    private void logError(Event event, ErrorLogConsumer errorLog, EventLogger elogger, String errorMessage, String documentNumber,
            String action, SapRequest sr) {
        BigDecimal amount = null;
        DebtCycleType cycleType = Utils.cycleType(event);
        final Party party = event.getParty();

        errorLog.accept(event.getExternalId(), Utils.getUserIdentifier(party), party.getName(),
                amount == null ? "" : amount.toPlainString(), cycleType == null ? "" : cycleType.getDescription(), errorMessage,
                "", "", "", "", "", "", "", "", "", documentNumber, action);
        elogger.log("%s: %s %s %s %n", event.getExternalId(), errorMessage, documentNumber, action);

        //Write to SapRequest in json format
        JsonObject returnMessage = new JsonObject();
        returnMessage.addProperty("ID Evento", event.getExternalId());
        returnMessage.addProperty("Utilizador", Utils.getUserIdentifier(party));
        returnMessage.addProperty("Nome", party.getName());
        returnMessage.addProperty("Ciclo", cycleType != null ? cycleType.getDescription() : "");
        returnMessage.addProperty("Mensagem", errorMessage);
        returnMessage.addProperty("Nº Documento", documentNumber);
        returnMessage.addProperty("Tipo Documento", action);

        sr.addIntegrationMessage("Documento", returnMessage);
    }

    public boolean hasPendingDocumentCancelations() {
        return event.getSapRequestSet().stream().anyMatch(r -> r.getIgnore() && !r.getIntegrated() && r.getOriginalRequest() != null);
    }

    public boolean canCancel() {
        final boolean hasNonDebtDocument = getFilteredSapRequestStream().anyMatch(r -> !r.isDebtDocument());
        return !hasNonDebtDocument && calculateDebtValue().isZero();
    }

    public Money calculateDebtValue() {
        return getDebtAmount().subtract(getDebtCreditAmount());
    }

}