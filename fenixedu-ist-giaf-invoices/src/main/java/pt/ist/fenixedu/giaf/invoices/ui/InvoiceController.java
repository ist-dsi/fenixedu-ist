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

import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.giaf.invoices.GiafEvent;

@SpringApplication(group = "logged", path = "giaf-invoice-viewer", title = "title.giaf.invoice.viewer",
        hint = "giaf-invoice-viewer")
@SpringFunctionality(app = InvoiceController.class, title = "title.giaf.invoice.viewer")
@RequestMapping("/giaf-invoice-viewer")
public class InvoiceController {

    @RequestMapping
    public String home(@RequestParam(required = false) String username, final Model model) {
        final User user = getUser(username);

        if (isAllowedToAccess(user)) {
            final Person person = user.getPerson();
            
            final JsonArray events = new JsonArray();
            person.getEventsSet().stream().sorted(Event.COMPARATOR_BY_DATE).map(this::toJsonArray).forEach(a -> events.addAll(a));
            model.addAttribute("events", events);
        }

        return "giaf-invoice-viewer/home";
    }

    private boolean isAllowedToAccess(final User user) {
        final User currentUser = Authenticate.getUser();
        return currentUser == user || isAcademicServiceStaff(currentUser);
    }

    private boolean isAcademicServiceStaff(final User user) {
        return AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_STUDENT_PAYMENTS).isMember(user)
                || AcademicAuthorizationGroup.get(AcademicOperationType.MANAGE_STUDENT_PAYMENTS_ADV).isMember(user);
    }

    private User getUser(final String username) {
        return username == null || username.isEmpty() ? Authenticate.getUser() : User.findByUsername(username);
    }

    private JsonArray toJsonArray(final Event event) {
        final JsonArray result = GiafEvent.readEventFile(event);
        for (final JsonElement e : result) {
            final JsonObject o = (JsonObject) e;
            o.addProperty("eventId", event.getExternalId());
            o.addProperty("eventDescription", event.getDescription().toString());
        }
        
        return result;
    }

    public static <T extends JsonElement> Collector<T, JsonArray, JsonArray> toJsonArray() {
        return Collector.of(JsonArray::new, (array, element) -> array.add(element), (one, other) -> {
            one.addAll(other);
            return one;
        }, Characteristics.IDENTITY_FINISH);
    }

}
