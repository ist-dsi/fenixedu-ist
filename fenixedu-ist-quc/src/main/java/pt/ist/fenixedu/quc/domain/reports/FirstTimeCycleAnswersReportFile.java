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

import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.reports.GepReportFile;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

import pt.ist.fenixedu.quc.domain.InquiryStudentCycleAnswer;
import pt.ist.fenixedu.quc.domain.QuestionAnswer;

public class FirstTimeCycleAnswersReportFile extends FirstTimeCycleAnswersReportFile_Base {

    @Override
    public String getJobName() {
        return "Respostas ciclo primeira vez QUC";
    }

    @Override
    protected String getPrefix() {
        return "quc1stTimeCycleAnswers";
    }

    @Override
    public void renderReport(Spreadsheet spreadsheet) throws Exception {
        spreadsheet.setHeader("Período Execução");
        spreadsheet.setHeader("Curso Execução");
        spreadsheet.setHeader("IstID");
        spreadsheet.setHeader("Ciclo");
        spreadsheet.setHeader("Pergunta");
        spreadsheet.setHeader("Resposta");

        for (Registration registration : Bennu.getInstance().getRegistrationsSet()) {
            InquiryStudentCycleAnswer studentCycleAnswer = registration.getInquiryStudentCycleAnswer();
            if (studentCycleAnswer != null && getExecutionYear().containsDate(studentCycleAnswer.getResponseDateTime())) {
                for (QuestionAnswer questionAnswer : studentCycleAnswer.getQuestionAnswersSet()) {
                    Row row = spreadsheet.addRow();
                    row.setCell(GepReportFile.getExecutionSemesterCode(ExecutionSemester.readByDateTime(studentCycleAnswer
                            .getResponseDateTime())));
                    String executionCoursesCodes =
                            registration.getFirstStudentCurricularPlan().getDegreeCurricularPlan().getExecutionDegreesSet()
                                    .stream().filter(ed -> ed.getExecutionYear() == getExecutionYear())
                                    .map(ed -> GepReportFile.getExecutionDegreeCode(ed)).collect(Collectors.joining(", "));
                    row.setCell(executionCoursesCodes);
                    row.setCell(studentCycleAnswer.getRegistration().getPerson().getUsername());
                    row.setCell(registration.getCycleType(getExecutionYear()) != null ? registration.getCycleType(
                            getExecutionYear()).getDescription() : "");
                    row.setCell(questionAnswer.getInquiryQuestion().getCode().toString());
                    row.setCell(questionAnswer.getAnswer());
                }
            }
        }

        for (PhdIndividualProgramProcess phdProgram : Bennu.getInstance().getProcessesSet().stream()
                .filter(p -> p instanceof PhdIndividualProgramProcess).map(p -> ((PhdIndividualProgramProcess) p))
                .collect(Collectors.toList())) {
            InquiryStudentCycleAnswer studentCycleAnswer = phdProgram.getInquiryStudentCycleAnswer();
            if (studentCycleAnswer != null && getExecutionYear().containsDate(studentCycleAnswer.getResponseDateTime())) {
                for (QuestionAnswer questionAnswer : studentCycleAnswer.getQuestionAnswersSet()) {
                    Row row = spreadsheet.addRow();
                    row.setCell(GepReportFile.getExecutionSemesterCode(ExecutionSemester.readByDateTime(studentCycleAnswer
                            .getResponseDateTime())));
                    if (phdProgram.getRegistration() != null) {
                        String executionCoursesCodes =
                                phdProgram.getRegistration().getFirstStudentCurricularPlan().getDegreeCurricularPlan()
                                        .getExecutionDegreesSet().stream()
                                        .filter(ed -> ed.getExecutionYear() == getExecutionYear())
                                        .map(ed -> GepReportFile.getExecutionDegreeCode(ed)).collect(Collectors.joining(", "));
                        row.setCell(executionCoursesCodes);
                    } else {
                        row.setCell("sem matrícula");
                    }
                    row.setCell(studentCycleAnswer.getPhdProcess().getPerson().getUsername());
                    if (phdProgram.getRegistration() != null) {
                        row.setCell(phdProgram.getRegistration().getCycleType(getExecutionYear()) != null ? phdProgram
                                .getRegistration().getCycleType(getExecutionYear()).getDescription() : "");
                    } else {
                        row.setCell("sem matrícula");
                    }
                    row.setCell(questionAnswer.getInquiryAnswer().getCode().toString());
                    row.setCell(questionAnswer.getAnswer());
                }
            }
        }
    }
}
