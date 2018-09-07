package pt.ist.fenixedu.integration.task;

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.fenixedu.academic.FenixEduAcademicConfiguration;
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
import org.fenixedu.academic.domain.person.Gender;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.PhoneUtil;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
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
                @TemplateParameter(id = "name", description = "message.template.registration.process.first.time.student.email.parameter.name"),
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

    private String CIIST_SMS_GATEWAY_URL = FenixEduAcademicConfiguration.getConfiguration().getCIISTSMSGatewayUrl();
    private HttpClient CIIST_CLIENT = new HttpClient();
    {
        final String CIIST_SMS_USERNAME = FenixEduAcademicConfiguration.getConfiguration().getCIISTSMSUsername();
        final String CIIST_SMS_PASSWORD = FenixEduAcademicConfiguration.getConfiguration().getCIISTSMSPassword();
        Credentials credentials = new UsernamePasswordCredentials(CIIST_SMS_USERNAME, CIIST_SMS_PASSWORD);
        CIIST_CLIENT.getState().setCredentials(AuthScope.ANY, credentials);
    }

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
        final Person person = registration.getPerson();
        final User user = person.getUser();
        final StudentCurricularPlan studentCurricularPlan = getStudentCurricularPlan(registration);
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
                .parameter("name", user.getDisplayName())
                .parameter("tutorName", tutorship.getTeacher().getPerson().getUser().getProfile().getDisplayName())
                .parameter("tutorEmail", tutorship.getTeacher().getPerson().getEmailForSendingEmails())
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
            .filter(pc -> isToday(today, pc.getWhenCreated()))
            .map(pc -> (EventPaymentCode) pc)
            .flatMap(pc -> pc.getEventPaymentCodeEntrySet().stream())
            .filter(e -> e.getEvent().getClass().isAssignableFrom(clazz))
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
        final Person person = registration.getPerson();
        final StudentCurricularPlan studentCurricularPlan = getStudentCurricularPlan(registration);
        final Tutorship tutorship = getTutorship(studentCurricularPlan);

        if (tutorship == null) {
            return;
        }

        final StringBuilder message = new StringBuilder("Bem-vindo ao Técnico. ");
        final Person tutor = tutorship.getTeacher().getPerson();
        if (tutor.getGender() == Gender.FEMALE) {
            message.append("Foi lhe atribuído o tutor "
                    + tutorship.getTeacher().getPerson().getUser().getProfile().getDisplayName() + ". ");
        } else {
            message.append("Foi lhe atribuído o tutor "
                    + tutorship.getTeacher().getPerson().getUser().getProfile().getDisplayName() + ". ");
        }
        message.append("Mais informações importantes no seu e-mail.");

        final Stream<String> numbers1 = person.getPartyContactsSet().stream()
            .filter(pc -> pc instanceof Phone)
            .map(pc -> ((Phone) pc).getNumber());
        final Stream<String> numbers2 = person.getPartyContactsSet().stream()
                .filter(pc -> pc instanceof MobilePhone)
                .map(pc -> ((MobilePhone) pc).getNumber());

        Stream.concat(numbers1, numbers2)
            .filter(n -> PhoneUtil.isMobileNumber(n))
            .map(n -> n.replace(" ", ""))
            .distinct()
            .forEach(n -> sendSMS(n, message.toString()));
    }

    private void sendSMS(final String number, final String message) {
        taskLog("Sending sms to number %s. SMS lenght: %s%n", number, message.length());
        taskLog("%s%n", message);

        PostMethod method = new PostMethod(CIIST_SMS_GATEWAY_URL);
        method.addParameter(new NameValuePair("number", number));
        method.addParameter(new NameValuePair("msg", message));
        try {
            CIIST_CLIENT.executeMethod(method);
            if (method.getStatusCode() != 200) {
                taskLog("Faild send sms with status code: %s%n", method.getStatusCode());
            }
        } catch (final HttpException e) {
            taskLog("Faild send sms with exception: %s%n", e.getMessage());
            e.printStackTrace();
        } catch (final IOException e) {
            taskLog("Faild send sms with exception: %s%n", e.getMessage());
            e.printStackTrace();
        }
    }

}
