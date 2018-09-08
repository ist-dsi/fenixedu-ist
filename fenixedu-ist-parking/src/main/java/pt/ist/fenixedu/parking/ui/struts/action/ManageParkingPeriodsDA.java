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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenixedu.parking.domain.ParkingRequestPeriod;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@StrutsFunctionality(app = ParkingManagerApp.class, path = "manage-request-periods", titleKey = "link.manageRequestsPeriods")
@Mapping(module = "parkingManager", path = "/manageParkingPeriods", input = "/exportParkingDB.do?method=prepareExportFile",
        formBean = "parkingRenewalForm")
@Forwards(@Forward(name = "manageRequestsPeriods", path = "/parkingManager/manageRequestsPeriods.jsp"))
public class ManageParkingPeriodsDA extends FenixDispatchAction {

    @EntryPoint
    public ActionForward prepareManageRequestsPeriods(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        List<ParkingRequestPeriod> parkingRequestPeriods =
                new ArrayList<ParkingRequestPeriod>(Bennu.getInstance().getParkingRequestPeriodsSet());
        Collections.sort(parkingRequestPeriods, Comparator.comparing(ParkingRequestPeriod::getBeginDate));
        request.setAttribute("parkingRequestPeriods", parkingRequestPeriods);
        return mapping.findForward("manageRequestsPeriods");
    }

    public ActionForward editRequestPeriod(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        request.setAttribute("parkingRequestPeriodToEdit", FenixFramework.getDomainObject(request.getParameter("externalId")));
        return prepareManageRequestsPeriods(mapping, actionForm, request, response);
    }

    public ActionForward deleteRequestPeriod(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        deleteParkingRequestPeriod(request.getParameter("externalId"));
        return prepareManageRequestsPeriods(mapping, actionForm, request, response);
    }

    @Atomic
    private void deleteParkingRequestPeriod(String id) {
        FenixFramework.<ParkingRequestPeriod> getDomainObject(id).delete();
    }
}