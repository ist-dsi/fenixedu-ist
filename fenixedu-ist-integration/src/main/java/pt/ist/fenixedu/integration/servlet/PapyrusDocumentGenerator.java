package pt.ist.fenixedu.integration.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.fenixedu.academic.domain.serviceRequests.documentRequests.DefaultDocumentGenerator;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DocumentGenerator;
import org.fenixedu.academic.report.FenixReport;
import org.fenixedu.bennu.BennuSpringContextHelper;
import org.fenixedu.bennu.papyrus.domain.PapyrusTemplate;
import org.fenixedu.bennu.papyrus.service.PapyrusPdfRendererService;

import com.google.common.io.ByteStreams;

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
        if (documents.size() == 1) {
            FenixReport fenixReport = documents.get(0);
            Optional<? extends PapyrusTemplate> template = PapyrusTemplate.findByNameAndLocale(fenixReport.getReportTemplateKey(), fenixReport.getLanguage());
            if (template.isPresent()) {
                return generate(template.get(), fenixReport);
            }
        }

        return defaultDocumentGenerator.generateReport(documents);
    }

    private byte[] generate(PapyrusTemplate template, FenixReport fenixReport) {
        try (ByteArrayInputStream templateStream = new ByteArrayInputStream(template.getTemplateHtml()
                                                                                .getBytes(StandardCharsets.UTF_8))) {
            InputStream result;
            if (template.getPrintSettings() != null) {
                result = getRendererService().render(templateStream, fenixReport.getPayload(), template.getPrintSettings());
            } else {
                result = getRendererService().render(templateStream, fenixReport.getPayload());
            }
            return ByteStreams.toByteArray(result);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

}
