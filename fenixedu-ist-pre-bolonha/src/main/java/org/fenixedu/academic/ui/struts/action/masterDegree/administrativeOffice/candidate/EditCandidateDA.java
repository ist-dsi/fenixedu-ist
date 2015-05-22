/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Pre Bolonha.
 *
 * FenixEdu IST Pre Bolonha is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Pre Bolonha is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Pre Bolonha.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Created on 14/Mar/2003
 *
 */
package org.fenixedu.academic.ui.struts.action.masterDegree.administrativeOffice.candidate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.util.LabelValueBean;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.person.GenderHelper;
import org.fenixedu.academic.domain.studentCurricularPlan.Specialization;
import org.fenixedu.academic.dto.InfoDegree;
import org.fenixedu.academic.dto.InfoExecutionDegree;
import org.fenixedu.academic.dto.InfoMasterDegreeCandidate;
import org.fenixedu.academic.dto.InfoPerson;
import org.fenixedu.academic.dto.comparators.ComparatorByNameForInfoExecutionDegree;
import org.fenixedu.academic.service.services.masterDegree.administrativeOffice.ReadMasterDegrees;
import org.fenixedu.academic.service.services.masterDegree.administrativeOffice.candidate.GetCandidatesByID;
import org.fenixedu.academic.service.services.masterDegree.administrativeOffice.candidate.GetCandidatesByPerson;
import org.fenixedu.academic.service.services.masterDegree.administrativeOffice.candidate.ReadCandidateList;
import org.fenixedu.academic.service.services.masterDegree.commons.candidate.ReadCandidateEnrolmentsByCandidateID;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.academic.ui.struts.action.commons.ChooseExecutionYearToEditCandidatesDA;
import org.fenixedu.academic.ui.struts.action.exceptions.FenixActionException;
import org.fenixedu.academic.ui.struts.action.resourceAllocationManager.utils.PresentationConstants;
import org.fenixedu.academic.ui.struts.config.FenixErrorExceptionHandler;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.Data;
import org.fenixedu.academic.util.SituationName;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.struts.annotations.ExceptionHandling;
import org.fenixedu.bennu.struts.annotations.Exceptions;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Nuno Nunes (nmsn@rnl.ist.utl.pt) Joana Mota (jccm@rnl.ist.utl.pt)
 * 
 */
@Mapping(path = "/editCandidate", module = "masterDegreeAdministrativeOffice",
        input = "/editCandidate.do?method=prepareEdit&error=1", formBean = "changeCandidateForm",
        functionality = ChooseExecutionYearToEditCandidatesDA.class)
@Forwards({ @Forward(name = "PrepareReady", path = "/masterDegreeAdministrativeOffice/changeCandidate_bd.jsp"),
        @Forward(name = "ChangeSuccess", path = "/masterDegreeAdministrativeOffice/visualizeCandidate_bd.jsp") })
@Exceptions(@ExceptionHandling(key = "resources.Action.exceptions.FenixActionException",
        handler = FenixErrorExceptionHandler.class, type = FenixActionException.class))
public class EditCandidateDA extends FenixDispatchAction {

    private static final Logger logger = LoggerFactory.getLogger(EditCandidateDA.class);

    /** request params * */
    public static final String REQUEST_DOCUMENT_TYPE = "documentType";

    public ActionForward prepareChoose(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        DynaActionForm listCandidatesForm = (DynaActionForm) form;

        String action = request.getParameter("action");

        if (action.equals("visualize")) {
            request.setAttribute(PresentationConstants.MASTER_DEGREE_CANDIDATE_ACTION, "label.action.visualize");
        } else if (action.equals("edit")) {
            request.setAttribute(PresentationConstants.MASTER_DEGREE_CANDIDATE_ACTION, "label.action.edit");
        }

        // Get the chosen exectionYear
        String executionYear = (String) request.getAttribute(PresentationConstants.EXECUTION_YEAR);
        listCandidatesForm.set("executionYear", executionYear);

        // Get the Degree List

        List degreeList = null;

        try {
            degreeList = ReadMasterDegrees.run(executionYear);
        } catch (Exception e) {
            throw new Exception(e);
        }

        // BeanComparator nameComparator = new
        // BeanComparator("infoDegreeCurricularPlan.infoDegree.nome");
        // Collections.sort(degreeList, nameComparator);
        Collections.sort(degreeList, new ComparatorByNameForInfoExecutionDegree());
        List newDegreeList = degreeList;
        List executionDegreeLabels = buildExecutionDegreeLabelValueBean(newDegreeList);

        request.setAttribute(PresentationConstants.DEGREE_LIST, executionDegreeLabels);

        // Create the Candidate Situation List

        request.setAttribute(PresentationConstants.CANDIDATE_SITUATION_LIST, SituationName.toArrayList());
        request.setAttribute("action", action);

        return mapping.findForward("PrepareReady");

    }

