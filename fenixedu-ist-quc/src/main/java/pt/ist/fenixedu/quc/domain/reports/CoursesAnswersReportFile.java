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
import pt.ist.fenixedu.quc.domain.InquiryStudentTeacherAnswer;
import pt.ist.fenixedu.quc.domain.QuestionAnswer;

public class CoursesAnswersReportFile extends CoursesAnswersReportFile_Base {

    public String getJobName() {
        return "Relatório respostas alunos";
    }

    protected String getPrefix() {
        return "studentAnswers";
    }

    public void renderReport(Spreadsheet spreadsheet) throws Exception {

        spreadsheet.setHeader("Código Período Execução");
        spreadsheet.setHeader("Código Resposta");
        spreadsheet.setHeader("Código Curso Execução");
        spreadsheet.setHeader("Código Disciplina Execução");
        spreadsheet.setHeader("Código Professorship");
        spreadsheet.setHeader("Tipo Turno");
        spreadsheet.setHeader("Código Pergunta");
        spreadsheet.setHeader("Pergunta");
        spreadsheet.setHeader("Código Resposta Disciplina");

        for (ExecutionSemester executionSemester : getExecutionYear().getExecutionPeriodsSet()) {
            for (InquiryCourseAnswer inquiryAnswer : executionSemester.getInquiryCourseAnswersSet()) {
                for (QuestionAnswer questionAnswer : inquiryAnswer.getQuestionAnswersSet()) {
                    Row row = spreadsheet.addRow();
                    row.setCell(GepReportFile.getExecutionSemesterCode(inquiryAnswer.getExecutionPeriod()));
                    row.setCell(inquiryAnswer.getCode().toString());
                    row.setCell(GepReportFile.getExecutionDegreeCode(inquiryAnswer.getExecutionDegreeCourse()));
                    row.setCell(GepReportFile.getExecutionCourseCode(inquiryAnswer.getExecutionCourse()));
                    row.setCell("");
                    row.setCell("");
                    row.setCell(questionAnswer.getInquiryQuestion().getCode().toString());
                    row.setCell(questionAnswer.getAnswer());
                    row.setCell("");
                }
                for (InquiryStudentTeacherAnswer studentTeacherAnswer : inquiryAnswer
                        .getAssociatedInquiryStudentTeacherAnswersSet()) {
                    for (QuestionAnswer questionAnswer : studentTeacherAnswer.getQuestionAnswersSet()) {
                        Row row = spreadsheet.addRow();
                        row.setCell("");
                        row.setCell(studentTeacherAnswer.getCode().toString());
                        row.setCell("");
                        row.setCell("");
                        row.setCell(GepReportFile.getProfessorshipCode(studentTeacherAnswer.getProfessorship()));
                        row.setCell(studentTeacherAnswer.getShiftType().toString());
                        row.setCell(String.valueOf(questionAnswer.getInquiryQuestion().getCode()));
                        row.setCell(questionAnswer.getAnswer());
                        row.setCell(inquiryAnswer.getCode().toString());
                    }
                }
            }
        }
    }
}
