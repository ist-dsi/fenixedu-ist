/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Evaluation.
 *
 * FenixEdu IST Teacher Evaluation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Evaluation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Evaluation.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.teacher.evaluation.ui.struts.action.credits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.TeacherAuthorization;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.google.common.collect.Sets;

import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.DepartmentCreditsBean;
import pt.ist.fenixedu.teacher.evaluation.domain.teacher.TeacherService;
import pt.ist.fenixedu.teacher.evaluation.ui.struts.action.DepartmentCreditsManagerApp;

@StrutsFunctionality(app = DepartmentCreditsManagerApp.class, path = "department-teacher-service",
        titleKey = "link.teacherService", bundle = "DepartmentAdmOfficeResources")
@Mapping(path = "/departmentTeacherService")
@Forwards(@Forward(name = "viewDepartmentTeacherService", path = "/teacher/evaluation/credits/viewDepartmentTeacherService.jsp"))
public class ViewDepartmentTeacherServiceDA extends FenixDispatchAction {
@EntryPoint
public ActionForward prepareViewDepartmentTeacherService(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws FenixServiceException {
    DepartmentCreditsBean departmentCreditsBean = new DepartmentCreditsBean();
    request.setAttribute("departmentCreditsBean", departmentCreditsBean);
    return mapping.findForward("viewDepartmentTeacherService");
}

public ActionForward exportDepartmentTeacherService(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws FenixServiceException, IOException {
    DepartmentCreditsBean departmentCreditsBean = getRenderedObject();
    List<Department> departments = new ArrayList<Department>();
    if (departmentCreditsBean.getDepartment() != null) {
        departments.add(departmentCreditsBean.getDepartment());
    } else {
        departments.addAll(departmentCreditsBean.getAvailableDepartments());
    }
        Spreadsheet firstSpreadsheet = null, spreadsheet = null;
    PeriodFormatter periodFormatter =
            new PeriodFormatterBuilder().printZeroAlways().minimumPrintedDigits(2).appendHours().appendSuffix(":")
                    .appendMinutes().toFormatter();
    for (Department department : departments) {
        if(spreadsheet==null) {
            firstSpreadsheet = spreadsheet = new Spreadsheet(department.getAcronym());
        }else {
            spreadsheet = spreadsheet.addSpreadsheet(department.getAcronym());
        }
        
        spreadsheet.setHeader(BundleUtil.getString(Bundle.DEPARTMENT_MEMBER, "label.teacherService.course.name"));
        spreadsheet.setHeader(BundleUtil.getString(Bundle.DEPARTMENT_MEMBER, "label.teacherService.course.degrees"));
        spreadsheet.setHeader(BundleUtil.getString(Bundle.DEPARTMENT_MEMBER, "label.teacherService.course.semester"));

        spreadsheet.setHeader(BundleUtil.getString(Bundle.DEPARTMENT_MEMBER,
                "label.teacherService.course.firstTimeEnrolledStudentsNumber"));
        spreadsheet.setHeader(BundleUtil.getString(Bundle.DEPARTMENT_MEMBER,
                "label.teacherService.course.secondTimeEnrolledStudentsNumber"));

        spreadsheet.setHeader(BundleUtil.getString(Bundle.DEPARTMENT_MEMBER,
                "label.teacherService.course.totalStudentsNumber"));

        org.fenixedu.academic.domain.ShiftType[] values = org.fenixedu.academic.domain.ShiftType.values();
        for (ShiftType shiftType : values) {
            spreadsheet.setHeader(BundleUtil.getString(Bundle.DEPARTMENT_MEMBER, "label.teacherServiceDistribution.hours")
                    + " " + shiftType.getFullNameTipoAula());
            //    spreadsheet.addHeader("# Alunos / # Turnos " + shiftType.getFullNameTipoAula());
        }

        spreadsheet.setHeader(BundleUtil.getString(Bundle.DEPARTMENT_MEMBER, "label.teacherService.course.totalHours"));
        spreadsheet.setHeader(BundleUtil.getString(Bundle.DEPARTMENT_MEMBER, "label.teacherService.course.availability"));

        for (ExecutionSemester executionSemester : departmentCreditsBean.getExecutionYear().getExecutionPeriodsSet()) {
            for (ExecutionCourse executionCourse : department.getDepartmentUnit()
                    .getAllExecutionCoursesByExecutionPeriod(executionSemester)) {

                Row row = spreadsheet.addRow();
                row.setCell(executionCourse.getNome());
                row.setCell(getDegreeSiglas(executionCourse));
                row.setCell(executionCourse.getExecutionPeriod().getSemester());

                int executionCourseFirstTimeEnrollementStudentNumber = executionCourse.getFirstTimeEnrolmentStudentNumber();
                int totalStudentsNumber = executionCourse.getTotalEnrolmentStudentNumber();
                int executionCourseSecondTimeEnrollementStudentNumber =
                        totalStudentsNumber - executionCourseFirstTimeEnrollementStudentNumber;
                row.setCell(executionCourseFirstTimeEnrollementStudentNumber);
                row.setCell(executionCourseSecondTimeEnrollementStudentNumber);
                row.setCell(totalStudentsNumber);

                Double totalHours = 0.0;
                for (ShiftType shiftType : values) {
                    Double shiftHours = executionCourse.getAllShiftUnitHours(shiftType).doubleValue();
                    totalHours += shiftHours;
                    row.setCell(shiftHours);
                }

                Duration totalShiftsDuration = new Duration(new Double(executionCourse.getAssociatedShifts().stream()
                        .mapToDouble(s -> s.getCourseLoadWeeklyAverage().doubleValue()).sum() * 3600000).longValue());
                row.setCell(periodFormatter.print(totalShiftsDuration.toPeriod()));
                int colNum = row.getCells().size();
                row.setCell("");

                Duration totalLecturedDuration = Duration.ZERO;
                for (Professorship professorship : executionCourse.getProfessorshipsSet()) {
                    Teacher teacher = professorship.getTeacher();
                    if (teacher != null) {
                        Duration teacherLecturedTime =
                                TeacherService.getLecturedDurationOnExecutionCourse(teacher, executionCourse);
                        totalLecturedDuration = totalLecturedDuration.plus(teacherLecturedTime);
                        row.setCell(teacher.getPerson().getUsername());
                        row.setCell(teacher.getPerson().getName());
                        row.setCell(periodFormatter.print(teacherLecturedTime.toPeriod()));
                    }

                }

                row.setCell(periodFormatter.print(totalShiftsDuration.minus(totalLecturedDuration).toPeriod()),
                        colNum);
            }
        }
        spreadsheet = spreadsheet.addSpreadsheet(department.getAcronym() + "_docentes");
        spreadsheet.setHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.teacher.id"));
        spreadsheet.setHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.teacher.name"));
        spreadsheet.setHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.category"));
        spreadsheet.setHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.course"));
        spreadsheet.setHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.degrees"));
        spreadsheet.setHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.execution-period"));
        spreadsheet.setHeader(BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.hours"));

        Set<Teacher> allTeachers = Sets.newHashSet(department.getAllTeachers(departmentCreditsBean.getExecutionYear()));
        for (Teacher teacher : allTeachers) {
            Row row = spreadsheet.addRow();
            row.setCell(teacher.getPerson().getUsername());
            row.setCell(teacher.getPerson().getProfile().getDisplayName());
            TeacherAuthorization teacherAuthorization =
                    teacher.getTeacherAuthorization(departmentCreditsBean.getExecutionYear().getAcademicInterval()).orElse(
                            null);
            if (teacherAuthorization != null) {
                row.setCell(teacherAuthorization.getTeacherCategory().getName().getContent());
            }
            for (Professorship professorship : teacher.getProfessorships(departmentCreditsBean.getExecutionYear())) {
                row = spreadsheet.addRow();
                row.setCell(3, professorship.getExecutionCourse().getNome());
                row.setCell(getDegreeSiglas(professorship.getExecutionCourse()));
                row.setCell(professorship.getExecutionCourse().getExecutionPeriod().getSemester());
                Duration teacherLecturedTime =
                        TeacherService.getLecturedDurationOnExecutionCourse(professorship.getTeacher(),
                                professorship.getExecutionCourse());
                row.setCell(periodFormatter.print(teacherLecturedTime.toPeriod()));
            }
        }
    }

    response.setContentType("text/plain");
    StringBuilder filename = new StringBuilder("servicoDocencia");
    filename.append((departments.size() == 1 ? departments.iterator().next().getAcronym() : "Departamentos"));
    filename.append("_").append(departmentCreditsBean.getExecutionYear().getQualifiedName().replaceAll("/", "_"))
            .append(".xls");
    response.setHeader("Content-disposition", "attachment; filename=" + filename.toString());
    final ServletOutputStream writer = response.getOutputStream();
    firstSpreadsheet.exportToXLSSheet(writer);
    writer.flush();
    response.flushBuffer();
    return null;
}

public String getDegreeSiglas(ExecutionCourse executionCourse) {
    return executionCourse.getAssociatedCurricularCoursesSet().stream().map(CurricularCourse::getDegreeCurricularPlan)
            .map(DegreeCurricularPlan::getDegree).map(Degree::getSigla).collect(Collectors.joining(", "));
}

}