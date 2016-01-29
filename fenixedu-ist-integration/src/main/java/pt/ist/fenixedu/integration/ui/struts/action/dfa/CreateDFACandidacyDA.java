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
package pt.ist.fenixedu.integration.ui.struts.action.dfa;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.candidacy.Candidacy;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.dto.administrativeOffice.candidacy.CreateDFACandidacyBean;
import org.fenixedu.academic.predicate.IllegalDataAccessException;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.integration.service.services.dfa.CreateCandidacy;

@StrutsFunctionality(app = DFAApplication.class, path = "create-candidacy",
        titleKey = "link.masterDegree.administrativeOffice.dfaCandidacy.createCandidacy")
@Mapping(path = "/createDfaCandidacy", module = "masterDegreeAdministrativeOffice",
        input = "/candidacy/chooseDFACandidacyExecutionDegree.jsp")
@Forwards(@Forward(name = "chooseExecutionDegree",
        path = "/masterDegreeAdministrativeOffice/candidacy/chooseDFACandidacyExecutionDegree.jsp"))
public class CreateDFACandidacyDA extends DFACandidacyDispatchAction {

    @EntryPoint
    public ActionForward prepareCreateCandidacy(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        CreateDFACandidacyBean createDFACandidacyBean = new CreateDFACandidacyBean();
        request.setAttribute("candidacyBean", createDFACandidacyBean);
        return mapping.findForward("chooseExecutionDegree");
    }

    public ActionForward createCandidacy(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {
        CreateDFACandidacyBean createDFACandidacyBean =
                (CreateDFACandidacyBean) RenderUtils.getViewState().getMetaObject().getObject();
        Candidacy candidacy = null;
        try {
            candidacy =
                    CreateCandidacy.run(createDFACandidacyBean.getExecutionDegree(), createDFACandidacyBean.getDegreeType(),
                            createDFACandidacyBean.getGivenNames(), createDFACandidacyBean.getFamilyNames(),
                            createDFACandidacyBean.getIdentificationNumber(), createDFACandidacyBean.getIdDocumentType(),
                            createDFACandidacyBean.getContributorNumber(), createDFACandidacyBean.getCandidacyDate());
        } catch (DomainException e) {
            addActionMessage(request, e.getMessage());
            RenderUtils.invalidateViewState();
            return prepareCreateCandidacy(mapping, actionForm, request, response);
        } catch (IllegalDataAccessException e) {
            addActionMessage(request, "error.not.authorized");
            RenderUtils.invalidateViewState();
            return prepareCreateCandidacy(mapping, actionForm, request, response);
        }

        storeCandidacyDataInRequest(request, candidacy);
        return mapping.findForward("viewCandidacyDetails");
    }

}
