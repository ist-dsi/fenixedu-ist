/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Parking.
 *
 * FenixEdu IST Parking is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Parking is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Parking.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.parking.ui.struts.action;

import java.util.List;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.DynaActionForm;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.messaging.core.domain.Message;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.parking.domain.ParkingGroup;
import pt.ist.fenixedu.parking.domain.ParkingParty;
import pt.ist.fenixedu.parking.domain.ParkingRequest;
import pt.ist.fenixedu.parking.domain.ParkingRequestState;
import pt.ist.fenixedu.parking.dto.ParkingCardSearchBean;
import pt.ist.fenixedu.parking.dto.ParkingCardSearchBean.ParkingCardSearchPeriod;
import pt.ist.fenixedu.parking.dto.ParkingCardSearchBean.ParkingCardUserState;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import com.google.common.base.Strings;

@StrutsFunctionality(app = ParkingManagerApp.class, path = "manage-parking-cards", titleKey = "link.parkingCards")
@Mapping(module = "parkingManager", path = "/manageParkingCards", input = "/exportParkingDB.do?method=prepareExportFile",
        formBean = "parkingRenewalForm")
@Forwards({ @Forward(name = "cardsRenewal", path = "/parkingManager/cardsRenewal.jsp"),
        @Forward(name = "showParkingDetails", path = "/parkingManager/showParkingDetails.jsp"),
        @Forward(name = "cardsSearch", path = "/parkingManager/cardsSearch.jsp") })
public class ManageParkingCardsDA extends FenixDispatchAction {

