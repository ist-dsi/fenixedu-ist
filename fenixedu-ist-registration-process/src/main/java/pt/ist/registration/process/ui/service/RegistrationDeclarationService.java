package pt.ist.registration.process.ui.service;

import java.util.Locale;
import java.util.Optional;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.springframework.stereotype.Service;

import pt.ist.registration.process.domain.RegistrationDeclarationFile;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
@Service
public class RegistrationDeclarationService {

    public Optional<RegistrationDeclarationFile> getRegistrationDeclarationFile(Registration registration, ExecutionYear
    executionYear, Locale locale) {
        return RegistrationDeclarationFile.getRegistrationDeclarationFile(registration, executionYear, locale);
    }
}
