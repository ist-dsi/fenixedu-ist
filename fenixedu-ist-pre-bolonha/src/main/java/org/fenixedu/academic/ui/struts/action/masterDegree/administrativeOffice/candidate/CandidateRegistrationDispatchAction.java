/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Core.
 *
 * FenixEdu Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Core.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Created on 14/Mar/2003
 *
 */
package org.fenixedu.academic.ui.struts.action.masterDegree.administrativeOffice.candidate;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.fenixedu.academic.dto.InfoCandidateRegistration;
import org.fenixedu.academic.dto.InfoExecutionDegree;
import org.fenixedu.academic.dto.InfoMasterDegreeCandidate;
import org.fenixedu.academic.service.services.commons.ReadExecutionDegreeByOID;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.service.services.exceptions.NonExistingServiceException;
import org.fenixedu.academic.service.services.masterDegree.administrativeOffice.candidate.GetBranchListByCandidateID;
import org.fenixedu.academic.service.services.masterDegree.administrativeOffice.candidate.GetCandidateRegistrationInformation;
import org.fenixedu.academic.service.services.masterDegree.administrativeOffice.candidate.GetCandidatesByID;
import org.fenixedu.academic.service.services.masterDegree.administrativeOffice.candidate.ReadCandidateForRegistration;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.academic.ui.struts.action.commons.ChooseExecutionYearDispatchAction;
import org.fenixedu.academic.ui.struts.action.exceptions.ActiveStudentCurricularPlanAlreadyExistsActionException;
import org.fenixedu.academic.ui.struts.action.exceptions.ExistingActionException;
import org.fenixedu.academic.ui.struts.action.exceptions.FenixActionException;
import org.fenixedu.academic.ui.struts.action.exceptions.GratuityValuesNotDefinedActionException;
import org.fenixedu.academic.ui.struts.action.exceptions.InvalidChangeActionException;
import org.fenixedu.academic.ui.struts.action.exceptions.InvalidInformationInFormActionException;
import org.fenixedu.academic.ui.struts.action.exceptions.InvalidStudentNumberActionException;
import org.fenixedu.academic.ui.struts.config.FenixErrorExceptionHandler;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.portal.servlet.PortalLayoutInjector;
import org.fenixedu.bennu.struts.annotations.ExceptionHandling;
import org.fenixedu.bennu.struts.annotations.Exceptions;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;

/**
 *
 * @author Nuno Nunes (nmsn@rnl.ist.utl.pt) Joana Mota (jccm@rnl.ist.utl.pt)
 *
 *
 */
@Mapping(path = "/candidateRegistration", module = "masterDegreeAdministrativeOffice", input = "/chooseMasterDegree_bd.jsp",
        formBean = "candidateRegistrationForm", functionality = ChooseExecutionYearDispatchAction.class)
@Forwards(value = {
        @Forward(name = "ListCandidates",
                path = "/masterDegreeAdministrativeOffice/candidate/displayCandidateListForRegistration_bd.jsp"),
        @Forward(name = "ShowConfirmation",
                path = "/masterDegreeAdministrativeOffice/candidate/confirmCandidateRegistration_bd.jsp"),
        @Forward(name = "PrepareCandidateList",
                path = "/masterDegreeAdministrativeOffice/candidateRegistration.do?method=getCandidateList"),
        @Forward(name = "ShowResult", path = "/masterDegreeAdministrativeOffice/candidate/candidateRegistered_bd.jsp"),
        @Forward(name = "Print", path = "/masterDegreeAdministrativeOffice/candidate/candidateRegistrationTemplatePrint.jsp") })
