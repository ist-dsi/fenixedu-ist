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
package pt.ist.fenixedu.quc.ui.struts.action.departmentMember;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.organizationalStructure.CompetenceCourseGroupUnit;
import org.fenixedu.academic.domain.organizationalStructure.DepartmentUnit;
import org.fenixedu.academic.domain.organizationalStructure.ScientificAreaUnit;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.struts.portal.EntryPoint;

import pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyConverter;
import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.quc.domain.InquiriesRoot;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.ResultClassification;
import pt.ist.fenixedu.quc.domain.ResultPersonCategory;
import pt.ist.fenixedu.quc.dto.CompetenceCourseResultsResume;
import pt.ist.fenixedu.quc.dto.CurricularCourseResumeResult;
import pt.ist.fenixedu.quc.dto.DepartmentExecutionSemester;
import pt.ist.fenixedu.quc.dto.DepartmentTeacherDetailsBean;
import pt.ist.fenixedu.quc.dto.DepartmentTeacherResultsResume;
import pt.ist.fenixedu.quc.dto.DepartmentUCResultsBean;
import pt.ist.fenixedu.quc.dto.TeacherShiftTypeGroupsResumeResult;
import pt.ist.fenixedu.quc.ui.struts.action.publico.ViewTeacherInquiryPublicResults;
import pt.ist.fenixframework.FenixFramework;

import com.google.common.base.Strings;

public abstract class ViewQUCResultsDA extends FenixDispatchAction {

