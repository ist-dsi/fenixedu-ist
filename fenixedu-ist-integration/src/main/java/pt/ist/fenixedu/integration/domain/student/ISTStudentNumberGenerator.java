package pt.ist.fenixedu.integration.domain.student;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Student.StudentNumberGenerator;

import pt.ist.fenixedu.integration.domain.user.management.IstUsernameCounter;

public class ISTStudentNumberGenerator implements StudentNumberGenerator {
    private final IstUsernameCounter counter;

    public ISTStudentNumberGenerator(IstUsernameCounter counter) {
        this.counter = counter;
    }

    @Override
    public Integer doGenerate(Person person) {
        String username = person.getUsername();
        if (username.startsWith("ist1")) {
            Integer numeric = Integer.valueOf(username.replace("ist1", ""));
            if (numeric >= counter.getFirstStudentNumberMatchingValue()) {
                return numeric;
            }
        }
        return (int) counter.getNext();
    }

}
