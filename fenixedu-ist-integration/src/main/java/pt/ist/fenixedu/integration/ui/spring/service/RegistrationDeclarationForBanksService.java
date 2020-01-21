package pt.ist.fenixedu.integration.ui.spring.service;

import java.io.InputStream;
import java.util.Locale;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.papyrus.service.PapyrusPdfRendererService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;

import pt.ist.papyrus.PapyrusClientException;
import pt.ist.registration.process.ui.service.RegistrationDeclarationDataProvider;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
@Service
public class RegistrationDeclarationForBanksService {
    private RegistrationDeclarationDataProvider dataProvider;
    private PapyrusPdfRendererService papyrusPdfRendererService;

    @Autowired
    public RegistrationDeclarationForBanksService(RegistrationDeclarationDataProvider dataProvider,
            PapyrusPdfRendererService papyrusPdfRendererService) {
        this.dataProvider = dataProvider;
        this.papyrusPdfRendererService = papyrusPdfRendererService;
    }

    public InputStream getRegistrationDeclarationFileForBanks(Registration registration) {
        try {
            JsonObject registrationDeclarationPayload = dataProvider
                    .getBasicRegistrationData(registration, ExecutionYear.readCurrentExecutionYear(),
                            Locale.forLanguageTag("pt-PT"));
            return papyrusPdfRendererService
                    .render("declaracao-matricula-banks", Locale.forLanguageTag("pt-PT"), registrationDeclarationPayload);
        } catch (PapyrusClientException e) {
            throw new Error(e);
        }
    }

}
