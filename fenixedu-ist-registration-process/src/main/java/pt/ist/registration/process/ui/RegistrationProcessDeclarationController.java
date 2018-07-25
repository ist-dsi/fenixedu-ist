package pt.ist.registration.process.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.bennu.rendering.annotations.BennuIntersection;
import org.fenixedu.bennu.rendering.annotations.BennuIntersections;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import pt.ist.registration.process.domain.DeclarationTemplate;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.domain.RegistrationDeclarationFileState;
import pt.ist.registration.process.domain.beans.DeclarationTemplateInputFormBean;
import pt.ist.registration.process.ui.service.RegistrationDeclarationCreatorService;
import pt.ist.registration.process.ui.service.RegistrationProcessDeclarationsService;
import pt.ist.registration.process.ui.service.SignCertAndStoreService;

@BennuIntersections({
        @BennuIntersection(location = "registration.process", position = "actions",
                file= "templates/registrationProcessDeclarationLink.html")
})
@SpringApplication(group = "logged", path = "registration-process", title = "title.registration.process.signed.declaration")
@SpringFunctionality(app = RegistrationProcessDeclarationController.class,
        title = "title.registration.process.signed.declaration")
@RequestMapping("/signed-documents")
public class RegistrationProcessDeclarationController {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationProcessDeclarationController.class);

    @Autowired
    private RegistrationProcessDeclarationsService registrationProcessDeclarationsService;

    @Autowired
    private SignCertAndStoreService signCertAndStoreService;

    @Autowired
    private RegistrationDeclarationCreatorService documentService;

    @RequestMapping(value = "/registration/{registration}", method = RequestMethod.GET)
    public String list(@PathVariable Registration registration, Model model) {
        logger.debug("Invoking RegistrationProcessDeclarationController.list => Registration: {}", registration.getExternalId());
        return listFiles(model, registration);
    }
    
    @RequestMapping(value = "/registration/{registration}", method = RequestMethod.POST)
    public String generateRegistrationDeclaration(@PathVariable Registration registration,
            @ModelAttribute DeclarationTemplateInputFormBean bean, Model model) {

        List<String> errors = new ArrayList<String>();

        final Person person = registration.getPerson();
        final DegreeType degreeType = registration.getDegreeType();
        
        final ExecutionYear executionYear = bean.getExecutionYear();
        final DeclarationTemplate template = bean.getDeclarationTemplate();
        
        logger.debug(
                "Invoking RegistrationProcessDeclarationController.generateRegistrationDeclaration => Registration: {} DeclarationTemplate : {} ExecutionYear: {}",
                registration.getExternalId(), template == null ? "null" : template.getExternalId(),
                executionYear == null ? "null" : executionYear.getExternalId());

        final Set<ExecutionYear> registrationExecutionYears = registration.getRegistrationDataByExecutionYearSet().stream()
                .map(RegistrationDataByExecutionYear::getExecutionYear).collect(Collectors.toSet());

        if (registrationProcessDeclarationsService.getSub23DeclarationTemplates().contains(template)) {
            final YearMonthDay ymd = person.getDateOfBirthYearMonthDay();
            final LocalDate today = new LocalDate();

            if (ymd == null || ymd.plusYears(24).isBefore(today)) {
                errors.add("label.declaration.generate.file.error.sub23.not.applicable");
            }
        }        

        if (!registrationExecutionYears.contains(executionYear)) {
            errors.add("label.declaration.generate.file.error.invalid.execution.year");
        }

        if (!degreeType.isBolonhaDegree() && !degreeType.isBolonhaMasterDegree() && !degreeType.isIntegratedMasterDegree()) {
            errors.add("label.declaration.generate.file.error.wrong.degree.type");
        }

        if (person.getExpirationDateOfDocumentIdYearMonthDay() == null) {
            errors.add("label.declaration.generate.file.error.missing.expiration.date.id.document");
        }

        if (person.getDateOfBirthYearMonthDay() == null) {
            errors.add("label.declaration.generate.file.error.missing.birth.date");
        }

        if (person.getIdDocumentType() == null) {
            errors.add("label.declaration.generate.file.error.missing.id.document.type");
        }

        if (person.getDocumentIdNumber() == null) {
            errors.add("label.declaration.generate.file.error.missing.id.document.number");
        }

        if (!errors.isEmpty()) {
            return listFiles(model, registration, errors);
        }        

        try {
            RegistrationDeclarationFile registrationDeclarationFile =
                    documentService.generateAndSaveFile(registration, executionYear, template);

            String queue = registrationProcessDeclarationsService.getQueue(registration);

            signCertAndStoreService.sendDocumentToBeSignedWithJob(registration, registrationDeclarationFile, queue);
        } catch (Exception e) {
            errors.add("label.declaration.generate.file.error.generating.file");
            e.printStackTrace();
            return listFiles(model, registration, errors);
        }

        return "redirect:/signed-documents/registration/" + registration.getExternalId();
    }

    @RequestMapping(value = "/registration/{registration}/file/{declarationFile}/retry",
            method = RequestMethod.GET)
    public String retryWorkflow(@PathVariable Registration registration,
            @PathVariable RegistrationDeclarationFile declarationFile, Model model) {
        logger.debug("Invoking RegistrationProcessDeclarationController.retryWorkflow => Registration: {}"
                + "RegistrationDeclarationFile : {}", registration.getExternalId(), declarationFile.getExternalId());

        List<String> errors = new ArrayList<String>();

        if (registration == null || declarationFile == null) {
            errors.add("label.declaration.retry.workflow.error.missing.object.missing");
        }

        RegistrationDeclarationFileState state = declarationFile.getState();

        if (state == null) {
            errors.add("label.declaration.retry.workflow.error.file.state.null");
        }

        if (!errors.isEmpty()) {
            return listFiles(model, registration, errors);
        }

        String filename = declarationFile.getFilename();
        String title = declarationFile.getDisplayName();
        String queue = registrationProcessDeclarationsService.getQueue(registration);
        String externalIdentifier = declarationFile.getUniqueIdentifier();

        try {
            logger.debug("Sending Registration Declaration {} of student {} to be signed", declarationFile.getUniqueIdentifier(),
                    registration.getNumber());
            signCertAndStoreService.sendDocumentToBeSigned(registration.getExternalId(), queue, title, title, filename,
                    declarationFile.getStream(), externalIdentifier);

            if (declarationFile.getState() == RegistrationDeclarationFileState.CREATED) {
                declarationFile.updateState(RegistrationDeclarationFileState.PENDING);
            }
        } catch (Error e) {
            e.printStackTrace();
            errors.add("label.declaration.retry.workflow.error.Exception");
            list(registration, model);
        }

        return "redirect:/signed-documents/registration/" + registration.getExternalId();
    }

    public String listFiles(Model model, Registration registration) {
        return listFiles(model, registration, new ArrayList<String>());
    }

    public String listFiles(Model model, Registration registration, List<String> errors) {
        model.addAttribute("registration", registration);
        model.addAttribute("declarationRegistrationFiles",
                registrationProcessDeclarationsService.getRegistrationDeclarationFileOrderedByDate(registration));
        model.addAttribute("declarationTemplateInputFormBean", new DeclarationTemplateInputFormBean());
        model.addAttribute("declarationTemplates", registrationProcessDeclarationsService.getDeclarationTemplates());
        model.addAttribute("errors", errors);

        return "registration-process/list";
    }
}