    public ActionForward getCandidates(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        DynaActionForm listCandidatesForm = (DynaActionForm) form;

        String action = request.getParameter("action");
        if (action.equals("visualize")) {
            request.setAttribute(PresentationConstants.MASTER_DEGREE_CANDIDATE_ACTION, "label.action.visualize");
        } else if (action.equals("edit")) {
            request.setAttribute(PresentationConstants.MASTER_DEGREE_CANDIDATE_ACTION, "label.action.edit");
        }

        // Get the Information
        String degreeTypeTemp = (String) listCandidatesForm.get("specialization");
        String degreeName = (String) listCandidatesForm.get("degree");
        String executionDegree = request.getParameter("executionDegreeOID");

        String candidateSituationTemp = (String) listCandidatesForm.get("candidateSituation");
        String candidateNumberTemp = (String) listCandidatesForm.get("candidateNumber");
        String executionYear = (String) listCandidatesForm.get("executionYear");

        Integer candidateNumber = null;
        Specialization specialization = null;
        SituationName situationName = null;

        if (degreeName.length() == 0) {
            degreeName = null;
        }
        if (candidateNumberTemp.length() != 0) {
            candidateNumber = Integer.valueOf(candidateNumberTemp);
        }
        if (degreeTypeTemp != null && degreeTypeTemp.length() != 0) {
            specialization = Specialization.valueOf(degreeTypeTemp);
        }
        if (candidateSituationTemp != null && candidateSituationTemp.length() != 0) {
            situationName = new SituationName(candidateSituationTemp);
        }

        List result = null;

        try {
            result = ReadCandidateList.run(executionDegree, specialization, situationName, candidateNumber, executionYear);
        } catch (Exception e) {
            throw new Exception(e);
        }
        if (result.size() != 0) {
            InfoMasterDegreeCandidate infoMasterDegreeCandidate = (InfoMasterDegreeCandidate) result.iterator().next();
            degreeName =
                    infoMasterDegreeCandidate.getInfoExecutionDegree().getInfoDegreeCurricularPlan().getInfoDegree().getNome()
                            + "-" + infoMasterDegreeCandidate.getInfoExecutionDegree().getInfoDegreeCurricularPlan().getName();
        }
        if (result.size() == 1) {
            InfoMasterDegreeCandidate infoMasterDegreeCandidate = (InfoMasterDegreeCandidate) result.iterator().next();
            request.setAttribute("candidateID", infoMasterDegreeCandidate.getExternalId());
            request.setAttribute(PresentationConstants.MASTER_DEGREE_CANDIDATE_LIST, result);
            return mapping.findForward("ActionReady");
        }
        // Create find query String
        String query = new String();
        query = "  - Ano Lectivo : " + executionYear + "<br />";
        // query = " - Degree : " + degreeName + "<br />";
        if (specialization == null && situationName == null && candidateNumber == null) {
            query += "  - Todos os criterios";
        } else {
            if (degreeName != null) {
                query += "  - Degree: " + degreeName + "<br />";
            }
            if (specialization != null) {
                query += "  - Tipo de Especializa��o: " + specialization.toString() + "<br />";
            }
            if (situationName != null) {
                query += "  - Situa��o do Candidato: " + situationName.toString() + "<br />";
            }
            if (candidateNumber != null) {
                query += "  - N�mero de Candidato: " + candidateNumber + "<br />";
            }
        }

        request.setAttribute(PresentationConstants.MASTER_DEGREE_CANDIDATE_LIST, result);
        request.setAttribute(PresentationConstants.MASTER_DEGREE_CANDIDATE_QUERY, query);

        return mapping.findForward("ChooseCandidate");

    }

