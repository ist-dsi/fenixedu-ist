package pt.ist.fenixedu.integration.task.updateData.payments;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.PaymentCodeType;
import org.fenixedu.academic.domain.accounting.events.SpecialSeasonEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.paymentCodes.AccountingEventPaymentCode;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.EnrolmentEvaluationState;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.messaging.core.domain.Message;
import org.joda.time.YearMonthDay;

import java.util.Collections;

@Task(englishTitle = "Create Events and Payment Codes for Special Season Enrolments", readOnly = false)
public class GenerateSpecialSeasonEnrolmentPaymentCodesAndEvents extends CronTask {

    private static final Money AMOUNT_TO_PAY = new Money("20");
    private static final String EVENT_DESCRIPTION = "Inscrição em época especial à disciplina ";

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
        final SpecialSeasonEnrolmentEvent event = new SpecialSeasonEnrolmentEvent(office, person, Collections.singleton(ee));

        final YearMonthDay today = new YearMonthDay();
        final AccountingEventPaymentCode paymentCode =
                AccountingEventPaymentCode.create(PaymentCodeType.SPECIAL_SEASON_ENROLMENT, today, today.plusDays(10), event,
                        AMOUNT_TO_PAY, AMOUNT_TO_PAY, event.getPerson());

        final User user = registration.getPerson().getUser();
        final String eventDescription = eventDescription(ee);

        taskLog("Generated payment codes for: %s -> %s%n", user.getUsername(), eventDescription);

        final String subject = "Pagamento Inscrição Época Especial - " + eventDescription;
        final String body =
                "A inscrição em exames de época especial ou extraordinário está sujeita ao " +
                "pagamento de um emolumento de 20 Euros por unidade curricular. " +
                "\n\n" +
                "A sua inscrição só é finalizada após o pagamento do referido emolumento " +
                "usando a referência multibanco junta. " +
                "\n\n" +
                "Há uma referência multibanco associada a cada unidade curricular a que " + 
                "pretende inscrever-se. " +
                "\n\n" +
                "Este pagamento deverá ser efectuado no prazo de 48h, sob pena " +
                "de a inscrição deixar de ser válida. " +
                "\n\n" +
                "Pode efetuar o pagamento via multibanco com os seguintes dados a partir " +
                "das 20:00 do dia de hoje (ou das 20:00 do dia seguinte se esta mensagem " +
                "tiver sido enviada depois das 18h00): " +
                "\n\n" +
                "Entidade: " + paymentCode.getEntityCode() + "\n" +
                "\n" +
                "Referência: " + paymentCode.getCode() + "\n\n" +
                "\n" +
                "Valor: " + AMOUNT_TO_PAY.toString() + " €" +
                "\n\n"
                ;
        Message.fromSystem()
                .replyToSender()
                .to(Group.users(user))
                .subject(subject)
                .textBody(body)
                .send();
    }

    private String eventDescription(final EnrolmentEvaluation ee) {
        return EVENT_DESCRIPTION + ee.getEnrolment().getCurricularCourse().getName(ee.getExecutionPeriod()) + " - "
                + ee.getExecutionPeriod().getQualifiedName();
    }

}