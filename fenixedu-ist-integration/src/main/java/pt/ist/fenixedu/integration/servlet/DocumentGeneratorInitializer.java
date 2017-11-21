package pt.ist.fenixedu.integration.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.serviceRequests.documentRequests.DefaultDocumentGenerator;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */

@WebListener
public class DocumentGeneratorInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        DefaultDocumentGenerator.setGenerator(new PapyrusDocumentGenerator());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
