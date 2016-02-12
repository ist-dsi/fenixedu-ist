package pt.ist.fenixedu.giaf.invoices;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.AcademicEvent;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.PaymentMode;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.ImprovementOfApprovedEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.dfa.DFACandidacyEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEventWithPaymentPlan;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import pt.ist.giaf.client.financialDocuments.InvoiceClient;

public class GiafEvent {

    private final static String DT_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public class GiafEventEntry {
        public final String invoiceNumber;
        public Money debt;
        public Money exempt = Money.ZERO;
        public Money payed = Money.ZERO;
        public Money fines = Money.ZERO;
        public Set<String> receiptIds = new HashSet<>();

        private GiafEventEntry(final String invoiceNumber, final Money debt) {
            this.invoiceNumber = invoiceNumber;
            this.debt = debt;
        }

        public Money amountStillInDebt() {
            return debt.subtract(exempt).subtract(payed).subtract(fines);
        }
    }

    private final File file;
    private final JsonArray array;
    public final Set<GiafEventEntry> entries = new HashSet<>();

    public GiafEvent(final Event event) {
        file = giafEventFile(event);
        array = readEventFile(event);

        for (final JsonElement je : array) {
            final JsonObject o = je.getAsJsonObject();
            final String invoiceNumber = o.get("invoiceNumber").getAsString();
            final String type = o.get("type").getAsString();
            final Money value = new Money(o.get("value").getAsString());

            final GiafEventEntry entry = entries.stream()
                    .filter(e -> Objects.equals(e.invoiceNumber, invoiceNumber))
                    .findAny().orElseGet(() -> new GiafEventEntry(invoiceNumber, Money.ZERO));
            entries.add(entry);
            
            if (type.equals("debt")) {
                if (!entry.debt.isZero()) {
                    throw new Error("Multiple debt entries for same invoice! There can be only one!");
                }
                entry.debt = value;
            } else if (type.equals("exemption")) {
                entry.exempt = entry.exempt.add(value);
            } else if (type.equals("payment")) {
                entry.payed = entry.payed.add(value);

                final String receiptId = o.get("receiptId").getAsString();
                entry.receiptIds.add(receiptId);
            } else if (type.equals("fine")) {
                entry.fines = entry.fines.add(value);

                final String receiptId = o.get("receiptId").getAsString();
                entry.receiptIds.add(receiptId);                    
            }
        }
    }

    public static JsonArray readEventFile(final Event event) {
        final File file = giafEventFile(event);
        if (file.exists()) {
            try {
                return new JsonParser().parse(new String(Files.readAllBytes(file.toPath()))).getAsJsonArray();
            } catch (JsonSyntaxException | IOException e) {
                throw new Error(e);
            }
        }        
        return new JsonArray();
    }

    private void persistLocalChanges() {
        Utils.writeFileWithoutFailuer(file.toPath(), array.toString().getBytes(), false);
    }

    private static File giafEventFile(final Event event) {
        final File dir = dirFor(event);
        return new File(dir, event.getExternalId() + ".json");
    }

    private Money addAll(final Function<GiafEventEntry, Money> f) {
        return entries.stream().map(e -> f.apply(e)).reduce(Money.ZERO, Money::add);
    }

    public Money debt() {
        return addAll((e) -> e.debt);
    }

    public Money exempt() {
        return addAll((e) -> e.exempt);
    }

    public Money payed() {
        return addAll((e) -> e.payed);
    }

    public Money fines() {
        return addAll((e) -> e.fines);
    }

    public boolean hasPayment(final AccountingTransactionDetail d) {
        return entries.stream().flatMap(e -> e.receiptIds.stream()).anyMatch(s -> s.equals(d.getExternalId()));
    }

    private void persistLocalChange(final String invoiceNumber, final String type, final Money value,
            final String receiptId, final String receiptNumber, final String date, final String paymentDate) {
        final JsonObject o = new JsonObject();
        o.addProperty("invoiceNumber", invoiceNumber);
        o.addProperty("type", type);
        o.addProperty("value", value.toPlainString());
        if (receiptId != null) {
            o.addProperty("receiptId", receiptId);
        }
        if (receiptNumber != null) {
            o.addProperty("receiptNumber", receiptNumber);
        }
        o.addProperty("date", date);
        if (paymentDate != null) {
            o.addProperty("paymentDate", paymentDate);
        }
        array.add(o);

        persistLocalChanges();
    }

