package pt.ist.registration.process.domain;

import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class RegistrationDeclarationFile extends RegistrationDeclarationFile_Base {

    public RegistrationDeclarationFile(String filename, String displayName, byte[] content, Registration registration, ExecutionYear executionYear,  Locale locale, String uniqueIdentifier) {
        super();
        init(displayName, filename, content);
        setUniqueIdentifier(uniqueIdentifier);
        setRegistration(registration);
        setExecutionYear(executionYear);
        setLocale(locale);
        setCreator(Authenticate.getUser());
        setLastUpdated(new DateTime());
        setState(RegistrationDeclarationFileState.CREATED);
    }

    @Override
    public void delete() {
        setCreator(null);
        setRegistration(null);
        setExecutionYear(null);
        super.delete();
    }

    @Override
    public boolean isAccessible(User user) {
        return AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_DOCUMENTS, getRegistration().getDegree()).isMember(user);
    }

    @Atomic(mode = TxMode.WRITE)
    public void updateState(RegistrationDeclarationFileState state) {
        setState(state);
        setLastUpdated(new DateTime());
    }

    @Atomic(mode = TxMode.WRITE)
    public void updateState(RegistrationDeclarationFileState state, String downloadLink) {
        setState(state);
        setDownloadSignedFileLink(downloadLink);
        setLastUpdated(new DateTime());

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
