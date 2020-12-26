package pt.ist.fenixedu.integration.task;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.accounting.paymentCodes.EventPaymentCode;
import org.fenixedu.academic.domain.accounting.paymentCodes.EventPaymentCodeEntry;
import org.fenixedu.academic.domain.candidacy.StudentCandidacy;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.MobilePhone;
import org.fenixedu.academic.domain.contacts.Phone;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.ui.struts.action.externalServices.PhoneValidationUtils;
import org.fenixedu.academic.util.PhoneUtil;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.messaging.core.domain.Message;
import org.fenixedu.messaging.core.template.DeclareMessageTemplate;
import org.fenixedu.messaging.core.template.TemplateParameter;
import org.joda.time.DateTime;

import pt.ist.fenixedu.integration.domain.student.importation.DgesStudentImportationProcess;
import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;


@DeclareMessageTemplate(
        id = "message.template.registration.process.first.time.student.email",
        description = "message.template.registration.process.first.time.student.email.description",
        subject = "message.template.registration.process.first.time.student.email.subject",
        text = "message.template.registration.process.first.time.student.email.body",
        parameters = {
                @TemplateParameter(id = "studentName", description = "message.template.registration.process.first.time.student.email.parameter.studentName"),
                @TemplateParameter(id = "tutorName", description = "message.template.registration.process.first.time.student.email.parameter.tutorName"),
                @TemplateParameter(id = "tutorEmail", description = "message.template.registration.process.first.time.student.email.parameter.tutorEmail"),

                @TemplateParameter(id = "paymentInfoInsuranceEntity", description = "message.template.registration.process.first.time.student.email.parameter.paymentInfoInsuranceEntity"),
                @TemplateParameter(id = "paymentInfoInsuranceReference", description = "message.template.registration.process.first.time.student.email.parameter.paymentInfoInsuranceReference"),
                @TemplateParameter(id = "paymentInfoInsuranceDate", description = "message.template.registration.process.first.time.student.email.parameter.paymentInfoInsuranceDate"),
                @TemplateParameter(id = "paymentInfoInsuranceValue", description = "message.template.registration.process.first.time.student.email.parameter.paymentInfoInsuranceValue"),

                @TemplateParameter(id = "paymentInfoAcademicFeeEntity", description = "message.template.registration.process.first.time.student.email.parameter.paymentInfoAcademicFeeEntity"),
                @TemplateParameter(id = "paymentInfoAcademicFeeReference", description = "message.template.registration.process.first.time.student.email.parameter.paymentInfoAcademicFeeReference"),
                @TemplateParameter(id = "paymentInfoAcademicFeeDate", description = "message.template.registration.process.first.time.student.email.parameter.paymentInfoAcademicFeeDate"),
                @TemplateParameter(id = "paymentInfoAcademicFeeValue", description = "message.template.registration.process.first.time.student.email.parameter.paymentInfoAcademicFeeValue"),

                @TemplateParameter(id = "paymentInfoTuitionEntity", description = "message.template.registration.process.first.time.student.email.parameter.paymentInfoTuitionEntity"),
                @TemplateParameter(id = "paymentInfoTuitionReference", description = "message.template.registration.process.first.time.student.email.parameter.paymentInfoTuitionReference"),
                @TemplateParameter(id = "paymentInfoTuitionDate", description = "message.template.registration.process.first.time.student.email.parameter.paymentInfoTuitionDate"),
                @TemplateParameter(id = "paymentInfoTuitionValue", description = "message.template.registration.process.first.time.student.email.parameter.paymentInfoTuitionValue"),
        },
        bundle = "resources.FenixeduIstIntegrationResources"
)
@Task(englishTitle = "Send First Time Student Notification Email and SMS.")
public class SendFirstTimeStudentNotifications extends CronTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getRegistrationToNotifySet().forEach(r -> sendNotifications(r));
    }

    private void sendNotifications(final Registration r) {
        final StudentCandidacy studentCandidacy = r.getStudentCandidacy();
        final DgesStudentImportationProcess process = studentCandidacy == null ? null : studentCandidacy.getDgesStudentImportationProcess(); 
        final boolean send = process != null;
        if (send) {
            sendSMS(r);
        }
        FenixFramework.atomic(() -> {
            if (send) {
                sendEmail(r);
            }
            r.setBennuCompletedRegistration(null);
        });
    }

    private void sendEmail(final Registration registration) {
        
        if (registration == null) {
            return;
        }
        final Person person = registration.getPerson();
        final User user = person.getUser();

        final StudentCurricularPlan studentCurricularPlan = getStudentCurricularPlan(registration);

        if (studentCurricularPlan == null) {
            return;
        }
        
        final Tutorship tutorship = getTutorship(studentCurricularPlan);

        final DateTime today = new DateTime();
        final EventPaymentCodeEntry insuranceCode = getPaymentCode(today, person, InsuranceEvent.class);
        final EventPaymentCodeEntry academicFeeCode = getPaymentCode(today, person, AdministrativeOfficeFeeEvent.class);
        final EventPaymentCodeEntry tuitionCode = getPaymentCode(today, person, GratuityEvent.class);

        if (academicFeeCode == null || insuranceCode == null || tuitionCode == null) {
            taskLog("Missing payment code information for user: %s%n", user.getUsername());
            return;
        }

        final String[] emails = person.getPartyContactsSet().stream()
                .filter(c -> c instanceof EmailAddress)
                .map(c -> (EmailAddress) c)
                .map(e -> e.getValue())
                .toArray(String[]::new);

        Message.fromSystem()
                .singleTos(emails)
                .to(Group.users(user))
                .template("message.template.registration.process.first.time.student.email")
                .parameter("studentName", user.getDisplayName())
                .parameter("tutorName", tutorship == null ? "Não Atribuido" : tutorship.getTeacher().getPerson().getUser().getProfile().getDisplayName())
                .parameter("tutorEmail", tutorship == null ? "Não Atribuido" : tutorship.getTeacher().getPerson().getEmailForSendingEmails())
                .parameter("paymentInfoInsuranceEntity", insuranceCode.getPaymentCode().getEntityCode())
                .parameter("paymentInfoInsuranceReference", insuranceCode.getPaymentCode().getCode())
                .parameter("paymentInfoInsuranceDate", insuranceCode.getDueDate().toString("yyyy-MM-dd"))
                .parameter("paymentInfoInsuranceValue", insuranceCode.getAmount().toPlainString())
                .parameter("paymentInfoAcademicFeeEntity", academicFeeCode.getPaymentCode().getEntityCode())
                .parameter("paymentInfoAcademicFeeReference", academicFeeCode.getPaymentCode().getCode())
                .parameter("paymentInfoAcademicFeeDate", academicFeeCode.getDueDate().toString("yyyy-MM-dd"))
                .parameter("paymentInfoAcademicFeeValue", academicFeeCode.getAmount().toPlainString())
                .parameter("paymentInfoTuitionEntity", tuitionCode.getPaymentCode().getEntityCode())
                .parameter("paymentInfoTuitionReference", tuitionCode.getPaymentCode().getCode())
                .parameter("paymentInfoTuitionDate", tuitionCode.getDueDate().toString("yyyy-MM-dd"))
                .parameter("paymentInfoTuitionValue", tuitionCode.getAmount().toPlainString())
                .and()
                .wrapped().send();
    }

    private EventPaymentCodeEntry getPaymentCode(final DateTime today, final Person person, final Class clazz) {
        return person.getPaymentCodesSet().stream()
            .filter(pc -> pc instanceof EventPaymentCode)
            .map(pc -> (EventPaymentCode) pc)
            .flatMap(pc -> pc.getEventPaymentCodeEntrySet().stream())
            .filter(entry -> clazz.isAssignableFrom(entry.getEvent().getClass()))
            .filter(entry -> isToday(today, entry.getCreated()))
            .peek(e -> taskLog("%s%n", e.getEvent().getClass().getName()))
            .findAny().orElse(null);
    }

    private boolean isToday(final DateTime today, final DateTime dt) {
        return today.getYear() == dt.getYear() && today.getMonthOfYear() == dt.getMonthOfYear() && today.getDayOfMonth() == dt.getDayOfMonth();
    }

    private Tutorship getTutorship(final StudentCurricularPlan studentCurricularPlan) {
        return studentCurricularPlan.getTutorshipsSet().stream()
                .filter(SendFirstTimeStudentNotifications::coversCurrentExecutionYear).findAny().orElse(null);
    }

    private static boolean coversCurrentExecutionYear(final Tutorship t) {
        return t.getCoveredExecutionYears().stream().anyMatch(ExecutionYear::isCurrent);
    }

    private StudentCurricularPlan getStudentCurricularPlan(final Registration registration) {
        return registration.getActiveStudentCurricularPlan();
    }

    private void sendSMS(final Registration registration) {
        if (registration == null) {
            return;
        }
        
        final Person person = registration.getPerson();

        final StudentCurricularPlan studentCurricularPlan = getStudentCurricularPlan(registration);
        if (studentCurricularPlan == null) {
            return;
        }
        final Tutorship tutorship = getTutorship(studentCurricularPlan);

        if (tutorship == null) {
            return;
        }

        final StringBuilder message = new StringBuilder("Já és aluno do Tecnico. Foi-te atribuído o Tutor ");
        message.append(tutorship.getTeacher().getPerson().getUser().getProfile().getDisplayName());
        message.append(". Mais informações importantes no e-mail.");

        final Stream<String> numbers1 = person.getPartyContactsSet().stream()
            .filter(pc -> pc instanceof Phone)
            .map(pc -> ((Phone) pc).getNumber());
        final Stream<String> numbers2 = person.getPartyContactsSet().stream()
                .filter(pc -> pc instanceof MobilePhone)
                .map(pc -> ((MobilePhone) pc).getNumber());

        Stream.concat(numbers1, numbers2)
            .filter(n -> PhoneUtil.isMobileNumber(n))
            .map(n -> n.replace(" ", ""))
            .map(n -> n.startsWith("+") ? n : "+351" + n)
            .distinct()
            .forEach(n -> sendSMS(n, normalize(message.toString())));
    }

    private String normalize(final String string) {
        return StringNormalizer.normalizePreservingCapitalizedLetters(string);
    }

    private void sendSMS(final String number, final String message) {
        taskLog("Sending sms to number %s. SMS lenght: %s%n", number, message.length());
        taskLog("%s%n", message);

        try {
            //final boolean result = PhoneValidationUtils.getInstance().sendSMS(number, message);
            //taskLog("Sent SMS via gateway to %s : %s%n", number, result);
            throw new Error("Need to review / reimplement code.");
        } catch (final Throwable t) {
            taskLog("Faild send sms with exception: %s to number %s%n", t.getMessage(), number);
            t.printStackTrace();
        }
    }

}
