/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST QUC.
 *
 * FenixEdu IST QUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST QUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST QUC.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * 
 */
package pt.ist.fenixedu.quc.ui.struts.action.gep;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeModuleScope;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.NonAffiliatedTeacher;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.organizationalStructure.UnitUtils;
import org.fenixedu.academic.dto.InfoDegreeCurricularPlan;
import org.fenixedu.academic.dto.InfoExecutionYear;
import org.fenixedu.academic.service.services.commons.ReadExecutionYearByID;
import org.fenixedu.academic.service.services.commons.ReadNotClosedExecutionYears;
import org.fenixedu.academic.service.services.exceptions.ExistingServiceException;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenixedu.quc.ui.struts.action.gep.inquiries.GepInquiriesApp;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

/**
 * @author - Shezad Anavarali (shezad@ist.utl.pt)
 * 
 */
@StrutsFunctionality(app = GepInquiriesApp.class, path = "teaching-staff", titleKey = "link.inquiries.teachingStaff")
@Mapping(module = "gep", path = "/teachingStaff", input = "/teachingStaff.do?method=prepare", formBean = "teachingStaffForm")
@Forwards(value = {
        @Forward(name = "chooseExecutionCourse", path = "/gep/teachingStaff/chooseExecutionCourse.jsp"),
        @Forward(name = "chooseExecutionYearAndDegreeCurricularPlan",
                path = "/gep/teachingStaff/chooseExecutionYearAndDegreeCurricularPlan.jsp"),
        @Forward(name = "viewTeachingStaff", path = "/gep/teachingStaff/viewTeachingStaff.jsp") })
public class TeachingStaffDispatchAction extends FenixDispatchAction {

