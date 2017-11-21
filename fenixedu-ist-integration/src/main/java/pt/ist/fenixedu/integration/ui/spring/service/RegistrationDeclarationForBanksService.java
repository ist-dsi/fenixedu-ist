package pt.ist.fenixedu.integration.ui.spring.service;

import java.io.InputStream;
import java.util.Locale;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.papyrus.service.PdfRendererService;
import org.fenixedu.bennu.papyrus.service.PdfTemplateResolver;
import org.fenixedu.bennu.papyrus.service.UnresolvableTemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;

import pt.ist.registration.process.ui.service.RegistrationDeclarationDataProvider;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
@Service
public class RegistrationDeclarationForBanksService {
    private RegistrationDeclarationDataProvider dataProvider;
    private PdfRendererService pdfRendererService;
    private PdfTemplateResolver pdfTemplateResolver;

    @Autowired
    public RegistrationDeclarationForBanksService(RegistrationDeclarationDataProvider dataProvider,
            PdfRendererService pdfRendererService, PdfTemplateResolver pdfTemplateResolver) {
        this.dataProvider = dataProvider;
        this.pdfRendererService = pdfRendererService;
        this.pdfTemplateResolver = pdfTemplateResolver;
    }

    public InputStream getRegistrationDeclarationFileForBanks(Registration registration) {
        try {
            JsonObject registrationDeclarationPayload = dataProvider
                    .getBasicRegistrationData(registration, ExecutionYear.readCurrentExecutionYear(), Locale
                            .forLanguageTag("pt-PT"));
            return pdfRendererService.render(pdfTemplateResolver.resolve("declaracao-matricula-banks.html"),
                    registrationDeclarationPayload);
        } catch (UnresolvableTemplateException e) {
            throw new Error(e);
        }
    }

}
