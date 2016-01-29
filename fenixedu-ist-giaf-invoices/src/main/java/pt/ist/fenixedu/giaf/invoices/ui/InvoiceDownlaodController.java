/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Invoices.
 *
 * FenixEdu IST GIAF Invoices is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Invoices is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Invoices.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.giaf.invoices.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import pt.ist.fenixedu.giaf.invoices.GiafInvoice;

import com.google.common.io.ByteStreams;

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
                ByteStreams.copy(inputStream, response.getOutputStream());
                response.flushBuffer();
            } catch (final IOException e) {
                throw new Error(e);
            }
        }
    }

}
