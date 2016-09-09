package pt.ist.registration.process.ui.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfFormField;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.ui.service.exception.ProblemsGeneratingDocumentException;

@Service
public class RegistrationDeclarationCreatorService {

    private final Font FONT_SMALL = FontFactory.getFont("Arial", 8, Font.NORMAL);
    private final Font FONT_NORMAL = FontFactory.getFont("Arial", 12, Font.NORMAL);
    private final Font FONT_HEADING_BOLD = FontFactory.getFont("Arial", 12, Font.NORMAL);
    private final Font FONT_TITLE = FontFactory.getFont("Arial", 22, Font.BOLD);

    @Atomic(mode = TxMode.WRITE)
    public RegistrationDeclarationFile generateAndSaveDocumentsForRegistration(Registration registration)
            throws ProblemsGeneratingDocumentException {
        Person person = registration.getPerson();
        String executionYear = ExecutionYear.readCurrentExecutionYear().getName();
        String studentName = person.getName().toUpperCase();
        String documentType = person.getIdDocumentType().getLocalizedName(new Locale("pt"));
        String idNumber = person.getDocumentIdNumber();
        String address = person.getAddress();
        String postalCode = person.getPostalCode();
        String curricularYear = Integer.toString(registration.getCurricularYear());
        String gender = person.getGender().toString();
        String course = registration.getDegree().getName();
        String naturality = person.getDistrictOfBirth();

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("studentName", studentName);
        ctx.put("gender", gender);
        ctx.put("executionYear", executionYear);
        ctx.put("naturality", naturality);
        ctx.put("documentType", documentType);
        ctx.put("idNumber", idNumber);
        ctx.put("address", address);
        ctx.put("postalCode", postalCode);
        ctx.put("curricularYear", curricularYear);
        ctx.put("course", course);

        try {
            byte[] document = generateDocument(ctx, registration);

            if (registration.getRegistrationDeclarationFile() != null) {
                registration.getRegistrationDeclarationFile().delete();
            }

            return new RegistrationDeclarationFile(idNumber + "_registration_declaration.pdf", document, registration);
        } catch (PebbleException | DocumentException | IOException e) {
            throw new ProblemsGeneratingDocumentException(e);
        }

    }

    private byte[] generateDocument(Map<String, Object> ctx, Registration registration)
            throws PebbleException, DocumentException, IOException {

        PebbleEngine engine = new PebbleEngine();

        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, bos);

        // todo: change this to put in the generic file
        document.open();
        PdfContentByte cb = writer.getDirectContent();

        PdfReader reader = new PdfReader(getClass().getResourceAsStream("/A01.pdf"));
        PdfImportedPage page = writer.getImportedPage(reader, 1);

        document.newPage();
        cb.addTemplate(page, 0, 0);
        generateHeader(document);
        generateBody(document, ctx, engine);

        document.close();

        return bos.toByteArray();

    }

    public byte[] generateDocumentWithSignatureField(Registration registration, String signatureFieldName)
            throws ProblemsGeneratingDocumentException {
        if (registration.getRegistrationDeclarationFile() == null) {
            return null;
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PdfReader original = new PdfReader(registration.getRegistrationDeclarationFile().getStream());
            PdfStamper stp = new PdfStamper(original, bos);
            PdfFormField sig = PdfFormField.createSignature(stp.getWriter());
            sig.setWidget(new Rectangle(100, 300, 500, 200), null);
            sig.setFlags(PdfAnnotation.FLAGS_PRINT);
            sig.put(PdfName.DA, new PdfString("/Helv 0 Tf 0 g"));
            sig.setFieldName(signatureFieldName);
            sig.setPage(1);
            stp.addAnnotation(sig, 1);

            PdfContentByte canvas = stp.getOverContent(1);
            Paragraph disclaimer = new Paragraph(
                    "Assinatura aposta no uso de poderes do Presidente do IST, delegados ou subdelegados no subscritor. A força probatória dum documento em que tenha sido aposta uma assinatura digital é a conferida pelo art.º 376 do Código Civil (por força do disposto no n.º 1 e no n.º 2 do art.º 3 do Decreto-Lei n.º 290-D/99, de 2 de Agosto).",
                    FONT_SMALL);
            disclaimer.setAlignment(Element.ALIGN_JUSTIFIED);
            ColumnText ct = new ColumnText(canvas);
            ct.setSimpleColumn(50, 150f, 540f, 50f);
            ct.addElement(disclaimer);
            ct.go();

            stp.close();
            return bos.toByteArray();
        } catch (IOException | DocumentException e) {
            throw new ProblemsGeneratingDocumentException(e);
        }
    }

    private void generateBody(Document document, Map<String, Object> ctx, PebbleEngine engine)
            throws DocumentException, PebbleException, IOException {
        Paragraph p = new Paragraph("DECLARAÇÃO", FONT_TITLE);
        p.setSpacingBefore(75);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setPaddingTop(400);
        p.setSpacingAfter(50);
        document.add(p);

        PebbleTemplate compiledTemplate = engine.getTemplate("template.txt");
        Writer writer = new StringWriter();

        compiledTemplate.evaluate(writer, ctx);
//		Paragraph title = new Paragraph(
//				"O/A TÉCNICO/A SUPERIOR DA SECÇÃO DE GRADUAÇÃO DO INSTITUTO SUPERIOR TÉCNICO DA UNIVERSIDADE DE LISBOA",
//				FONT_BOLD);
//		document.add(title);

        Paragraph body = new Paragraph(writer.toString(), FONT_NORMAL);
        body.setSpacingBefore(10);
        body.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(body);

        Paragraph paragraphDate = new Paragraph("Secretaria dos serviços académicos");
        paragraphDate.setSpacingBefore(30);
        document.add(paragraphDate);
    }

    private void generateHeader(Document document) throws DocumentException {
        Paragraph graduationUnitParagraph = new Paragraph("Secção de Graduação", FONT_HEADING_BOLD);
        graduationUnitParagraph.setAlignment(Element.ALIGN_RIGHT);
        graduationUnitParagraph.setSpacingAfter(5);
        document.add(graduationUnitParagraph);

        LineSeparator line = new LineSeparator(1, 40, null, Element.ALIGN_RIGHT, -5);
        document.add(line);

        Paragraph officeParagraph = new Paragraph("Secretaria dos Serviços Académicos");
        officeParagraph.setSpacingBefore(5);
        officeParagraph.setAlignment(Element.ALIGN_RIGHT);
        document.add(officeParagraph);
    }

}
