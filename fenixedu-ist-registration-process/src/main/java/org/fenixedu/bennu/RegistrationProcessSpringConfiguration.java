package org.fenixedu.bennu;

import static org.fenixedu.bennu.core.signals.Signal.registerWithoutTransaction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.fenixedu.bennu.core.signals.HandlerRegistration;
import org.fenixedu.bennu.spring.BennuSpringModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import pt.ist.registration.process.handler.CandidacySignalHandler;

@BennuSpringModule(basePackages = "pt.ist.registration.process", bundles = "RegistrationProcessResources")
public class RegistrationProcessSpringConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationProcessSpringConfiguration.class);

    private static final String REGISTRATION_CREATED_SIGNAL = "academic.candidacy.registration.created";

    private HandlerRegistration candidacySignalHandlerRegistration;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @PostConstruct
    public void startup() {
        logger.info("Register registration declaration signal");
        CandidacySignalHandler candidacySignalHandler = webApplicationContext.getBean(CandidacySignalHandler.class);
        candidacySignalHandlerRegistration = registerWithoutTransaction(REGISTRATION_CREATED_SIGNAL, candidacySignalHandler);
    }

    @PreDestroy
    public void destroy() {
        logger.info("Unregister registration declaration signal");
        candidacySignalHandlerRegistration.unregister();
    }

}
