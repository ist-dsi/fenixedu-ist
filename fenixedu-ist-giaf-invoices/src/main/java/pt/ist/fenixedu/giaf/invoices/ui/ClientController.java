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

import java.io.IOException;

import org.fenixedu.PostalCodeValidator;
import org.fenixedu.TINValidator;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.bennu.core.security.SkipCSRF;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.i18n.I18N;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import eu.europa.ec.taxud.tin.algorithm.TINValid;
import pt.ist.fenixedu.domain.ExternalClient;
import pt.ist.fenixedu.domain.SapRoot;

@SpringFunctionality(app = InvoiceDownlaodController.class, title = "title.client.management")
@RequestMapping("/client-management")
public class ClientController {

    @Autowired
    private MessageSource messageSource;

    private String homeRedirect(final Model model) {
        return "redirect:/client-management";
    }

    @RequestMapping(method = RequestMethod.GET)
    public String home(final Model model) {
        return "client-management/home";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String uploadClientFile(final Model model, final RedirectAttributes redirectAttributes, final @RequestParam( required = true ) MultipartFile file) {
        try {
            final byte[] content = file.getBytes();
            if (content == null || content.length == 0) {
                error(redirectAttributes, "label.error.file.upload.no.content");
            } else {
                final String s = new String(content);
                final String[] lines = s.split("\n");
                int c = 0;
                for (int i = 1; i < lines.length; i++) {
                    final String[] line = lines[i].split("\t");
                    if (line.length != 10) {
                        error(redirectAttributes, "label.error.file.upload.invalid.format.at.line", i+1, lines[i]);
                    } else {
                        final String clientId = line[0];
                        final String vatNumber = line[1];
                        final String fiscalCountry = line[2];
                        final String companyName = line[3];
                        final String country = line[4];
                        final String street = line[5];
                        final String city = line[6];
                        final String region = line[7];
                        final String postalCode = line[8];
                        final String nationality = line[9];

                        if (fiscalCountry.length() != 2 || Country.readByTwoLetterCode(fiscalCountry) == null) {
                            error(redirectAttributes, "label.error.file.upload.invalid.country.at.line", i+1, fiscalCountry);
                        } else if (!TINValidator.isValid(fiscalCountry, vatNumber)) {
                                error(redirectAttributes, "label.error.file.upload.invalid.tinNumber.at.line", i+1, fiscalCountry, vatNumber);
                        } else if (!PostalCodeValidator.isValidAreaCode(fiscalCountry, postalCode)) {
                            error(redirectAttributes, "label.error.file.upload.invalid.postCode.at.line", i+1, fiscalCountry, postalCode);
                        } else {
                            ExternalClient.createOrUpdate(clientId, vatNumber, fiscalCountry, companyName, country, street, city, region, postalCode, nationality);
                            c++;
                        }
                    }
                }
                redirectAttributes.addFlashAttribute("message", messageSource.getMessage("label.clients.createOrUpdatedCount", new Object[] { c }, I18N.getLocale()));
            }
        } catch (final IOException e) {
            throw new Error(e);
        }
        return homeRedirect(model);
    }

    @SkipCSRF
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String search(final Model model, final @RequestParam ExternalClient client) {
        model.addAttribute("client", client.toJson().toString());
        return "client-management/home";
    }

    @SkipCSRF
    @RequestMapping(value = "/availableClients", method = RequestMethod.POST, produces = "application/json; charset=UTF-8")
    public @ResponseBody String availableUnits(final @RequestParam(required = false, value = "term") String term, final Model model) {
        final JsonArray result = new JsonArray();
        final String trimmedValue = term.trim();
        final String[] input = StringNormalizer.normalize(trimmedValue).split(" ");

        SapRoot.getInstance().getExternalClientSet().stream()
            .filter(c -> matchesClient(c, input))
            .forEach(c -> {
                final JsonObject o = new JsonObject();
                o.addProperty("id", c.getExternalId());
                o.addProperty("name", c.getPresentationName());
                result.add(o);
            });

        return result.toString();
    }

    private boolean matchesClient(final ExternalClient c, final String[] input) {
        for (final String s : input) {
            if (c.getAccountId().equals(s) || c.getClientId().equals(s) || c.getVatNumber().equals(s) || c.getCompanyName().contains(s)) {
                return true;
            }
        }
        return false;
    }

    private void error(final RedirectAttributes model, final String key, final Object... args) {
        model.addFlashAttribute("errors", messageSource.getMessage(key, args, I18N.getLocale()));
    }

}
