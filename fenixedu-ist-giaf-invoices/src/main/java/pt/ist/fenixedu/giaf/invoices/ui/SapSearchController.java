package pt.ist.fenixedu.giaf.invoices.ui;

import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;

import java.util.Set;

@SpringFunctionality(app = InvoiceDownloadController.class, title = "title.sap.search")
@RequestMapping("/sap-search")
public class SapSearchController {

    @RequestMapping(method = RequestMethod.GET)
    public String home(final Model model) {
        return "sap-integration/search";
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String search(final Model model, final String sapNumber) {
        String eventOid = SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sapRequest -> sapNumber.equalsIgnoreCase(sapRequest.getDocumentNumber())
                    || sapNumber.equalsIgnoreCase(sapRequest.getSapDocumentNumber()))
                .map(sapRequest -> sapRequest.getEvent().getExternalId())
                .findAny().orElse(null);

        if (eventOid == null) {
            model.addAttribute("sizeWarning", BundleUtil.getString("resources.GiafInvoicesResources", "warning.no.documents"));
            return "sap-integration/search";
        } else {
            model.addAttribute("event", eventOid);
            return "sap-invoice-viewer/eventDetails";
        }
    }
}