    @EntryPoint
    public ActionForward prepareCardsSearch(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {

        ParkingCardSearchBean parkingCardSearchBean = null;
        if (getRenderedObject("parkingCardSearchBean") != null) {
            parkingCardSearchBean = getRenderedObject("parkingCardSearchBean");
        } else {
            parkingCardSearchBean = new ParkingCardSearchBean();
        }
        request.setAttribute("parkingCardSearchBean", parkingCardSearchBean);
        return mapping.findForward("cardsSearch");
    }

    public ActionForward searchCards(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        if (request.getParameter("prepareRenewal") != null) {
            return prepareCardsRenewal(mapping, actionForm, request, response);
        }
        ParkingCardSearchBean parkingCardSearchBean = getRenderedObject("parkingCardSearchBean");
        RenderUtils.invalidateViewState();
        if (parkingCardSearchBean == null) {
            parkingCardSearchBean = getSearchParameters(request);
        }
        parkingCardSearchBean.doSearch();
        parkingCardSearchBean.orderSearchedParkingParties();
        request.setAttribute("parkingCardSearchBean", parkingCardSearchBean);
        return mapping.findForward("cardsSearch");
    }

    private ParkingCardSearchBean getSearchParameters(HttpServletRequest request) {
        ParkingCardSearchBean parkingCardSearchBean = new ParkingCardSearchBean();
        String parkingCardUserState = request.getParameter("parkingCardUserState");
        if (!Strings.isNullOrEmpty(parkingCardUserState)) {
            parkingCardSearchBean.setParkingCardUserState(ParkingCardUserState.valueOf(parkingCardUserState));
        }
        String parkingGroupID = request.getParameter("parkingGroupID");
        if (!Strings.isNullOrEmpty(parkingGroupID)) {
            parkingCardSearchBean.setParkingGroup(FenixFramework.<ParkingGroup> getDomainObject(parkingGroupID));
        }
        String actualEndDate = request.getParameter("actualEndDate");
        if (!Strings.isNullOrEmpty(actualEndDate)) {
            DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd");
            parkingCardSearchBean.setActualEndDate(dtf.parseDateTime(actualEndDate).toYearMonthDay());
        }
        String parkingCardSearchPeriod = request.getParameter("parkingCardSearchPeriod");
        if (!Strings.isNullOrEmpty(parkingCardSearchPeriod)) {
            parkingCardSearchBean.setParkingCardSearchPeriod(ParkingCardSearchPeriod.valueOf(parkingCardSearchPeriod));
        }
        return parkingCardSearchBean;
    }

    public ActionForward prepareCardsRenewal(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String[] selectedParkingCards = ((DynaActionForm) actionForm).getStrings("selectedParkingCards");
        ParkingCardSearchBean parkingCardSearchBean = getRenderedObject("parkingCardSearchBean");
        RenderUtils.invalidateViewState();
        parkingCardSearchBean.getSelectedParkingParties().clear();
        for (String selectedParkingCard : selectedParkingCards) {
            parkingCardSearchBean.getSelectedParkingParties().add(
                    FenixFramework.<ParkingParty> getDomainObject(selectedParkingCard));
        }
        if (parkingCardSearchBean.getSelectedParkingParties().isEmpty()) {
            setMessage(request, "message.noParkingPartiesSelected");
            request.setAttribute("parkingCardSearchBean", parkingCardSearchBean);
            return mapping.findForward("cardsSearch");
        }
        parkingCardSearchBean.orderSelectedParkingParties();
        request.setAttribute("parkingCardSearchBean", parkingCardSearchBean);
        return mapping.findForward("cardsRenewal");
    }

    public ActionForward renewParkingCards(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        ParkingCardSearchBean parkingCardSearchBean = getRenderedObject("parkingCardSearchBean");
        if (request.getParameter("cancel") != null) {
            return searchCards(mapping, actionForm, request, response);
        }
        if (request.getParameter("remove") != null) {
            String[] parkingCardsToRemove = ((DynaActionForm) actionForm).getStrings("parkingCardsToRemove");
            for (String element : parkingCardsToRemove) {
                parkingCardSearchBean.removeSelectedParkingParty(element);
            }
            request.setAttribute("parkingCardSearchBean", parkingCardSearchBean);
            return mapping.findForward("cardsRenewal");
        }
        renewParkingCards(parkingCardSearchBean.getSelectedParkingParties(), parkingCardSearchBean.getRenewalEndDate(),
                parkingCardSearchBean.getNewParkingGroup(), parkingCardSearchBean.getEmailText());
        parkingCardSearchBean.getSelectedParkingParties().clear();
        parkingCardSearchBean.setRenewalEndDate(null);
        parkingCardSearchBean.setNewParkingGroup(null);
        request.setAttribute("parkingCardSearchBean", parkingCardSearchBean);
        return searchCards(mapping, actionForm, request, response);
    }

    public ActionForward showParkingDetails(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        ParkingCardSearchBean parkingCardSearchBean = getSearchParameters(request);
        request.setAttribute("parkingParty", FenixFramework.getDomainObject(request.getParameter("parkingPartyID")));
        request.setAttribute("parkingCardSearchBean", parkingCardSearchBean);
        return mapping.findForward("showParkingDetails");
    }

    private void setMessage(HttpServletRequest request, String msg) {
        ActionMessages actionMessages = getMessages(request);
        actionMessages.add("message", new ActionMessage(msg));
        saveMessages(request, actionMessages);
    }

    @Atomic
    private void renewParkingCards(List<ParkingParty> parkingParties, DateTime newEndDate, ParkingGroup newParkingGroup,
            String emailText) {
        DateTime newBeginDate = new DateTime();
        for (ParkingParty parkingParty : parkingParties) {
            parkingParty.renewParkingCard(newBeginDate, newEndDate, newParkingGroup);
            ParkingRequest parkingRequest = parkingParty.getLastRequest();
            if (parkingRequest != null && parkingRequest.getParkingRequestState() == ParkingRequestState.PENDING) {
                parkingRequest.setParkingRequestState(ParkingRequestState.ACCEPTED);
                parkingRequest.setNote(emailText);
            }
            String email = null;
            EmailAddress defaultEmailAddress = parkingParty.getParty().getDefaultEmailAddress();
            if (defaultEmailAddress != null) {
                email = defaultEmailAddress.getValue();
            }

            if (emailText != null && emailText.trim().length() != 0 && email != null) {
                ResourceBundle bundle = ResourceBundle.getBundle("resources.ParkingResources", I18N.getLocale());
                Message.fromSystem()
                        .replyTo(bundle.getString("label.fromAddress"))
                        .singleBcc(email)
                        .subject(bundle.getString("label.subject"))
                        .textBody(emailText)
                        .send();
            }
        }
    }

}