    public GiafEventEntry newGiafEventEntry(final Event event, final Money debt) {
        final String now = new DateTime().toString(DT_FORMAT);

        final String invoiceNumber = createInvoice(toJsonDebt(event, debt));

        persistLocalChange(invoiceNumber, "debt", debt, null, null, now, null);

        final GiafEventEntry entry = new GiafEventEntry(invoiceNumber, debt);
        entries.add(entry);
        return entry;
    }

    public void pay(final AccountingTransactionDetail d) {
        final GiafEventEntry entry = openEntry();
        final Money txAmount = d.getTransaction().getAmountWithAdjustment();
        final Money amountStillInDebt = entry == null ? Money.ZERO : entry.amountStillInDebt();

        final Money payed = txAmount.greaterThan(amountStillInDebt) ? amountStillInDebt : txAmount;
        final Money fines = txAmount.subtract(payed);

        final String now = new DateTime().toString(DT_FORMAT);
        final String registryDate = d.getWhenRegistered().toString(DT_FORMAT);

        if (payed.isPositive()) {
            final String receiptNumber = createInvoice(toJsonPayment(d, entry.invoiceNumber, payed));

            persistLocalChange(entry.invoiceNumber, "payment", payed, d.getExternalId(), receiptNumber, now, registryDate);

            entry.payed = entry.payed.add(payed);
        }

        if (fines.isPositive()) {
            final GiafEventEntry fineEntry = newGiafEventEntry(d.getEvent(), fines);

            final String receiptNumber = createInvoice(toJsonPayment(d, fineEntry.invoiceNumber, fines));

            persistLocalChange(fineEntry.invoiceNumber, "fine", fines, d.getExternalId(), receiptNumber, now, registryDate);
        }

        if (entry != null) {
            entry.receiptIds.add(d.getExternalId());
        }
    }

    public void exempt(final GiafEventEntry entry, final Event event, final Money exempt) {
        final String now = new DateTime().toString(DT_FORMAT);

        final String receiptNumber = createInvoice(toJsonExemption(event, entry.invoiceNumber, exempt));

        persistLocalChange(entry.invoiceNumber, "exemption", exempt, null, receiptNumber, now, null);

        entry.exempt = entry.exempt.add(exempt);
    }

    public GiafEventEntry openEntry() {
        return entries.stream().filter(e -> e.amountStillInDebt().isPositive()).findAny().orElse(null);
    }

    public JsonObject toJson(final Event event, final String invoiceId, final String type, final Money value, final String observation) {
        final Person person = event.getPerson();
        final ExecutionYear debtYear = Utils.executionYearOf(event);
        final DebtCycleType cycleType = Utils.cycleTypeFor(event, debtYear);
        final String eventDescription = event.getDescription().toString();
        final String articleCode = Utils.mapToArticleCode(event, eventDescription);
        final String rubrica = mapToRubrica(event, eventDescription);
        final String costCenter = costCenterFor(event);
        final String clientId = Utils.toClientCode(person);

        final JsonObject o = new JsonObject();
        o.addProperty("id", createGiafInteractionId());
        if (invoiceId != null) {
            o.addProperty("invoiceId", invoiceId);
        }
        o.addProperty("date", toString(new Date()));
        o.addProperty("type", type);
        o.addProperty("series", "13");
        o.addProperty("group", "212");
        o.addProperty("clientId", clientId);

        o.addProperty("vatNumber", "");
        o.addProperty("name", "");
        o.addProperty("country", "");
        o.addProperty("postalCode", "");
        o.addProperty("locality", "");
        o.addProperty("street", "");

        o.addProperty("doorNumber", 1);
        o.addProperty("paymentType", "PP");
        o.addProperty("sellerId", costCenter);
        o.addProperty("currency", "EUR");
        o.addProperty("accountingUnit", "10");
        o.addProperty("reference", debtYear.getName());
        o.addProperty("observation", cycleType == null ? "Outros" : cycleType.getDescription());
        o.addProperty("username", "CRISTINAC");

        final JsonArray a = new JsonArray();
        {
            final JsonObject e = new JsonObject();
            e.addProperty("line", 1);
            e.addProperty("type", "2");
            e.addProperty("article", articleCode);
            e.addProperty("description", eventDescription);
            e.addProperty("unitType", "UN");
            e.addProperty("quantity", BigDecimal.ONE);
            e.addProperty("unitPrice", value.getAmount());
            e.addProperty("vat", BigDecimal.ZERO);
            e.addProperty("discount", BigDecimal.ZERO);
            e.addProperty("costCenter", costCenter);
            e.addProperty("responsible", "9910");
            e.addProperty("subCenter", "RP" + costCenter);
            e.addProperty("legalArticle", "M99");
            e.addProperty("rubrica", rubrica);
            e.addProperty("observation", observation);
            e.addProperty("reference", debtYear.getName());
            a.add(e);
        }
        o.add("entries", a);
        return o;
    }