    @EntryPoint
    public ActionForward resumeResults(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        DepartmentUnit departmentUnit = getDepartmentUnit(request);
        ExecutionSemester executionSemester = getExecutionSemester(request);

        List<CompetenceCourseResultsResume> competenceCoursesToAudit = new ArrayList<CompetenceCourseResultsResume>();
        for (ScientificAreaUnit scientificAreaUnit : departmentUnit.getScientificAreaUnits()) {
            for (CompetenceCourseGroupUnit competenceCourseGroupUnit : scientificAreaUnit.getCompetenceCourseGroupUnits()) {
                for (CompetenceCourse competenceCourse : competenceCourseGroupUnit.getCompetenceCourses()) {
                    CompetenceCourseResultsResume competenceCourseResultsResume = null;
                    for (ExecutionCourse executionCourse : competenceCourse
                            .getExecutionCoursesByExecutionPeriod(executionSemester)) {
                        if (InquiriesRoot.isAvailableForInquiry(executionCourse)) {
                            for (ExecutionDegree executionDegree : executionCourse.getExecutionDegrees()) {
                                CurricularCourseResumeResult courseResumeResult =
                                        new CurricularCourseResumeResult(executionCourse, executionDegree,
                                                "label.inquiry.execution", executionDegree.getDegree().getSigla(),
                                                AccessControl.getPerson(), ResultPersonCategory.DEPARTMENT_PRESIDENT, false,
                                                true, true, getShowAllComments(), true);
                                if (courseResumeResult.getResultBlocks().size() > 1) {
                                    if (ResultClassification.getForAudit(executionCourse, executionDegree) != null) {
                                        if (competenceCourseResultsResume == null) {
                                            competenceCourseResultsResume = new CompetenceCourseResultsResume(competenceCourse);
                                            competenceCoursesToAudit.add(competenceCourseResultsResume);
                                        }
                                        competenceCourseResultsResume.addCurricularCourseResumeResult(courseResumeResult);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Collections.sort(
                competenceCoursesToAudit,
                Comparator.comparing(CompetenceCourseResultsResume::getCompetenceCourse,
                        Comparator.comparing(CompetenceCourse::getName)));

        List<DepartmentTeacherResultsResume> teachersResumeToImprove =
                getDepartmentTeachersResume(departmentUnit, executionSemester, false, true);
        Collections.sort(
                teachersResumeToImprove,
                Comparator.comparing(DepartmentTeacherResultsResume::getTeacher,
                        Comparator.comparing(Teacher::getPerson, Comparator.comparing(Person::getName))));

        request.setAttribute("competenceCoursesToAudit", competenceCoursesToAudit);
        request.setAttribute("teachersResumeToImprove", teachersResumeToImprove);
        request.setAttribute("nothingToImprove", competenceCoursesToAudit.isEmpty() && teachersResumeToImprove.isEmpty());

        ViewTeacherInquiryPublicResults.setTeacherScaleColorException(executionSemester, request);
        return mapping.findForward("viewResumeResults");
    }

    protected abstract DepartmentUnit getDepartmentUnit(HttpServletRequest request);

    public ActionForward competenceResults(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        DepartmentUnit departmentUnit = getDepartmentUnit(request);
        ExecutionSemester executionSemester = getExecutionSemester(request);

        Map<ScientificAreaUnit, List<CompetenceCourseResultsResume>> competenceCoursesByScientificArea =
                new TreeMap<ScientificAreaUnit, List<CompetenceCourseResultsResume>>(
                        Comparator.comparing(ScientificAreaUnit::getName));
        for (ScientificAreaUnit scientificAreaUnit : departmentUnit.getScientificAreaUnits()) {
            for (CompetenceCourseGroupUnit competenceCourseGroupUnit : scientificAreaUnit.getCompetenceCourseGroupUnits()) {
                for (CompetenceCourse competenceCourse : competenceCourseGroupUnit.getCompetenceCourses()) {
                    CompetenceCourseResultsResume competenceCourseResultsResume = null;
                    for (ExecutionCourse executionCourse : competenceCourse
                            .getExecutionCoursesByExecutionPeriod(executionSemester)) {
                        if (InquiriesRoot.isAvailableForInquiry(executionCourse)) {
                            for (ExecutionDegree executionDegree : executionCourse.getExecutionDegrees()) {
                                CurricularCourseResumeResult courseResumeResult =
                                        new CurricularCourseResumeResult(executionCourse, executionDegree,
                                                "label.inquiry.execution", executionDegree.getDegree().getSigla(),
                                                AccessControl.getPerson(), ResultPersonCategory.DEPARTMENT_PRESIDENT, false,
                                                true, false, getShowAllComments(), true);
                                if (courseResumeResult.getResultBlocks().size() > 1) {
                                    List<CompetenceCourseResultsResume> competenceCourses =
                                            competenceCoursesByScientificArea.get(scientificAreaUnit);
                                    if (competenceCourses == null) {
                                        competenceCourses = new ArrayList<CompetenceCourseResultsResume>();
                                        competenceCoursesByScientificArea.put(scientificAreaUnit, competenceCourses);
                                    }
                                    if (competenceCourseResultsResume == null) {
                                        competenceCourseResultsResume = new CompetenceCourseResultsResume(competenceCourse);
                                        competenceCourses.add(competenceCourseResultsResume);
                                    }
                                    competenceCourseResultsResume.addCurricularCourseResumeResult(courseResumeResult);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (ScientificAreaUnit scientificAreaUnit : competenceCoursesByScientificArea.keySet()) {
            Collections.sort(
                    competenceCoursesByScientificArea.get(scientificAreaUnit),
                    Comparator.comparing(CompetenceCourseResultsResume::getCompetenceCourse,
                            Comparator.comparing(CompetenceCourse::getName)));
        }
        request.setAttribute("competenceCoursesByScientificArea", competenceCoursesByScientificArea);
        ViewTeacherInquiryPublicResults.setTeacherScaleColorException(executionSemester, request);
        return mapping.findForward("viewCompetenceResults");
    }

    public ActionForward teachersResults(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {

        DepartmentUnit departmentUnit = getDepartmentUnit(request);
        ExecutionSemester executionSemester = getExecutionSemester(request);

        List<DepartmentTeacherResultsResume> teachersResume =
                getDepartmentTeachersResume(departmentUnit, executionSemester, true, false);
        Collections.sort(
                teachersResume,
                Comparator.comparing(DepartmentTeacherResultsResume::getTeacher,
                        Comparator.comparing(Teacher::getPerson, Comparator.comparing(Person::getName))));

        request.setAttribute("teachersResume", teachersResume);
        ViewTeacherInquiryPublicResults.setTeacherScaleColorException(executionSemester, request);
        return mapping.findForward("viewTeachersResults");
    }

    public ActionForward showTeacherResultsAndComments(ActionMapping actionMapping, ActionForm actionForm,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        ExecutionSemester executionSemester =
                FenixFramework.getDomainObject(getFromRequest(request, "executionSemesterOID").toString());
        Person person = FenixFramework.getDomainObject(getFromRequest(request, "personOID").toString());

        DepartmentTeacherDetailsBean departmentTeacherDetailsBean =
                new DepartmentTeacherDetailsBean(person, executionSemester, AccessControl.getPerson(), Boolean.valueOf(request
                        .getParameter("backToResume")));

        request.setAttribute("executionPeriod", executionSemester);
        request.setAttribute("departmentTeacherDetailsBean", departmentTeacherDetailsBean);
        request.setAttribute("showComment", getFromRequest(request, "showComment"));
        request.setAttribute("showAllComments", request.getParameter("showAllComments"));
        ViewTeacherInquiryPublicResults.setTeacherScaleColorException(executionSemester, request);
        return actionMapping.findForward("departmentTeacherView");
    }

    public ActionForward showUCResultsAndComments(ActionMapping actionMapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        ExecutionCourse executionCourse =
                FenixFramework.getDomainObject(getFromRequest(request, "executionCourseOID").toString());
        ExecutionDegree executionDegree =
                FenixFramework.getDomainObject(getFromRequest(request, "executionDegreeOID").toString());
        DepartmentUCResultsBean departmentUCResultsBean =
                new DepartmentUCResultsBean(executionCourse, executionDegree, AccessControl.getPerson(), Boolean.valueOf(request
                        .getParameter("backToResume")));

        request.setAttribute("executionPeriod", executionCourse.getExecutionPeriod());
        request.setAttribute("executionCourse", executionCourse);
        request.setAttribute("departmentUCResultsBean", departmentUCResultsBean);
        request.setAttribute("showAllComments", request.getParameter("showAllComments"));
        ViewTeacherInquiryPublicResults.setTeacherScaleColorException(executionCourse.getExecutionPeriod(), request);
        return actionMapping.findForward("departmentUCView");
    }

    private List<DepartmentTeacherResultsResume> getDepartmentTeachersResume(DepartmentUnit departmentUnit,
            ExecutionSemester executionSemester, boolean allTeachers, boolean backToResume) {
        List<DepartmentTeacherResultsResume> teachersResume = new ArrayList<DepartmentTeacherResultsResume>();
        for (Teacher teacher : departmentUnit.getDepartment().getAllTeachers(executionSemester)) {
            DepartmentTeacherResultsResume departmentTeacherResultsResume = null;
            for (Professorship professorship : teacher.getProfessorships(executionSemester)) {
                if (!professorship.getInquiryResultsSet().isEmpty()) {
                    if (allTeachers || InquiryResult.hasResultsToImprove(professorship)) {
                        Collection<InquiryResult> professorshipResults = professorship.getInquiryResultsSet();
                        if (!professorshipResults.isEmpty()) {
                            for (ShiftType shiftType : getShiftTypes(professorshipResults)) {
                                List<InquiryResult> teacherShiftResults =
                                        InquiryResult.getInquiryResults(professorship, shiftType);
                                if (!teacherShiftResults.isEmpty()) {
                                    TeacherShiftTypeGroupsResumeResult teacherShiftTypeGroupsResumeResult =
                                            new TeacherShiftTypeGroupsResumeResult(professorship, shiftType,
                                                    ResultPersonCategory.TEACHER, "label.inquiry.shiftType",
                                                    RenderUtils.getEnumString(shiftType), false);

                                    if (departmentTeacherResultsResume == null) {
                                        departmentTeacherResultsResume =
                                                new DepartmentTeacherResultsResume(teacher, AccessControl.getPerson(),
                                                        ResultPersonCategory.DEPARTMENT_PRESIDENT, backToResume,
                                                        getShowAllComments());
                                        teachersResume.add(departmentTeacherResultsResume);
                                    }
                                    departmentTeacherResultsResume
                                            .addTeacherShiftTypeGroupsResumeResult(teacherShiftTypeGroupsResumeResult);
                                }
                            }
                        }
                    }
                }
            }
        }
        return teachersResume;
    }

    public abstract boolean getShowAllComments();

    private ExecutionSemester getExecutionSemester(HttpServletRequest request) {
        DepartmentExecutionSemester departmentExecutionSemester = getRenderedObject("executionSemesterBean");
        ExecutionSemester executionSemester = null;
        if (departmentExecutionSemester != null) {
            executionSemester = departmentExecutionSemester.getExecutionSemester();
        } else {
            String executionSemesterOID = request.getParameter("executionSemesterOID");
            String departmentUnitOID = request.getParameter("departmentUnitOID");
            if (Strings.isNullOrEmpty(executionSemesterOID)) {
                executionSemester = ExecutionSemester.readActualExecutionSemester().getPreviousExecutionPeriod();
            } else {
                executionSemester = FenixFramework.getDomainObject(executionSemesterOID);
            }
            departmentExecutionSemester = new DepartmentExecutionSemester();
            departmentExecutionSemester.setExecutionSemester(executionSemester);
            departmentExecutionSemester.setDepartmentUnitOID(departmentUnitOID);
        }
        request.setAttribute("executionSemesterBean", departmentExecutionSemester);
        request.setAttribute("executionSemester", executionSemester);
        return executionSemester;
    }

    private Set<ShiftType> getShiftTypes(Collection<InquiryResult> professorshipResults) {
        Set<ShiftType> shiftTypes = new HashSet<ShiftType>();
        for (InquiryResult inquiryResult : professorshipResults) {
            shiftTypes.add(inquiryResult.getShiftType());
        }
        return shiftTypes;
    }

    public static class ExecutionSemesterQucProvider implements DataProvider {
        @Override
        public Object provide(Object source, Object currentValue) {
            List<ExecutionSemester> executionSemesters = new ArrayList<ExecutionSemester>();
            ExecutionSemester oldQucExecutionSemester = ExecutionSemester.readBySemesterAndExecutionYear(2, "2009/2010");
            for (ExecutionSemester executionSemester : Bennu.getInstance().getExecutionPeriodsSet()) {
                if (executionSemester.isAfter(oldQucExecutionSemester)) {
                    executionSemesters.add(executionSemester);
                }
            }
            Collections.sort(executionSemesters, Comparator.reverseOrder());
            return executionSemesters;
        }

        @Override
        public Converter getConverter() {
            return new DomainObjectKeyConverter();
        }
    }
}
