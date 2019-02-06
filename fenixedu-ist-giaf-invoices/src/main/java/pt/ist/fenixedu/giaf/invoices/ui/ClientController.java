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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fenixedu.PostalCodeValidator;
import org.fenixedu.TINValidator;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.PaymentMethod;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.ui.spring.controller.manager.PaymentMethodService;
import org.fenixedu.bennu.SapSdkConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.security.SkipCSRF;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.domain.ExternalClient;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.dml.runtime.RelationAdapter;
import pt.ist.sap.client.SapStructure;

@SpringFunctionality(app = InvoiceDownloadController.class, title = "title.client.management")
@RequestMapping("/client-management")
public class ClientController {

    static {
        PaymentMethod.getRelationAccountingTransactionDetailPaymentMethod().addListener(new RelationAdapter<AccountingTransactionDetail, PaymentMethod>() {

            @Override
            public void beforeAdd(final AccountingTransactionDetail txd, final PaymentMethod pm) {
                if (txd != null && pm != null && pm == Bennu.getInstance().getInternalPaymentMethod() && !Group.dynamic("sapIntegrationManager").isMember(Authenticate.getUser())) {
                    throw new DomainException(BundleUtil.getLocalizedString("resources.GiafInvoicesResources", "error.only.sapIntegrationManager.can.use.this.payment.method").getContent());
                }
            }

        });
    }


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
                        final String clientId = line[0].trim();
                        final String vatNumber = line[1].trim();
                        final String fiscalCountry = line[2].trim();
                        final String companyName = line[3].trim();
                        final String country = line[4].trim();
                        final String street = line[5].trim();
                        final String city = line[6].trim();
                        final String region = line[7].trim();
                        final String postalCode = line[8].trim();
                        final String nationality = line[9].trim();

