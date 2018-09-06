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

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.fenixedu.academic.domain.DomainOperationLog;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.commons.i18n.I18N;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pt.ist.fenixedu.integration.domain.BpiCard;
import pt.ist.fenixedu.integration.domain.CardDataAuthorizationLog;
import pt.ist.fenixedu.integration.domain.SantanderCard;
import pt.ist.fenixedu.integration.domain.cgd.CgdCard;
import pt.ist.fenixedu.integration.ui.spring.service.SendCgdCardService;

@SpringApplication(group = "logged", path = "authorize-personal-data-access", title = "authorize.personal.data.access.title")
@SpringFunctionality(app = AuthorizePersonalDataAccessController.class, title = "authorize.personal.data.access.title")
@Controller
@RequestMapping("/authorize-personal-data-access")
public class AuthorizePersonalDataAccessController {

    private static final String santanderCardTitle = "authorize.personal.data.access.title.santander.card";
    private static final String santanderCardMessage = "authorize.personal.data.access.description.santander.card";
    private static final String cgdCardTitle = "authorize.personal.data.access.title.cgd.card";
    private static final String cgdCardMessage = "authorize.personal.data.access.description.cgd.card";

    private static final String santanderBankTitle = "authorize.personal.data.access.title.santander.bank";
    private static final String santanderBankMessage = "authorize.personal.data.access.description.santander.bank";
    private static final String cgdBankTitle = "authorize.personal.data.access.title.cgd.bank";
    private static final String cgdBankMessage = "authorize.personal.data.access.description.cgd.bank";
    private static final String bpiBankTitle = "authorize.personal.data.access.title.bpi.bank";
    private static final String bpiBankMessage = "authorize.personal.data.access.description.bpi.bank";
    
    public SendCgdCardService sendCgdCardService;
    public MessageSource messageSource;


    @Autowired
    public AuthorizePersonalDataAccessController(SendCgdCardService sendCgdCardService, MessageSource messageSource) {
        this.sendCgdCardService = sendCgdCardService;
        this.messageSource = messageSource;
    }