    public JsonObject toJsonDebt(final Event event, final Money value) {
        final JsonObject o = toJson(event, null, "F", value, "");
        o.addProperty("dueDate", toString(getDueDate(event)));
        return o;
    }

    public JsonObject toJsonPayment(final AccountingTransactionDetail detail, final String invoiceId, final Money value) {
        final AccountingTransaction transaction = detail.getTransaction();
        final Event event = transaction.getEvent();

        final JsonObject o = toJson(event, invoiceId, "V", value, "");
        o.addProperty("paymentDate", toString(transaction.getWhenRegistered().toDate()));
        o.addProperty("paymentMethod", toPaymentMethod(transaction.getPaymentMode()));
        o.addProperty("documentNumber", Utils.toPaymentDocumentNumber(detail));
        return o;
    }

    public JsonObject toJsonExemption(final Event event, final String invoiceId, final Money value) {
        final StringBuilder builder = new StringBuilder();
        if (!event.getDiscountsSet().isEmpty()) {
            builder.append("Desconto");
        }
        for (final Exemption exemption : event.getExemptionsSet()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(exemption.getDescription().toString());
        }
        final String observation = max80(builder.toString());
        final JsonObject json = toJson(event, invoiceId, "E", value, observation.isEmpty() ? "Isencao desconto ou estorno" : observation);
        final int year = Year.now().getValue();
        if (invoiceId != null && !invoiceId.startsWith(Integer.toString(year))) {
            json.addProperty("date", invoiceId.substring(0, 4) + "-12-30 23:00:00");
        }
        return json;
    }

    private String max80(final String s) {
        final int l = s.length();
        return l > 80 ? s.substring(0, 80) : s;
    }

