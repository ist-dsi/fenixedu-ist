package pt.ist.fenixedu.integration.ui.spring.service;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.qubit.solution.fenixedu.integration.cgd.services.form43.CgdForm43Sender;
import pt.ist.fenixedu.integration.domain.cgd.CgdCard;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

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
    protected void sendCgdCard(CgdCard card) {
        final Person person = card.getUser().getPerson();
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
                    }
                }
            }
        }
    }

    @Async
    public void asyncSendCgdCard(CgdCard card)  {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        sendCgdCard(card);
    }


}
