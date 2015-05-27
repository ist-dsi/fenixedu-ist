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

import org.fenixedu.academic.domain.reports.GepReportFile;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

import pt.ist.fenixedu.quc.domain.StudentInquiryExecutionPeriod;
import pt.ist.fenixedu.quc.domain.StudentInquiryRegistry;

public class WorkloadSummaryBoardReportFile extends WorkloadSummaryBoardReportFile_Base {

    public String getJobName() {
        return "Relatório sobre o quadro inicial da carga de trabalho";
    }

    protected String getPrefix() {
        return "workloadSummary";
    }

    public void renderReport(Spreadsheet spreadsheet) throws Exception {
        spreadsheet.setHeader("Código Período Execução");
        spreadsheet.setHeader("Código Aluno");
        spreadsheet.setHeader("Código Disciplina Execução");
        spreadsheet.setHeader("Estado Inquérito");
        spreadsheet.setHeader("ECTS Estimados");
        spreadsheet.setHeader("% Horas Semanais Gastas");
        spreadsheet.setHeader("Dias Estudo em Época Exame");
        spreadsheet.setHeader("% Frequência às Aulas");
        spreadsheet.setHeader("Horas Semanais em Época Aulas");

        for (StudentInquiryRegistry inquiryRegistry : Bennu.getInstance().getStudentsInquiryRegistriesSet()) {
            StudentInquiryExecutionPeriod inquiryExecutionPeriod = inquiryRegistry.getStudentInquiryExecutionPeriod();
            if ((inquiryExecutionPeriod != null && inquiryExecutionPeriod.getExecutionPeriod().getExecutionYear() == getExecutionYear())
                    || inquiryExecutionPeriod == null) {
                Row row = spreadsheet.addRow();
                row.setCell(inquiryExecutionPeriod != null ? GepReportFile.getExecutionSemesterCode(inquiryExecutionPeriod
                        .getExecutionPeriod()) : null);
                row.setCell(inquiryRegistry.getStudent().getPerson().getUsername());
                row.setCell(GepReportFile.getExecutionCourseCode(inquiryRegistry.getExecutionCourse()));
                row.setCell(inquiryRegistry.getState().name());
                row.setCell(inquiryRegistry.getWeeklyHoursSpentPercentage());
                row.setCell(inquiryRegistry.getStudyDaysSpentInExamsSeason());
                row.setCell(inquiryRegistry.getAttendenceClassesPercentage());
                row.setCell(inquiryExecutionPeriod != null ? inquiryExecutionPeriod.getWeeklyHoursSpentInClassesSeason() : null);
            }
        }
    }
}
