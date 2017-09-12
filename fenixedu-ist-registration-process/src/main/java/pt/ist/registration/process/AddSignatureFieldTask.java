package pt.ist.registration.process;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfFormField;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfString;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
public class AddSignatureFieldTask {

    public static void main(String... args) throws IOException {
        byte[] bytes = addSignatureField(100, 220, 500, 120, "signatureField", new FileInputStream("/tmp/file.pdf"));

        try (FileOutputStream fileOutputStream = new FileOutputStream("/tmp/y.pdf")) {
            fileOutputStream.write(bytes);
        }

    }
    
    private static byte[] addSignatureField(int llx, int lly, int urx, int ury, String signatureFieldName, InputStream
            originalPdfStream) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PdfReader original = new PdfReader(originalPdfStream);
            PdfStamper stp = new PdfStamper(original, bos);
            PdfFormField sig = PdfFormField.createSignature(stp.getWriter());
            sig.setWidget(new Rectangle(llx, lly, urx, ury), null);
            sig.setFlags(PdfAnnotation.FLAGS_PRINT);
            sig.put(PdfName.DA, new PdfString("/Helv 0 Tf 0 g"));
            sig.setFieldName(signatureFieldName);
            sig.setPage(2);
            stp.addAnnotation(sig, 2);
            stp.getOverContent(2);
            stp.close();
            return bos.toByteArray();
        } catch (IOException | DocumentException e) {
            throw new Error(e);
        }
    }
}
