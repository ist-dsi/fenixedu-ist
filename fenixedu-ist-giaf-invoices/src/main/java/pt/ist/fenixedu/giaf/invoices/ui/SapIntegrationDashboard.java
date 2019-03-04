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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@SpringFunctionality(app = InvoiceDownloadController.class, title = "title.client.management")
@RequestMapping("/sap-integration-dashboard")
public class SapIntegrationDashboard {

    public static class Report {

        private int pending;
        private int sent;
        private int integrated;
        private Money pendingValue = Money.ZERO;
        private Money errorValue = Money.ZERO;
        private Map<String, int[]> errors = Collections.synchronizedMap(new HashMap<>());

        public Report() {
        }

        public int getPending() {
            return pending;
        }

        public int getSent() {
            return sent;
        }

        public int getIntegrated() {
            return integrated;
        }

        public int getErrorCount() {
            return sent - integrated;
        }

        public Money getPendingValue() {
            return pendingValue;
        }

        public Money getErrorValue() {
            return errorValue;
        }

        public SortedMap<String, Integer> getErrors() {
            SortedMap<String, Integer> result = new TreeMap<>();
            errors.forEach((k, v) -> {
                result.put(k, Integer.valueOf(v[0]));
            });
            return result;
        }

    }
    
    public static class Dashboard {

        private SortedMap<SapRequestType, Report> typeReport = Collections.synchronizedSortedMap(new TreeMap<>());

        public Dashboard() {
            SapRoot.getInstance().getSapRequestSet().stream()
                .parallel().forEach(this::process);;
        }

        private void process(final SapRequest sapRequest) {
            processSapRequest(sapRequest, () -> reportFor(sapRequest), (report) -> {
                final JsonObject integrationMessage = sapRequest.getIntegrationMessageAsJson();
                process(report, integrationMessage, "Cliente");
                process(report, integrationMessage, "Documento");
            });
        }

        private Report reportFor(final SapRequest sapRequest) {
            final SapRequestType type = sapRequest.getRequestType();
            return typeReport.computeIfAbsent(type, (t) -> new Report());
        }

        private void process(final Report report, final JsonObject integrationMessage, final String key) {
            final JsonElement error = integrationMessage.get(key);
            if (error != null && !error.isJsonNull()) {
                if (error.isJsonArray()) {
                    error.getAsJsonArray().forEach(e -> process(report, e.getAsJsonObject()));
                } else {
                    process(report, error.getAsJsonObject());
                }
            }
        }

        private void process(final Report report, final JsonObject error) {
            final String message = error.get("Mensagem").getAsString();
            report.errors.computeIfAbsent(message, (k) -> new int[] { 0 })[0]++;
        }

        public SortedMap<SapRequestType, Report> getTypeReport() {
            return typeReport;
        }

    }

    private static void processSapRequest(final SapRequest sapRequest, final Supplier<Report> reportSupplier, final Consumer<Report> consumer) {
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                if (!sapRequest.getIgnore()) {
                    final Report report = reportSupplier.get();
                    if (sapRequest.getSent()) {
                        report.sent++;
                        if (sapRequest.getIntegrated()) {
                            report.integrated++;
                        } else {
                            synchronized (report) {
                                report.errorValue = report.errorValue.add(sapRequest.getValue().add(sapRequest.getAdvancement()));
                            }
                            consumer.accept(report);
                        }
                    } else {
                        report.pending++;
                        synchronized (report) {
                            report.pendingValue = report.pendingValue.add(sapRequest.getValue().add(sapRequest.getAdvancement()));
                        }
                    }
                }
                return null;
            }, new AtomicInstance(Atomic.TxMode.READ, false));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public String home(final Model model) {
        model.addAttribute("dashboard", new Dashboard());
        return "sap-integration/dashboard";
    }

    @RequestMapping(value = "/{sapRequestType}", method = RequestMethod.GET)
    public String sapRequestType(@PathVariable final SapRequestType sapRequestType, final Model model) {
        final Report report = new Report();
        final Set<SapRequest> errors = new TreeSet<>(SapRequest.COMPARATOR_BY_EVENT_AND_ORDER);
        SapRoot.getInstance().getSapRequestSet().stream()
            .filter(sr -> sr.getRequestType() == sapRequestType)
            .forEach(sr -> processSapRequest(sr, () -> report, (r) -> errors.add(sr)));
        model.addAttribute("sapRequestType", sapRequestType);
        model.addAttribute("report", report);
        model.addAttribute("errors", errors);
        return "sap-integration/sapRequestType";
    }

}
