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
import org.fenixedu.academic.domain.accounting.PaymentCode;
import org.fenixedu.academic.domain.accounting.PaymentCodeType;
import org.fenixedu.academic.domain.accounting.paymentCodes.AccountingEventPaymentCode;
import org.fenixedu.academic.domain.accounting.paymentCodes.InstallmentPaymentCode;
import org.fenixedu.academic.domain.candidacy.StudentCandidacy;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.MobilePhone;
import org.fenixedu.academic.domain.contacts.Phone;
import org.fenixedu.academic.domain.person.Gender;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.Money;
import org.fenixedu.academic.util.PhoneUtil;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.messaging.core.domain.Message;
import org.joda.time.YearMonthDay;

import pt.ist.fenixedu.integration.domain.student.importation.DgesStudentImportationProcess;
import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;


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

        final String subject = "Bem-vindo ao Técnico - Informações Úteis";

        final StringBuilder b = new StringBuilder();
        b.append("Caro Estudante,\n");
        b.append("\n");
        b.append("Bem-vindo ao Técnico!\n");
        b.append("\n");
        b.append("Neste e-mail encontra informações úteis para a sua integração no Técnico. "
                + "Em particular, encontra o nome do seu tutor, horário, referências "
                + "multibanco para o pagamento das propinas, indicações sobre a declarações "
                + "de matrícula com assinatura electrónica entre outras informações.\n");
        b.append("\n");
        b.append("O principal sistema de informação académico do Técnico é o FenixEdu, onde "
                + "poderá consultar e gerir o seu percurso como estudante, ao longo dos "
                + "próximos anos. Este sistema encontra-se disponível no seguinte endereço:\n");
        b.append("\n");
        b.append("   https://fenix.tecnico.ulisboa.pt\n");
        b.append("\n");
        b.append("Ao autenticar-se neste sistema, com as credenciais que lhe foram entregues "
                + "no dia da sua matrícula, poderá aceder ao portal de estudante. Neste portal " + "poderá:\n");
        b.append("\n");
        b.append("   - consultar o seu horário;\n");
        b.append("   - obter informações sobre o seu tutor;\n");
        b.append("   - obter informações sobre o meio e o estado de pagamento das suas propinas;\n");
        b.append("   - consultar o corpo de delegados, entre muitas outras opções.\n");
        b.append("\n");
        b.append("Pode encontrar no seguinte endereço informações em como configurar e utilizar o "
                + "seu endereço de e-mail institucional:\n");
        b.append("\n");
        b.append("   https://dsi.tecnico.ulisboa.pt/servicos/recursos-e-mail/e-mail/\n");
        b.append("\n");
        if (tutorship != null) {
            final Person tutor = tutorship.getTeacher().getPerson();
            if (tutor.getGender() == Gender.FEMALE) {
                b.append("O tutor que lhe foi designado é a Professora "
                        + tutorship.getTeacher().getPerson().getUser().getProfile().getDisplayName() + ". ");
            } else {
                b.append("O tutor que lhe foi designado é o Professor "
                        + tutorship.getTeacher().getPerson().getUser().getProfile().getDisplayName() + ". ");
            }
            b.append("Pode entrar em contacto com o seu tutor atraves do endereço: "
                    + tutorship.getTeacher().getPerson().getEmailForSendingEmails() + "\n");
            b.append("\n");
        }
        b.append("Brevemente deverá receber no seu endereço de e-mail institucional informações sobre como "
                + "obter a sua declaração de matrícula. Trata-se de um documento eletrónico que deverá "
                + "entregar em todas as entidades perante as quais necessite de comprovar que se encontra "
                + "matriculado no Técnico. Por ser um documento eletrónico, pouco prático de transportar, "
                + "o documento contém um link de acesso, podendo assim imprimir e que permitirá à "
                + "entidade aceder facilmente ao documento eletrónico.\n");
        b.append("\n");
        b.append("A data limite para pagamento da propina é de 10 dias a partir da data da sua matricula. "
                + "Deve optar entre o pagamento da propina na totalidade ou pagamento em sete prestações. "
                + "Após a data limite referida é cobrado 1% ao mês sobre a propina da 1ª prestação. "
                + "De seguida são apresentadas as referência multibanco para pagamento da taxa de "
                + "secretaria/seguro escolar e da propina, na sua totalidade ou em prestações.\n");

        b.append("\n");
        b.append("Taxa de secretaria / seguro escolar\n");
        for (final PaymentCode paymentCode : person.getPaymentCodesSet()) {
            if (PaymentCodeType.ADMINISTRATIVE_OFFICE_FEE_AND_INSURANCE.equals(paymentCode.getType())) {
                appendPaymentCodeInformation(b, paymentCode);
            }
        }
        b.append("\n");
        b.append("Propina na totalidade\n");
        for (final PaymentCode paymentCode : person.getPaymentCodesSet()) {
            if (PaymentCodeType.GRATUITY_FIRST_INSTALLMENT.equals(paymentCode.getType())
                    && !(paymentCode instanceof InstallmentPaymentCode)) {
                appendPaymentCodeInformation(b, paymentCode);
            }
        }
        b.append("\n");
        b.append("Propina em prestações\n");
        final int i[] = { 0 };
        person.getPaymentCodesSet().stream()
                .filter(pc -> pc instanceof InstallmentPaymentCode).sorted((pc1, pc2) -> pc1.getCode().compareTo(pc2.getCode()))
                .map(pc -> (InstallmentPaymentCode) pc)
                .forEach(pc -> appendInstallmentPaymentCodeInformation(b, pc, ++i[0]));
        b.append("\n");
        b.append("Desejamos que tenha sucesso no seu percurso no Técnico, certos de que todos estarão ao "
                + "seu dispor para que tal se concretize.\n");
        b.append("\n");
        b.append("---\n");
        b.append("Esta mensagem foi enviada por meio do sistema FénixEdu, em nome do Técnico Lisboa\n");

        final String[] emails = person.getPartyContactsSet().stream()
            .filter(c -> c instanceof EmailAddress)
            .map(c -> (EmailAddress) c)
            .map(e -> e.getValue())
            .toArray(String[]::new);
        FenixFramework.atomic(() -> send(subject, b, emails, user));
    }

    private void send(final String subject, final StringBuilder b, final String[] toAddress, final User toUser) {
        Message.fromSystem().content(subject, b.toString(), null).singleTos(toAddress).to(Group.users(toUser)).send();
    }

    private void appendInstallmentPaymentCodeInformation(final StringBuilder b, final AccountingEventPaymentCode paymentCode,
            final int i) {
        b.append("\n");
        b.append("Prestação " + i + "\n");
        appendPaymentCodeInformation(b, paymentCode);
    }

    private void appendPaymentCodeInformation(final StringBuilder b, final PaymentCode paymentCode) {
        final String description = paymentCode.getDescription();
        final YearMonthDay endDate = paymentCode.getEndDate();
        final String entityCode = paymentCode.getEntityCode();
        final String formattedCode = paymentCode.getFormattedCode();
        final Money maxAmount = paymentCode.getMaxAmount();
        final Money minAmount = paymentCode.getMinAmount();
        final YearMonthDay startDate = paymentCode.getStartDate();
        final PaymentCodeType type = paymentCode.getType();

        b.append("   Entidade: " + entityCode + "\n");
        b.append("   Referência: " + formattedCode + "\n");
        b.append("   Data limite: " + endDate.toString("yyyy-MM-dd") + "\n");
        b.append("   Valor: " + minAmount.toString() + "\n");
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
