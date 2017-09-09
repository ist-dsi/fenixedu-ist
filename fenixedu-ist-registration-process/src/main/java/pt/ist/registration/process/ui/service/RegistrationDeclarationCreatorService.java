package pt.ist.registration.process.ui.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfFormField;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfString;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.ui.service.exception.ProblemsGeneratingDocumentException;

@Service
public class RegistrationDeclarationCreatorService {

    private PdfRendererService pdfRendererService;
    private PdfTemplateResolver pdfTemplateResolver;
    private RegistrationDeclarationDataProvider dataProvider;
    private QRCodeGenerator qrCodeGenerator;

    @Autowired
    public RegistrationDeclarationCreatorService(PdfRendererService pdfRendererService, PdfTemplateResolver pdfTemplateResolver,
            RegistrationDeclarationDataProvider dataProvider, QRCodeGenerator qrCodeGenerator) {
        this.pdfRendererService = pdfRendererService;
        this.pdfTemplateResolver = pdfTemplateResolver;
        this.dataProvider = dataProvider;
        this.qrCodeGenerator = qrCodeGenerator;
    }

    public RegistrationDeclarationFile generateAndSaveDocumentsForRegistration(Registration registration,
            ExecutionYear executionYear, Locale locale) throws ProblemsGeneratingDocumentException {
        String uniqueIdentifier = UUID.randomUUID().toString();
        JsonObject payload = getRegistrationDeclarationPayload(registration, executionYear, locale, uniqueIdentifier);
        String templateName = String.format("declaracao-matricula-%s.html", locale.getLanguage().toLowerCase());
        try {
            InputStream template = pdfTemplateResolver.resolve(templateName);
            byte[] document = pdfRendererService.renderToByteArray(template, payload);
            String executionYearName = executionYear.getName().replaceAll("/", "-");
            String filename = String.format("%s_declaracao_%s_%s_%s.pdf", executionYearName, locale.getLanguage(), registration
            .getDegree().getSigla(), registration.getPerson().getUsername());
            return createRegistrationDocumentFile(registration, executionYear, locale, uniqueIdentifier, document, filename);
        } catch (UnresolvableTemplateException | PdfRenderingException e) {
            throw new Error(e);
        }
    }

    @Atomic(mode = TxMode.WRITE)
    protected RegistrationDeclarationFile createRegistrationDocumentFile(Registration registration, ExecutionYear executionYear,
            Locale locale, String uniqueIdentifier, byte[] document, String filename) {
        return new RegistrationDeclarationFile(filename, document, registration, executionYear, locale, uniqueIdentifier);
    }

    public JsonObject getRegistrationDeclarationPayload(Registration registration, ExecutionYear executionYear, Locale locale,
            String uniqueIdentifier) {
        JsonObject payload = dataProvider.getBasicRegistrationData(registration, executionYear, locale);
        String certifierUrl = getCertifierUrl(uniqueIdentifier);
        payload.addProperty("certifierUrl", certifierUrl);
        payload.addProperty("qrCodeDataURI",
                "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodeGenerator.generate(certifierUrl)));
        return payload;
    }

    public byte[] generateDocumentWithSignatureField(RegistrationDeclarationFile registrationDeclarationFile,
            String signatureFieldName) throws ProblemsGeneratingDocumentException {
        if (registrationDeclarationFile == null) {
            return null;
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PdfReader original = new PdfReader(registrationDeclarationFile.getStream());
            PdfStamper stp = new PdfStamper(original, bos);
            PdfFormField sig = PdfFormField.createSignature(stp.getWriter());
            sig.setWidget(new Rectangle(100, 300, 500, 200), null);
            sig.setFlags(PdfAnnotation.FLAGS_PRINT);
            sig.put(PdfName.DA, new PdfString("/Helv 0 Tf 0 g"));
            sig.setFieldName(signatureFieldName);
            sig.setPage(1);
            stp.addAnnotation(sig, 1);

            stp.getOverContent(1);
            stp.close();
            return bos.toByteArray();
        } catch (IOException | DocumentException e) {
            throw new ProblemsGeneratingDocumentException(e);
        }
    }

    private String getCertifierUrl(String uniqueIdentifier) {
        UriBuilder uriBuilder = UriBuilder.fromUri(RegistrationProcessConfiguration.getConfiguration().certifierUrl());
        return uriBuilder.path(uniqueIdentifier).build().toASCIIString();
    }


}
