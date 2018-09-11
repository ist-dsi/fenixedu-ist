package pt.ist.fenixedu.integration.ui.spring.service;

import org.apache.commons.lang.BooleanUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qubit.solution.fenixedu.integration.cgd.services.form43.CgdForm43Sender;
import pt.ist.fenixedu.integration.domain.cgd.CgdCard;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */

@Service
public class SendCgdCardService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendCgdCardService.class);

    private RegistrationDeclarationForBanksService registrationDeclarationForBanksService;


    @Autowired
    public SendCgdCardService(RegistrationDeclarationForBanksService registrationDeclarationForBanksService) {
        this.registrationDeclarationForBanksService = registrationDeclarationForBanksService;
    }

    @Atomic(mode = TxMode.READ)
    public String sendCgdCard(CgdCard card) {
        if (card == null) {
            return "CGD: Não existe cartão para este pedido.";
        }
        final Person person = card.getUser().getPerson();
        final String username = card.getUser().getUsername();
        if (BooleanUtils.isTrue(card.getAllowSendBankDetails())) {
            if (person != null) {
                final Student student = person.getStudent();
                if (student != null) {
                    for (final Registration registration : student.getRegistrationsSet()) {
                        if (registration.isActive()) {
                            CgdForm43Sender sender = new CgdForm43Sender();
                            boolean form = sender.sendForm43For(registration);
                            boolean attachment = sender.uploadFormAttachment(registration,registrationDeclarationForBanksService
                                    .getRegistrationDeclarationFileForBanks(registration));
                            LOGGER.info("Sent Form43 ({}) and registration declaration file ({}) for registration {}",
                                    form, attachment, registration.getExternalId() );
                            if (form && attachment) {
                                FenixFramework.atomic(() -> card.setSuccessfulSentData(new DateTime()));
                                return String.format("CGD: Comunicação efectuada à CGD com sucesso para o utilizador %s",
                                        username);
                            } else {
                                return String.format("CGD: Comunicação falhou para o utilizador %s. Contactar a CGD.", username);
                            }
                        }
                    }
                    return String.format("CGD: Não existe uma matrícula activa para o aluno %s", username);
                }
                return String.format("CGD: Utilizador %s não é aluno", username);
            } else {
                return  String.format("CGD: Utilizador %s não tem pessoa activa", username);
            }
        }
        return String.format("CGD: %s - É necessário autorização a cedência de dados à CGD para efeitos de abertura de conta",
                username) ;
    }

}
