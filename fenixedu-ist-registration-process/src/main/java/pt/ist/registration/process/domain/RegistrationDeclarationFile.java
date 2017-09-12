package pt.ist.registration.process.domain;

import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.User;

public class RegistrationDeclarationFile extends RegistrationDeclarationFile_Base {

    public RegistrationDeclarationFile(String filename, String displayName, byte[] content, Registration registration, ExecutionYear executionYear,  Locale locale, String uniqueIdentifier) {
        super();
        getRegistrationDeclarationFile(registration, executionYear, filename, locale).ifPresent(RegistrationDeclarationFile::delete);
        init(displayName, filename, content);
        setUniqueIdentifier(uniqueIdentifier);
        setRegistration(registration);
        setExecutionYear(executionYear);
        setLocale(locale);
    }

    @Override
    public void delete() {
        setRegistration(null);
        setExecutionYear(null);
        super.delete();
    }

    @Override
    public boolean isAccessible(User user) {
        return false;
    }

    public static Optional<RegistrationDeclarationFile> getRegistrationDeclarationFile(Registration registration, ExecutionYear
            executionYear, String filename, Locale locale) {
        return registration.getRegistrationDeclarationFileSet().stream().filter(file -> file.getExecutionYear() ==
                executionYear && file.getFilename().equals(filename) && file.getLocale() == locale ).findAny();
    }

    public static Optional<RegistrationDeclarationFile> getRegistrationDeclarationFile(Registration registration, String
    uniqueIdentifier) {
        return registration.getRegistrationDeclarationFileSet().stream().filter(file -> file.getUniqueIdentifier().equals
        (uniqueIdentifier)).findAny();
    }

}
