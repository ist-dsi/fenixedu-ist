package pt.ist.fenixedu.giaf.invoices;

import java.math.BigDecimal;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
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
import org.fenixedu.academic.domain.accounting.PaymentMethod;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.domain.accounting.calculator.DebtEntry;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.ExcessRefund;
import org.fenixedu.academic.domain.accounting.calculator.PartialPayment;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.domain.accounting.calculator.Refund;
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
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.generated.sources.saft.sap.SAFTPTPaymentType;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSettlementType;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSourceBilling;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSourcePayment;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

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

    private static final String MORADA_DESCONHECIDO = "Desconhecido";
    private static final String EMPTY_JSON = "{}";
    private static final int MAX_SIZE_ADDRESS = 100;
    private static final int MAX_SIZE_CITY = 50;
    private static final int MAX_SIZE_REGION = 50;
    private static final int MAX_SIZE_POSTAL_CODE = 20;
    private static final int MAX_SIZE_VAT_NUMBER = 20;
    public static final String PROCESS_ID = "006";
    public static final String IST_VAT_NUMBER = "501507930";
    private static final String SIBS_DATE_FORMAT = "yyyy-MM-dd";
    public LocalDate currentDate = new LocalDate();
    public Event event = null;

    public SapEvent(final Event event) {
        this.event = event;
    }

    public Stream<SapRequest> getFilteredSapRequestStream() {
        return event.getSapRequestSet().stream().filter(r -> !r.getIgnore());
    }

    public SapRequest registerInvoice(Money debtFenix, Event event, boolean isGratuity, boolean isNewDate) {

        if (isToProcessDebt(isGratuity, isNewDate, getDocumentDate(event.getWhenOccured(), isNewDate))) {
            registerDebt(debtFenix, event, isNewDate);
        }

        String clientId = ClientMap.uVATNumberFor(event.getParty());
        JsonObject data = toJsonInvoice(event, debtFenix, getDocumentDate(event.getWhenOccured(), isNewDate), new DateTime(),
                clientId, false, false, false);

        String documentNumber = getDocumentNumber(data, false);
        return new SapRequest(event, clientId, debtFenix, documentNumber, SapRequestType.INVOICE, Money.ZERO, data);
    }

    @Atomic
    public void registerInvoice(final Money value, final boolean gratuity, final boolean isNewDate, final ExternalClient externalClient, final String pledgeNumber) {
        final String clientId;
        final JsonObject data;
        if (externalClient == null) {
            clientId = ClientMap.uVATNumberFor(event.getParty());
            data = toJsonInvoice(event, value, getDocumentDate(event.getWhenOccured(), isNewDate), new DateTime(), clientId, false, false, false);
        } else {
            clientId = externalClient.getClientId();
            data = toJsonInvoice(externalClient, value, getDocumentDate(event.getWhenOccured(), true), new DateTime(), false, false, pledgeNumber);
        }
        final String documentNumber = getDocumentNumber(data, false);
        new SapRequest(event, clientId, value, documentNumber, SapRequestType.INVOICE, Money.ZERO, data);
    }

    @Atomic
    public void transferInvoice(final SapRequest sapRequest, final ExternalClient externalClient, final Money amountToTransfer,
                                final String pledgeNumber) {
        final SapRequestType requestType = sapRequest.getRequestType();
        if (requestType != SapRequestType.INVOICE) {
            throw new Error("label.error.document.is.not.an.invoice");
        }
        if (amountToTransfer.isZero()) {
            throw new Error("label.error.value.to.transfer.must.be.posituve");
        }
        final Money invoiceValue = sapRequest.getValue();
        final Money consumedAmount = sapRequest.consumedAmount();
        final Money availableInvoiceValue = invoiceValue.subtract(consumedAmount);
        final Money remainder = availableInvoiceValue.subtract(amountToTransfer);
        if (remainder.isNegative()) {
            throw new Error("label.error.amount.exceeds.invoice.value");
        } else {
            registerCredit(event, EventProcessor.getCreditEntry(availableInvoiceValue), availableInvoiceValue, sapRequest, false);

            final JsonObject data = toJsonInvoice(externalClient, amountToTransfer, getDocumentDate(event.getWhenOccured(), true), new DateTime(), false, false, pledgeNumber);
            final String documentNumber = getDocumentNumber(data, false);
            new SapRequest(event, externalClient.getClientId(), amountToTransfer, documentNumber, SapRequestType.INVOICE, Money.ZERO, data);
        }
        if (remainder.isPositive()) {
            final JsonObject data = toJsonInvoice(event, remainder, getDocumentDate(event.getWhenOccured(), true), new DateTime(), sapRequest.getClientId(), false, false, false);
            final String documentNumber = getDocumentNumber(data, false);
            new SapRequest(event, sapRequest.getClientId(), remainder, documentNumber, SapRequestType.INVOICE, Money.ZERO, data);
        }
    }

    @Atomic
    public void closeDocument(final SapRequest sapRequest) {
        final SapRequestType requestType = sapRequest.getRequestType();
        if (requestType != SapRequestType.INVOICE
                && requestType != SapRequestType.DEBT
                && requestType != SapRequestType.INVOICE_INTEREST) {
            throw new Error("label.document.type.cannot.be.closed");
        }
        if (sapRequest.isReferencedByOtherRequest()) {
            throw new Error("label.error.invoice.already.used");
        }

        final Money documentValue = sapRequest.getValue();
        final CreditEntry creditEntry = EventProcessor.getCreditEntry(documentValue);
        if (requestType == SapRequestType.INVOICE || requestType == SapRequestType.INVOICE_INTEREST) {
            final SapRequest creditRequest = registerCredit(event, creditEntry, documentValue, sapRequest, false);
            creditRequest.setIgnore(true);
        } else if (requestType == SapRequestType.DEBT) {
            final SapRequest debtCreditRequest = registerDebtCredit(sapRequest.getClientId(), event, documentValue, creditEntry, sapRequest, true);
            debtCreditRequest.setIgnore(true);
        }
        sapRequest.setIgnore(true);
    }

    @Atomic
    public void cancelDocument(final SapRequest sapRequest) {
        final SapRequestType requestType = sapRequest.getRequestType();
        if (requestType != SapRequestType.INVOICE
                && requestType != SapRequestType.INVOICE_INTEREST
                && requestType != SapRequestType.PAYMENT
                && requestType != SapRequestType.PAYMENT_INTEREST
                && requestType != SapRequestType.ADVANCEMENT
                && requestType != SapRequestType.CREDIT
                && requestType != SapRequestType.CLOSE_INVOICE) {
            throw new Error("label.document.type.cannot.be.canceled");
        }
        if (sapRequest.isReferencedByOtherRequest()) {
            throw new Error("label.error.invoice.already.used");
        }

        JsonObject jsonAnnulled = new JsonParser().parse(sapRequest.getRequest()).getAsJsonObject();
        if (requestType == SapRequestType.INVOICE
                || requestType == SapRequestType.INVOICE_INTEREST
                || requestType == SapRequestType.ADVANCEMENT
                || requestType == SapRequestType.CREDIT) {
            final JsonObject workDocument = jsonAnnulled.get("workingDocument").getAsJsonObject();
            workDocument.addProperty("workStatus", "A");
            workDocument.addProperty("documentDate", new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
        }
        if (requestType == SapRequestType.CREDIT) {
            final JsonObject paymentDocument = jsonAnnulled.get("paymentDocument").getAsJsonObject();
            final JsonObject workDocument = jsonAnnulled.get("workingDocument").getAsJsonObject();
            workDocument.addProperty("workOriginDocNumber", paymentDocument.get("paymentDocumentNumber").getAsString());
        }
        if (requestType == SapRequestType.PAYMENT
                || requestType == SapRequestType.PAYMENT_INTEREST
                || requestType == SapRequestType.ADVANCEMENT
                || requestType == SapRequestType.CREDIT
                || requestType == SapRequestType.CLOSE_INVOICE) {
            final JsonObject paymentDocument = jsonAnnulled.get("paymentDocument").getAsJsonObject();
            paymentDocument.addProperty("paymentStatus", "A");
            paymentDocument.addProperty("paymentDate", new DateTime().toString("yyyy-MM-dd HH:mm:ss"));
        }

        final SapRequest sapRequestAnnulled = new SapRequest(sapRequest.getEvent(), sapRequest.getClientId(), sapRequest.getValue(),
                sapRequest.getDocumentNumber(), sapRequest.getRequestType(), sapRequest.getAdvancement(), jsonAnnulled);
        sapRequest.setAnulledRequest(sapRequestAnnulled);
        sapRequestAnnulled.setIgnore(true);
    }

    @Atomic
    public void cancelCredit(final SapRequest sapRequest) {
        if (sapRequest.getRequestType() != SapRequestType.CREDIT) {
            throw new Error("label.document.type.cancel.not.credit");
        }
        registerInvoice(sapRequest.getValue(), sapRequest.getEvent(), sapRequest.getEvent().isGratuity(), true);
    }

    @Atomic
    public void cancelDebt() {
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

    public void updateInvoiceWithNewClientData() {
        for (final SapRequest sapRequest : getFilteredSapRequestStream().filter(r -> !r.isInitialization()).collect(Collectors.toSet())) {
            updateInvoiceWithNewClientData(sapRequest);
        }
    }

    private void updateInvoiceWithNewClientData(final SapRequest sapRequest) {
        final SapRequestType requestType = sapRequest.getRequestType();
        if (requestType != SapRequestType.INVOICE) {
            return;
        }
        if (sapRequest.isReferencedByOtherRequest()) {
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
        final SapRequest creditRequest = registerCredit(event, EventProcessor.getCreditEntry(invoiceValue), invoiceValue, sapRequest, false);
        sapRequest.setIgnore(true);
        creditRequest.setIgnore(true);

        final JsonObject data = toJsonInvoice(event, invoiceValue, getDocumentDate(event.getWhenOccured(), true), new DateTime(), clientId, false, false, false);
        final String documentNumber = getDocumentNumber(data, false);
        new SapRequest(event, clientId, invoiceValue, documentNumber, SapRequestType.INVOICE, Money.ZERO, data);
    }

    private SapRequest registerDebt(Money debtFenix, Event event, boolean isNewDate) {
        String clientId = ClientMap.uVATNumberFor(event.getParty());
        JsonObject data = toJsonDebt(event, debtFenix, clientId, getDocumentDate(event.getWhenOccured(), isNewDate),
                new DateTime(), true, "NG", true, isNewDate, null);

        String documentNumber = getDocumentNumber(data, false);
        return new SapRequest(event, clientId, debtFenix, documentNumber, SapRequestType.DEBT, Money.ZERO, data);
    }

    private void registerDebtCredit(CreditEntry creditEntry, Event event, boolean isNewDate) {

        String clientId = ClientMap.uVATNumberFor(event.getParty());
        SimpleImmutableEntry<List<SapRequest>, Money> openDebtsAndRemainingValue = getOpenDebtsAndRemainingValue();
        List<SapRequest> openDebts = openDebtsAndRemainingValue.getKey();
        Money remainingAmount = openDebtsAndRemainingValue.getValue();
        final Money amountToRegister = new Money(creditEntry.getUsedAmountInDebts());
        if (amountToRegister.greaterThan(remainingAmount)) {
            if (openDebts.size() > 1) {
                // dividir o valor da isenção pelas várias dívidas
                registerDebtCreditList(event, openDebts, amountToRegister, creditEntry, remainingAmount,
                        clientId, isNewDate);
            } else {
                throw new Error("There is no open debt to credit exemption: " + creditEntry.getId() + " for event: " + event.getExternalId());
            }
        } else {
            //tudo normal
            registerDebtCredit(clientId, event, amountToRegister, creditEntry, openDebts.get(0), isNewDate);
        }
    }

    private void registerDebtCreditList(Event event, List<SapRequest> openDebts, Money amountToRegister, CreditEntry creditEntry,
                                        Money remainingAmount, String clientId, boolean isNewDate) {
        if (amountToRegister.greaterThan(remainingAmount)) {
            if (openDebts.size() > 1) {
                registerDebtCredit(clientId, event, remainingAmount, creditEntry, openDebts.get(0), isNewDate);
                registerDebtCreditList(event, openDebts.subList(1, openDebts.size()), amountToRegister.subtract(remainingAmount),
                        creditEntry, openDebts.get(1).getValue(), clientId, isNewDate);
            } else {
                throw new Error("There is no open debt to credit exemption: " + creditEntry.getId() + " for event: " + event.getExternalId());
            }
        } else {
            registerDebtCredit(clientId, event, amountToRegister, creditEntry, openDebts.get(0), isNewDate);
        }
    }

    private SapRequest registerDebtCredit(String clientId, Event event, Money amountToRegister, CreditEntry creditEntry,
                                          SapRequest debtRequest, boolean isNewDate) {
        checkValidDocumentNumber(debtRequest.getDocumentNumber(), event);

        JsonObject data = toJsonDebtCredit(event, amountToRegister, clientId,
                getDocumentDate(creditEntry.getCreated(), isNewDate), new DateTime(), true, "NJ", false, isNewDate, debtRequest);
        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest =
                new SapRequest(event, clientId, amountToRegister, documentNumber, SapRequestType.DEBT_CREDIT, Money.ZERO, data);
        sapRequest.setCreditId(creditEntry.getId());
        return sapRequest;
    }

    public void registerCredit(Event event, CreditEntry creditEntry, boolean isGratuity, boolean isPastPayment) {
        Money amountToCredit = new Money(creditEntry.getUsedAmountInDebts());
        if (!amountToCredit.isPositive()) {
            throw new Error("There is no debt value for the credit exemption: " + creditEntry.getId() + " for event: " + event.getExternalId());
        }

        // diminuir divida no sap (se for propina diminuir dívida) e credit note na última factura existente
        if (isToProcessDebt(isGratuity, true, new DateTime())) {
            registerDebtCredit(creditEntry, event, false);
        }

        final SortedMap<SapRequest, Money> openInvoices = getOpenInvoicesAndRemainingValue();
        if (openInvoices.isEmpty()) {
            throw new Error("There is no open invoice to credit exemption: " + creditEntry.getId() + " for event: " + event.getExternalId());
        }
        for (final Entry<SapRequest, Money> entry : openInvoices.entrySet()) {
            if (amountToCredit.isPositive()) {
                final SapRequest invoice = entry.getKey();
                final Money openInvoiceAmount = entry.getValue();

                final Money amountForInvoice = Money.min(openInvoiceAmount, amountToCredit);
                registerCredit(event, creditEntry, amountForInvoice, invoice, isPastPayment);
                //only if the credit value is not equal to the original invoice value must we close the invoice
                //otherwise there is no need to send a closing document
                if (!invoice.getValue().equals(openInvoiceAmount) && openInvoiceAmount.equals(amountToCredit)) {
                    registerFinalZeroPayment(invoice, creditEntry.getId(), null);
                }
                amountToCredit = amountToCredit.subtract(amountForInvoice);
            }
        }
        if (amountToCredit.isPositive()) {
            throw new Error("Trying to credit more value than existing open invoices for exemption: " + creditEntry.getId()
                    + " for event: " + event.getExternalId());
        }
    }

    private SapRequest registerCredit(Event event, CreditEntry creditEntry, Money creditAmount, SapRequest sapInvoiceRequest, boolean isPastPayment) {
        checkValidDocumentNumber(sapInvoiceRequest.getDocumentNumber(), event);

        DateTime documentDate = getDocumentDate(creditEntry.getCreated(), false);
        if (sapInvoiceRequest.getDocumentDate().getYear() > documentDate.getYear()) {
            documentDate = sapInvoiceRequest.getDocumentDate();
        }
        JsonObject data = toJsonCredit(event, documentDate, creditAmount, sapInvoiceRequest, false, true, isPastPayment);
        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest =
                new SapRequest(event, sapInvoiceRequest.getClientId(), creditAmount, documentNumber, SapRequestType.CREDIT, Money.ZERO, data);
        sapRequest.setCreditId(creditEntry.getId());
        return sapRequest;
    }

    public void registerReimbursement(final Refund refund, final DebtExemption debtExemption) {
        org.fenixedu.academic.domain.accounting.Refund domainRefund = FenixFramework.getDomainObject(refund.getId());
        if (getPayedAmount().getAmount().compareTo(refund.getAmount()) == 0) {
            final Money paymentsInSap = getFilteredSapRequestStream()
                    .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE && !sr.isInitialization())
                    .sorted(SapRequest.COMPARATOR_BY_EVENT_AND_ORDER)
                    .map(sr -> registerReimbursement(sr, domainRefund, null, debtExemption, true))
                    .reduce(Money.ZERO, Money::add);

            if (!refund.getAmount().equals(paymentsInSap.getAmount())) {
                throw new Error("The value given for refund (" + refund.getId() + ") does not match the value of payments in SAP."
                        + "Refund amount: " + refund.getAmount() + " - payments in SAP: " + paymentsInSap.getAmountAsString());
            }
        } else if (!getOpenInvoicesAndRemainingValue().isEmpty()) {
            throw new Error("It's not possible to reimburse a partial value from an event that is not closed. For refund: "
                    + refund.getId() + " - amount: " + refund.getAmount());
        } else {
            final List<SapRequest> sapRequests = getFilteredSapRequestStream()
                    .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE && !sr.isInitialization())
                    .sorted(SapRequest.COMPARATOR_BY_EVENT_AND_ORDER.reversed()).collect(Collectors.toList());
            Money refundValue = domainRefund.getAmount();
            for (SapRequest sr : sapRequests) {
                final Money payedAmountInInvoice = calculateAmountPayedForInvoice(sr);
                registerReimbursement(sr, domainRefund, Money.min(refundValue, payedAmountInInvoice), debtExemption, false);
                refundValue = refundValue.subtract(payedAmountInInvoice);
                if (!refundValue.isPositive()) {
                    return;
                }
            }
        }
    }

    private Money registerReimbursement(final SapRequest sapInvoiceRequest, final org.fenixedu.academic.domain.accounting.Refund refund,
                                        final Money refundValue, final DebtExemption debtExemption, final boolean refundTotal) {
        checkValidDocumentNumber(sapInvoiceRequest.getDocumentNumber(), event);

        final String clientId = sapInvoiceRequest.getClientId();

        final Money valueToExempt = refundTotal ? invoiceValueWithoutCredits(sapInvoiceRequest) : refundValue;
        final Money valueToRefund = refundTotal ? calculateAmountPayedForInvoice(sapInvoiceRequest) : refundValue;

        //if the invoice generated debt we have to send a debt credit
        if (isToProcessDebt(event.isGratuity(), true, new DateTime())) {
            registerDebtCredit(EventProcessor.getCreditEntry(valueToExempt), event, true);
        }

        if (refundTotal && valueToRefund.isZero()) {
            //when a reimbursement of total payed is done the event is closed, so we have to close all invoices
            //if for an invoice no payment was done we just have to send a credit note to close it, no real reimbursement needed
            final JsonObject jsonCredit = toJsonCredit(event, new DateTime(), valueToExempt, sapInvoiceRequest, false, true, false);
            final String documentNumber = getDocumentNumber(jsonCredit, true);
            final SapRequest sapRequestNA = new SapRequest(event, clientId, valueToExempt, documentNumber, SapRequestType.CREDIT, Money.ZERO, jsonCredit);
            sapRequestNA.setRefund(refund);
            sapRequestNA.setCreditId(debtExemption.getId());
        } else {
            final JsonObject data = toJsonReimbursement(event, valueToRefund, valueToExempt, sapInvoiceRequest, false, true);
            final String documentNumber = getDocumentNumber(data, true);
            SapRequest reimbursement = new SapRequest(event, clientId, valueToRefund, documentNumber, SapRequestType.REIMBURSEMENT, Money.ZERO, data);
            reimbursement.setRefund(refund);

            //we must create a fictious credit note request so that we know that the invoice is closed
            SapRequest creditNoteRequest = new SapRequest(event, clientId, valueToExempt, documentNumber, SapRequestType.CREDIT, Money.ZERO, data);
            creditNoteRequest.setSent(true);
            creditNoteRequest.setWhenSent(new DateTime());
            creditNoteRequest.setIntegrated(true);
            creditNoteRequest.setRefund(refund);
            creditNoteRequest.setCreditId(debtExemption.getId());
        }
        return valueToRefund;
    }

    private Money invoiceValueWithoutCredits(final SapRequest sapInvoiceRequest) {
        final Money value = sapInvoiceRequest.getValue();
        final Money credits = getFilteredSapRequestStream()
                .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT && sr.refersToDocument(sapInvoiceRequest.getDocumentNumber()))
                .map(SapRequest::getValue)
                .reduce(Money.ZERO, Money::add);
        return value.subtract(credits);
    }

    private Money calculateAmountPayedForInvoice(final SapRequest sapInvoiceRequest) {
        return getFilteredSapRequestStream()
            .filter(sr -> (sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.ADVANCEMENT)
                    && sr.refersToDocument(sapInvoiceRequest.getDocumentNumber()))
            .map(sr -> sr.getValue())
            .reduce(Money.ZERO, Money::add);
    }

    public void registerReimbursementAdvancement(ExcessRefund excessRefund) {
        excessRefund.getPartialPayments().stream().forEach(p -> registerReimbursementAdvancement(p, excessRefund));
    }

    private SapRequest registerReimbursementAdvancement(PartialPayment partialPayment, ExcessRefund excessRefund) {
        final List<SapRequest> paymentsFor = getAdvancementPaymentsFor(partialPayment.getCreditEntry().getId()).collect(Collectors.toList());
        if (paymentsFor.size() > 1) {
            throw new Error("More than one advancement payment was done with ID " +
                    partialPayment.getCreditEntry().getId() + " - can not make reimbursement");
        }
        SapRequest advPayment = paymentsFor.iterator().next();
        if (advPayment.getAdvancement().lessThan(new Money(partialPayment.getAmount()))) {
            throw new Error("Trying to reimburse more value than what was sent in request for payment: " +
                    partialPayment.getCreditEntry().getId() + " - can not make reimbursement");
        }

        String clientId = ClientMap.uVATNumberFor(event.getParty());
        JsonObject data = toJsonReimbursementAdvancement(advPayment, false, true);

        String documentNumber = getDocumentNumber(data, true);
        SapRequest reimbursementRequest = new SapRequest(event, clientId, new Money(partialPayment.getAmount()), documentNumber, SapRequestType.REIMBURSEMENT, Money.ZERO, data);
        reimbursementRequest.setAdvancementRequest(advPayment);
        reimbursementRequest.setRefund(FenixFramework.getDomainObject(excessRefund.getId()));
        return reimbursementRequest;
    }

    public void registerPastPayment(final Payment payment) {
        String clientId = ClientMap.uVATNumberFor(event.getParty());
        AccountingTransaction transaction = FenixFramework.getDomainObject(payment.getId());

        final Money payedInterest = new Money(payment.getUsedAmountInInterests().add(payment.getUsedAmountInFines()));
        if (payedInterest.isPositive()) {
            registerInterest(payedInterest, clientId, transaction.getTransactionDetail());
        }

        final Money amountPayed = new Money(payment.getUsedAmountInDebts());
        if (amountPayed.isPositive()) {
            // registering the invoice
            SapRequest pastInvoiceRequest = registerPastInvoice(amountPayed, clientId,
                    transaction.getTransactionDetail(), transaction.getTransactionDetail().getWhenRegistered());
            
            // if it is an internal imputation gets a different treatment
            if (Bennu.getInstance().getInternalPaymentMethod() == transaction.getTransactionDetail().getPaymentMethod()) {
                processInternalPayment(payment, true);
            } else {
                // registering the payment
                registerSinglePayment(transaction.getTransactionDetail(), amountPayed, pastInvoiceRequest, false, true, SapRequestType.PAYMENT);
            }
        }
    }

    private SapRequest registerPastInvoice(final Money amountPayed, final String clientId, final AccountingTransactionDetail transactionDetail,
                                           DateTime paymentDate) {
        JsonObject data = toJsonInvoice(event, amountPayed, paymentDate, new DateTime(), clientId, false, false, true);

        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest = new SapRequest(event, clientId, amountPayed, documentNumber, SapRequestType.INVOICE, Money.ZERO, data);
        sapRequest.setPayment(transactionDetail.getTransaction());
        return sapRequest;
    }

    private void registerInterest(final Money payedInterest, final String clientId, final AccountingTransactionDetail transactionDetail) {

        if (hasInterestPayment(transactionDetail.getTransaction())) {
            return; //both invoice and payment requests have been processed
        }

        // registering the invoice
        SapRequest interestInvoiceRequest = registerInterestInvoice(payedInterest, clientId, transactionDetail,
                transactionDetail.getWhenRegistered());
        // registering the payment
        registerInterestPayment(payedInterest, interestInvoiceRequest, transactionDetail);
    }

    private SapRequest registerInterestInvoice(final Money payedInterest, final String clientId,
                                               final AccountingTransactionDetail transactionDetail, DateTime paymentDate) {
        DateTime now = new DateTime();
        DateTime invoiceDate = paymentDate;
        if (paymentDate.getYear() < now.getYear()) {
            invoiceDate = now;
        }
        JsonObject data = toJsonInvoice(event, payedInterest, invoiceDate, new DateTime(), clientId, false, true, false);

        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest = new SapRequest(event, clientId, payedInterest, documentNumber,
                SapRequestType.INVOICE_INTEREST, Money.ZERO, data);
        sapRequest.setPayment(transactionDetail.getTransaction());
        return sapRequest;
    }

    private void registerInterestPayment(final Money payedInterest, SapRequest sapInvoiceRequest,
                                         final AccountingTransactionDetail transactionDetail) {
        registerSinglePayment(transactionDetail, payedInterest, sapInvoiceRequest, true, false, SapRequestType.PAYMENT_INTEREST);
    }

    public void registerPayment(final CreditEntry payment) {

        final AccountingTransactionDetail transactionDetail =
                ((AccountingTransaction) FenixFramework.getDomainObject(payment.getId())).getTransactionDetail();

        // if it is an internal imputation gets a different treatment
        if (Bennu.getInstance().getInternalPaymentMethod() == transactionDetail.getPaymentMethod()) {
            processInternalPayment(payment, false);
            return;
        }

        final String clientId = ClientMap.uVATNumberFor(event.getParty());

        final Money payedInterest = new Money(payment.getUsedAmountInInterests().add(payment.getUsedAmountInFines()));
        if (payedInterest.isPositive()) {
            registerInterest(payedInterest, clientId, transactionDetail);
        }

        Money payedAmount = new Money(payment.getAmount()).subtract(payedInterest);
        if (payedAmount.isZero()) {
            //it was all used for interests, there is nothing more to register
            return;
        }

        final SortedMap<SapRequest, Money> openInvoices = getOpenInvoicesAndRemainingValue();
        if (openInvoices.size() == 0) {
            registerAdvancementOnly(clientId, transactionDetail, payedAmount);
        } else {
            registerPayment(transactionDetail, payedAmount, (Entry<SapRequest, Money>[]) openInvoices.entrySet().toArray(new Entry[0]));
        }
    }

    private void processInternalPayment(final CreditEntry payment, final boolean isPastPayment) {
        if (!hasCredit(payment.getId())) {
            Set<SapRequest> before = new HashSet<>(event.getSapRequestSet());
            registerCredit(event, payment, false, isPastPayment);
            Set<SapRequest> after = new HashSet<>(event.getSapRequestSet());
            after.removeAll(before);
            //associate all requests originated from registerCredit to the payment
            after.forEach(sr -> sr.setPayment(FenixFramework.getDomainObject(payment.getId())));
        }
    }

    private void registerPayment(final AccountingTransactionDetail transactionDetail, final Money payedAmount, final Entry<SapRequest, Money>[] openInvoices) {
        if (payedAmount.isPositive()) {
            final Entry<SapRequest, Money> entry = openInvoices[0];
            final SapRequest openInvoice = entry.getKey();
            final Money openInvoiceValue = entry.getValue();

            if (openInvoiceValue.greaterOrEqualThan(payedAmount)) {
                registerPayment(transactionDetail, payedAmount, openInvoice, openInvoiceValue, false);
            } else if (openInvoices.length > 1) {
                registerPayment(transactionDetail, openInvoiceValue, openInvoice, openInvoiceValue, false);
                registerPayment(transactionDetail, payedAmount.subtract(openInvoiceValue), Arrays.copyOfRange(openInvoices, 1, openInvoices.length));
            } else {
                registerAdvancement(openInvoiceValue, payedAmount.subtract(openInvoiceValue), openInvoice, transactionDetail);
            }
        }
    }

    private void registerAdvancementOnly(final String clientId, final AccountingTransactionDetail transactionDetail, final Money payedAmount) {
        final JsonObject data = toJsonAdvancementOnly(clientId, transactionDetail, payedAmount);
        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest = new SapRequest(event, clientId, Money.ZERO, documentNumber,
                SapRequestType.ADVANCEMENT, payedAmount, data);
        sapRequest.setPayment(transactionDetail.getTransaction());
    }

    private JsonObject toJsonAdvancementOnly(final String clientId, final AccountingTransactionDetail transactionDetail, final Money amount) {
        final JsonObject clientData = toJsonClient(event.getParty(), clientId);
        JsonObject data = toJson(event, clientData, transactionDetail.getWhenRegistered(), false, false, false, true, false);
        JsonObject paymentDocument = toJsonPaymentDocument(Money.ZERO, "NP", "", transactionDetail.getWhenRegistered(),
                getPaymentMechanism(transactionDetail), getPaymentMethodReference(transactionDetail),
                SAFTPTSettlementType.NL.toString(), true);
        paymentDocument.addProperty("isWithoutLines", true);
        paymentDocument.addProperty("noPaymentTotals", true);
        paymentDocument.addProperty("isAdvancedPayment", true);
        paymentDocument.addProperty("excessPayment", amount.getAmountAsString());
        addSibsMetadata(paymentDocument, transactionDetail);

        JsonObject workingDocument = toJsonWorkDocument(getDocumentDate(transactionDetail.getWhenRegistered(), false),
                new DateTime(), amount, "NA", false, transactionDetail.getWhenRegistered());
        workingDocument.addProperty("isAdvancedPayment", true);
        workingDocument.addProperty("paymentDocumentNumber", paymentDocument.get("paymentDocumentNumber").getAsString());
        //the payment document has to refer the working document credit note number
        paymentDocument.addProperty("originatingOnDocumentNumber", workingDocument.get("workingDocumentNumber").getAsString());

        data.add("workingDocument", workingDocument);
        data.add("paymentDocument", paymentDocument);

        return data;
    }

    private void registerPayment(AccountingTransactionDetail transactionDetail, Money payedAmount, SapRequest sapInvoiceRequest, Money remainingInvoiceAmount, boolean isPastPayment) {

        checkValidDocumentNumber(sapInvoiceRequest.getDocumentNumber(), event);

        if (payedAmount.equals(sapInvoiceRequest.getValue())) {
            registerSinglePayment(transactionDetail, payedAmount, sapInvoiceRequest, false, isPastPayment, SapRequestType.PAYMENT);
        } else if (payedAmount.equals(remainingInvoiceAmount)) {
            registerFinalPayment(transactionDetail, payedAmount, sapInvoiceRequest, false, SapRequestType.PAYMENT, getPaymentsAndCreditsFor(sapInvoiceRequest));
        } else {
            //é um pagamento parcial é um pagamento normal
            registerSinglePayment(transactionDetail, payedAmount, sapInvoiceRequest, false, isPastPayment, SapRequestType.PAYMENT);
        }
    }

    private void registerFinalPayment(final AccountingTransactionDetail transactionDetail, final Money payedAmount, final SapRequest sapInvoiceRequest,
                                      final boolean isInterest, final SapRequestType requestType, final Stream<SapRequest> documentsFor) {

        if (sapInvoiceRequest.getDocumentDate().getYear() > transactionDetail.getWhenRegistered().getYear()) {
            registerAdvancementAndUsedIt(transactionDetail, payedAmount, sapInvoiceRequest, requestType);
            registerFinalZeroPayment(sapInvoiceRequest, null, transactionDetail.getTransaction());
        } else {
            JsonObject data = toJsonFinalPayment(transactionDetail, payedAmount, sapInvoiceRequest, isInterest, documentsFor);

            String documentNumber = getDocumentNumber(data, true);
            SapRequest sapRequest = new SapRequest(event, sapInvoiceRequest.getClientId(), payedAmount, documentNumber,
                    requestType, Money.ZERO, data);
            sapRequest.setPayment(transactionDetail.getTransaction());
        }
    }

    private void registerAdvancementAndUsedIt(final AccountingTransactionDetail transactionDetail, final Money payedAmount, final SapRequest sapInvoiceRequest, final SapRequestType requestType) {
        registerAdvancementOnly(sapInvoiceRequest.getClientId(), transactionDetail, payedAmount);
        CreditEntry creditEntry = new Payment(transactionDetail.getTransaction().getExternalId(), null, null, null, null, null);
        PartialPayment<DebtEntry> partialPayment = new PartialPayment<DebtEntry>(creditEntry, null, null);
        registerAdvancementInPayment(null, partialPayment, transactionDetail, event, payedAmount, sapInvoiceRequest, requestType, true);
    }

    private JsonObject toJsonFinalPayment(final AccountingTransactionDetail transactionDetail, final Money payedAmount, final SapRequest sapInvoiceRequest,
                                          final boolean isInterest, final Stream<SapRequest> documentsFor) {
        //the debit amount of the final payment must be the value of the invoice
        JsonObject data = toJsonPayment(transactionDetail, sapInvoiceRequest.getValue(), sapInvoiceRequest, isInterest, false);
        JsonObject paymentDocument = data.get("paymentDocument").getAsJsonObject();

        //for the excessAmount the payment amount must be the the actual amount payed
        toJsonDocumentsList(documentsFor, paymentDocument, payedAmount.subtract(sapInvoiceRequest.getValue()));
        return data;
    }

    /**
     * Closes an invoice that has several documents in which the last one is a credit note or a payment
     * @param sapInvoiceRequest
     * @param creditId
     * @param payment
     */
    public void registerFinalZeroPayment(final SapRequest sapInvoiceRequest, final String creditId, final AccountingTransaction payment) {
        JsonObject data = toJsonFinalZeroPayment(sapInvoiceRequest, getPaymentsAndCreditsFor(sapInvoiceRequest));

        String documentNumber = getDocumentNumber(data, true);
        SapRequest finalPayment = new SapRequest(event, sapInvoiceRequest.getClientId(), Money.ZERO, documentNumber,
                SapRequestType.CLOSE_INVOICE, Money.ZERO, data);
        finalPayment.setCreditId(creditId);
        finalPayment.setPayment(payment);
    }

    private JsonObject toJsonFinalZeroPayment(final SapRequest sapInvoiceRequest, final Stream<SapRequest> documentsFor) {
        JsonObject data = toJsonPaymentZero(sapInvoiceRequest);
        JsonObject paymentDocument = data.get("paymentDocument").getAsJsonObject();

        toJsonDocumentsList(documentsFor, paymentDocument, sapInvoiceRequest.getValue().negate());
        return data;
    }

    private void toJsonDocumentsList(final Stream<SapRequest> documentsFor, final JsonObject paymentDocument, final Money excessPayment) {
        final JsonArray documents = new JsonArray();
        documentsFor.forEach(sr -> {
            JsonObject line = new JsonObject();
            line.addProperty("amount", sr.getValue().getAmountAsString());
            line.addProperty("isToDebit", false);
            //if it is credit note we need to send the associated NP
            String originDocNumber = sr.getRequestType().equals(SapRequestType.CREDIT) ? sr.getDocumentNumberForType("NP") : sr.getDocumentNumber();
            line.addProperty("originDocNumber", originDocNumber);
            documents.add(line);
        });
        paymentDocument.addProperty("excessPayment", excessPayment.toPlainString());
        paymentDocument.add("documents", documents);
        paymentDocument.addProperty("noPaymentTotals", true);
    }

    private void registerSinglePayment(final AccountingTransactionDetail transactionDetail, final Money payedAmount, final SapRequest sapInvoiceRequest,
                                       final boolean isInterest, final boolean isPastPayment, final SapRequestType requestType) {

        if (sapInvoiceRequest.getDocumentDate().getYear() > transactionDetail.getWhenRegistered().getYear()) {
            registerAdvancementAndUsedIt(transactionDetail, payedAmount, sapInvoiceRequest, requestType);
        } else {
            JsonObject data = toJsonPayment(transactionDetail, payedAmount, sapInvoiceRequest, isInterest, isPastPayment);
            String documentNumber = getDocumentNumber(data, true);

            SapRequest sapRequest = new SapRequest(event, sapInvoiceRequest.getClientId(), payedAmount, documentNumber,
                    requestType, Money.ZERO, data);
            sapRequest.setPayment(transactionDetail.getTransaction());
        }
    }

    private void registerAdvancement(Money amount, Money advancement, SapRequest sapInvoiceRequest, AccountingTransactionDetail transactionDetail) {
        checkValidDocumentNumber(sapInvoiceRequest.getDocumentNumber(), event);

        if (amount.equals(sapInvoiceRequest.getValue())) {
            JsonObject data = toJsonAdvancement(amount, advancement, sapInvoiceRequest, transactionDetail);
            String documentNumber = getDocumentNumber(data, true);
            SapRequest sapRequest = new SapRequest(event, sapInvoiceRequest.getClientId(), amount, documentNumber,
                    SapRequestType.ADVANCEMENT, advancement, data);
            sapRequest.setPayment(transactionDetail.getTransaction());
        } else {
            registerFinalPayment(transactionDetail, amount, sapInvoiceRequest, false, SapRequestType.PAYMENT, getPaymentsAndCreditsFor(sapInvoiceRequest));
            registerAdvancementOnly(ClientMap.uVATNumberFor(transactionDetail.getEvent().getParty()), transactionDetail, advancement);
        }
    }

    public void registerAdvancementInPayment(final Payment payment) {
        AccountingTransactionDetail paymentDetail = ((AccountingTransaction) FenixFramework.getDomainObject(payment.getId())).getTransactionDetail();
        Event originEvent = paymentDetail.getTransaction().getRefund().getEvent();
        DebtInterestCalculator calculator = originEvent.getDebtInterestCalculator(new DateTime());
        ExcessRefund excessRefund = calculator.getExcessRefundStream().filter(er -> er.getId().equals(payment.getRefundId())).findAny().get();

        final Map<PartialPayment, Money> paymentMoneyMap = excessRefund.getPartialPayments().stream().collect(Collectors.toMap(p -> p, p -> new Money(p.getAmount())));

        Money interests = new Money(payment.getUsedAmountInFines().add(payment.getUsedAmountInInterests()));
        if (interests.isPositive()) {
            registerAdvancementInPaymentInterest(excessRefund, interests, paymentDetail, originEvent, paymentMoneyMap);
        }

        paymentMoneyMap.forEach((partialPayment, pAmount) -> {
            registerAdvancementInPayment(excessRefund, partialPayment, pAmount, paymentDetail, originEvent, getOpenInvoicesAndRemainingValue().entrySet().toArray(new Entry[0]));
        });

    }

    private void registerAdvancementInPaymentInterest(final ExcessRefund excessRefund, final Money interest, final AccountingTransactionDetail payment,
                                                      final Event originEvent, final Map<PartialPayment, Money> partialPayments) {
        Money interestAmount = new Money(interest.getAmount());
        for (Entry<PartialPayment, Money> entry : partialPayments.entrySet()) {
            if (interestAmount.isPositive()) {
                Money pAmount = entry.getValue();
                PartialPayment partialPayment = entry.getKey();
                Money interestToProcess = Money.min(pAmount, interestAmount);

                final String clientId = ClientMap.uVATNumberFor(event.getParty());
                SapRequest interestInvoice = registerInterestInvoice(interestToProcess, clientId, payment, payment.getWhenRegistered());
                //if the invoice was pushed to the next year, this payment must be too
                boolean isNewDate = interestInvoice.getDocumentDate().getYear() > payment.getWhenRegistered().getYear();
                registerAdvancementInPayment(excessRefund, partialPayment, payment, originEvent, interestToProcess, interestInvoice, SapRequestType.PAYMENT_INTEREST, isNewDate);
                partialPayments.put(partialPayment, Money.max(Money.ZERO, pAmount.subtract(interestAmount)));
                interestAmount = interestAmount.subtract(pAmount);
            }
        }
    }

    private void registerAdvancementInPayment(final ExcessRefund excessRefund, final PartialPayment partialPayment, final Money amountUsed, final AccountingTransactionDetail payment,
                                              final Event originEvent, final Entry<SapRequest, Money>[] openInvoices) {
        if (amountUsed.isPositive()) {
            final Entry<SapRequest, Money> entry = openInvoices[0];
            final SapRequest openInvoice = entry.getKey();
            final Money openInvoiceValue = entry.getValue();

            if (openInvoiceValue.greaterOrEqualThan(amountUsed)) {
                registerAdvancementInPayment(excessRefund, partialPayment, payment, originEvent, amountUsed, openInvoice, SapRequestType.PAYMENT, false);
                //this advancement use closes this invoice money wise, so we have to close it with a document
                //unless the advancement is the only paying document and it closes by itself the invoice
                if (openInvoiceValue.compareTo(amountUsed) == 0 && openInvoice.getValue().compareTo(amountUsed) != 0) {
                    registerFinalZeroPayment(openInvoice, null, payment.getTransaction());
                }
            } else if (openInvoices.length > 1) {
                registerAdvancementInPayment(excessRefund, partialPayment, payment, originEvent, openInvoiceValue, openInvoice, SapRequestType.PAYMENT, false);
                registerAdvancementInPayment(excessRefund, partialPayment, amountUsed.subtract(openInvoiceValue), payment, originEvent, Arrays.copyOfRange(openInvoices, 1, openInvoices.length));
            } else {
                throw new Error("There is no open invoice to register payment: " + payment.getExternalId() + " - that resulted from an advance");
            }
        }
    }

    private void registerAdvancementInPayment(final ExcessRefund excessRefund, final PartialPayment partialPayment, final AccountingTransactionDetail payment,
                                              final Event originEvent, final Money amountUsed, final SapRequest openInvoice, final SapRequestType requestType, final boolean isNewDate) {
        SapEvent sapOriginEvent = new SapEvent(originEvent);
        SapRequest originalPayment = sapOriginEvent.getAdvancementPaymentsFor(partialPayment.getCreditEntry().getId()).findAny()
                .orElseThrow(() -> new Error("There is no advancement registered for paymentId: " + partialPayment.getCreditEntry().getId()));
        JsonObject advInPayment = toJsonUseAdvancementInPayment(payment, originalPayment, amountUsed, openInvoice.getDocumentNumber(), isNewDate);
        final SapRequest sapRequest = new SapRequest(event, originalPayment.getClientId(), amountUsed, getDocumentNumber(advInPayment, true),
                requestType, Money.ZERO, advInPayment);
        sapRequest.setPayment(payment.getTransaction());
        if (excessRefund != null) {
            sapRequest.setRefund(FenixFramework.getDomainObject(excessRefund.getId()));
        }
        sapRequest.setAdvancementRequest(originalPayment);
    }

    private JsonObject toJsonUseAdvancementInPayment(final AccountingTransactionDetail payment, final SapRequest originalPayment, final Money amountToUse, final String invoiceNumber, final boolean isNewDate) {
        DateTime documentDate = payment.getWhenRegistered();
        if (isNewDate) {
            documentDate = new DateTime();
        }
        JsonObject data =
                toJson(event, originalPayment.getClientJson(), documentDate, false, false, false, false, false);

        JsonObject paymentDocument = toJsonPaymentDocument(amountToUse, "NP", invoiceNumber, documentDate,
                "OU", getPaymentMethodReference(payment), SAFTPTSettlementType.NN.toString(), true);
        paymentDocument.addProperty("excessPayment", amountToUse.negate().toPlainString());//the payment amount must be zero
        paymentDocument.addProperty("isToCreditTotal", true);
        paymentDocument.addProperty("isAdvancedPayment", true);
        paymentDocument.addProperty("originatingOnDocumentNumber", originalPayment.getDocumentNumberForType("NA"));

        data.add("paymentDocument", paymentDocument);

        return data;
    }

    private void addSibsMetadata(final JsonObject json, final AccountingTransactionDetail transactionDetail) {
        if (PaymentMethod.getSibsPaymentMethod() == transactionDetail.getPaymentMethod()) {
            SibsTransactionDetail sibsTx = (SibsTransactionDetail) transactionDetail;
            YearMonthDay sibsDate = sibsTx.getSibsLine().getHeader().getWhenProcessedBySibs();
            json.addProperty("sibsDate", sibsDate.toString(SIBS_DATE_FORMAT));
        }
    }

    public boolean processPendingRequests(final Event event, final ErrorLogConsumer errorLog, final EventLogger elogger) {
        final Set<SapRequest> requests = new TreeSet<>(SapRequest.COMPARATOR_BY_ORDER);
        requests.addAll(event.getSapRequestSet());
        for (final SapRequest sr : requests) {
            if (!processPendingRequests(sr, errorLog, elogger)) {
                return false;
            }
        }
        return true;
    }

    public boolean processPendingRequests(final SapRequest sr, final ErrorLogConsumer errorLog, final EventLogger elogger) {
        if (!SapRoot.getInstance().getAllowCommunication()) {
            return false;
        }
        if (!sr.getIntegrated() && sr.allowedToSend()) {
            final JsonParser jsonParser = new JsonParser();
            final JsonObject data = (JsonObject) jsonParser.parse(sr.getRequest());

            final JsonObject result = sendDataToSap(sr, data);

            if (!checkAndRegisterIntegration(event, errorLog, elogger, data, sr.getDocumentNumber(), sr,
                    result, sr.getRequestType().toString(), sr.getRequestType().isToGetDocument())) {
                return false;
            }

            final SapRequest originalRequest = sr.getOriginalRequest();
            if (originalRequest != null) {
                originalRequest.setIgnore(true);
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
                        new SapDocumentFile(sapRequest,sanitize(sapDocumentNumber) + ".pdf",
                                Base64.getDecoder().decode(docResult.get("documentBase64").getAsString()));
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
        return documentDate;
    }

    private boolean isToProcessDebt(boolean isGratuity, final boolean isNewDate, final DateTime documentDate) {
        return (isGratuity || event instanceof ExternalScholarshipPhdGratuityContribuitionEvent)
                && event.getWhenOccured().isAfter(EventWrapper.LIMIT)
                && isNotPastDebtEndDate(isNewDate, documentDate);
    }

    private boolean isNotPastDebtEndDate(final boolean isNewDate, final DateTime documentDate) {
        return getDebtInterval(documentDate, isNewDate) != null;
    }

    /**
     * Sends the data to SAP
     *
     * @param sapRequest - the domain representation of the request
     * @param data       - the necessary data to invoke the service for the specified operation
     * @return The result of the SAP service invocation, with the status of the documents and clients and also the xml request
     * sent. In case of an unexpected exception returns the exception message
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
                                     boolean isInterest, boolean isPastPayment) {
        JsonObject data =
                toJson(event, sapInvoiceRequest.getClientJson(), transactionDetail.getWhenRegistered(), false, false, isInterest, false, isPastPayment);
        JsonObject paymentDocument = toJsonPaymentDocument(amount, "NP", sapInvoiceRequest.getDocumentNumber(), transactionDetail.getWhenRegistered(),
                getPaymentMechanism(transactionDetail), getPaymentMethodReference(transactionDetail),
                SAFTPTSettlementType.NL.toString(), true);
        addSibsMetadata(paymentDocument, transactionDetail);

        data.add("paymentDocument", paymentDocument);
        return data;
    }

    private JsonObject toJsonPaymentZero(SapRequest sapInvoiceRequest) {
        DateTime now = new DateTime();
        JsonObject data =
                toJson(event, sapInvoiceRequest.getClientJson(), now, false, false, false, false, false);
        JsonObject paymentDocument = toJsonPaymentDocument(sapInvoiceRequest.getValue(), "NP", sapInvoiceRequest.getDocumentNumber(),
                now,"OU", "", SAFTPTSettlementType.NN.toString(), true);

        data.add("paymentDocument", paymentDocument);
        return data;
    }

    private JsonObject toJsonAdvancement(Money amount, Money excess, SapRequest sapInvoiceRequest,
                                         AccountingTransactionDetail transactionDetail) {
        JsonObject data =
                toJson(event, sapInvoiceRequest.getClientJson(), transactionDetail.getWhenRegistered(), false, false, false, true, false);
        JsonObject paymentDocument = toJsonPaymentDocument(amount, "NP", sapInvoiceRequest.getDocumentNumber(), transactionDetail.getWhenRegistered(),
                getPaymentMechanism(transactionDetail), getPaymentMethodReference(transactionDetail),
                SAFTPTSettlementType.NL.toString(), true);
        paymentDocument.addProperty("excessPayment", excess.toPlainString());
        paymentDocument.addProperty("isAdvancedPayment", true);
        addSibsMetadata(paymentDocument, transactionDetail);

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
                                    boolean isDebtRegistration, boolean isNewDate, boolean isPastPayment) {
        JsonObject json = toJson(event, sapInvoiceRequest.getClientJson(), new DateTime(), isDebtRegistration, isNewDate, false, false, isPastPayment);
        JsonObject workDocument = toJsonWorkDocument(documentDate, new DateTime(), creditAmount, "NA", false, new DateTime());
        workDocument.addProperty("workOriginDocNumber", sapInvoiceRequest.getDocumentNumber());
        json.add("workingDocument", workDocument);

        String workingDocumentNumber = workDocument.get("workingDocumentNumber").getAsString();
        JsonObject paymentDocument = toJsonPaymentDocument(creditAmount, "NP", workingDocumentNumber, documentDate, "OU", "",
                SAFTPTSettlementType.NN.toString(), false);
        paymentDocument.addProperty("isCreditNote", true);
        paymentDocument.addProperty("excessPayment", creditAmount.negate().toPlainString());//the payment amount must be zero

        final JsonArray documents = new JsonArray();
        JsonObject line = new JsonObject();
        line.addProperty("amount", creditAmount.getAmountAsString());
        line.addProperty("isToDebit", true);
        line.addProperty("originDocNumber", sapInvoiceRequest.getDocumentNumber());
        documents.add(line);
        paymentDocument.add("documents", documents);

        json.add("paymentDocument", paymentDocument);
        return json;
    }

    private JsonObject toJsonReimbursementAdvancement(SapRequest advancement, boolean isDebtRegistration, boolean isNewDate) {
        JsonObject json = toJson(event, advancement.getClientJson(), new DateTime(), isDebtRegistration, isNewDate, false, false, false);

        JsonObject paymentDocument = toJsonPaymentDocument(advancement.getAdvancement(), "NR", advancement.getDocumentNumber(),
                new DateTime(), "OU", "", SAFTPTSettlementType.NR.toString(), false);
        paymentDocument.addProperty("isReimbursement", true);
        paymentDocument.addProperty("reimbursementStatus", "PENDING");
        paymentDocument.addProperty("excessPayment", advancement.getAdvancement().negate().toPlainString());//the payment amount must be zero
        json.add("paymentDocument", paymentDocument);

        return json;
    }

    private JsonObject toJsonReimbursement(Event event, Money amountToRefund, final Money amountToCredit, SapRequest sapInvoiceRequest,
                                           boolean isDebtRegistration, boolean isNewDate) {
        JsonObject json = toJson(event, sapInvoiceRequest.getClientJson(), new DateTime(), isDebtRegistration, isNewDate, false, false, false);
        JsonObject workDocument = toJsonWorkDocument(new DateTime(), new DateTime(), amountToCredit, "NA", false, new DateTime());

        workDocument.addProperty("workOriginDocNumber", sapInvoiceRequest.getDocumentNumber());
        json.add("workingDocument", workDocument);

        String workingDocumentNumber = workDocument.get("workingDocumentNumber").getAsString();
        JsonObject paymentDocument = toJsonPaymentDocument(amountToRefund, "NR", workingDocumentNumber, new DateTime(), "OU", "",
                SAFTPTSettlementType.NR.toString(), false);
        paymentDocument.addProperty("isReimbursement", true);
        paymentDocument.addProperty("reimbursementStatus", "PENDING");
        paymentDocument.addProperty("excessPayment", amountToRefund.negate().toPlainString());//the payment amount must be zero
        json.add("paymentDocument", paymentDocument);

        return json;
    }

    private JsonObject toJsonPaymentDocument(Money amount, String documentType, String workingDocumentNumber,
                                             DateTime paymentDate, String paymentMechanism, String paymentMethodReference, String settlementType,
                                             boolean isDebit) {
        JsonObject paymentDocument = new JsonObject();
        paymentDocument.addProperty("paymentDocumentNumber", documentType + getDocumentNumber());
        paymentDocument.addProperty("paymentDate", paymentDate.toString(GiafInvoiceConfiguration.DT_FORMAT));
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
                                     boolean isDebtRegistration, boolean isInterest, boolean isPastPayment) {
        final JsonObject clientData = toJsonClient(event.getParty(), clientId);
        JsonObject json = toJson(event, clientData, documentDate, isDebtRegistration, true, isInterest, false, isPastPayment);
        JsonObject workDocument =
                toJsonWorkDocument(documentDate, entryDate, debtFenix, "ND", true, new DateTime(Utils.getDueDate(event)));

        json.add("workingDocument", workDocument);
        return json;
    }

    private JsonObject toJsonInvoice(final ExternalClient externalClient, final Money debtFenix,
                                     final DateTime documentDate, final DateTime entryDate, final boolean isDebtRegistration,
                                     final boolean isInterest, final String pledgeNumber) {

        final JsonObject clientData = toJsonClient(externalClient);
        JsonObject json = toJson(event, clientData, documentDate, isDebtRegistration, true, isInterest, false, false);
        JsonObject workDocument =
                toJsonWorkDocument(documentDate, entryDate, debtFenix, "ND", true, new DateTime(Utils.getDueDate(event)));
        if (!Strings.isNullOrEmpty(pledgeNumber)) {
            workDocument.addProperty("metadata", "{\"COMPROMISSO\":\"" + pledgeNumber + "\"}");
        }

        json.add("workingDocument", workDocument);
        return json;
    }

    private JsonObject toJsonDebt(Event event, Money debtFenix, String clientId, DateTime documentDate, DateTime entryDate,
                                  boolean isDebtRegistration, String docType, boolean isToDebit, boolean isNewDate, String originalMetadata) {
        final JsonObject clientData = toJsonClient(event.getParty(), clientId);
        JsonObject json = toJson(event, clientData, documentDate, isDebtRegistration, isNewDate, false, false, false);
        JsonObject workDocument =
                toJsonWorkDocument(documentDate, entryDate, debtFenix, docType, isToDebit, new DateTime(Utils.getDueDate(event)));

        if (originalMetadata == null) {
            final LocalDate[] debtInterval = getDebtInterval(documentDate, isNewDate);
            final ExecutionYear executionYear = Utils.executionYearOf(event);
            String metadata = String.format("{\"ANO_LECTIVO\":\"%s\", \"START_DATE\":\"%s\", \"END_DATE\":\"%s\"}",
                    executionYear.getName(), debtInterval[0].toString("yyyy-MM-dd"), debtInterval[1].toString("yyyy-MM-dd"));
            workDocument.addProperty("metadata", metadata);
        } else {
            workDocument.addProperty("metadata", originalMetadata);
        }

        json.add("workingDocument", workDocument);
        return json;
    }

    private LocalDate[] getDebtInterval(final DateTime documentDate, final boolean isNewDate) {
        LocalDate startDate = isNewDate ? currentDate : documentDate.toLocalDate();
        final LocalDate endDate;
        if (event instanceof PhdGratuityEvent) {
            PhdGratuityEvent phdEvent = (PhdGratuityEvent) event;
            final LocalDate localDate = phdEvent.getPhdGratuityDate().getYear() == phdEvent.getYear() ?
                    phdEvent.getPhdGratuityDate().toLocalDate() : phdEvent.getWhenOccured().toLocalDate();
            endDate = localDate.plusYears(1);
        } else {
            final ExecutionYear executionYear = Utils.executionYearOf(event);
            if (startDate.isBefore(executionYear.getBeginLocalDate())) {
                startDate = executionYear.getBeginLocalDate();
            }
            endDate = executionYear.getEndDateYearMonthDay().toLocalDate();
        }
        return startDate.isAfter(endDate) ? null : new LocalDate[]{startDate, endDate};
    }

    private JsonObject toJsonDebtCredit(Event event, Money debtFenix, String clientId, DateTime documentDate, DateTime entryDate,
                                        boolean isDebtRegistration, String docType, boolean isToDebit, boolean isNewDate, SapRequest debtRequest) {
        JsonObject request = new JsonParser().parse(debtRequest.getRequest()).getAsJsonObject();
        String originalMetadata = request.get("workingDocument").getAsJsonObject().get("metadata").getAsString();
        JsonObject json = toJsonDebt(event, debtFenix, clientId, documentDate, entryDate, isDebtRegistration, docType, isToDebit,
                isNewDate, originalMetadata);
        JsonObject workingDocument = json.get("workingDocument").getAsJsonObject();
        workingDocument.addProperty("workOriginDocNumber", debtRequest.getDocumentNumber());
        return json;
    }

    private JsonObject toJsonWorkDocument(DateTime documentDate, DateTime entryDate, Money amount, String documentType,
                                          boolean isToDebit, DateTime dueDate) {
        JsonObject workDocument = new JsonObject();
        workDocument.addProperty("documentDate", documentDate.toString(GiafInvoiceConfiguration.DT_FORMAT));
        workDocument.addProperty("entryDate", entryDate.toString(GiafInvoiceConfiguration.DT_FORMAT));
        workDocument.addProperty("dueDate", dueDate.toString(GiafInvoiceConfiguration.DT_FORMAT));
        workDocument.addProperty("workingDocumentNumber", documentType + getDocumentNumber());
        workDocument.addProperty("sourceBilling", SAFTPTSourceBilling.P.toString());
        workDocument.addProperty("workingAmount", amount.getAmountAsString());
        workDocument.addProperty("taxPayable", BigDecimal.ZERO);
        workDocument.addProperty("workType", "DC");
        workDocument.addProperty("workStatus", "N");

        workDocument.addProperty("isToDebit", isToDebit);
        workDocument.addProperty("isToCredit", !isToDebit);

        workDocument.addProperty("taxExemptionReason", "M99");
        workDocument.addProperty("unitOfMeasure", "UNID");

        return workDocument;
    }

    public JsonObject toJson(final Event event, final JsonObject clientData, DateTime documentDate, boolean isDebtRegistration,
                             boolean isNewDate, boolean isInterest, boolean isAdvancement, boolean isPastPayment) {
        final JsonObject json = toJsonCommon(documentDate, isNewDate);

        final String description = event.getDescription().toString();
        final SimpleImmutableEntry<String, String> product = mapToProduct(event, description, isDebtRegistration, isInterest, isAdvancement, isPastPayment);
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
        json.addProperty("processId", PROCESS_ID);
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
        json.addProperty("fromDate", isNewDate ? new DateTime().toString(GiafInvoiceConfiguration.DT_FORMAT)
                : documentDate.toString(GiafInvoiceConfiguration.DT_FORMAT));
        json.addProperty("toDate", new DateTime().toString(GiafInvoiceConfiguration.DT_FORMAT)); //tem impacto no ano fiscal!!!
        json.addProperty("productCompanyTaxId", "999999999");
        json.addProperty("productId", "FenixEdu/FenixEdu");
        json.addProperty("productVersion", "5.0.0.0");
        json.addProperty("softwareCertificateNumber", 0);
        json.addProperty("taxAccountingBasis", "P");
        json.addProperty("taxEntity", "Global");
        json.addProperty("taxRegistrationNumber", IST_VAT_NUMBER);
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
                physicalAddress != null && physicalAddress.getAddress() != null && !Strings.isNullOrEmpty(physicalAddress.getAddress().trim()) ?
                        Utils.limitFormat(MAX_SIZE_ADDRESS, physicalAddress.getAddress()) : MORADA_DESCONHECIDO);

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
        clientData.addProperty("vatNumber", externalClient.getFiscalCountry() + externalClient.getVatNumber());
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

    private void checkValidDocumentNumber(String documentNumber, Event event) {
        if ('0' == documentNumber.charAt(2)) {
            throw new Error("Houve uma tentativa de efectuar uma operação sobre o documento: " + documentNumber
                    + " - evento: " + event.getExternalId());
        }
    }

    private Long getDocumentNumber() {
        return SapRoot.getInstance().getAndSetNextDocumentNumber();
    }

    private String getPaymentMethodReference(AccountingTransactionDetail transactionDetail) {
        return transactionDetail.getPaymentReference();
    }

    private String getPaymentMechanism(AccountingTransactionDetail transactionDetail) {
        return transactionDetail.getPaymentMethod().getCode();
    }

    /**
     * Returns the open invoices and the remaining value of the first open invoice
     * The list is ordered, the first open invoice is the first of the list
     *
     * @return
     */
    private SortedMap<SapRequest, Money> getOpenInvoicesAndRemainingValue() {
        final SortedMap<SapRequest, Money> openInvoiceMap = new TreeMap<>(SapRequest.COMPARATOR_BY_EVENT_AND_ORDER);
        return getInvoiceEntries().sorted(SapRequest.COMPARATOR_BY_ORDER)
                .filter(sr -> sr.openInvoiceValue().isPositive())
                .collect(Collectors.toMap(sr -> sr, sr -> sr.openInvoiceValue(), (sr1, sr2) -> sr1, () -> openInvoiceMap));
    }

    private Stream<SapRequest> getInvoiceEntries() {
        return getFilteredSapRequestStream().filter(sr -> sr.getRequestType() == SapRequestType.INVOICE);
    }

    private Stream<SapRequest> getPaymentsAndCreditsFor(SapRequest request) {
        return getFilteredSapRequestStream()
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.CREDIT)
                .filter(sr -> sr.refersToDocument(request.getDocumentNumber())).sorted(SapRequest.COMPARATOR_BY_ORDER);
    }

    /**
     * Returns the open debts and the remaining value of the first open debt
     * The list is ordered, the first open debt is the first of the list
     *
     * @return
     */
    private SimpleImmutableEntry<List<SapRequest>, Money> getOpenDebtsAndRemainingValue() {
        List<SapRequest> debtEntries = getDebtEntries().sorted(SapRequest.COMPARATOR_BY_ORDER).collect(Collectors.toList());
        Money debtAmount = Money.ZERO;
        Money firstRemainingValue = Money.ZERO;
        Money totalAmount = getDebtCreditAmount();
        List<SapRequest> openDebtEntries = new ArrayList<SapRequest>();
        for (SapRequest debtEntry : debtEntries) {
            debtAmount = debtAmount.add(debtEntry.getValue());
            if (debtAmount.greaterOrEqualThan(totalAmount)) {
                if (firstRemainingValue.isZero()) {
                    firstRemainingValue = debtAmount.subtract(totalAmount);
                }
                openDebtEntries.add(debtEntry);
            }
        }
        return new SimpleImmutableEntry<>(openDebtEntries, firstRemainingValue);
    }

    private Stream<SapRequest> getDebtEntries() {
        return getFilteredSapRequestStream().filter(sr -> sr.getRequestType() == SapRequestType.DEBT)
                .filter(sr -> sr.getValue().isPositive());
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
        JsonArray errorMessages = new JsonArray();
        for (int iter = 0; iter < jsonArray.size(); iter++) {
            JsonObject json = jsonArray.get(iter).getAsJsonObject();
            if (!"S".equals(json.get("status").getAsString())) {
                checkStatus = false;
                String errorMessage = json.get("errorDescription").getAsString();
                JsonObject error = logError(event, errorLog, elogger, errorMessage, json.get("documentNumber").getAsString(), action, sapRequest);
                errorMessages.add(error);
            }
        }
        if (errorMessages.size() != 0) {
            sapRequest.addIntegrationMessage("Documento", errorMessages);
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

    public boolean hasPayment(final String transactionId) {
        return getPaymentsFor(transactionId).findAny().isPresent();
    }

    private Stream<SapRequest> getPaymentsFor(final String transactionDetailId) {
        return getFilteredSapRequestStream()
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.PAYMENT_INTEREST || sr.getRequestType() == SapRequestType.ADVANCEMENT)
                .filter(sr -> sr.getPayment() != null).filter(sr -> transactionDetailId.equals(sr.getPayment().getExternalId()));
    }

    private Stream<SapRequest> getAdvancementPaymentsFor(final String transactionDetailId) {
        return getFilteredSapRequestStream()
                .filter(sr -> sr.getRequestType() == SapRequestType.ADVANCEMENT)
                .filter(sr -> sr.getPayment() != null && transactionDetailId.equals(sr.getPayment().getExternalId()));
    }

    public boolean hasInterestPayment(final AccountingTransaction transaction) {
        return getFilteredSapRequestStream().filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                .filter(sr -> transaction == sr.getPayment()).findAny().isPresent();
    }

    public boolean hasCredit(String creditId) {
        return getFilteredSapRequestStream().filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
                .anyMatch(sr -> creditId.equals(sr.getCreditId()));
    }

    public boolean hasRefund(final String refundId) {
        return getFilteredSapRequestStream().filter(sr -> sr.getRequestType() == SapRequestType.REIMBURSEMENT)
                .anyMatch(sr -> refundId.equals(sr.getRefund().getExternalId()));
    }

    public static SimpleImmutableEntry<String, String> mapToProduct(Event event, String eventDescription,
                                                                    boolean isDebtRegistration, boolean isInterest,
                                                                    boolean isAdvancement, boolean isPastEvent) {
        if (isInterest) {
            return new SimpleImmutableEntry<String, String>("0036", "MULTAS");
        }
        if (isAdvancement) {
            return new SimpleImmutableEntry<String, String>("0056", "ADIANTAMENTO");
        }
        if (isPastEvent) {
            return new SimpleImmutableEntry<String, String>("0063", "REGULARIZAÇAO ANOS ANTERIORES");
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

    private JsonObject logError(Event event, ErrorLogConsumer errorLog, EventLogger elogger, String errorMessage, String documentNumber,
                                String action, SapRequest sr) {
        BigDecimal amount = sr.getValue() != null ? sr.getValue().getAmount() : BigDecimal.ZERO;
        amount = amount.add(sr.getAdvancement() != null ? sr.getAdvancement().getAmount() : BigDecimal.ZERO);
        DebtCycleType cycleType = Utils.cycleType(event);
        final Party party = event.getParty();

        errorLog.accept(event.getExternalId(), Utils.getUserIdentifier(party), party.getName(),
                amount.toPlainString(), cycleType == null ? "" : cycleType.getDescription(), errorMessage,
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

        return returnMessage;
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

    public SapRequest fakeSapRequest(final SapRequestType requestType, final String documentNumber, final Money amount, final String creditId) {
        final String clientId = ClientMap.uVATNumberFor(event.getParty());
        final SapRequest sapRequest = new SapRequest(event, clientId, amount, documentNumber, requestType, Money.ZERO, new JsonObject());
        sapRequest.setCreditId(creditId);
        sapRequest.setSent(true);
        sapRequest.setWhenSent(new DateTime());
        sapRequest.setIntegrated(true);
        return sapRequest;
    }

    @Atomic
    public void resendSapRequest(final ErrorLogConsumer errorLogConsumer, final EventLogger elogger, final SapRequest sapRequest) {
        sapRequest.setIntegrated(false);
        sapRequest.getSapDocumentFile().setSapRequest(null);
        processPendingRequests(sapRequest, errorLogConsumer, elogger);
    }

}