                        if (fiscalCountry.length() != 2 || Country.readByTwoLetterCode(fiscalCountry) == null) {
                            error(redirectAttributes, "label.error.file.upload.invalid.country.at.line", i+1, fiscalCountry);
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
        if (client != null) {
            model.addAttribute("client", client.toJson().toString());
        }
        return "client-management/home";
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public String delete(final Model model, final @RequestParam ExternalClient clientToDelete) {
        if (clientToDelete != null) {
            clientToDelete.delete();
        }
        return homeRedirect(model);
    }

    @SkipCSRF
    @RequestMapping(value = "/availableClients", method = RequestMethod.POST, produces = "application/json; charset=UTF-8")
    public @ResponseBody String availableClients(final @RequestParam(required = false, value = "term") String term, final Model model) {
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

    @SkipCSRF
    @RequestMapping(value = "/availableInternalUnits", method = RequestMethod.POST, produces = "application/json; charset=UTF-8")
    public @ResponseBody String availableInternalUnits(final @RequestParam(required = false, value = "term") String term, final Model model) {
        final JsonArray result = new JsonArray();
        final String trimmedValue = term.trim();
        final String[] input = StringNormalizer.normalize(trimmedValue).split(" ");

        final SapStructure sapStructure = new SapStructure();
        final JsonObject sapInput = new JsonObject();
        sapInput.addProperty("institution", SapSdkConfiguration.getConfiguration().sapServiceInstitutionCode());

        final JsonArray projects = sapStructure.listSanitizedProjects(sapInput);
        for (final JsonElement pepElement : sapStructure.listSanitizedProjects(sapInput)) {
            final JsonObject pepObject = pepElement.getAsJsonObject();
            final String pepUnitSapId = getString(pepObject, "unitSapId");

            for (final JsonElement aggregationElement : pepObject.get("elements").getAsJsonArray()) {
                final JsonObject aggregationObject = aggregationElement.getAsJsonObject();

                for (final JsonElement actionElement : aggregationObject.get("elements").getAsJsonArray()) {
                    final JsonObject actionObject = actionElement.getAsJsonObject();
                    final String actionDescription = getString(actionObject, "description");
                    final String actionElementSapId = getString(actionObject, "elementSapId");
                    final String aggregationDescription = getString(aggregationObject, "description");
                    final String subProjectName = aggregationDescription + " - " + actionDescription;

                    if (matches(input, actionElementSapId) || matches(input, subProjectName)) {

                        final JsonObject o = new JsonObject();
                        o.addProperty("id", actionElementSapId);
                        o.addProperty("name", actionElementSapId);
                        result.add(o);
                    }
                }
            }
        }

        return result.toString();
    }

    private static String getString(final JsonObject jo, final String key) {
        final JsonElement e = jo.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET, produces = "application/xlsx")
    public void download(final HttpServletResponse response) {
        final String name = "ExternalClients";
        final Spreadsheet sheet = new Spreadsheet(name);
        SapRoot.getInstance().getExternalClientSet().stream()
            .forEach(c -> {
                final Row row = sheet.addRow();
                row.setCell("AccountId", c.getAccountId());
                row.setCell("BillingIndicator", c.getBillingIndicator());
                row.setCell("City", c.getCity());
                row.setCell("ClientId", c.getClientId());
                row.setCell("CompanyName", c.getCompanyName());
                row.setCell("Country", c.getCountry());
                row.setCell("FiscalCountry", c.getFiscalCountry());
                row.setCell("Nationality", c.getNationality());
                row.setCell("PostalCode", c.getPostalCode());
                row.setCell("Region", c.getRegion());
                row.setCell("Street", c.getStreet());
                row.setCell("VatNumber", c.getVatNumber());
            });
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            sheet.exportToXLSSheet(stream);
            response.setHeader("Content-Disposition", "attachment; filename=" + name + ".xlsx");
            response.getOutputStream().write(stream.toByteArray());
            response.flushBuffer();
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    @RequestMapping(value = "/manageDefaultPaymentMethods", method = RequestMethod.POST)
    public String manageDefaults(Model model, @RequestParam PaymentMethod defaultCashPaymentMethod,
            @RequestParam PaymentMethod defaultSibsPaymentMethod, @RequestParam PaymentMethod defaultRefundPaymentMethod,
            @RequestParam PaymentMethod defaultInternalPaymentMethod, final HttpServletRequest request) {
        try {
            new PaymentMethodService().setDefaultPaymentMethods(defaultCashPaymentMethod, defaultSibsPaymentMethod, defaultRefundPaymentMethod);
            FenixFramework.atomic(() -> {
                defaultInternalPaymentMethod.setInternalBennu(Bennu.getInstance());
            });
            return "redirect:/payment-methods-management";
        } catch (DomainException de) {
            return "redirect:/payment-methods-management/manageDefaults";
        }
    }

    private boolean matchesClient(final ExternalClient c, final String[] input) {
        if (input.length == 0) {
            return false;
        }
        int matchCount = 0;
        final String accountId = StringNormalizer.normalize(c.getAccountId());
        final String clientId = StringNormalizer.normalize(c.getClientId());
        final String vatNumber = StringNormalizer.normalize(c.getVatNumber());
        final String companyName = StringNormalizer.normalize(c.getCompanyName());
        for (final String s : input) {
            if (accountId.indexOf(s) >= 0 || clientId.indexOf(s) >= 0 || vatNumber.indexOf(s) >= 0 || companyName.indexOf(s) >= 0) {
                matchCount++;
            }
        }
        return matchCount == input.length;
    }

    private boolean matches(final String[] input, final String string) {
        if (string == null || string.isEmpty() || input.length == 0) {
            return false;
        }
        for (final String s : input) {
            if (string.indexOf(s) < 0 ) {
                return false;
            }
        }
        return true;
    }

    private void error(final RedirectAttributes model, final String key, final Object... args) {
        model.addFlashAttribute("errors", messageSource.getMessage(key, args, I18N.getLocale()));
    }

}
