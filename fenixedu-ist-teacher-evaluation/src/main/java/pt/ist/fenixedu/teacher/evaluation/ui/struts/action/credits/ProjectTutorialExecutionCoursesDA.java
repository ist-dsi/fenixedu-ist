/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Credits.
 *
 * FenixEdu IST Teacher Credits is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Credits is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Credits.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.teacher.evaluation.ui.struts.action.credits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;
import org.fenixedu.commons.spreadsheet.StyledExcelSpreadsheet;

import pt.ist.fenixedu.contracts.domain.organizationalStructure.PersonFunction;
import pt.ist.fenixedu.contracts.domain.organizationalStructure.PersonFunctionShared;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.DepartmentCreditsBean;
import pt.ist.fenixedu.teacher.evaluation.ui.struts.action.DepartmentCreditsManagerApp;
import pt.ist.fenixframework.FenixFramework;

@StrutsFunctionality(app = DepartmentCreditsManagerApp.class, path = "execution-course-types",
        titleKey = "label.executionCourses.types", bundle = "TeacherCreditsSheetResources")
@Mapping(path = "/projectTutorialCourses")
@Forwards({
        @Forward(name = "exportDepartmentCourses", path = "/teacher/evaluation/credits/export/exportDepartmentCourses.jsp"),
        @Forward(name = "showDepartmentExecutionCourses",
                path = "/teacher/evaluation/credits/department/showDepartmentExecutionCourses.jsp") })
public class ProjectTutorialExecutionCoursesDA extends FenixDispatchAction {

