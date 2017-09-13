package pt.ist.registration.process.ui.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.io.ByteStreams;
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
import pt.ist.registration.process.domain.DeclarationTemplate;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.domain.SignatureFieldSettings;
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

    public RegistrationDeclarationFile generateAndSaveFile(Registration registration,
            ExecutionYear executionYear, DeclarationTemplate declarationTemplate) throws ProblemsGeneratingDocumentException {

        String username = registration.getPerson().getUsername();
        String language = declarationTemplate.getLocale().getLanguage();
        String degreeName = registration.getDegree().getSigla();
        String executionYearName = executionYear.getName().replaceAll("/", "-");
        Locale locale = declarationTemplate.getLocale();

        String displayName =  String.format(declarationTemplate.getDisplayNameFormat(), username, language, degreeName,
                executionYearName);
        String filename = String.format(declarationTemplate.getFilenameFormat(), executionYearName, locale.getLanguage(), registration
                .getDegree().getSigla(), registration.getPerson().getUsername());

        String uniqueIdentifier = UUID.randomUUID().toString();
        JsonObject payload = getRegistrationDeclarationPayload(registration, executionYear, locale, uniqueIdentifier);

        try (ByteArrayInputStream templateStream = new ByteArrayInputStream(declarationTemplate.getTemplateHtml().getBytes(StandardCharsets.UTF_8))){
            byte[] document = null;
            InputStream documentStream = pdfRendererService.render(templateStream, payload);
            if (declarationTemplate.getSignatureFieldsSettings() != null) {
                document = generateDocumentWithSignatureField(documentStream, declarationTemplate
                        .getSignatureFieldsSettings());
            } else {
                document = ByteStreams.toByteArray(documentStream);
            }
            
            return createRegistrationDocumentFile(registration, executionYear, locale, uniqueIdentifier, document, displayName, filename);
        } catch (IOException e) {
            throw new ProblemsGeneratingDocumentException(e);
        }
    }

    @Atomic(mode = TxMode.WRITE)
    protected RegistrationDeclarationFile createRegistrationDocumentFile(Registration registration, ExecutionYear
            executionYear, Locale locale, String uniqueIdentifier, byte[] document, String displayName, String filename) {
        return new RegistrationDeclarationFile(filename, displayName, document, registration, executionYear, locale, uniqueIdentifier);
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

    public byte[] generateDocumentWithSignatureField(InputStream fileStream, SignatureFieldSettings settings)
            throws
    ProblemsGeneratingDocumentException {
        if (fileStream == null) {
            return null;
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PdfReader original = new PdfReader(fileStream);
            PdfStamper stp = new PdfStamper(original, bos);
            PdfFormField sig = PdfFormField.createSignature(stp.getWriter());
            sig.setWidget(new Rectangle(settings.getLlx(), settings.getLly(),
            settings.getUrx(), settings.getUry()), null);
            sig.setFlags(PdfAnnotation.FLAGS_PRINT);
            sig.put(PdfName.DA, new PdfString("/Helv 0 Tf 0 g"));
            sig.setFieldName(settings.getName());
            sig.setPage(settings.getPage());
            stp.addAnnotation(sig, settings.getPage());

            stp.getOverContent(settings.getPage());
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