    public ActionForward prepare(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {

        request.setAttribute("executionYears", ReadNotClosedExecutionYears.run());

        return mapping.findForward("chooseExecutionYearAndDegreeCurricularPlan");
    }

    @EntryPoint
    public ActionForward selectExecutionYear(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {

        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        String executionYearID = executionYear.getExternalId();

        List degreeCurricularPlans = readActiveDegreeCurricularPlansByExecutionYear(executionYearID);
        final ComparatorChain comparatorChain = new ComparatorChain();
        comparatorChain.addComparator(new BeanComparator("infoDegree.tipoCurso"));
        comparatorChain.addComparator(new BeanComparator("infoDegree.nome"));
        Collections.sort(degreeCurricularPlans, comparatorChain);

        request.setAttribute("degreeCurricularPlans", degreeCurricularPlans);
        request.setAttribute("executionYears", ReadNotClosedExecutionYears.run());
        request.setAttribute("executionYear", executionYear);

        return mapping.findForward("chooseExecutionYearAndDegreeCurricularPlan");
    }

    public ActionForward selectExecutionDegree(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {

        DynaActionForm dynaActionForm = (DynaActionForm) actionForm;
        String degreeCurricularPlanID = (String) dynaActionForm.get("degreeCurricularPlanID");
        String executionYearID = (String) dynaActionForm.get("executionYearID");

        Set<DegreeModuleScope> degreeModuleScopes =
                readActiveCurricularCourseScopesByDegreeCurricularPlanIDAndExecutionYearID(degreeCurricularPlanID,
                        executionYearID);

        SortedSet<DegreeModuleScope> sortedScopes = new TreeSet<DegreeModuleScope>(DegreeModuleScope.COMPARATOR_BY_NAME);
        sortedScopes.addAll(degreeModuleScopes);

        InfoExecutionYear infoExecutionYear = ReadExecutionYearByID.run(executionYearID);

        request.setAttribute("sortedScopes", sortedScopes);
        request.setAttribute("executionYear", infoExecutionYear.getYear());

        return mapping.findForward("chooseExecutionCourse");
    }

    public ActionForward viewTeachingStaff(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {

        String executionCourseID = request.getParameter("executionCourseID");

        ExecutionCourse executionCourse = FenixFramework.getDomainObject(executionCourseID);

        List<Unit> institutions = UnitUtils.readAllExternalInstitutionUnits();
        Collections.sort(institutions, new BeanComparator("name"));

        request.setAttribute("professorships", executionCourse.getProfessorshipsSet());
        request.setAttribute("institutions", institutions);
        request.setAttribute("nonAffiliatedTeachers", executionCourse.getNonAffiliatedTeachersSet());

        DynaActionForm dynaActionForm = (DynaActionForm) actionForm;
        dynaActionForm.set("executionCourseID", executionCourseID);
        dynaActionForm.set("nonAffiliatedTeacherID", null);

        return mapping.findForward("viewTeachingStaff");
    }

    public ActionForward createNewNonAffiliatedTeacher(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {

        DynaActionForm dynaActionForm = (DynaActionForm) actionForm;
        String nonAffiliatedTeacherName = (String) dynaActionForm.get("nonAffiliatedTeacherName");
        String nonAffiliatedTeacherInstitutionID = (String) dynaActionForm.get("nonAffiliatedTeacherInstitutionID");
        String nonAffiliatedTeacherInstitutionName = (String) dynaActionForm.get("nonAffiliatedTeacherInstitutionName");

        if (nonAffiliatedTeacherName.length() == 0) {
            // define a teacher name!
            return viewTeachingStaff(mapping, actionForm, request, response);
        }

        if (StringUtils.isEmpty(nonAffiliatedTeacherInstitutionID) && nonAffiliatedTeacherInstitutionName.length() == 0) {
            // define an institution!
            return viewTeachingStaff(mapping, actionForm, request, response);
        }

        final Unit institution =
                StringUtils.isEmpty(nonAffiliatedTeacherInstitutionID) ? (Unit) insertInstitution(nonAffiliatedTeacherInstitutionName) : (Unit) FenixFramework
                        .getDomainObject(nonAffiliatedTeacherInstitutionID);

        NonAffiliatedTeacher.associateToInstitutionAndExecutionCourse(nonAffiliatedTeacherName, institution,
                FenixFramework.<ExecutionCourse> getDomainObject((String) dynaActionForm.get("executionCourseID")));

        return viewTeachingStaff(mapping, actionForm, request, response);

    }

    public ActionForward removeNonAffiliatedTeacher(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws FenixServiceException {

        final ExecutionCourse executionCourse = getDomainObject(request, "executionCourseID");
        final NonAffiliatedTeacher nonAffiliatedTeacher = getDomainObject(request, "nonAffiliatedTeacherID");

        nonAffiliatedTeacher.removeExecutionCourse(executionCourse);

        return viewTeachingStaff(mapping, actionForm, request, response);
    }

    @Atomic
    public static Unit insertInstitution(String institutionName) throws FenixServiceException {
        if (UnitUtils.readExternalInstitutionUnitByName(institutionName) != null) {
            throw new ExistingServiceException("error.exception.commons.institution.institutionAlreadyExists");
        }
        return Unit.createNewNoOfficialExternalInstitution(institutionName);
    }

    public static Set<DegreeModuleScope> readActiveCurricularCourseScopesByDegreeCurricularPlanIDAndExecutionYearID(
            String degreeCurricularPlanId, String executionYearID) {
        final ExecutionYear executionYear = FenixFramework.getDomainObject(executionYearID);
        final DegreeCurricularPlan degreeCurricularPlan = FenixFramework.getDomainObject(degreeCurricularPlanId);
        final Set<DegreeModuleScope> degreeModuleScopes = degreeCurricularPlan.getDegreeModuleScopes();
        final Set<DegreeModuleScope> result =
                new TreeSet<DegreeModuleScope>(
                        DegreeModuleScope.COMPARATOR_BY_CURRICULAR_YEAR_AND_SEMESTER_AND_CURRICULAR_COURSE_NAME);
        for (final DegreeModuleScope degreeModuleScope : degreeModuleScopes) {
            if (degreeModuleScope.isActiveForExecutionYear(executionYear)) {
                result.add(degreeModuleScope);
            }
        }
        return result;
    }

    protected List readActiveDegreeCurricularPlansByExecutionYear(String executionYearID) throws FenixServiceException {
        ExecutionYear executionYear = FenixFramework.getDomainObject(executionYearID);

        Collection executionDegrees = null;
        if (executionYear != null) {
            executionDegrees = executionYear.getExecutionDegreesSet();
        }

        if (executionDegrees == null) {
            throw new FenixServiceException("nullDegree");
        }

        List infoDegreeCurricularPlans = (List) CollectionUtils.collect(executionDegrees, new Transformer() {
            @Override
            public Object transform(Object obj) {
                ExecutionDegree cursoExecucao = (ExecutionDegree) obj;
                DegreeCurricularPlan degreeCurricularPlan = cursoExecucao.getDegreeCurricularPlan();
                return InfoDegreeCurricularPlan.newInfoFromDomain(degreeCurricularPlan);
            }
        });

        return infoDegreeCurricularPlans;
    }

}