    @EntryPoint
    public ActionForward showDepartmentExecutionCourses(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws NumberFormatException, FenixServiceException {
        DepartmentCreditsBean departmentCreditsBean = getRenderedObject();
        if (departmentCreditsBean == null) {
            departmentCreditsBean = new DepartmentCreditsBean();
        }
        request.setAttribute("departmentCreditsBean", departmentCreditsBean);
        return mapping.findForward("showDepartmentExecutionCourses");
    }

    public ActionForward prepareExportDepartmentCourses(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws NumberFormatException, FenixServiceException {
        DepartmentCreditsBean departmentCreditsBean = new DepartmentCreditsBean();
        request.setAttribute("departmentCreditsBean", departmentCreditsBean);
        return mapping.findForward("exportDepartmentCourses");
    }

    public ActionForward exportDepartmentCourses(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws NumberFormatException, FenixServiceException, IOException {
        DepartmentCreditsBean departmentCreditsBean = getRenderedObject();
        List<Department> departments = new ArrayList<Department>();
        if (departmentCreditsBean.getDepartment() != null) {
            departments.add(departmentCreditsBean.getDepartment());
        } else {
            departments.addAll(departmentCreditsBean.getAvailableDepartments());
        }
        StyledExcelSpreadsheet spreadsheet = new StyledExcelSpreadsheet();
        for (Department department : departments) {
            String sheetName = "Disciplinas_" + department.getAcronym();
            spreadsheet.getSheet(sheetName);
            spreadsheet.newHeaderRow();
            spreadsheet.addHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.course"), 10000);
            spreadsheet.addHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.degrees"));
            spreadsheet.addHeader(BundleUtil.getString(Bundle.ENUMERATION, "DISSERTATION"));
            spreadsheet.addHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.shift.type"));
            spreadsheet.addHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.hasSchedule"));
            spreadsheet.addHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.enrolmentsNumber"));
            for (ExecutionCourse executionCourse : department.getDepartmentUnit().getAllExecutionCoursesByExecutionPeriod(
                    departmentCreditsBean.getExecutionSemester())) {
                spreadsheet.newRow();
                spreadsheet.addCell(executionCourse.getName());
                spreadsheet.addCell(executionCourse.getDegreePresentationString());
                spreadsheet.addCell(BundleUtil.getString(Bundle.RENDERER, String.valueOf(executionCourse.isDissertation())
                        .toUpperCase()));
                spreadsheet.addCell(executionCourse.getProjectTutorialCourse() ? "A" : "B");
                spreadsheet.addCell(executionCourse.hasAnyLesson() ? "S" : "N");
                spreadsheet.addCell(executionCourse.getEnrolmentCount());
            }
        }

        response.setContentType("text/plain");
        response.setHeader("Content-disposition", "attachment; filename=Disciplinas.xls");
        final ServletOutputStream writer = response.getOutputStream();
        spreadsheet.getWorkbook().write(writer);
        writer.flush();
        response.flushBuffer();
        return null;
    }

    public ActionForward exportDepartmentPersonFunctions(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws NumberFormatException, FenixServiceException, IOException {
        DepartmentCreditsBean departmentCreditsBean = getRenderedObject();
        List<Department> departments = new ArrayList<Department>();
        if (departmentCreditsBean.getDepartment() != null) {
            departments.add(departmentCreditsBean.getDepartment());
        } else {
            departments.addAll(departmentCreditsBean.getAvailableDepartments());
        }
        StyledExcelSpreadsheet spreadsheet = new StyledExcelSpreadsheet();

        for (Department department : departments) {
            String sheetName = "Cargos_" + department.getAcronym();
            spreadsheet.getSheet(sheetName);
            spreadsheet.newHeaderRow();
            spreadsheet.addHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.teacher.id",
                    Unit.getInstitutionAcronym()));
            spreadsheet.addHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.name"), 10000);
            spreadsheet.addHeader(
                    BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.managementPosition.position"), 10000);
            spreadsheet.addHeader(
                    BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.managementPosition.unit"), 10000);
            spreadsheet.addHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources",
                    "label.teacher-dfp-student.percentage"));
            spreadsheet.addHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources",
                    "label.managementPosition.credits"));
            for (Teacher teacher : department.getAllTeachers(departmentCreditsBean.getExecutionSemester())) {

                for (PersonFunction personFunction : PersonFunction.getPersonFuntions(teacher.getPerson(), departmentCreditsBean
                        .getExecutionSemester().getBeginDateYearMonthDay(), departmentCreditsBean.getExecutionSemester()
                        .getEndDateYearMonthDay())) {
                    spreadsheet.newRow();
                    spreadsheet.addCell(personFunction.getPerson().getUsername());
                    spreadsheet.addCell(personFunction.getPerson().getName());
                    spreadsheet.addCell(personFunction.getFunction().getName());
                    spreadsheet.addCell(personFunction.getFunction().getUnit().getPresentationName());
                    spreadsheet.addCell(personFunction instanceof PersonFunctionShared ? ((PersonFunctionShared) personFunction)
                            .getPercentage() : "-");
                    spreadsheet.addCell(personFunction.getCredits());
                }
            }
        }

        response.setContentType("text/plain");
        String filename = "cargos_" + departmentCreditsBean.getExecutionSemester().getQualifiedName().replaceAll(" ", "_");
        response.setHeader("Content-disposition", "attachment; filename=" + filename + ".xls");
        final ServletOutputStream writer = response.getOutputStream();
        spreadsheet.getWorkbook().write(writer);
        writer.flush();
        response.flushBuffer();
        return null;
    }

    public ActionForward changeExecutionCourseType(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws NumberFormatException, FenixServiceException {

        ExecutionCourse executionCourse = FenixFramework.getDomainObject((String) getFromRequest(request, "executionCourseOid"));
        executionCourse.changeProjectTutorialCourse();
        Department department = FenixFramework.getDomainObject((String) getFromRequest(request, "departmentOid"));
        DepartmentCreditsBean departmentCreditsBean =
                new DepartmentCreditsBean(department, new ArrayList<Department>(rootDomainObject.getDepartmentsSet()));
        request.setAttribute("departmentCreditsBean", departmentCreditsBean);
        return mapping.findForward("showDepartmentExecutionCourses");
    }

}