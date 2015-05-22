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

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.reports.GepReportFile;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

import pt.ist.fenixedu.quc.domain.InquiryCourseAnswer;

public class InitialAnswersReportFile extends InitialAnswersReportFile_Base {

    public String getJobName() {
        return "Relatório sobre as respostas iniciais";
    }

    protected String getPrefix() {
        return "initialAnswers";
    }

    public void renderReport(Spreadsheet spreadsheet) throws Exception {

        spreadsheet.setHeader("Código Período Execução");
        spreadsheet.setHeader("Data Resposta");
        spreadsheet.setHeader("Código Resposta");
        spreadsheet.setHeader("Código Curso Execução");
        spreadsheet.setHeader("Código Disciplina Execução");
        spreadsheet.setHeader("Justificação Não Resposta");
        spreadsheet.setHeader("Outra Justificação");
        spreadsheet.setHeader("Nota");
        spreadsheet.setHeader("Nota Entrada");
        spreadsheet.setHeader("Tipo Aluno");
        spreadsheet.setHeader("Cometeu Fraude");
        spreadsheet.setHeader("Nº Inscrições");
        spreadsheet.setHeader("% Assiduidade Aulas");
        spreadsheet.setHeader("Horas semanais gastas em Trabalho Autónomo");
        spreadsheet.setHeader("% Horas semanais");
        spreadsheet.setHeader("Nº dias gastos em Época Exames");
        spreadsheet.setHeader("Duração da resposta (ms)");

        for (ExecutionSemester executionSemester : getExecutionYear().getExecutionPeriodsSet()) {
            for (InquiryCourseAnswer inquiryAnswer : executionSemester.getInquiryCourseAnswersSet()) {
                Row row = spreadsheet.addRow();
                row.setCell(GepReportFile.getExecutionSemesterCode(inquiryAnswer.getExecutionPeriod()));
                row.setCell(inquiryAnswer.getResponseDateTime().toString());
                row.setCell(inquiryAnswer.getCode().toString());
                row.setCell(GepReportFile.getExecutionDegreeCode(inquiryAnswer.getExecutionDegreeCourse()));
                row.setCell(GepReportFile.getExecutionCourseCode(inquiryAnswer.getExecutionCourse()));
                row.setCell(inquiryAnswer.getNotAnsweredJustification() != null ? inquiryAnswer.getNotAnsweredJustification()
                        .toString() : "");
                row.setCell(inquiryAnswer.getNotAnsweredOtherJustification());
                row.setCell(inquiryAnswer.getGrade() != null ? inquiryAnswer.getGrade().toString() : "");
                row.setCell(inquiryAnswer.getEntryGrade() != null ? inquiryAnswer.getEntryGrade().toString() : "");
                row.setCell(inquiryAnswer.getRegistrationProtocol().getDescription().getContent());
                row.setCell(inquiryAnswer.getCommittedFraud());
                row.setCell(inquiryAnswer.getNumberOfEnrolments());
                row.setCell(inquiryAnswer.getAttendenceClassesPercentage());
                row.setCell(inquiryAnswer.getWeeklyHoursSpentInAutonomousWork());
                row.setCell(inquiryAnswer.getWeeklyHoursSpentPercentage());
                row.setCell(inquiryAnswer.getStudyDaysSpentInExamsSeason());
                row.setCell(String.valueOf(inquiryAnswer.getAnswerDuration()));
            }
        }
    }
}