    private String mapToRubrica(Event event, String eventDescription) {
        if (event.isGratuity()) {
            final GratuityEvent gratuityEvent = (GratuityEvent) event;
            final StudentCurricularPlan scp = gratuityEvent.getStudentCurricularPlan();
            final Degree degree = scp.getDegree();
            if (scp.getRegistration().getRegistrationProtocol().isAlien()) {
                return "0075";// PROPINAS INTERNACIONAL
            }
            if (degree.isFirstCycle() && degree.isSecondCycle()) {
                return "0030";// 724114 PROPINAS MESTRADO INTEGRADO
            }
            if (degree.isFirstCycle()) {
                return "0027";// 724111 PROPINAS 1 CICLO
            }
            if (degree.isSecondCycle()) {
                return "0028";// 724112 PROPINAS 2 CICLO
            }
            if (degree.isThirdCycle()) {
                return "0029";// 724113 PROPINAS 3 CICLO
            }
            return "0076";// 724116 PROPINAS - OUTROS
        }
        if (event instanceof PhdGratuityEvent) {
            return "0029";// 724113 PROPINAS 3 CICLO
        }
        if (event.isResidenceEvent()) {
            return null;
        }
        if (event.isFctScholarshipPhdGratuityContribuitionEvent()) {
            return null;
        }
        if (event.isAcademicServiceRequestEvent()) {
            if (eventDescription.indexOf(" Reingresso") >= 0) {
                return "0035";// 72419 OUTRAS TAXAS
            }
            return "0037";// 7246 EMOLUMENTOS
        }
        if (event.isDfaRegistrationEvent()) {
            return "0031";// 72412 TAXAS DE MATRICULA
        }
        if (event.isIndividualCandidacyEvent()) {
            return "0031";// 72412 TAXAS DE MATRICULA
        }
        if (event.isEnrolmentOutOfPeriod()) {
            return "0035";// 72419 OUTRAS TAXAS
        }
        if (event instanceof AdministrativeOfficeFeeAndInsuranceEvent) {
            return "0031";// 72412 TAXAS DE MATRICULA
        }
        if (event instanceof InsuranceEvent) {
            return "0034";// 72415 SEGURO ESCOLAR
        }
        if (event.isSpecializationDegreeRegistrationEvent()) {
            return "0031";// 72412 TAXAS DE MATRICULA
        }
        if (event instanceof ImprovementOfApprovedEnrolmentEvent) {
            return "0033";// 72414 TAXAS DE MELHORIAS DE NOTAS
        }
        if (event instanceof DFACandidacyEvent) {
            return "0031";// 72412 TAXAS DE MATRICULA"
        }
        if (event.isPhdEvent()) {
            if (eventDescription.indexOf("Taxa de Inscri") >= 0) {
                return "0031";// 72412 TAXAS DE MATRICULA
            }
            if (eventDescription.indexOf("Requerimento de provas") >= 0) {
                return "0032";// 72413 TAXAS  DE EXAMES
            }
            return "0031";// 72412 TAXAS DE MATRICULA
        }
        throw new Error("not.supported: " + event.getExternalId());
    }

    private String costCenterFor(final Event event) {
        if (event instanceof PhdGratuityEvent) {
            return "8312";
        }
        if (event instanceof AcademicEvent) {
            final AcademicEvent academicEvent = (AcademicEvent) event;
            final AdministrativeOffice administrativeOffice = academicEvent.getAdministrativeOffice();
            if (administrativeOffice != null) {
                final Unit unit = administrativeOffice.getUnit();
                if (unit != null) {
                    final Integer costCenter = unit.getCostCenterCode();
                    if (costCenter != null) {
                        return costCenter.toString();
                    }
                }
            }
        }
        if (event instanceof InsuranceEvent) {
            final InsuranceEvent insuranceEvent = (InsuranceEvent) event;
            final ExecutionYear executionYear = insuranceEvent.getExecutionYear();
            final Person person = event.getPerson();
            if (!person.getPhdIndividualProgramProcessesSet().isEmpty()) {
                return "8312";
            }
            final Student student = person.getStudent();
            if (student != null) {
                for (final Registration registration : student.getRegistrationsSet()) {
                    for (final RegistrationState registrationState : registration.getRegistrationStates(executionYear)) {
                        if (registrationState.isActive()) {
                            final DegreeType degreeType = registration.getDegree().getDegreeType();
                            if (degreeType.isAdvancedFormationDiploma() || degreeType.isAdvancedSpecializationDiploma()
                                    || degreeType.isSpecializationCycle() || degreeType.isSpecializationDegree()
                                    || degreeType.isThirdCycle()) {
                                return "8312";
                            }
                            final Space campus = registration.getCampus(executionYear);
                            if (campus != null && campus.getName().startsWith("T")) {
                                return "7640";
                            }
                        }

                    }
                }
            }
        }
        throw new Error("Unknown cost center for event: " + event.getExternalId());
    }

