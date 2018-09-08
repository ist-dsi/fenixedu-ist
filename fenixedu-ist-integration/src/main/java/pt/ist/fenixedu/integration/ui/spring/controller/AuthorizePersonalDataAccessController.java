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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fenixedu.academic.domain.DomainOperationLog;
import org.fenixedu.academic.domain.candidacy.StudentCandidacy;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.google.common.base.Strings;
import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.GenericChecksumRewriter;
import pt.ist.fenixedu.integration.domain.BpiCard;
import pt.ist.fenixedu.integration.domain.CardDataAuthorizationLog;
import pt.ist.fenixedu.integration.domain.SantanderCard;
import pt.ist.fenixedu.integration.domain.cgd.CgdCard;
import pt.ist.fenixedu.integration.ui.spring.service.AuthorizePersonalDataAccessService;
import pt.ist.fenixframework.FenixFramework;

@SpringApplication(group = "logged", path = "authorize-personal-data-access", title = "authorize.personal.data.access.title")
@SpringFunctionality(app = AuthorizePersonalDataAccessController.class, title = "authorize.personal.data.access.title")
@Controller
@RequestMapping("/authorize-personal-data-access")
public class AuthorizePersonalDataAccessController {


    private final AuthorizePersonalDataAccessService dataService;

    @Autowired
    public AuthorizePersonalDataAccessController(AuthorizePersonalDataAccessService authorizePersonalDataAccessService) {
        this.dataService = authorizePersonalDataAccessService;
    }


    private void validateCandidacy(@RequestParam(defaultValue = "") String candidacy) {
        if (!Strings.isNullOrEmpty(candidacy)) {
            final StudentCandidacy studentCandidacy = FenixFramework.getDomainObject(candidacy);
            if (studentCandidacy.getPerson().getUser() != Authenticate.getUser()) {
                throw new DomainException("unauthorized");
            }
        }
    }
    
    private boolean checkCardDataAuthorizationWorkflowComplete() {
        User user = Authenticate.getUser();

        return SantanderCard.finishedCardDataAuthorization(user) && BpiCard.finishedCardDataAuthorization(user) && CgdCard.finishedCardDataAuthorization();
    }

    private void addAuthorizationDetailsInfo(Model model, String title, String message) {
        model.addAttribute("title", title);
        model.addAttribute("message", message);
    }

    private String checkAuthorizationDetails(Model model, String titleKey, String messageKey) {
        addAuthorizationDetailsInfo(model, titleKey, messageKey);

        return "fenixedu-ist-integration/personalDataAccess/checkAuthorizationDetails";
    }

    private String chooseAuthorizationDetails(Model model, String titleKey, String messageKey) {
        addAuthorizationDetailsInfo(model, titleKey, messageKey);

        return "fenixedu-ist-integration/personalDataAccess/chooseAuthorizationDetails";
    }

