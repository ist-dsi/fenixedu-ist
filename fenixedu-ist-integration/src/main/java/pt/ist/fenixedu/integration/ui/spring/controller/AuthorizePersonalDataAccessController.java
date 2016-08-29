/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.ui.spring.controller;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.joda.time.LocalDate;
import org.joda.time.Years;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import pt.ist.fenixedu.integration.domain.BpiCard;
import pt.ist.fenixedu.integration.domain.SantanderCard;
import pt.ist.fenixedu.integration.domain.cgd.CgdCard;

@SpringApplication(group = "logged", path = "authorize-personal-data-access", title = "authorize.personal.data.access.title")
@SpringFunctionality(app = AuthorizePersonalDataAccessController.class, title = "authorize.personal.data.access.title")
@Controller
@RequestMapping("/authorize-personal-data-access")
public class AuthorizePersonalDataAccessController {

    @RequestMapping(method = RequestMethod.GET)
    public String viewAuthorizationDetails(Model model) {
        model.addAttribute("allowCGDAccess", CgdCard.getGrantAccess());
        return "fenixedu-ist-integration/personalDataAccess/viewAuthorizationDetails";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String setCandidateBanksAccess(@RequestParam(required = false) boolean allowAccessCgd,
            @RequestParam(required = false) Boolean allowAccessBpi, @RequestParam(required = false) Boolean allowAccessSantander,
            @RequestParam(required = false) String qs, Model model) {
        final CgdCard card = CgdCard.setGrantAccess(allowAccessCgd);
        if (card != null) {
            card.send();
        }

        if (allowAccessBpi != null) {
            final boolean authorizeBpi = allowAccessBpi.booleanValue();
            BpiCard.setGrantAccess(authorizeBpi, Authenticate.getUser());
        }

        if (allowAccessSantander != null) {
            final boolean authorizeSantander = allowAccessSantander.booleanValue();
            SantanderCard.setGrantAccess(authorizeSantander, Authenticate.getUser());
        }
        return "redirect:" + qs;
    }

}
