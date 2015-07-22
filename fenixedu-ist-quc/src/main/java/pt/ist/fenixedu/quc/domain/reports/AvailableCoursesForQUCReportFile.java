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
package pt.ist.fenixedu.quc.domain.reports;

import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.reports.GepReportFile;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

import pt.ist.fenixedu.quc.domain.InquiriesRoot;

public class AvailableCoursesForQUCReportFile extends AvailableCoursesForQUCReportFile_Base {

    @Override
    public String getJobName() {
        return "Disciplinas disponíveis para QUC";
    }

    @Override
    protected String getPrefix() {
        return "qucAvailableCourses";
    }

    @Override
    public void renderReport(Spreadsheet spreadsheet) throws Exception {
        spreadsheet.setHeader("Código Curso Execução");
        spreadsheet.setHeader("Curso");
        spreadsheet.setHeader("Nome disciplina");
        spreadsheet.setHeader("Semestre");
        spreadsheet.setHeader("Código disciplina execução");
        spreadsheet.setHeader("Disponível para inquérito");
        for (final Degree degree : Degree.readNotEmptyDegrees()) {
            if (checkDegreeType(getDegreeType(), degree)) {
                for (final DegreeCurricularPlan degreeCurricularPlan : degree.getDegreeCurricularPlansSet()) {
                    if (checkExecutionYear(getExecutionYear(), degreeCurricularPlan)) {
                        for (final CurricularCourse curricularCourse : degreeCurricularPlan.getAllCurricularCourses()) {
                            if (checkExecutionYear(getExecutionYear(), curricularCourse)) {
                                fillData(spreadsheet, degree, curricularCourse);
                            }
                        }
                    }
                }
            }
        }
    }

    private void fillData(final Spreadsheet spreadsheet, final Degree degree, final CurricularCourse curricularCourse) {
        Set<ExecutionCourse> executionCourseSet = getExecutionCourses(curricularCourse, degree);
        for (ExecutionCourse executionCourse : executionCourseSet) {
            if (executionCourse != null) {
                addRow(spreadsheet, degree, curricularCourse, executionCourse, executionCourse.getExecutionPeriod());
                if (curricularCourse.isAnual()) {
                    ExecutionSemester executionPeriod = executionCourse.getExecutionPeriod();
                    if (executionPeriod.getSemester() == 1) {
                        addRow(spreadsheet, degree, curricularCourse, executionCourse, executionPeriod.getNextExecutionPeriod());
                    }
                }
            }
        }
    }

    private void addRow(final Spreadsheet spreadsheet, final Degree degree, final CurricularCourse curricularCourse,
            final ExecutionCourse executionCourse, final ExecutionSemester executionSemester) {
        for (ExecutionDegree executionDegree : degree.getExecutionDegreesForExecutionYear(executionSemester.getExecutionYear())) {
            Row row = spreadsheet.addRow();
            row.setCell(GepReportFile.getExecutionDegreeCode(executionDegree));
            row.setCell(degree.getNameI18N().getContent());
            row.setCell(curricularCourse.getName());
            row.setCell(executionCourse.getExecutionPeriod().getName());
            row.setCell(GepReportFile.getExecutionCourseCode(executionCourse));
            row.setCell(InquiriesRoot.isAvailableForInquiry(executionCourse) == true ? "Sim" : "Não");
        }
    }

    private Set<ExecutionCourse> getExecutionCourses(CurricularCourse curricularCourse, Degree degree) {
        Set<ExecutionCourse> result = new HashSet<ExecutionCourse>();
        for (ExecutionCourse executionCourse : curricularCourse.getExecutionCoursesByExecutionYear(getExecutionYear())) {
            for (ExecutionDegree executionDegree : executionCourse.getExecutionDegrees()) {
                if (executionDegree.getDegree() == degree) {
                    result.add(executionCourse);
                }
            }
        }
        return result;
    }
}
