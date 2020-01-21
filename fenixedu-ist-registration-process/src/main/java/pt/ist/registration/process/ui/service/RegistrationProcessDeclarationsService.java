package pt.ist.registration.process.ui.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.ExternalEnrolment;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.papyrus.domain.PapyrusTemplate;
import org.springframework.stereotype.Service;

import pt.ist.registration.process.domain.RegistrationDeclarationFile;

@Service
public class RegistrationProcessDeclarationsService {

    private Comparator<RegistrationDeclarationFile> COMPARATOR_BY_CREATION_DATE = new Comparator<RegistrationDeclarationFile>() {
        @Override
        public int compare(final RegistrationDeclarationFile f1, final RegistrationDeclarationFile f2) {
            return f2.getCreationDate().compareTo(f1.getCreationDate());
        }
    };

    public Set<PapyrusTemplate> getDeclarationTemplates() {
        return Bennu.getInstance().getPapyrusTemplateSet();
    }

    public Set<PapyrusTemplate> getSub23DeclarationTemplates() {
        return getDeclarationTemplates().stream().filter(dt -> dt.getName().contains("sub23")).collect(Collectors.toSet());
    }

    public Set<RegistrationDeclarationFile> getRegistrationDeclarationFileOrderedByDate(Registration registration) {
        Set<RegistrationDeclarationFile> file_by_date = new TreeSet<>(COMPARATOR_BY_CREATION_DATE);

        file_by_date.addAll(registration.getRegistrationDeclarationFileSet());

        return file_by_date;
    }

    public String getQueue(Registration registration) {
        String campusName = registration.getCampus().getName().toLowerCase();

        if (campusName.contains("alameda")) {
            return RegistrationProcessConfiguration.getConfiguration().signerAlamedaQueue();
        }

        if (campusName.contains("taguspark")) {
            return RegistrationProcessConfiguration.getConfiguration().signerTagusparkQueue();
        }

        return null;
    }

    public List<ExecutionYear> getEnrolmentExecutionYears(Registration registration) {
        return registration.getStudentCurricularPlansSet()
                .stream()
                .flatMap(scp -> Stream.concat(scp.getEnrolmentStream().map(CurriculumLine::getExecutionYear),
                        registration.getExternalEnrolmentsSet().stream().map(ExternalEnrolment::getExecutionYear)))
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }
}
