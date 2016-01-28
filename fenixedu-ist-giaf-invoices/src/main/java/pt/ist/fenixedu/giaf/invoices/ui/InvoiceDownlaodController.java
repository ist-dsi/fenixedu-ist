package pt.ist.fenixedu.giaf.invoices.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import pt.ist.fenixedu.giaf.invoices.GiafInvoice;

@SpringFunctionality(app = InvoiceController.class, title = "title.giaf.invoice.viewer")
@RequestMapping("/giaf-invoice-downloader")
public class InvoiceDownlaodController {

    @RequestMapping(value = "/{detail}", method = RequestMethod.GET, produces = "application/pdf")
    public void invoice(@PathVariable AccountingTransactionDetail detail, final HttpServletResponse response) {
        final String id = detail.getExternalId();
        final String invoiceNumber = InvoiceController.toInvoiceNumber(id);
        final File file = GiafInvoice.fileForDocument(id);
        if (file.exists()) {
            try (final FileInputStream inputStream = new FileInputStream(file)) {
                response.setHeader("Content-Disposition", "attachment; filename=" + invoiceNumber + ".pdf");
                IOUtils.copy(inputStream, response.getOutputStream());
                response.flushBuffer();
            } catch (final IOException e) {
                throw new Error(e);
            }
        }
    }

}
