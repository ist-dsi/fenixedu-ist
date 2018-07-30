package pt.ist.fenixedu.giaf.invoices;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.datetime.joda.DateTimeFormatterFactory;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixedu.domain.SapDocumentFile;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.util.PostalCodeValidator;
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
    private static final DateTimeFormatter localDateFormatter =
            new DateTimeFormatterFactory("yyyy-MM-dd").createDateTimeFormatter();
    public LocalDate currentDate = new LocalDate();

    public Event event = null;

    public SapEvent(final Event event) {
        this.event = event;
    }

    public boolean registerInvoice(Money debtFenix, Event event, boolean isGratuity, boolean isNewDate, ErrorLogConsumer errorLog,
            EventLogger elogger) throws Exception {

        if (isToProcessDebt(event, isGratuity)) {
            //if debt is greater than invoice, then there was a debt registered and the correspondent invoice failed, don't register the debt again
            if (!getDebtAmount().greaterThan(getInvoiceAmount())) {
                boolean result = registerDebt(debtFenix, event, isNewDate, errorLog, elogger);
                if (!result) { //if the debt register didn't went ok we wont register the invoice
                    return result;
                }
            }
        }

        String clientId = ClientMap.uVATNumberFor(event.getParty());
        JsonObject data = toJsonInvoice(event, debtFenix, getDocumentDate(event.getWhenOccured()), new DateTime(), clientId,
                false, isNewDate, false);

        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest =
                new SapRequest(event, clientId, debtFenix, documentNumber, SapRequestType.INVOICE, Money.ZERO, data);
        JsonObject result = sendDataToSap(sapRequest, data);

        if (checkAndRegisterIntegration(event, errorLog, elogger, data, documentNumber, sapRequest, result,
                SapRequestType.INVOICE.name(), true)) {
            // if there are amounts in advancement we need to register them in the new invoice
            //TODO if we do this the value registered in the advancement will be counted 2 times in different categories
//            Money advancementAmount = getAdvancementAmount();
//            if (advancementAmount.isPositive()) {
//                return registerPaymentFromAdvancement(event, clientId, advancementAmount, sapRequest, errorLog, elogger);
//            } else {
//                return true;
//            }
            return true;
        } else {
            return false;
        }
    }

    private boolean registerPaymentFromAdvancement(Event event, String clientId, Money advancementAmount,
            SapRequest invoiceRequest, ErrorLogConsumer errorLog, EventLogger elogger) throws Exception {

        checkValidDocumentNumber(invoiceRequest.getDocumentNumber(), event);

        // ver se o valor da divida é superior ou igual ao advancement, se for tudo ok, caso contrário é registar o pagamento no valor da factura
        // e abater esse valor ao advancement
        Money amountToRegister = advancementAmount;
        if (advancementAmount.greaterThan(invoiceRequest.getValue())) {
            amountToRegister = invoiceRequest.getValue();
        }

        JsonObject data = toJsonPaymentFromAdvancement(event, invoiceRequest.getDocumentNumber(), clientId, amountToRegister);
        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest = new SapRequest(event, clientId, amountToRegister, documentNumber, SapRequestType.PAYMENT,
                amountToRegister.negate(), data);
        JsonObject result = sendDataToSap(sapRequest, data);

        return checkAndRegisterIntegration(event, errorLog, elogger, data, documentNumber, sapRequest, result,
                "PaymentFromAdvancement", true);
    }

    private boolean registerDebt(Money debtFenix, Event event, boolean isNewDate, ErrorLogConsumer errorLog, EventLogger elogger)
            throws Exception {

        String clientId = ClientMap.uVATNumberFor(event.getParty());
        JsonObject data = toJsonDebt(event, debtFenix, clientId, getDocumentDate(event.getWhenOccured()), new DateTime(), true,
                "NG", true, isNewDate);

        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest = new SapRequest(event, clientId, debtFenix, documentNumber, SapRequestType.DEBT, Money.ZERO, data);
        JsonObject result = sendDataToSap(sapRequest, data);

        return checkAndRegisterIntegration(event, errorLog, elogger, data, documentNumber, sapRequest, result,
                SapRequestType.DEBT.name(), false);
    }

    private boolean registerDebtCredit(CreditEntry creditEntry, Event event, boolean isNewDate, ErrorLogConsumer errorLog,
            EventLogger elogger) throws Exception {

        String clientId = ClientMap.uVATNumberFor(event.getParty());
        SimpleImmutableEntry<List<SapRequest>, Money> openDebtsAndRemainingValue = getOpenDebtsAndRemainingValue();
        List<SapRequest> openDebts = openDebtsAndRemainingValue.getKey();
        Money remainingAmount = openDebtsAndRemainingValue.getValue();
        if (creditEntry.getAmount().compareTo(remainingAmount.getAmount()) == 1) {
            if (openDebts.size() > 1) {
                // dividir o valor da isenção pelas várias dívidas
                return registerDebtCreditList(event, openDebts, new Money(creditEntry.getAmount()), creditEntry, remainingAmount,
                        clientId, isNewDate, errorLog, elogger);
            } else {
                // o valor da isenção é superior ao valor em dívida
                String debtNumber = "";
                if (openDebts.size() == 1) { // mas só existe uma dívida abertura
                    debtNumber = openDebts.get(0).getDocumentNumber();
                } else { // não existe nenhuma dívida aberta, ir buscar a última
                    debtNumber = getLastDebtNumber();
                }
                return registerDebtCredit(clientId, event, new Money(creditEntry.getAmount()), creditEntry, debtNumber, isNewDate,
                        errorLog, elogger);
            }
        } else {
            //tudo normal
            return registerDebtCredit(clientId, event, new Money(creditEntry.getAmount()), creditEntry,
                    openDebts.get(0).getDocumentNumber(), isNewDate, errorLog, elogger);
        }
    }

    private boolean registerDebtCreditList(Event event, List<SapRequest> openDebts, Money amountToRegister,
            CreditEntry creditEntry, Money remainingAmount, String clientId, boolean isNewDate, ErrorLogConsumer errorLog,
            EventLogger elogger) throws Exception {
        if (amountToRegister.greaterThan(remainingAmount)) {
            if (openDebts.size() > 1) {
                boolean result = registerDebtCredit(clientId, event, remainingAmount, creditEntry,
                        openDebts.get(0).getDocumentNumber(), isNewDate, errorLog, elogger);
                if (!result) { //if the first debt register didn't went ok, we abort the next ones
                    return result;
                }
                return registerDebtCreditList(event, openDebts.subList(1, openDebts.size()),
                        amountToRegister.subtract(remainingAmount), creditEntry, openDebts.get(1).getValue(), clientId, isNewDate,
                        errorLog, elogger);
            } else {
                return registerDebtCredit(clientId, event, amountToRegister, creditEntry, openDebts.get(0).getDocumentNumber(),
                        isNewDate, errorLog, elogger);
            }
        } else {
            return registerDebtCredit(clientId, event, amountToRegister, creditEntry, openDebts.get(0).getDocumentNumber(),
                    isNewDate, errorLog, elogger);
        }
    }

    private boolean registerDebtCredit(String clientId, Event event, Money amountToRegister, CreditEntry creditEntry,
            String debtCreditDocNumber, boolean isNewDate, ErrorLogConsumer errorLog, EventLogger elogger) throws Exception {
        checkValidDocumentNumber(debtCreditDocNumber, event);

        JsonObject data = toJsonDebtCredit(event, amountToRegister, clientId, getDocumentDate(creditEntry.getCreated()),
                new DateTime(), true, "NJ", false, isNewDate, debtCreditDocNumber);
        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest =
                new SapRequest(event, clientId, amountToRegister, documentNumber, SapRequestType.DEBT_CREDIT, Money.ZERO, data);
        sapRequest.setCreditId(creditEntry.getId());
        JsonObject result = sendDataToSap(sapRequest, data);

        return checkAndRegisterIntegration(event, errorLog, elogger, data, documentNumber, sapRequest, result,
                SapRequestType.DEBT_CREDIT.name(), false);
    }

    public boolean registerCredit(Event event, CreditEntry creditEntry, boolean isGratuity, ErrorLogConsumer errorLog,
            EventLogger elogger) throws Exception {
        // diminuir divida no sap (se for propina diminuir dívida) e credit note na última factura existente
        // se o valor pago nesta factura for superior à nova dívida, o que fazer? terá que existir nota crédito no fenix -> sim

        if (isToProcessDebt(event, isGratuity)) {
            //if the debt credit amount is greater than the credit amount it means that a credit debt was registered but the correspondent invoice credit failed
            //we don't register the credit debt again
            if (!getDebtCreditAmount().greaterThan(getCreditAmount())) {
                boolean result = registerDebtCredit(creditEntry, event, true, errorLog, elogger);
                if (!result) { //if the debt credit didn't went ok, we abort the credit register
                    return result;
                }
            }
        }

        String clientId = ClientMap.uVATNumberFor(event.getParty());

        SimpleImmutableEntry<List<SapRequest>, Money> openInvoicesAndRemainingValue = getOpenInvoicesAndRemainingValue();
        List<SapRequest> openInvoices = openInvoicesAndRemainingValue.getKey();
        Money remainingAmount = openInvoicesAndRemainingValue.getValue();
        if (creditEntry.getAmount().compareTo(remainingAmount.getAmount()) == 1) {
            if (openInvoices.size() > 1) {
                // dividir o valor da isenção pelas várias facturas....
                return registerCreditList(event, openInvoices, creditEntry, new Money(creditEntry.getAmount()), remainingAmount,
                        clientId, errorLog, elogger);
            } else {
                // o valor da isenção é superior ao valor em dívida
                String invoiceNumber = "";
                if (openInvoices.size() == 1) { // mas só existe uma factura abertura
                    invoiceNumber = openInvoices.get(0).getDocumentNumber();
                } else { // não existe nenhuma factura aberta, ir buscar a última
                    invoiceNumber = getLastInvoiceNumber();
                }
                return registerCredit(clientId, event, creditEntry, new Money(creditEntry.getAmount()), invoiceNumber, errorLog,
                        elogger);
            }
        } else {
            //tudo normal
            return registerCredit(clientId, event, creditEntry, new Money(creditEntry.getAmount()),
                    openInvoices.get(0).getDocumentNumber(), errorLog, elogger);
        }
    }

    private boolean registerCreditList(Event event, List<SapRequest> openInvoices, CreditEntry creditEntry,
            Money amountToRegister, Money remainingAmount, String clientId, ErrorLogConsumer errorLog, EventLogger elogger)
            throws Exception {
        if (amountToRegister.greaterThan(remainingAmount)) {
            if (openInvoices.size() > 1) {
                boolean result = registerCredit(clientId, event, creditEntry, remainingAmount,
                        openInvoices.get(0).getDocumentNumber(), errorLog, elogger);
                if (!result) { //if the first credit register didn't went ok, we abort the next ones
                    return result;
                }
                return registerCreditList(event, openInvoices.subList(1, openInvoices.size()), creditEntry,
                        amountToRegister.subtract(remainingAmount), openInvoices.get(1).getValue(), clientId, errorLog, elogger);
            } else {
                return registerCredit(clientId, event, creditEntry, amountToRegister, openInvoices.get(0).getDocumentNumber(),
                        errorLog, elogger);
            }
        } else {
            return registerCredit(clientId, event, creditEntry, amountToRegister, openInvoices.get(0).getDocumentNumber(),
                    errorLog, elogger);
        }
    }

    private boolean registerCredit(String clientId, Event event, CreditEntry creditEntry, Money creditAmount,
            String invoiceNumber, ErrorLogConsumer errorLog, EventLogger elogger) throws Exception {
        checkValidDocumentNumber(invoiceNumber, event);

        JsonObject data = toJsonCredit(event, getDocumentDate(creditEntry.getCreated()), creditAmount, clientId, invoiceNumber,
                false, true);
        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest =
                new SapRequest(event, clientId, creditAmount, documentNumber, SapRequestType.CREDIT, Money.ZERO, data);
        sapRequest.setCreditId(creditEntry.getId());

        JsonObject result = sendDataToSap(sapRequest, data);

        return checkAndRegisterIntegration(event, errorLog, elogger, data, documentNumber, sapRequest, result,
                SapRequestType.CREDIT.name(), true);
    }

    public boolean registerReimbursement(Event event, Money amount, ErrorLogConsumer errorLog, EventLogger elogger)
            throws Exception {

        String invoiceNumber = getLastInvoiceNumber();
        checkValidDocumentNumber(invoiceNumber, event);

        String clientId = ClientMap.uVATNumberFor(event.getParty());
        JsonObject data = toJsonReimbursement(event, amount, clientId, invoiceNumber, false, true);

        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest =
                new SapRequest(event, clientId, amount, documentNumber, SapRequestType.REIMBURSEMENT, Money.ZERO, data);
        JsonObject result = sendDataToSap(sapRequest, data);

        return checkAndRegisterIntegration(event, errorLog, elogger, data, documentNumber, sapRequest, result,
                SapRequestType.REIMBURSEMENT.name(), true);
    }

    private boolean registerInterest(final Money payedInterest, final String clientId,
            final AccountingTransactionDetail transactionDetail, final ErrorLogConsumer errorLog, final EventLogger elogger)
            throws Exception {

        if (hasInterestPayment(transactionDetail.getTransaction())) {
            return true; //both invoice and payment requests have been integrated
        }

        String invoiceNumber = null;
        SapRequest interestInvoiceRequest = getInterestInvoiceRequest(transactionDetail.getTransaction());
        if (interestInvoiceRequest != null) {
            // the interest invoice was integrated but for some reason the interest payment request was not created
            invoiceNumber = interestInvoiceRequest.getDocumentNumber();
        } else {
            // registering the invoice
            invoiceNumber = registerInterestInvoice(payedInterest, clientId, transactionDetail,
                    getDocumentDate(transactionDetail.getWhenProcessed()), errorLog, elogger);
        }

        // even if the invoice fails (in the integration with SAP) we want to register the request
        // so that when the invoice interest is integrated the pending interest payment is resent and integrated also 
        // registering the payment
        return registerInterestPayment(payedInterest, clientId, transactionDetail, errorLog, elogger, invoiceNumber);
    }

    private String registerInterestInvoice(final Money payedInterest, final String clientId,
            final AccountingTransactionDetail transactionDetail, DateTime paymentDate, final ErrorLogConsumer errorLog,
            final EventLogger elogger) throws Exception {
        JsonObject data = toJsonInvoice(transactionDetail.getEvent(), payedInterest, paymentDate, new DateTime(), clientId, false,
                true, true);

        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest = new SapRequest(transactionDetail.getEvent(), clientId, payedInterest, documentNumber,
                SapRequestType.INVOICE_INTEREST, Money.ZERO, data);
        sapRequest.setPayment(transactionDetail.getTransaction());
        JsonObject result = sendDataToSap(sapRequest, data);

        if (checkAndRegisterIntegration(transactionDetail.getEvent(), errorLog, elogger, data, documentNumber, sapRequest, result,
                SapRequestType.INVOICE_INTEREST.name(), true)) {
            return documentNumber;
        } else {
            return null;
        }
    }

    private boolean registerInterestPayment(final Money payedInterest, final String clientId,
            final AccountingTransactionDetail transactionDetail, final ErrorLogConsumer errorLog, final EventLogger elogger,
            String invoiceNumber) throws Exception {
        JsonObject data = toJsonPayment(transactionDetail, payedInterest, invoiceNumber, clientId, true);

        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest = new SapRequest(transactionDetail.getEvent(), clientId, payedInterest, documentNumber,
                SapRequestType.PAYMENT_INTEREST, Money.ZERO, data);
        sapRequest.setPayment(transactionDetail.getTransaction());
        JsonObject result = sendDataToSap(sapRequest, data);

        return checkAndRegisterIntegration(transactionDetail.getEvent(), errorLog, elogger, data, documentNumber, sapRequest,
                result, SapRequestType.PAYMENT_INTEREST.name(), true);
    }

    public boolean registerPayment(Payment payment, ErrorLogConsumer errorLog, EventLogger elogger) throws Exception {

        AccountingTransactionDetail transactionDetail =
                ((AccountingTransaction) FenixFramework.getDomainObject(payment.getId())).getTransactionDetail();
        String clientId = ClientMap.uVATNumberFor(transactionDetail.getEvent().getParty());

        // ir buscar a ultima factura aberta e verificar se o pagamento ultrapassa o valor da factura
        // e associar o restante à(s) factura(s) seguinte(s)
        SimpleImmutableEntry<List<SapRequest>, Money> openInvoicesAndRemainingAmount = getOpenInvoicesAndRemainingValue();

        Money payedAmount = new Money(payment.getUsedAmountInDebts());
        Money firstRemainingAmount = openInvoicesAndRemainingAmount.getValue();
        List<SapRequest> openInvoices = openInvoicesAndRemainingAmount.getKey();

        final Money payedInterest = new Money(payment.getUsedAmountInInterests().add(payment.getUsedAmountInFines()));
        if (payedInterest.isPositive()) {
            boolean result = registerInterest(payedInterest, clientId, transactionDetail, errorLog, elogger);
            if (!result) {
                return result;
            }
        }

        if (payedAmount.isZero()) {
            if (payedInterest.isZero()) {
                //there was a payment made and the debt is already closed, it's an extra payment (advancement)
                payedAmount = new Money(payment.getAmount());
            } else {
                //it was all used for interests, there is nothing more to register
                return true;
            }
        }

        if (firstRemainingAmount.isZero()) {
            // não há facturas abertas, fazer adiantamento, sobre a última factura!!
            return registerAdvancement(Money.ZERO, payedAmount, getLastInvoiceNumber(), clientId, transactionDetail, errorLog,
                    elogger);
        }

        if (firstRemainingAmount.lessThan(payedAmount)) {
            // quer dizer que ou há outra factura aberta ou é um pagamento em excesso
            // dividir o valor pago pela facturas e registar n pagamentos ou registar um pagamento adiamento

            if (openInvoices.size() == 1) {
                // só há uma factura aberta -> fazer adiantamento
                String invoiceNumber = openInvoices.get(0).getDocumentNumber();
                return registerAdvancement(firstRemainingAmount, payedAmount.subtract(firstRemainingAmount), invoiceNumber,
                        clientId, transactionDetail, errorLog, elogger);
            } else {
                // vai distribuir o pagamento pelas restantes facturas abertas
                return registerPaymentList(openInvoices, payedAmount, firstRemainingAmount, clientId, transactionDetail, errorLog,
                        elogger);
            }
        } else {
            // tudo ok, é só registar o pagamento
            return registerPayment(transactionDetail, payedAmount, openInvoices.get(0).getDocumentNumber(), clientId, errorLog,
                    elogger);
        }
    }

    private boolean registerPaymentList(List<SapRequest> openInvoices, Money amountToRegister, Money remainingAmount,
            String clientId, AccountingTransactionDetail transactionDetail, ErrorLogConsumer errorLog, EventLogger elogger)
            throws Exception {
        if (amountToRegister.greaterThan(remainingAmount)) {
            if (openInvoices.size() > 1) {
                boolean successful = registerPayment(transactionDetail, remainingAmount, openInvoices.get(0).getDocumentNumber(),
                        clientId, errorLog, elogger);
                if (successful) {
                    return registerPaymentList(openInvoices.subList(1, openInvoices.size()),
                            amountToRegister.subtract(remainingAmount), openInvoices.get(1).getValue(), clientId,
                            transactionDetail, errorLog, elogger);
                } else {
                    return false;
                }
            } else {
                // neste ponto sabemos sempre que existe pelo menos uma factura em aberto e que o remaining amout nunca é zero
                // portanto se não entrou no if de cima quer dizer que só existe uma factura aberta
                // registar adiantamento
                String invoiceNumber = openInvoices.get(0).getDocumentNumber();
                return registerAdvancement(remainingAmount, amountToRegister.subtract(remainingAmount), invoiceNumber, clientId,
                        transactionDetail, errorLog, elogger);
            }
        } else {
            return registerPayment(transactionDetail, amountToRegister, openInvoices.get(0).getDocumentNumber(), clientId,
                    errorLog, elogger);
        }
    }

    private boolean registerPayment(AccountingTransactionDetail transactionDetail, Money payedAmount, String invoiceNumber,
            String clientId, ErrorLogConsumer errorLog, EventLogger elogger) throws Exception {

        checkValidDocumentNumber(invoiceNumber, transactionDetail.getEvent());

        JsonObject data = toJsonPayment(transactionDetail, payedAmount, invoiceNumber, clientId, false);
        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest = new SapRequest(transactionDetail.getEvent(), clientId, payedAmount, documentNumber,
                SapRequestType.PAYMENT, Money.ZERO, data);
        sapRequest.setPayment(transactionDetail.getTransaction());

        JsonObject result = sendDataToSap(sapRequest, data);

        if (checkAndRegisterIntegration(transactionDetail.getEvent(), errorLog, elogger, data, documentNumber, sapRequest, result,
                SapRequestType.PAYMENT.name(), true)) {
            return true;
        } else {
            if (hasPayment(transactionDetail.getTransaction(), sapRequest)) {
                //TODO já existe um pagamento feito com este id (noutro request) o que quer dizer que isto é um pagamento que se dividiu por n facturas e um deles deu erro
                // este pagamento não vai voltar a ser processado e é preciso dar uma mensagem de erro diferente para isto ser corrigido manualmente
                logError(transactionDetail.getEvent(), errorLog, elogger, "WARNING: one of multiple payments has failed!!!",
                        documentNumber, SapRequestType.PAYMENT.name(), sapRequest);
            }
            return false;
        }
    }

    private boolean registerAdvancement(Money amount, Money advancement, String invoiceNumber, String clientId,
            AccountingTransactionDetail transactionDetail, ErrorLogConsumer errorLog, EventLogger elogger) throws Exception {
        checkValidDocumentNumber(invoiceNumber, transactionDetail.getEvent());

        JsonObject data = toJsonAdvancement(amount, advancement, invoiceNumber, clientId, transactionDetail);
        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest = new SapRequest(transactionDetail.getEvent(), clientId, amount, documentNumber,
                SapRequestType.ADVANCEMENT, advancement, data);
        sapRequest.setPayment(transactionDetail.getTransaction());
        JsonObject result = sendDataToSap(sapRequest, data);

        return checkAndRegisterIntegration(transactionDetail.getEvent(), errorLog, elogger, data, documentNumber, sapRequest,
                result, SapRequestType.ADVANCEMENT.name(), true);
    }

    public boolean processPendingRequests(Event event, ErrorLogConsumer errorLog, EventLogger elogger) {
        Set<SapRequest> requests = new TreeSet<>(Comparator.comparing(SapRequest::getWhenCreated));
        requests.addAll(event.getSapRequestSet());
        for (SapRequest sr : requests) {
            if (!sr.getIntegrated()) {
                JsonParser jsonParser = new JsonParser();
                JsonObject data = (JsonObject) jsonParser.parse(sr.getRequest());

                JsonObject result = sendDataToSap(sr, data);

                return checkAndRegisterIntegration(event, errorLog, elogger, data, sr.getDocumentNumber(), sr, result,
                        sr.getRequestType().toString(), true);
            }
        }
        return true;
    }

    private boolean checkAndRegisterIntegration(Event event, ErrorLogConsumer errorLog, EventLogger elogger, JsonObject data,
            String documentNumber, SapRequest sapRequest, JsonObject result, String action, boolean getDocument) {
        if (result.get("exception") == null) {
            boolean docIsIntregrated = checkDocumentsStatus(result, sapRequest, event, errorLog, elogger, action);
            checkClientStatus(result, event, errorLog, elogger, action, data, sapRequest);
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
                sapRequest.setIntegrationMessage(EMPTY_JSON);
                return true;
            } else {
                return false;
            }
        } else {
            logError(event, errorLog, elogger, result.get("exception").getAsString(), documentNumber, action, sapRequest);
            return false;
        }
    }

    private DateTime getDocumentDate(DateTime documentDate) {
        if (documentDate.getYear() < currentDate.getYear()) {
            return new DateTime(currentDate.getYear() - 1, 12, 31, 23, 59);
        }
        return documentDate;
    }

    private boolean isToProcessDebt(Event event, boolean isGratuity) {
        return isGratuity && (event instanceof ExternalScholarshipPhdGratuityContribuitionEvent)
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
        try {
            result = SapFinantialClient.comunicate(data);
        } catch (Exception e) {
            e.printStackTrace();
            result = new JsonObject();
            result.addProperty("exception", responseFromException(e));
            return result;
        }
        sapRequest.setWhenSent(new DateTime());
        sapRequest.setSent(true);
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

    private JsonObject toJsonPayment(AccountingTransactionDetail transactionDetail, Money amount, String invoiceNumber,
            String clientId, boolean isInterest) throws Exception {
        JsonObject data =
                toJson(transactionDetail.getEvent(), clientId, transactionDetail.getWhenRegistered(), false, false, isInterest);
        JsonObject paymentDocument = toJsonPaymentDocument(amount, "NP", invoiceNumber, transactionDetail.getWhenRegistered(),
                getPaymentMechanism(transactionDetail), getPaymentMethodReference(transactionDetail),
                SAFTPTSettlementType.NL.toString(), true);

        data.add("paymentDocument", paymentDocument);
        return data;
    }

    private JsonObject toJsonPaymentFromAdvancement(Event event, String invoiceNumber, String clientId, Money amount)
            throws Exception {
        JsonObject data = toJson(event, clientId, new DateTime(), false, false, false);
        JsonObject paymentDocument = toJsonPaymentDocument(amount, "NP", invoiceNumber, new DateTime(), "OU", "",
                SAFTPTSettlementType.NN.toString(), true);
        paymentDocument.addProperty("excessPayment", amount.negate().toPlainString());//the payment amount must be zero

        data.add("paymentDocument", paymentDocument);
        return data;
    }

    private JsonObject toJsonAdvancement(Money amount, Money excess, String invoiceNumber, String clientId,
            AccountingTransactionDetail transactionDetail) throws Exception {
        JsonObject data =
                toJson(transactionDetail.getEvent(), clientId, transactionDetail.getWhenRegistered(), false, false, false);
        JsonObject paymentDocument = toJsonPaymentDocument(amount, "NP", invoiceNumber, transactionDetail.getWhenRegistered(),
                getPaymentMechanism(transactionDetail), getPaymentMethodReference(transactionDetail),
                SAFTPTSettlementType.NL.toString(), true);
        paymentDocument.addProperty("excessPayment", excess.toPlainString());
        paymentDocument.addProperty("isAdvancedPayment", true);

        JsonObject workingDocument = toJsonWorkDocument(getDocumentDate(transactionDetail.getWhenRegistered()), new DateTime(),
                excess, "NA", false, transactionDetail.getWhenRegistered());
        workingDocument.addProperty("isAdvancedPayment", true);
        workingDocument.addProperty("paymentDocumentNumber", paymentDocument.get("paymentDocumentNumber").getAsString());

        paymentDocument.addProperty("originatingOnDocumentNumber", workingDocument.get("workingDocumentNumber").getAsString());

        data.add("workingDocument", workingDocument);
        data.add("paymentDocument", paymentDocument);
        return data;
    }

    private JsonObject toJsonCredit(Event event, DateTime documentDate, Money creditAmount, String clientId, String invoiceNumber,
            boolean isDebtRegistration, boolean isNewDate) throws Exception {
        JsonObject json = toJson(event, clientId, new DateTime(), isDebtRegistration, isNewDate, false);
        JsonObject workDocument = toJsonWorkDocument(documentDate, new DateTime(), creditAmount, "NA", false, new DateTime());
        workDocument.addProperty("workOriginDocNumber", invoiceNumber);
        json.add("workingDocument", workDocument);

        String workingDocumentNumber = workDocument.get("workingDocumentNumber").getAsString();
        JsonObject paymentDocument = toJsonPaymentDocument(creditAmount, "NP", workingDocumentNumber, documentDate, "OU", "",
                SAFTPTSettlementType.NN.toString(), false);
        paymentDocument.addProperty("isCreditNote", true);
        paymentDocument.addProperty("paymentOriginDocNumber", invoiceNumber);
        paymentDocument.addProperty("excessPayment", creditAmount.negate().toPlainString());//the payment amount must be zero
        json.add("paymentDocument", paymentDocument);

        return json;
    }

    private JsonObject toJsonReimbursement(Event event, Money amount, String clientId, String invoiceNumber,
            boolean isDebtRegistration, boolean isNewDate) throws Exception {
        JsonObject json = toJson(event, clientId, new DateTime(), isDebtRegistration, isNewDate, false);
        JsonObject workDocument = toJsonWorkDocument(new DateTime(), new DateTime(), amount, "NA", false, new DateTime());
        workDocument.addProperty("workOriginDocNumber", invoiceNumber);
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
            boolean isDebtRegistration, boolean isNewDate, boolean isInterest) throws Exception {
        JsonObject json = toJson(event, clientId, documentDate, isDebtRegistration, true, isInterest);
        JsonObject workDocument =
                toJsonWorkDocument(documentDate, entryDate, debtFenix, "ND", true, new DateTime(Utils.getDueDate(event)));

        json.add("workingDocument", workDocument);
        return json;
    }

    private JsonObject toJsonDebt(Event event, Money debtFenix, String clientId, DateTime documentDate, DateTime entryDate,
            boolean isDebtRegistration, String docType, boolean isToDebit, boolean isNewDate) throws Exception {
        JsonObject json = toJson(event, clientId, documentDate, isDebtRegistration, isNewDate, false);
        JsonObject workDocument =
                toJsonWorkDocument(documentDate, entryDate, debtFenix, docType, isToDebit, new DateTime(Utils.getDueDate(event)));

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

        json.add("workingDocument", workDocument);
        return json;
    }

    private JsonObject toJsonDebtCredit(Event event, Money debtFenix, String clientId, DateTime documentDate, DateTime entryDate,
            boolean isDebtRegistration, String docType, boolean isToDebit, boolean isNewDate, String debtDocNumber)
            throws Exception {
        JsonObject json = toJsonDebt(event, debtFenix, clientId, documentDate, entryDate, isDebtRegistration, docType, isToDebit,
                isNewDate);
        JsonObject workingDocument = json.get("workingDocument").getAsJsonObject();
        workingDocument.addProperty("workOriginDocNumber", debtDocNumber);
        return json;
    }

    private JsonObject toJsonWorkDocument(DateTime documentDate, DateTime entryDate, Money amount, String documentType,
            boolean isToDebit, DateTime dueDate) throws Exception {
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

    public JsonObject toJson(final Event event, final String clientId, DateTime documentDate, boolean isDebtRegistration,
            boolean isNewDate, boolean isInterest) {
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
        SimpleImmutableEntry<String, String> product =
                mapToProduct(event, event.getDescription().toString(), isDebtRegistration, isInterest);
        json.addProperty("productDescription", product.getValue());
        json.addProperty("productCode", product.getKey());

        final JsonObject clientData = toJsonClient(event.getParty(), clientId);
        json.add("clientData", clientData);

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
        clientData.addProperty("postalCode",
                !Strings.isNullOrEmpty(postalCode) ? postalCode : PostalCodeValidator.examplePostCodeFor(countryCode));

        clientData.addProperty("vatNumber", Utils.limitFormat(MAX_SIZE_VAT_NUMBER, clientId));
        clientData.addProperty("fiscalCountry", countryCode);
        clientData.addProperty("nationality", party.getCountry().getCode());
        clientData.addProperty("billingIndicator", 0);

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

    private Long getDocumentNumber() throws Exception {
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
        return event.getSapRequestSet().stream().filter(sr -> sr.getRequestType() == SapRequestType.INVOICE);
    }

    private String getLastInvoiceNumber() {
        return getInvoiceEntries().max(SapRequest.DOCUMENT_NUMBER_COMPARATOR).map(SapRequest::getDocumentNumber).orElse("");
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
        return event.getSapRequestSet().stream().filter(sr -> sr.getRequestType() == SapRequestType.DEBT)
                .filter(sr -> sr.getValue().isPositive());
    }

    private String getLastDebtNumber() {
        Optional<SapRequest> findFirst = getDebtEntries().sorted(SapRequest.DOCUMENT_NUMBER_COMPARATOR.reversed()).findFirst();
        return findFirst.isPresent() ? findFirst.get().getDocumentNumber() : "";
    }

    private void checkClientStatus(JsonObject result, Event event, ErrorLogConsumer errorLog, EventLogger elogger, String action,
            JsonObject sentData, SapRequest sr) {
        JsonArray jsonArray = result.getAsJsonArray("customers");
        for (int iter = 0; iter < jsonArray.size(); iter++) {
            JsonObject json = jsonArray.get(iter).getAsJsonObject();
            if (!"S".equals(json.get("status").getAsString())) {
                logError(event, json.get("customerId").getAsString(), errorLog, elogger, json.get("returnMessage").getAsString(),
                        action, sentData, sr);
            }
        }
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
        return event.getSapRequestSet().stream().filter(sr -> sr.getRequestType().equals(sapRequestType))
                .map(SapRequest::getValue).reduce(Money.ZERO, Money::add);
    }

    public Money getDebtAmount() {
        return addAll(SapRequestType.DEBT);
//        return event.getSapRequestSet().stream().filter(sr -> sr.getRequestType().equals(SapRequestType.DEBT))
//                .filter(sr -> sr.getValue().isPositive()).map(sr -> sr.getValue()).reduce(Money.ZERO, Money::add);
    }

    public Money getDebtCreditAmount() {
        return addAll(SapRequestType.DEBT_CREDIT);
//        return event.getSapRequestSet().stream().filter(sr -> sr.getRequestType().equals(SapRequestType.DEBT))
//                .filter(sr -> sr.getValue().isNegative()).map(sr -> sr.getValue().abs()).reduce(Money.ZERO, Money::add);
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
        return event.getSapRequestSet().stream().filter(sr -> sr.getRequestType().equals(SapRequestType.ADVANCEMENT))
                .map(SapRequest::getAdvancement).reduce(Money.ZERO, Money::add);
    }

    public Money getFinesAmount() {
        return addAll(SapRequestType.FINE);
    }

    public Money getReimbursementsAmount() {
        return addAll(SapRequestType.REIMBURSEMENT);
    }

    public boolean hasPayment(final String transactionDetailId) {
        return event.getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.ADVANCEMENT)
                .filter(sr -> sr.getPayment() != null)
                .filter(sr -> transactionDetailId.equals(sr.getPayment().getExternalId())).findAny().isPresent();
    }

    public boolean hasPayment(final AccountingTransaction transaction, SapRequest sapRequest) {
        return event.getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.ADVANCEMENT)
                .filter(sr -> sr.getPayment() != null)
                .filter(sr -> transaction == sr.getPayment()).filter(sr -> sr != sapRequest).findAny().isPresent();
    }

    public boolean hasInterestPayment(final AccountingTransaction transaction) {
        return event.getSapRequestSet().stream().filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                .filter(sr -> transaction == sr.getPayment()).findAny().isPresent();
    }

    public SapRequest getInterestInvoiceRequest(final AccountingTransaction transaction) {
        return event.getSapRequestSet().stream().filter(sr -> sr.getRequestType() == SapRequestType.INVOICE_INTEREST)
                .filter(sr -> transaction == sr.getPayment()).findAny().orElse(null);
    }

    public boolean hasCredit(String creditId) {
        return event.getSapRequestSet().stream().filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
                .filter(sr -> creditId.equals(sr.getCreditId())).findAny().isPresent();
    }

    private void logPastPayments(final AccountingTransactionDetail d, final String clientId, final Money txAmount,
            final String receiptNumber, final String now, final String registryDate) {
        final Event event = d.getEvent();
        final ExecutionYear debtYear = Utils.executionYearOf(event);

        final StringBuilder builder = new StringBuilder();
        builder.append(event.getExternalId());
        builder.append("\t");
        builder.append(d.getExternalId());
        builder.append("\t");
        builder.append(clientId == null ? " " : clientId);
        builder.append("\t");
        builder.append(txAmount.toPlainString());
        builder.append("\t");
        builder.append(receiptNumber);
        builder.append("\t");
        builder.append(now);
        builder.append("\t");
        builder.append(registryDate);
        builder.append("\t");
        builder.append(debtYear.getName());
        builder.append("\n");
        appendPastLog(builder.toString());
    }

    private void appendPastLog(final String line) {
        final File file = new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/sap_past_event_log.csv");
        if (!file.exists()) {
            final StringBuilder builder = new StringBuilder();
            builder.append("Event");
            builder.append("\t");
            builder.append("Transaction");
            builder.append("\t");
            builder.append("ClientId");
            builder.append("\t");
            builder.append("TX Amount");
            builder.append("\t");
            builder.append("Receipt Number");
            builder.append("\t");
            builder.append("Sent To Accounting");
            builder.append("\t");
            builder.append("Registry Date");
            builder.append("\t");
            builder.append("Debt Year");
            builder.append("\n");
            append(file, builder.toString());
        }
        append(file, line);
    }

    private void append(final File file, final String line) {
        try {
            Files.write(file.toPath(), line.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND,
                    StandardOpenOption.WRITE);
        } catch (final IOException e) {
            throw new Error(e);
        }
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

}