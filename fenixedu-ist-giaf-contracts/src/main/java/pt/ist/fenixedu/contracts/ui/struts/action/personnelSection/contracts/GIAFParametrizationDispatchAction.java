/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Contracts.
 *
 * FenixEdu IST GIAF Contracts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Contracts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Contracts.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.contracts.ui.struts.action.personnelSection.contracts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenixedu.contracts.ui.struts.action.personnelSection.PersonnelSectionApplication;

@StrutsFunctionality(app = PersonnelSectionApplication.class, path = "giaf-interface", titleKey = "title.giaf.interface",
        accessGroup = "#managers")
@Mapping(path = "/giafParametrization")
@Forwards({ @Forward(name = "show-menu", path = "/personnelSection/contracts/showGiafMenu.jsp"),
        @Forward(name = "show-contract-situations", path = "/personnelSection/contracts/showContractSituations.jsp"),
        @Forward(name = "show-professional-categories", path = "/personnelSection/contracts/showProfessionalCategories.jsp"),
        @Forward(name = "show-grantOwner-equivalences", path = "/personnelSection/contracts/showGrantOwnerEquivalences.jsp"),
        @Forward(name = "show-service-exemptions", path = "/personnelSection/contracts/showServiceExemptions.jsp"),
        @Forward(name = "show-absences", path = "/personnelSection/contracts/showAbsences.jsp") })
public class GIAFParametrizationDispatchAction extends FenixDispatchAction {

    @EntryPoint
    public ActionForward showMenu(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward("show-menu");
    }

    public ActionForward showContractSituations(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {
        request.setAttribute("contractSituations", Bennu.getInstance().getContractSituationsSet());
        return mapping.findForward("show-contract-situations");
    }

    public ActionForward showProfessionalCategories(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {
        request.setAttribute("professionalCategories", Bennu.getInstance().getProfessionalCategoriesSet());
        return mapping.findForward("show-professional-categories");
    }

    public ActionForward showGrantOwnerEquivalences(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {
        request.setAttribute("grantOwnerEquivalences", Bennu.getInstance().getGrantOwnerEquivalencesSet());
        return mapping.findForward("show-grantOwner-equivalences");
    }

    public ActionForward showServiceExemptions(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {
        request.setAttribute("serviceExemptions", Bennu.getInstance().getServiceExemptionsSet());
        return mapping.findForward("show-service-exemptions");
    }

    public ActionForward showAbsences(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {
        request.setAttribute("absences", Bennu.getInstance().getAbsencesSet());
        return mapping.findForward("show-absences");
    }

}