    private boolean checkCardDataAuthorizationWorkflowComplete() {
        User user = Authenticate.getUser();

        return SantanderCard.finishedCardDataAuthorization(user) && BpiCard.finishedCardDataAuthorization(user) && CgdCard.finishedCardDataAuthorization();
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, new Object[0], I18N.getLocale());
    }

    private void addAuthorizationDetailsInfo(Model model, String titleKey, String messageKey) {
        model.addAttribute("title", getMessage(titleKey));
        model.addAttribute("message", getMessage(messageKey));
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
    public String intro(){
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }

        return "redirect:/authorize-personal-data-access/santander-card";
    }

    @RequestMapping(value = "/review", method = RequestMethod.GET)
    public String reviewCardDataAuthorization(Model model) {
        User user = Authenticate.getUser();

        model.addAttribute("santanderBankTitle", getMessage(santanderBankTitle));
        model.addAttribute("santanderBankMessage", getMessage(santanderBankMessage));
        model.addAttribute("cgdBankTitle", getMessage(cgdBankTitle));
        model.addAttribute("cgdBankMessage", getMessage(cgdBankMessage));
        model.addAttribute("bpiBankTitle", getMessage(bpiBankTitle));
        model.addAttribute("bpiBankMessage", getMessage(bpiBankMessage));

        model.addAttribute("allowSantanderBankAccess", SantanderCard.getAllowSendBankDetails(user));
        model.addAttribute("allowCgdBankAccess", CgdCard.getAllowSendBank());
        model.addAttribute("allowBpiBankAccess", BpiCard.getAllowSendBankDetails(user));

        return "fenixedu-ist-integration/personalDataAccess/reviewAuthorizationDetails";
    }

    @RequestMapping(value = "/review/santander-bank", method = RequestMethod.POST)
    public String reviewSantanderCardDataAuthorizationSubmit(RedirectAttributes redirAttrs, @RequestParam boolean allowSantanderBankAccess) {
        SantanderCard.setGrantBankAccess(allowSantanderBankAccess, Authenticate.getUser(), getMessage(santanderBankTitle), getMessage(santanderBankMessage));

        redirAttrs.addFlashAttribute("success", true);
        return "redirect:/authorize-personal-data-access/review";
    }

    @RequestMapping(value = "/review/cgd-bank", method = RequestMethod.POST)
    public String reviewCgdCardDataAuthorizationSubmit(RedirectAttributes redirAttrs, @RequestParam boolean allowCgdBankAccess) {
        CgdCard.setGrantBankAccess(allowCgdBankAccess, getMessage(cgdBankTitle), getMessage(cgdBankMessage));

        redirAttrs.addFlashAttribute("success", true);
        return "redirect:/authorize-personal-data-access/review";
    }

    @RequestMapping(value = "/review/bpi-bank", method = RequestMethod.POST)
    public String reviewBpiCardDataAuthorizationSubmit(RedirectAttributes redirAttrs, @RequestParam boolean allowBpiBankAccess) {
        BpiCard.setGrantBankAccess(allowBpiBankAccess, Authenticate.getUser(), getMessage(bpiBankTitle), getMessage(bpiBankMessage));

        redirAttrs.addFlashAttribute("success", true);
        return "redirect:/authorize-personal-data-access/review";
    }

    @RequestMapping(value = "santander-card", method = RequestMethod.GET)
    public String santanderCardAuthorization(HttpServletRequest request, Model model) {
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }

        return checkAuthorizationDetails(model, santanderCardTitle, santanderCardMessage);
    }

    @RequestMapping(value = "santander-card", method = RequestMethod.POST)
    public String santanderCardAuthorizationSubmit(RedirectAttributes redirectAttributes) {
        SantanderCard.setGrantCardAccess(true, Authenticate.getUser(), getMessage(santanderCardTitle), getMessage(santanderCardMessage));

        return "redirect:/authorize-personal-data-access/cgd-card";
    }

    @RequestMapping(value = "/cgd-card", method = RequestMethod.GET)
    public String cgdCardAuthorization(Model model) {
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }

        return checkAuthorizationDetails(model, cgdCardTitle, cgdCardMessage);
    }

    @RequestMapping(value = "/cgd-card", method = RequestMethod.POST)
    public String cgdCardAuthorizationSubmit() {
        final CgdCard card = CgdCard.setGrantCardAccess(true, getMessage(cgdCardTitle), getMessage(cgdCardMessage));
        if (card != null) {
            sendCgdCardService.asyncSendCgdCard(card);
        }

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
//        BpiCard.setGrantCardAccess(allowAccess, Authenticate.getUser(), getMessage(bpiCardTitle), getMessage(bpiCardMessage));

        return "redirect:/authorize-personal-data-access/santander-bank";
    }

    @RequestMapping(value = "/santander-bank", method = RequestMethod.GET)
    public String santanderBankAuthorization(Model model) {
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }

        return chooseAuthorizationDetails(model, santanderBankTitle, santanderBankMessage);
    }

    @RequestMapping(value = "/santander-bank", method = RequestMethod.POST)
    public String santanderBankAuthorizationSubmit(@RequestParam boolean allowAccess) {
        SantanderCard.setGrantBankAccess(allowAccess, Authenticate.getUser(), getMessage(santanderBankTitle), getMessage(santanderBankMessage));

        return "redirect:/authorize-personal-data-access/cgd-bank";
    }

    @RequestMapping(value = "/cgd-bank", method = RequestMethod.GET)
    public String cgdBankAuthorization(Model model) {
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }

        return chooseAuthorizationDetails(model, cgdBankTitle, cgdBankMessage);
    }

    @RequestMapping(value = "/cgd-bank", method = RequestMethod.POST)
    public String cgdBankAuthorizationSubmit(@RequestParam boolean allowAccess) {
        CgdCard.setGrantBankAccess(allowAccess, getMessage(cgdBankTitle), getMessage(cgdBankMessage));

        return "redirect:/authorize-personal-data-access/bpi-bank";
    }

    @RequestMapping(value = "/bpi-bank", method = RequestMethod.GET)
    public String bpiBankAuthorization(Model model) {
        if (checkCardDataAuthorizationWorkflowComplete()) {
            return "redirect://authorize-personal-data-access/review";
        }

        return chooseAuthorizationDetails(model, bpiBankTitle, bpiBankMessage);
    }

    @RequestMapping(value = "/bpi-bank", method = RequestMethod.POST)
    public String bpiBankAuthorizationSubmit(@RequestParam boolean allowAccess) {
        BpiCard.setGrantBankAccess(allowAccess, Authenticate.getUser(), getMessage(bpiBankTitle), getMessage(bpiBankMessage));

        return "redirect:/authorize-personal-data-access/concluded";
    }

    @RequestMapping(value = "/concluded", method = RequestMethod.GET)
    public String concluded() {

        return "fenixedu-ist-integration/personalDataAccess/concludedAuthorizationDetails";
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
