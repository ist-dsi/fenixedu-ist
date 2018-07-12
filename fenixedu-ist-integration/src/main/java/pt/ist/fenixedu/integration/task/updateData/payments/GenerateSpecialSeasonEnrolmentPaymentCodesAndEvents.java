package pt.ist.fenixedu.integration.task.updateData.payments;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.PaymentCodeType;
import org.fenixedu.academic.domain.accounting.events.SpecialSeasonEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.paymentCodes.AccountingEventPaymentCode;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.messaging.core.domain.Message;
import org.fenixedu.messaging.core.template.DeclareMessageTemplate;
import org.fenixedu.messaging.core.template.TemplateParameter;
import org.joda.time.YearMonthDay;

@DeclareMessageTemplate(id = "special.season.enrolment.payment.info.message.template",
        description = "special.season.enrolment.payment.info.message.description",
        subject = "special.season.enrolment.payment.info.message.subject",
        text = "special.season.enrolment.payment.info.message.body",
        parameters = {
                @TemplateParameter(id = "eventDescriptionPT",
                        description = "special.season.enrolment.payment.info.message.parameter.event.description.pt"),
                @TemplateParameter(id = "eventDescriptionEN",
                        description = "special.season.enrolment.payment.info.message.parameter.event.description.en"),
                @TemplateParameter(id = "paymentEntityCode", description = "special.season.enrolment.payment.info.message.parameter.entity.code"),
                @TemplateParameter(id = "paymentCode", description = "special.season.enrolment.payment.info.message.parameter.code"),
                @TemplateParameter(id = "amount", description = "special.season.enrolment.payment.info.message.parameter.amount"),
        },
        bundle = "resources.FenixeduIstIntegrationResources")

@Task(englishTitle = "Create Events and Payment Codes for Special Season Enrolments", readOnly = false)
public class GenerateSpecialSeasonEnrolmentPaymentCodesAndEvents extends CronTask {

    private static final Money AMOUNT_TO_PAY = new Money("20");
    private static final String EVENT_DESCRIPTION_PT = "Inscrição em época especial à disciplina ";
    private static final String EVENT_DESCRIPTION_EN = "Special Season Enrolment Course ";
    private static final Set<StatuteType> EXCEPTION_STATUTE_TYPES = Bennu.getInstance().getSpecialSeasonStatuteExceptionSet();

    @Override
    public void runTask() {

        ExecutionYear.readCurrentExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(es -> es.getEnrolmentsSet().stream())
                .flatMap(e -> e.getEvaluationsSet().stream())
                .filter(ee -> ee.getEvaluationSeason().isSpecial())
                .filter(ee -> !ee.getEnrolmentEvaluationState().equals(EnrolmentEvaluationState.ANNULED_OBJ))
                .filter(this::missingEvent)
                .forEach(this::createEvent);
    }

    private boolean missingEvent(final EnrolmentEvaluation ee) {
        return ee.getSpecialSeasonEnrolmentEvent() == null;
    }

    private void createEvent(final EnrolmentEvaluation ee) {
        final Registration registration = ee.getRegistration();
        final Degree degree = registration.getDegree();
        final AdministrativeOffice office = degree.getAdministrativeOffice();
        final Person person = registration.getPerson();
        final Student student = person.getStudent();
        final Collection<StatuteType> statuteTypes =
                student.getStatutesTypesValidOnAnyExecutionSemesterFor(ee.getEnrolment().getExecutionYear());
        final User user = registration.getPerson().getUser();

        if (statuteTypes.stream().anyMatch(st -> EXCEPTION_STATUTE_TYPES.contains(st))) {
            taskLog("Skipped payment for student due to exception statute: %s%n", user.getUsername());
            return;
        }

        final SpecialSeasonEnrolmentEvent event = new SpecialSeasonEnrolmentEvent(office, person, Collections.singleton(ee));

        final YearMonthDay today = new YearMonthDay();
        final AccountingEventPaymentCode paymentCode =
                AccountingEventPaymentCode.create(PaymentCodeType.SPECIAL_SEASON_ENROLMENT, today, today.plusDays(10), event,
                        AMOUNT_TO_PAY, AMOUNT_TO_PAY, event.getPerson());

        final String eventDescriptionPT = eventDescriptionPT(ee);
        final String eventDescriptionEN = eventDescriptionEN(ee);

        taskLog("Generated payment codes for: %s -> %s%n", user.getUsername(), eventDescriptionEN);

        Message.fromSystem()
               .replyToSender()
               .to(Group.users(user))
               .template("special.season.enrolment.payment.info.message.template")
                   .parameter("eventDescriptionPT", eventDescriptionPT)
                   .parameter("eventDescriptionEN", eventDescriptionEN)
                   .parameter("paymentEntityCode", paymentCode.getEntityCode())
                   .parameter("paymentCode", paymentCode.getCode())
                   .parameter("amount", AMOUNT_TO_PAY.toString())
                   .and()
               .send();

    }

    private String eventDescriptionPT(final EnrolmentEvaluation ee) {
        return EVENT_DESCRIPTION_PT + ee.getEnrolment().getCurricularCourse().getName(ee.getExecutionPeriod()) + " - "
                + ee.getExecutionPeriod().getQualifiedName();
    }

    private String eventDescriptionEN(final EnrolmentEvaluation ee) {
        ExecutionSemester executionSemester = ee.getExecutionPeriod();
        String semester = executionSemester.getSemester() == 1 ? "1st" : "2nd" + " Semester ";
        String year = executionSemester.getExecutionYear().getYear().toString();
        return EVENT_DESCRIPTION_EN + ee.getEnrolment().getCurricularCourse().getNameEn(ee.getExecutionPeriod()) + " - "
                + semester + year;
    }
}