    public ActionForward chooseCandidate(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String personID = request.getParameter("personID");
        request.setAttribute("candidateID", request.getParameter("candidateID"));

        // Read the Candidates for This Person

        List result = null;

        try {
            result = GetCandidatesByPerson.run(personID);
        } catch (Exception e) {
            throw new Exception(e);
        }

        request.setAttribute(PresentationConstants.MASTER_DEGREE_CANDIDATE_LIST, result);

        return mapping.findForward("ActionReady");
    }

    public ActionForward visualize(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        User userView = getUserView(request);
        String candidateID = (String) request.getAttribute("candidateID");

        if (candidateID == null) {
            candidateID = request.getParameter("candidateID");
        }

        // Read the Candidates for This Person

        InfoMasterDegreeCandidate result = null;

        try {
            result = GetCandidatesByID.run(candidateID);
        } catch (Exception e) {
            throw new Exception(e);
        }

        List candidateStudyPlan = getCandidateStudyPlanByCandidateID(candidateID, userView);

        if (candidateStudyPlan != null) {
            request.setAttribute("studyPlan", candidateStudyPlan);
        }

        request.setAttribute(PresentationConstants.MASTER_DEGREE_CANDIDATE, result);
        return mapping.findForward("VisualizeCandidate");
    }

    public ActionForward prepareEdit(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        DynaActionForm editCandidateForm = (DynaActionForm) form;

        String candidateID = (String) request.getAttribute("candidateID");
        if (candidateID == null) {
            candidateID = request.getParameter("candidateID");
        }

        // Read the Candidates for This Person

        InfoMasterDegreeCandidate infoMasterDegreeCandidate = null;

        try {
            infoMasterDegreeCandidate = GetCandidatesByID.run(candidateID);
        } catch (Exception e) {
            throw new Exception(e);
        }

        boolean validationError = request.getParameter("error") != null;
        if (!validationError) {
            populateForm(editCandidateForm, infoMasterDegreeCandidate);
        }

        List<LabelValueBean> nationalityList = new ArrayList<>();
        for (Country country : Bennu.getInstance().getCountrysSet()) {
            nationalityList.add(new LabelValueBean(country.getNationality(), country.getNationality()));
        }

        request.setAttribute(PresentationConstants.NATIONALITY_LIST_KEY, nationalityList);
        request.setAttribute(PresentationConstants.MASTER_DEGREE_CANDIDATE, infoMasterDegreeCandidate);
        request.setAttribute(PresentationConstants.SEX_LIST_KEY,
                GenderHelper.getSexLabelValues((Locale) request.getAttribute(Globals.LOCALE_KEY)));
        request.setAttribute(PresentationConstants.MONTH_DAYS_KEY, Data.getMonthDays());
        request.setAttribute(PresentationConstants.MONTH_LIST_KEY, Data.getMonths());
        request.setAttribute(PresentationConstants.YEARS_KEY, getYears());

        request.setAttribute(PresentationConstants.EXPIRATION_YEARS_KEY, getExpirationYears());

        request.setAttribute(PresentationConstants.CANDIDATE_SITUATION_LIST, SituationName.toArrayList());

        return mapping.findForward("PrepareReady");
    }

    public static List<LabelValueBean> getYears() {
        List<LabelValueBean> result = new ArrayList<>();
        for (int i = LocalDate.now().getYear(); i > 1900; i--) {
            result.add(new LabelValueBean(Integer.toString(i), Integer.toString(i)));
        }
        return result;
    }

    public static List<LabelValueBean> getExpirationYears() {
        List<LabelValueBean> result = new ArrayList<>();
        for (int i = LocalDate.now().getYear() - 10; i < LocalDate.now().getYear() + 20; i++) {
            result.add(new LabelValueBean(Integer.toString(i), Integer.toString(i)));
        }
        return result;
    }

