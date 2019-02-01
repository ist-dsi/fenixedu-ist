package pt.ist.fenixedu.giaf.invoices.ui;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.ui.spring.controller.AcademicAdministrationSpringApplication;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.FenixFramework;

@Controller
@SpringFunctionality(app = AcademicAdministrationSpringApplication.class, title = "title.accounting.internalPayment")
@RequestMapping(AccountingInternalPaymentController.REQUEST_MAPPING)
public class AccountingInternalPaymentController {

    public static final String REQUEST_MAPPING = "/accounting-internalPayment";

    private static final Logger logger = LoggerFactory.getLogger(AccountingInternalPaymentController.class);

    public static class InternalPaymentInfo implements Serializable {

        private final SapRequest sapRequest;
        private final SapRequest invoiceSapRequest;
        private final AccountingTransaction accountingTransaction;

        public InternalPaymentInfo(final SapRequest sapRequest, final AccountingTransaction accountingTransaction) {
            this.sapRequest = sapRequest;
            this.accountingTransaction = accountingTransaction;
            this.invoiceSapRequest = sapRequest.getEvent().getSapRequestSet().stream()
                    .filter(sr -> sr != sapRequest && sapRequest.refersToDocument(sr.getDocumentNumber()))
                    .findAny().orElse(null);
        }

        public SapRequest getSapRequest() {
            return sapRequest;
        }

        public AccountingTransaction getAccountingTransaction() {
            return accountingTransaction;
        }

        public SapRequest getInvoiceSapRequest() {
            return invoiceSapRequest;
        }

    }

    private Comparator<InternalPaymentInfo> ENTRY_COMPARATOR = (ipi1, ipi2) -> {
        final int c = ipi1.getSapRequest().getWhenSent().compareTo(ipi2.getSapRequest().getWhenSent());
        return c == 0 ? ipi1.getSapRequest().getExternalId().compareTo(ipi2.getSapRequest().getExternalId()) : c;
    };

    @RequestMapping(method = RequestMethod.GET)
    public String home() {
        final DateTime now = new DateTime();
        return "redirect:" + REQUEST_MAPPING + "/search?start=" + toString(now.minusDays(3)) + "&end=" + toString(now);
    }

    @RequestMapping(value="/search", method = RequestMethod.GET)
    public String search(final @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime start,
            final @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime end, final Model model) {
        model.addAttribute("start", start);
        model.addAttribute("end", end);

        final List<InternalPaymentInfo> internalPayments = SapRoot.getInstance().getSapRequestSet().stream()
            .filter(sr -> !sr.getIgnore() && sr.getIntegrated() && sr.getRequestType() == SapRequestType.CREDIT && sr.getRequest().length() > 2)
            .filter(sr -> !sr.getWhenSent().isBefore(start) && !sr.getWhenSent().isAfter(end))
            .map(sr -> toInternalPaymentInfo(sr))
            .filter(ipi -> ipi != null)
            .sorted(ENTRY_COMPARATOR.reversed())
            .collect(Collectors.toList());

        model.addAttribute("internalPayments", internalPayments);

        return view("search");
    }

    private InternalPaymentInfo toInternalPaymentInfo(final SapRequest sapRequest) {
        final DomainObject object = FenixFramework.getDomainObject(sapRequest.getCreditId());
        if (object instanceof AccountingTransaction) {
            final AccountingTransaction tx = (AccountingTransaction) object;
            if (tx.getPaymentMethod() == Bennu.getInstance().getInternalPaymentMethod()) {
                return new InternalPaymentInfo(sapRequest, tx);
            }
        }
        return null;
    }

    protected String view(final String view) {
        return "fenixedu-academic/accounting/internalPayment/" + view;
    }

    private String toString(final DateTime dateTime) {
        return dateTime.toString(ISODateTimeFormat.dateTime());
    }

    @RequestMapping(value="/{sapRequest}/consolidate", method = RequestMethod.POST)
    public String consolidate(final @PathVariable SapRequest sapRequest, final Model model) {
        final DateTime whenSent = sapRequest.getWhenSent();
        sapRequest.consolidate();
        return "redirect:" + REQUEST_MAPPING + "/search?start=" + toString(whenSent.minusDays(2)) + "&end=" + toString(whenSent.plusDays(1));
    }

    @RequestMapping(value="/{sapRequest}/revertConsolidation", method = RequestMethod.POST)
    public String revertConsolidation(final @PathVariable SapRequest sapRequest, final Model model) {
        final DateTime whenSent = sapRequest.getWhenSent();
        sapRequest.revertConsolidation();
        return "redirect:" + REQUEST_MAPPING + "/search?start=" + toString(whenSent.minusDays(2)) + "&end=" + toString(whenSent.plusDays(1));
    }

}