@Exceptions({
        @ExceptionHandling(key = "resources.Action.exceptions.InvalidInformationInFormActionException",
                handler = FenixErrorExceptionHandler.class, type = InvalidInformationInFormActionException.class),
        @ExceptionHandling(key = "resources.Action.exceptions.InvalidChangeActionException",
                handler = FenixErrorExceptionHandler.class, type = InvalidChangeActionException.class),
        @ExceptionHandling(key = "resources.Action.exceptions.ExistingActionException",
                handler = FenixErrorExceptionHandler.class, type = ExistingActionException.class),
        @ExceptionHandling(key = "resources.Action.exceptions.ActiveStudentCurricularPlanAlreadyExistsActionException",
                handler = FenixErrorExceptionHandler.class, type = ActiveStudentCurricularPlanAlreadyExistsActionException.class),
        @ExceptionHandling(key = "resources.Action.exceptions.InvalidStudentNumberActionException",
                handler = FenixErrorExceptionHandler.class, type = InvalidStudentNumberActionException.class),
        @ExceptionHandling(key = "resources.Action.exceptions.GratuityValuesNotDefinedActionException",
                handler = FenixErrorExceptionHandler.class, type = GratuityValuesNotDefinedActionException.class) })
public class CandidateRegistrationDispatchAction extends FenixDispatchAction {

    public ActionForward getCandidateList(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        DynaActionForm candidateRegistration = (DynaActionForm) form;

        String executionYearString = request.getParameter("executionYear");
        String degreeCode = request.getParameter("degree");

        String executionDegree = request.getParameter("executionDegreeID");
        if ((executionYearString == null) || (executionYearString.length() == 0)) {
            executionYearString = (String) candidateRegistration.get("executionYear");
        }

        if ((degreeCode == null) || (degreeCode.length() == 0)) {
            degreeCode = (String) candidateRegistration.get("degreeCode");
        }

        User userView = getUserView(request);

        List result = null;

        try {

            result = ReadCandidateForRegistration.run(executionDegree);
        } catch (NonExistingServiceException e) {
            ActionErrors errors = new ActionErrors();
            errors.add("nonExisting", new ActionError("error.candidatesNotFound"));
            saveErrors(request, errors);
            return mapping.getInputForward();
        } catch (FenixServiceException e) {
            throw new FenixActionException(e);
        }

        BeanComparator nameComparator = new BeanComparator("infoPerson.nome");
        Collections.sort(result, nameComparator);

        request.setAttribute("candidateList", result);
        candidateRegistration.set("degreeCode", degreeCode);
        candidateRegistration.set("executionYear", executionYearString);
        candidateRegistration.set("candidateID", null);

        InfoExecutionDegree infoExecutionDegree = ReadExecutionDegreeByOID.run(executionDegree);

        request.setAttribute("infoExecutionDegree", infoExecutionDegree);
        return mapping.findForward("ListCandidates");
    }

    public ActionForward prepareCandidateRegistration(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        DynaActionForm candidateRegistration = (DynaActionForm) form;

        User userView = getUserView(request);
        String candidateID = request.getParameter("candidateID");

        candidateRegistration.set("candidateID", candidateID);
        candidateRegistration.set("studentNumber", null);
        List branchList = null;
        try {

            branchList = GetBranchListByCandidateID.run(candidateID);
        } catch (FenixServiceException e) {
            throw new FenixActionException(e);
        }

        request.setAttribute("branchList", branchList);

        InfoMasterDegreeCandidate infoMasterDegreeCandidate = null;
        try {
            infoMasterDegreeCandidate = GetCandidatesByID.run(candidateID);
        } catch (NonExistingServiceException e) {
            throw new FenixActionException(e);
        }
        candidateRegistration.set("branchID", null);
        request.setAttribute("infoMasterDegreeCandidate", infoMasterDegreeCandidate);

        return mapping.findForward("ShowConfirmation");
    }

    public ActionForward preparePrint(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        DynaActionForm candidateRegistration = (DynaActionForm) form;

        User userView = getUserView(request);
        String candidateID = (String) candidateRegistration.get("candidateID");

        InfoCandidateRegistration infoCandidateRegistration = null;
        infoCandidateRegistration = GetCandidateRegistrationInformation.run(candidateID);

        request.setAttribute("infoCandidateRegistration", infoCandidateRegistration);
        request.setAttribute("infoExecutionDegree", infoCandidateRegistration.getInfoMasterDegreeCandidate()
                .getInfoExecutionDegree());

        PortalLayoutInjector.skipLayoutOn(request);
        return mapping.findForward("Print");
    }

}