    private void populateForm(DynaActionForm editCandidateForm, InfoMasterDegreeCandidate infoMasterDegreeCandidate) {
        // Fill in The Form

        InfoPerson infoPerson = infoMasterDegreeCandidate.getInfoPerson();

        editCandidateForm.set("identificationDocumentNumber", infoPerson.getNumeroDocumentoIdentificacao());
        editCandidateForm.set("identificationDocumentType", infoPerson.getTipoDocumentoIdentificacao().toString());
        editCandidateForm.set("identificationDocumentIssuePlace", infoPerson.getLocalEmissaoDocumentoIdentificacao());
        editCandidateForm.set("name", infoPerson.getNome());

        Calendar birthDate = Calendar.getInstance();
        if (infoPerson.getNascimento() == null) {
            editCandidateForm.set("birthDay", Data.OPTION_DEFAULT);
            editCandidateForm.set("birthMonth", Data.OPTION_DEFAULT);
            editCandidateForm.set("birthYear", Data.OPTION_DEFAULT);
        } else {
            birthDate.setTime(infoPerson.getNascimento());
            editCandidateForm.set("birthDay", new Integer(birthDate.get(Calendar.DAY_OF_MONTH)).toString());
            editCandidateForm.set("birthMonth", new Integer(birthDate.get(Calendar.MONTH)).toString());
            editCandidateForm.set("birthYear", new Integer(birthDate.get(Calendar.YEAR)).toString());
        }

        Calendar identificationDocumentIssueDate = Calendar.getInstance();
        if (infoPerson.getDataEmissaoDocumentoIdentificacao() == null) {
            editCandidateForm.set("idIssueDateDay", Data.OPTION_DEFAULT);
            editCandidateForm.set("idIssueDateMonth", Data.OPTION_DEFAULT);
            editCandidateForm.set("idIssueDateYear", Data.OPTION_DEFAULT);
        } else {
            identificationDocumentIssueDate.setTime(infoPerson.getDataEmissaoDocumentoIdentificacao());
            editCandidateForm.set("idIssueDateDay",
                    new Integer(identificationDocumentIssueDate.get(Calendar.DAY_OF_MONTH)).toString());
            editCandidateForm
                    .set("idIssueDateMonth", new Integer(identificationDocumentIssueDate.get(Calendar.MONTH)).toString());
            editCandidateForm.set("idIssueDateYear", new Integer(identificationDocumentIssueDate.get(Calendar.YEAR)).toString());
        }

        Calendar identificationDocumentExpirationDate = Calendar.getInstance();
        if (infoPerson.getDataValidadeDocumentoIdentificacao() == null) {
            editCandidateForm.set("idExpirationDateDay", Data.OPTION_DEFAULT);
            editCandidateForm.set("idExpirationDateMonth", Data.OPTION_DEFAULT);
            editCandidateForm.set("idExpirationDateYear", Data.OPTION_DEFAULT);
        } else {
            identificationDocumentExpirationDate.setTime(infoPerson.getDataValidadeDocumentoIdentificacao());
            editCandidateForm.set("idExpirationDateDay",
                    new Integer(identificationDocumentExpirationDate.get(Calendar.DAY_OF_MONTH)).toString());
            editCandidateForm.set("idExpirationDateMonth",
                    new Integer(identificationDocumentExpirationDate.get(Calendar.MONTH)).toString());
            editCandidateForm.set("idExpirationDateYear",
                    new Integer(identificationDocumentExpirationDate.get(Calendar.YEAR)).toString());
        }

        editCandidateForm.set("fatherName", infoPerson.getNomePai());
        editCandidateForm.set("motherName", infoPerson.getNomeMae());

        if (infoPerson.getInfoPais() == null) {
            editCandidateForm.set("nationality", BundleUtil.getString(Bundle.GLOBAL, "default.nationality"));
        } else {
            editCandidateForm.set("nationality", infoPerson.getInfoPais().getNationality());
        }

        editCandidateForm.set("birthPlaceParish", infoPerson.getFreguesiaNaturalidade());
        editCandidateForm.set("birthPlaceMunicipality", infoPerson.getConcelhoNaturalidade());
        editCandidateForm.set("birthPlaceDistrict", infoPerson.getDistritoNaturalidade());
        editCandidateForm.set("address", infoPerson.getMorada());
        editCandidateForm.set("place", infoPerson.getLocalidade());
        editCandidateForm.set("postCode", infoPerson.getCodigoPostal());
        editCandidateForm.set("addressParish", infoPerson.getFreguesiaMorada());
        editCandidateForm.set("addressMunicipality", infoPerson.getConcelhoMorada());
        editCandidateForm.set("addressDistrict", infoPerson.getDistritoMorada());
        editCandidateForm.set("telephone", infoPerson.getTelefone());
        editCandidateForm.set("mobilePhone", infoPerson.getTelemovel());
        editCandidateForm.set("email", infoPerson.getEmail());
        editCandidateForm.set("webSite", infoPerson.getEnderecoWeb());
        editCandidateForm.set("contributorNumber", infoPerson.getNumContribuinte());
        editCandidateForm.set("occupation", infoPerson.getProfissao());
        editCandidateForm.set("username", infoPerson.getUsername());
        editCandidateForm.set("areaOfAreaCode", infoPerson.getLocalidadeCodigoPostal());
        editCandidateForm.set("situation", infoMasterDegreeCandidate.getInfoCandidateSituation().getSituation().toString());
        editCandidateForm.set("specializationArea", infoMasterDegreeCandidate.getSpecializationArea());
        if (infoMasterDegreeCandidate.getAverage() != null) {
            editCandidateForm.set("average", infoMasterDegreeCandidate.getAverage().toString());
        }
        editCandidateForm.set("majorDegree", infoMasterDegreeCandidate.getMajorDegree());
        editCandidateForm.set("majorDegreeSchool", infoMasterDegreeCandidate.getMajorDegreeSchool());

        editCandidateForm.set("candidateID", infoMasterDegreeCandidate.getExternalId());

        if ((infoPerson.getSexo() != null)) {
            editCandidateForm.set("sex", infoPerson.getSexo().toString());
        }
        if (infoPerson.getMaritalStatus() != null) {
            editCandidateForm.set("maritalStatus", infoPerson.getMaritalStatus().toString());
        }

        if (infoMasterDegreeCandidate.getMajorDegreeYear() != null) {
            if ((infoMasterDegreeCandidate.getMajorDegreeYear().intValue() == 0)) {
                editCandidateForm.set("majorDegreeYear", null);
            } else {
                editCandidateForm.set("majorDegreeYear", String.valueOf(infoMasterDegreeCandidate.getMajorDegreeYear()));
            }
        }

    }