    private String toString(final Date d) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    private Date getDueDate(final Event event) {
        final DateTime dueDate;
        if (event instanceof GratuityEventWithPaymentPlan) {
            final GratuityEventWithPaymentPlan gratuityEventWithPaymentPlan = (GratuityEventWithPaymentPlan) event;
            dueDate = findLastDueDate(gratuityEventWithPaymentPlan);
        } else if (event instanceof PhdGratuityEvent) {
            final PhdGratuityEvent phdGratuityEvent = (PhdGratuityEvent) event;
            dueDate = phdGratuityEvent.getLimitDateToPay();
        } else if (event instanceof AdministrativeOfficeFeeAndInsuranceEvent) {
            final AdministrativeOfficeFeeAndInsuranceEvent insuranceEvent = (AdministrativeOfficeFeeAndInsuranceEvent) event;
            final YearMonthDay ymd = insuranceEvent.getAdministrativeOfficeFeePaymentLimitDate();
            dueDate = ymd != null ? ymd.plusDays(1).toDateTimeAtMidnight() : getDueDateByPaymentCodes(event);
        } else {
            dueDate = getDueDateByPaymentCodes(event);
        }
        return dueDate.toDate();
    }

    private DateTime getDueDateByPaymentCodes(final Event event) {
        final YearMonthDay ymd =
                event.getPaymentCodesSet().stream().map(pc -> pc.getEndDate()).max((c1, c2) -> c1.compareTo(c2)).orElse(null);
        return ymd != null ? ymd.plusDays(1).toDateTimeAtMidnight() : event.getWhenOccured();
    }

    private DateTime findLastDueDate(final GratuityEventWithPaymentPlan event) {
        return event.getInstallments().stream().map(i -> i.getEndDate().toDateTimeAtMidnight()).max(new Comparator<DateTime>() {
            @Override
            public int compare(DateTime o1, DateTime o2) {
                return o1.compareTo(o2);
            }
        }).orElse(null);
    }

    private String toPaymentMethod(final PaymentMode paymentMode) {
        switch (paymentMode) {
        case CASH:
            return "N";
        case ATM:
            return "SIBS";
        default:
            throw new Error();
        }
    }

    private String createInvoice(final JsonObject jo) {
        final File dir = file.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        final File logFile = new File(dir, "log.json");
        Utils.writeFileWithoutFailuer(logFile.toPath(), (jo.toString() + "\n").getBytes(), true);

        final JsonObject result = produceInvoice(jo);
        final JsonElement documentNumber = result.get("documentNumber");
        final JsonElement pdfBase64 = result.get("pdfBase64");
        if (documentNumber != null && pdfBase64 != null) {
            final File documentFile = new File(dir, sanitize(documentNumber.getAsString()) + ".pdf");
            Utils.writeFileWithoutFailuer(documentFile.toPath(), Base64.getDecoder().decode(pdfBase64.getAsString()), false);
        }
        return documentNumber == null ? null : documentNumber.getAsString();
    }

    private String sanitize(final String s) {
        return s.replace('/', '_').replace('\\', '_');
    }

    private JsonObject produceInvoice(final JsonObject jo) {
        final JsonObject result = InvoiceClient.produceInvoice(jo);
        final JsonElement errorMessage = result.get("errorMessage");
        if (errorMessage != null) {
            final String message = errorMessage.getAsString();
            if (message.indexOf("PK_2012.GC_FACTURA_DET_I99") > 0 && message.indexOf("unique constraint") > 0
                    && message.indexOf("violated") > 0) {
                jo.addProperty("id", createGiafInteractionId());
                return produceInvoice(jo);
            } else {
                throw new Error(errorMessage.getAsString());
            }
        }
        return result;
    }

    private String createGiafInteractionId() {
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        final int offset = uuid.length() - 30;
        return offset > 0 ? uuid.substring(offset) : uuid;
    }

    private static File dirFor(final Event event) {
        final String id = event.getExternalId();
        final String dirPath = GiafInvoiceConfiguration.getConfiguration().giafInvoiceDir() + splitPath(id) + File.separator + id;
        final File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File receiptFile(final Event event, final String filename) {
        final File dir = dirFor(event);
        final File receiptFile = new File(dir, complete(filename));
        return receiptFile.exists() ? receiptFile : null;
    }

    private static String complete(final String filename) {
        return filename.endsWith(".pdf") ? filename : filename + ".pdf";
    }

    private static String splitPath(final String id) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < id.length() - 1; i++, i++) {
            b.append(id.charAt(i));
            b.append(id.charAt(i + 1));
            b.append(File.separatorChar);
        }
        return b.toString();
    }

}
