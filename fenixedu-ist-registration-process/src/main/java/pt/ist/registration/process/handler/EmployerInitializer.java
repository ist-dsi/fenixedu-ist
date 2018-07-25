package pt.ist.registration.process.handler;

import org.springframework.stereotype.Component;

import org.fenixedu.employer.Employer;
import org.fenixedu.employer.backoff.ExponentialBackoff;
import org.fenixedu.employer.workflow.SimpleWorkflow;

@Component
public class EmployerInitializer {

    private Employer employer;

    public EmployerInitializer() {
        this.employer = new Employer(new SimpleWorkflow(), new ExponentialBackoff(), 5);
    }

    public Employer getEmployer() {
        return employer;
    }

}