    private List buildExecutionDegreeLabelValueBean(List executionDegreeList) {
        List executionDegreeLabels = new ArrayList();
        Iterator iterator = executionDegreeList.iterator();
        while (iterator.hasNext()) {
            InfoExecutionDegree infoExecutionDegree = (InfoExecutionDegree) iterator.next();
            String name = infoExecutionDegree.getInfoDegreeCurricularPlan().getInfoDegree().getNome();

            name = infoExecutionDegree.getInfoDegreeCurricularPlan().getInfoDegree().getDegreeType().toString() + " em " + name;

            name +=
                    duplicateInfoDegree(executionDegreeList, infoExecutionDegree) ? "-"
                            + infoExecutionDegree.getInfoDegreeCurricularPlan().getName() : "";

            executionDegreeLabels.add(new LabelValueBean(name, infoExecutionDegree.getInfoDegreeCurricularPlan().getInfoDegree()
                    .getNome()));
            //
        }
        return executionDegreeLabels;
    }

    private boolean duplicateInfoDegree(List executionDegreeList, InfoExecutionDegree infoExecutionDegree) {
        InfoDegree infoDegree = infoExecutionDegree.getInfoDegreeCurricularPlan().getInfoDegree();
        Iterator iterator = executionDegreeList.iterator();

        while (iterator.hasNext()) {
            InfoExecutionDegree infoExecutionDegree2 = (InfoExecutionDegree) iterator.next();
            if (infoDegree.equals(infoExecutionDegree2.getInfoDegreeCurricularPlan().getInfoDegree())
                    && !(infoExecutionDegree.equals(infoExecutionDegree2))) {
                return true;
            }

        }
        return false;
    }

    private List getCandidateStudyPlanByCandidateID(String candidateID, User userView) {

        try {
            return ReadCandidateEnrolmentsByCandidateID.runReadCandidateEnrolmentsByCandidateID(candidateID);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

}