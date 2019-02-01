package pt.ist.fenixedu.giaf.invoices.ui;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Refund;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.ui.spring.controller.AcademicAdministrationSpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

@Controller
@SpringFunctionality(app = AcademicAdministrationSpringApplication.class, title = "title.accounting.refund")
@RequestMapping(AccountingRefundController.REQUEST_MAPPING)
public class AccountingRefundController {

    public static final String REQUEST_MAPPING = "/accounting-refund";

    private static final Logger logger = LoggerFactory.getLogger(AccountingRefundController.class);

    public static class RefundInfo implements Serializable {

        private final Refund refund;
        private Set<SapRequest> sapRequests;
        private final String clientToRefund;

        public RefundInfo(final Refund refund) {
            this.refund = refund;
            this.sapRequests = refund.getSapRequestSet();
            if (sapRequests.isEmpty()) {
                sapRequests = refund.getEvent().getSapRequestSet().stream()
                    .filter(sr -> sr.getRequestType() == SapRequestType.REIMBURSEMENT)
                    .filter(r -> r.getRefund() == null)
                    .filter(r -> r.getValue().equals(refund.getAmount()))
                    .collect(Collectors.toSet());
            }
            if (sapRequests.isEmpty()) {
                sapRequests = refund.getEvent().getSapRequestSet().stream()
                    .filter(sr -> sr.getRequestType() == SapRequestType.REIMBURSEMENT)
                    .filter(r -> r.getRefund() == null)
                    .collect(Collectors.toSet());
            }
            if (refund.getExcessOnly()) {
                clientToRefund = clientToRefund(refund.getEvent().getParty());
            } else {
                clientToRefund = sapRequests.stream()
                    .map(sr -> sr.getClientData())
                    .map(cd -> cd.getClientId() + " " + cd.getCompanyName())
                    .collect(Collectors.joining("<br/>"));
            }
        }

        private String clientToRefund(final Party party) {
            return party == null ? "" : party.getSocialSecurityNumber() + " " + party.getName();        
        }

        public Refund getRefund() {
            return refund;
        }

        public String getClientToRefund() {
            return clientToRefund;
        }

        public Set<SapRequest> getSapRequests() {
            return sapRequests;
        }
    }

    private Comparator<RefundInfo> ENTRY_COMPARATOR = (ri1, ri2) -> {
        final int c = ri1.getRefund().getWhenOccured().compareTo(ri2.getRefund().getWhenOccured());
        return c == 0 ? ri1.getRefund().getExternalId().compareTo(ri1.getRefund().getExternalId()) : c;
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

        final List<RefundInfo> refunds = SapRoot.getInstance().getSapRequestSet().stream()
            .filter(sr -> !sr.getIgnore() && sr.getRequestType() == SapRequestType.REIMBURSEMENT)
            .map(sr -> toRefund(sr))
            .filter(r -> r != null)
            .distinct()
            .filter(r -> !r.getWhenOccured().isBefore(start) && !r.getWhenOccured().isAfter(end))
            .map(r -> new RefundInfo(r))
            .sorted(ENTRY_COMPARATOR.reversed())
            .collect(Collectors.toList());

        model.addAttribute("refunds", refunds);

        return view("search");
    }

    private Refund toRefund(final SapRequest sapRequest) {
        final Refund refundFromRequest = sapRequest.getRefund();
        if (refundFromRequest != null) {
            return refundFromRequest;
        }
        final Event event = sapRequest.getEvent();
        return event.getRefundSet().stream()
            .filter(r -> r.getAccountingTransaction() == null)
            .filter(r -> r.getAmount().equals(sapRequest.getValue()))
            .findAny().orElseGet(() -> event.getRefundSet().stream().filter(r -> r.getAccountingTransaction() == null).findAny().orElse(null));
    }

    protected String view(final String view) {
        return "fenixedu-academic/accounting/refund/" + view;
    }

    private String toString(final DateTime dateTime) {
        return dateTime.toString(ISODateTimeFormat.dateTime());
    }

}
