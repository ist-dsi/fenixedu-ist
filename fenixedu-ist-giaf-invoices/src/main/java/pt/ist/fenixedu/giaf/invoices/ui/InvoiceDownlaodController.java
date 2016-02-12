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

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.io.ByteStreams;

import pt.ist.fenixedu.giaf.invoices.GiafEvent;

@SpringFunctionality(app = InvoiceController.class, title = "title.giaf.invoice.viewer")
@RequestMapping("/giaf-invoice-downloader")
public class InvoiceDownlaodController {

    @RequestMapping(value = "/{event}/{filename}", method = RequestMethod.GET, produces = "application/pdf")
    public void invoice(@PathVariable Event event, @PathVariable String filename, final HttpServletResponse response) {
        if (sAllowedToAccess(event)) {
            final File file =  GiafEvent.receiptFile(event, filename);
            if (file != null && file.exists()) {
                try (final FileInputStream inputStream = new FileInputStream(file)) {
                    response.setHeader("Content-Disposition", "attachment; filename=" + filename);
                    ByteStreams.copy(inputStream, response.getOutputStream());
                    response.flushBuffer();
                } catch (final IOException e) {
                    throw new Error(e);
                }
            }
        } else {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getClass();
        }
    }

    private boolean sAllowedToAccess(final Event event) {
        final User user = Authenticate.getUser();
        return isOwner(event, user) || isAcademicServiceStaff(user);
    }

    private boolean isOwner(final Event event, final User user) {
        final Person person = event == null ? null : event.getPerson();
        return user != null && user == person.getUser();
    }

    private boolean isAcademicServiceStaff(final User user) {
        return AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_STUDENT_PAYMENTS).isMember(user)
                || AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_STUDENT_PAYMENTS_ADV).isMember(user);
    }

}
