package pt.ist.registration.process.domain;

import java.util.UUID;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.User;

public class RegistrationDeclarationFile extends RegistrationDeclarationFile_Base {

    public RegistrationDeclarationFile(String filename, byte[] content, Registration registration) {
        super();
        init(filename, filename, content);
        setUniqueIdentifier(UUID.randomUUID().toString());
        setRegistration(registration);
    }

    @Override
    public void delete() {
        setRegistration(null);
        super.delete();
    }

    @Override
    public boolean isAccessible(User user) {
        return false;
    }

}