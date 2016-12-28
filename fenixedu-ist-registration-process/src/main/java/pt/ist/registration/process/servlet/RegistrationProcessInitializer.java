package pt.ist.registration.process.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.bennu.core.signals.HandlerRegistration;
import org.fenixedu.bennu.core.signals.Signal;
import pt.ist.registration.process.handler.CandidacySignalHandler;
import pt.ist.registration.process.ui.service.RegistrationDeclarationCreatorService;
import pt.ist.registration.process.ui.service.SignCertAndStoreService;

@WebListener
public class RegistrationProcessInitializer implements ServletContextListener {

    private static final String REGISTRATION_CREATED_SIGNAL = "academic.candidacy.registration.created";

    private HandlerRegistration registrationDeclarationSignalHandler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        registrationDeclarationSignalHandler = Signal.registerWithoutTransaction(REGISTRATION_CREATED_SIGNAL,
                new CandidacySignalHandler(new RegistrationDeclarationCreatorService(), new SignCertAndStoreService()));
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (registrationDeclarationSignalHandler != null) {
            Signal.unregister(registrationDeclarationSignalHandler);
        }
    }
}