package pt.ist.fenixedu.integration.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Strings;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DefaultDocumentGenerator;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DocumentGenerator;
import org.fenixedu.academic.report.FenixReport;
import org.fenixedu.bennu.papyrus.service.PdfRenderingException;
import org.fenixedu.bennu.spring.BennuSpringContextHelper;
import org.fenixedu.bennu.papyrus.domain.PapyrusTemplate;
import org.fenixedu.bennu.papyrus.service.PapyrusPdfRendererService;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
public class PapyrusDocumentGenerator implements DocumentGenerator {

    private PapyrusPdfRendererService rendererService;
    private DocumentGenerator defaultDocumentGenerator;

    PapyrusDocumentGenerator() {
        this.defaultDocumentGenerator = new DefaultDocumentGenerator();
    }

    private PapyrusPdfRendererService getRendererService() {
        if (this.rendererService == null) {
            this.rendererService = BennuSpringContextHelper.getBean(PapyrusPdfRendererService.class);
        }
        return this.rendererService;
    }

    @Override
    public byte[] generateReport(List<? extends FenixReport> documents) {
        Optional<? extends PapyrusTemplate> template;

        if (documents.size() == 1) {
            final FenixReport fenixReport = documents.get(0);

            template = PapyrusTemplate.findByNameAndLocale(fenixReport.getReportTemplateKey(), fenixReport.getLanguage());
            if (template.isPresent()) {
                return generate(template.get(), fenixReport);
            }
        }

        return defaultDocumentGenerator.generateReport(documents);
    }

    private byte[] generate(PapyrusTemplate template, FenixReport fenixReport) {
        String templateHtml = template.getTemplateHtml();

        if (Strings.isNullOrEmpty(templateHtml)) {
            try {
                return getRendererService()
                        .renderToByteArray(template.getName(), fenixReport.getLanguage(), fenixReport.getPayload());
            } catch (PdfRenderingException e) {
                throw new Error(e);
            }
        }

        try (ByteArrayInputStream templateStream = new ByteArrayInputStream(templateHtml.getBytes(StandardCharsets.UTF_8))) {
            if (template.getPrintSettings() != null) {
                return getRendererService()
                        .renderToByteArray(templateStream, fenixReport.getPayload(), template.getPrintSettings());
            }

            return getRendererService().renderToByteArray(templateStream, fenixReport.getPayload());
        } catch (IOException | PdfRenderingException e) {
            throw new Error(e);
        }
    }

}