    @RequestMapping(method = RequestMethod.GET)
    public String intro(RedirectAttributes redirectAttributes){
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }
        return "redirect:/authorize-personal-data-access/santander-card";
    }

    @RequestMapping(value = "/review", method = RequestMethod.GET)
    public String reviewCardDataAuthorization(Model model) {
        User user = Authenticate.getUser();

        model.addAttribute("santanderBankTitle", dataService.getSantanderBankTitle());
        model.addAttribute("santanderBankMessage", dataService.getSantanderBankMessage());
        model.addAttribute("cgdBankTitle", dataService.getCgdBankTitle());
        model.addAttribute("cgdBankMessage", dataService.getCgdBankMessage());
        model.addAttribute("bpiBankTitle", dataService.getBpiBankTitle());
        model.addAttribute("bpiBankMessage", dataService.getBpiBankMessage());

        model.addAttribute("allowSantanderBankAccess", SantanderCard.getAllowSendBankDetails(user));
        model.addAttribute("allowCgdBankAccess", CgdCard.getAllowSendBank());
        model.addAttribute("allowBpiBankAccess", BpiCard.getAllowSendBankDetails(user));

        return "fenixedu-ist-integration/personalDataAccess/reviewAuthorizationDetails";
    }

    @RequestMapping(value = "/review/santander-bank", method = RequestMethod.POST)
    public String reviewSantanderCardDataAuthorizationSubmit(RedirectAttributes redirAttrs, @RequestParam boolean allowSantanderBankAccess) {
        dataService.setSantanderGrantBankAccess(allowSantanderBankAccess, Authenticate.getUser());


        redirAttrs.addFlashAttribute("success", true);
        return "redirect:/authorize-personal-data-access/review";
    }

    @RequestMapping(value = "/review/cgd-bank", method = RequestMethod.POST)
    public String reviewCgdCardDataAuthorizationSubmit(RedirectAttributes redirAttrs, @RequestParam boolean allowCgdBankAccess) {
        dataService.setCgdGrantBankAccess(allowCgdBankAccess, Authenticate.getUser());


        redirAttrs.addFlashAttribute("success", true);
        return "redirect:/authorize-personal-data-access/review";
    }

    @RequestMapping(value = "/review/bpi-bank", method = RequestMethod.POST)
    public String reviewBpiCardDataAuthorizationSubmit(RedirectAttributes redirAttrs, @RequestParam boolean allowBpiBankAccess) {
        dataService.setBpiGrantBankAccess(allowBpiBankAccess, Authenticate.getUser());



        redirAttrs.addFlashAttribute("success", true);
        return "redirect:/authorize-personal-data-access/review";
    }

    @RequestMapping(value = "santander-card", method = RequestMethod.GET)
    public String santanderCardAuthorization(HttpServletRequest request, Model model) {
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }

        return checkAuthorizationDetails(model, dataService.getSantanderCardTitle(), dataService.getSantanderCardMessage());
    }

    @RequestMapping(value = "santander-card", method = RequestMethod.POST)
    public String santanderCardAuthorizationSubmit(RedirectAttributes redirectAttributes) {
        dataService.setSantanderGrantCardAccess(true, Authenticate.getUser());

        return "redirect:/authorize-personal-data-access/cgd-card";
    }

    @RequestMapping(value = "/cgd-card", method = RequestMethod.GET)
    public String cgdCardAuthorization(Model model) {
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }

        return checkAuthorizationDetails(model, dataService.getCgdCardTitle(), dataService.getCgdCardMessage());
    }

    @RequestMapping(value = "/cgd-card", method = RequestMethod.POST)
    public String cgdCardAuthorizationSubmit() {
        dataService.setCgdGrantCardAccess(true, Authenticate.getUser());

        return "redirect:/authorize-personal-data-access/bpi-card";
    }

    @RequestMapping(value = "/bpi-card", method = RequestMethod.GET)
    public String bpiCardAuthorization(Model model) {
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }

        return "redirect:/authorize-personal-data-access/santander-bank";
    }

    @RequestMapping(value = "/bpi-card", method = RequestMethod.POST)
    public String bpiCardAuthorizationSubmit(@RequestParam boolean allowAccess) {
//        BpiCard.setGrantCardAccess(allowAccess, Authenticate.getUser(), dataService.getBpiCardTitle(), dataService.getBpiCardMessage());

        return "redirect:/authorize-personal-data-access/santander-bank";
    }

    @RequestMapping(value = "/santander-bank", method = RequestMethod.GET)
    public String santanderBankAuthorization(Model model, RedirectAttributes redirectAttributes, @RequestParam(defaultValue =
            "") String candidacy) {
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }
        model.addAttribute("candidacy", candidacy);
        return chooseAuthorizationDetails(model, dataService.getSantanderBankTitle(), dataService.getSantanderBankMessage());
    }

    @RequestMapping(value = "/santander-bank", method = RequestMethod.POST)
    public String santanderBankAuthorizationSubmit(@RequestParam boolean allowAccess, @RequestParam(defaultValue = "") String
            candidacy, RedirectAttributes redirectAttributes) {
        validateCandidacy(candidacy);
        SantanderCard.setGrantBankAccess(allowAccess, Authenticate.getUser(), dataService.getSantanderBankTitle(), dataService.getSantanderBankMessage());
        redirectAttributes.addFlashAttribute("candidacy", candidacy);
        return "redirect:/authorize-personal-data-access/cgd-bank";
    }



    @RequestMapping(value = "/cgd-bank", method = RequestMethod.GET)
    public String cgdBankAuthorization(Model model, @ModelAttribute("candidacy") String candidacy) {
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }
        
        model.addAttribute("candidacy", candidacy);
        return chooseAuthorizationDetails(model, dataService.getCgdBankTitle(), dataService.getCgdBankMessage());
    }

    @RequestMapping(value = "/cgd-bank", method = RequestMethod.POST)
    public String cgdBankAuthorizationSubmit(@RequestParam boolean allowAccess, @ModelAttribute("candidacy") String candidacy, RedirectAttributes redirectAttributes) {
        validateCandidacy(candidacy);
        dataService.setCgdGrantBankAccess(allowAccess, Authenticate.getUser());
        redirectAttributes.addFlashAttribute("candidacy", candidacy);
        return "redirect:/authorize-personal-data-access/bpi-bank";
    }

    @RequestMapping(value = "/bpi-bank", method = RequestMethod.GET)
    public String bpiBankAuthorization(Model model, @ModelAttribute("candidacy") String candidacy) {
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }
        model.addAttribute("candidacy", candidacy);
        return chooseAuthorizationDetails(model, dataService.getBpiBankTitle(), dataService.getBpiBankMessage());
    }

    @RequestMapping(value = "/bpi-bank", method = RequestMethod.POST)
    public String bpiBankAuthorizationSubmit(@RequestParam boolean allowAccess, @RequestParam(defaultValue = "") String
            candidacy, RedirectAttributes redirectAttributes) {
        validateCandidacy(candidacy);
        BpiCard.setGrantBankAccess(allowAccess, Authenticate.getUser(), dataService.getBpiBankTitle(), dataService.getBpiBankMessage());

        redirectAttributes.addFlashAttribute("candidacy", candidacy);
        return "redirect:/authorize-personal-data-access/concluded";
    }

    @RequestMapping(value = "/concluded", method = RequestMethod.GET)
    public String concluded(Model model, @ModelAttribute("candidacy") String candidacy,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        validateCandidacy(candidacy);
        if (!Strings.isNullOrEmpty(candidacy)) {
            response.sendRedirect(finishRegistrationProcess(request, candidacy));
            return null;
        }

        return "fenixedu-ist-integration/personalDataAccess/concludedAuthorizationDetails";
    }

    private static String finishRegistrationProcess(HttpServletRequest request, final String candidacy) {
        String url = "/student/firstTimeCandidacyDocuments.do?method=showCandidacyDetails&candidacyID=" + candidacy;
        String urlWithChecksum =
                GenericChecksumRewriter.injectChecksumInUrl(request.getContextPath(), url, request.getSession(false));
        return CoreConfiguration.getConfiguration().applicationUrl() + urlWithChecksum;
    }

    @RequestMapping(value = "/history", method = RequestMethod.GET)
    public String authorizationHistory(Model model) {
        final List<DomainOperationLog> cardOperationLogs = Authenticate.getUser().getPerson()
                .getDomainOperationLogsSet().stream().filter(CardDataAuthorizationLog.class::isInstance)
                .sorted(DomainOperationLog.COMPARATOR_BY_WHEN_DATETIME.reversed())
                .collect(Collectors.toList());
        model.addAttribute("cardAuthorizationLogs", cardOperationLogs);

        return "fenixedu-ist-integration/personalDataAccess/authorizationDetailsHistory";
    }

}
