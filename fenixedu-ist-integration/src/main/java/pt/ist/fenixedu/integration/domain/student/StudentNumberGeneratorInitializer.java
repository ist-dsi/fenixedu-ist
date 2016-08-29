package pt.ist.fenixedu.integration.domain.student;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.student.Student;

import pt.ist.fenixedu.integration.domain.user.management.IstUsernameCounter;

@WebListener
public class StudentNumberGeneratorInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        IstUsernameCounter counter = IstUsernameCounter.ensureSingleton();
        Student.setStudentNumberGenerator(new ISTStudentNumberGenerator(counter));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}