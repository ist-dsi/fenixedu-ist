package pt.ist.fenixedu.domain.documents;

import com.google.gson.JsonObject;
import org.fenixedu.NumberToText;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.commons.i18n.I18N;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.giaf.invoices.Utils;

public class DebtCertificate {

    public static void create(final Event event) {
        FinancialDocument.createFinancialDocument(event,
                reportEntry -> toJson(event, reportEntry),
                "carta-de-liquidacao-at",
                "Certidao de Divida",
                json -> "debtCertificate" + event.getExternalId());
    }

    private static JsonObject toJson(final Event event, final ReportEntry reportEntry) {
        final DateTime letterDate = letterDateFor(event);
        return toJson(event, reportEntry, letterDate.toString(LiquidationLetter.DATE_FORMAT));
    }

    private static DateTime letterDateFor(final Event event) {
        final FinancialDocument liquidationLetter = event.getFinancialDocumentSet().stream()
                .filter(doc -> LiquidationLetter.DOCUMENT_TYPE.equals(doc.getDocumentType()))
                .max((doc1, doc2) -> doc1.getFinancialDocumentFile().getCreationDate().compareTo(doc2.getFinancialDocumentFile().getCreationDate()))
                .orElse(null);
        return liquidationLetter == null ? new DateTime() : liquidationLetter.getFinancialDocumentFile().getCreationDate();
    }

    private static JsonObject toJson(final Event event, final ReportEntry reportEntry, final String limitDate) {
        final JsonObject json = new JsonObject();

        final Person person = event.getPerson();
        LiquidationLetter.addPersonalInformation(json, event, person);

        final ExecutionYear executionYear = Utils.executionYearOf(event);
        json.addProperty("degree", degreeFor(event, executionYear));
        json.addProperty("schoolYear", executionYear.getName());
        json.addProperty("studentEnrollDate", enrolmentDateFor(event, executionYear));
        json.addProperty("limitDate",  limitDate);

        json.addProperty("debtValue", reportEntry.amount.toPlainString());
        json.addProperty("debtValueExtend", NumberToText.toText(I18N.getLocale(), reportEntry.amount.getAmount()));
        json.addProperty("debtInterest", reportEntry.interest.toPlainString());
        json.addProperty("debtInterestExtend", NumberToText.toText(I18N.getLocale(), reportEntry.interest.getAmount()));
        final Money total = reportEntry.amount.add(reportEntry.interest);
        json.addProperty("debtTotal", total.toPlainString());
        json.addProperty("debtTotalExtend", NumberToText.toText(I18N.getLocale(), total.getAmount()));

        json.addProperty("presidentGender", "male");
        json.addProperty("certificateDate", new LocalDate().toString(LiquidationLetter.DATE_FORMAT));

        return json;
    }

    private static String degreeFor(final Event event, final ExecutionYear executionYear) {
        if (event instanceof GratuityEvent) {
            final GratuityEvent gratuityEvent = (GratuityEvent) event;
            return gratuityEvent.getDegree().getPresentationName(executionYear);
        } else if (event instanceof CustomEvent) {
            CustomEvent customEvent = (CustomEvent) event;
            if (EventTemplate.Type.TUITION.name().equals(JsonUtils.get(customEvent.getConfigObject(), "type"))) {
                final Registration registration = JsonUtils.toDomainObject(customEvent.getConfigObject(), "registration");
                return registration.getDegree().getPresentationName(executionYear);
            }
        }
        return "";
    }

    private static String enrolmentDateFor(final Event event, final ExecutionYear executionYear) {
        StudentCurricularPlan scp = null;
        if (event instanceof GratuityEvent) {
            final GratuityEvent gratuityEvent = (GratuityEvent) event;
            scp = gratuityEvent.getStudentCurricularPlan();

        } else if (event instanceof CustomEvent) {
            CustomEvent customEvent = (CustomEvent) event;
            if (EventTemplate.Type.TUITION.name().equals(JsonUtils.get(customEvent.getConfigObject(), "type"))) {
                final Registration registration = JsonUtils.toDomainObject(customEvent.getConfigObject(), "registration");
                scp = registration.getStudentCurricularPlan(executionYear);
            }
        }
        if (scp != null) {
            final DateTime enrolmentDate = scp.getRoot().getCurriculumLineStream()
                    .filter(cl -> cl.getExecutionYear() == executionYear)
                    .map(cl -> cl.getCreationDateDateTime())
                    .min(DateTime::compareTo)
                    .orElseGet(() -> executionYear.getBeginDateYearMonthDay().toDateTimeAtMidnight());
            return enrolmentDate.toString("yyyy-MM-dd");
        }
        return "";
    }

}
