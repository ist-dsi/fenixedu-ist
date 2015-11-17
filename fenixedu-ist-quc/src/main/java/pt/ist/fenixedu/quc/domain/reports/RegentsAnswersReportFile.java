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
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.reports.GepReportFile;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

import pt.ist.fenixedu.quc.domain.QuestionAnswer;
import pt.ist.fenixedu.quc.domain.RegentInquiryTemplate;

public class RegentsAnswersReportFile extends RegentsAnswersReportFile_Base {

    @Override
    public String getJobName() {
        return "Respostas regentes QUC";
    }

    @Override
    protected String getPrefix() {
        return "qucRegentsAnswers";
    }

    @Override
    public void renderReport(Spreadsheet spreadsheet) throws Exception {
        spreadsheet.setHeader("Período Execução");
        spreadsheet.setHeader("Disciplina Execução");
        spreadsheet.setHeader("Regente");
        spreadsheet.setHeader("Pergunta");
        spreadsheet.setHeader("Resposta");

        for (ExecutionSemester executionSemester : getExecutionYear().getExecutionPeriodsSet()) {
            for (Professorship professorship : Bennu.getInstance().getProfessorshipsSet()) {
                if (professorship.getExecutionCourse().getExecutionPeriod() == executionSemester) {
                    boolean isToAnswer =
                            RegentInquiryTemplate.hasToAnswerRegentInquiry(professorship)
                                    && professorship.getInquiryRegentAnswer() != null;
                    if (isToAnswer) {
                        for (QuestionAnswer questionAnswer : professorship.getInquiryRegentAnswer().getQuestionAnswersSet()) {
                            Row row = spreadsheet.addRow();
                            row.setCell(GepReportFile.getExecutionSemesterCode(executionSemester));
                            row.setCell(GepReportFile.getExecutionCourseCode(professorship.getExecutionCourse()));
                            row.setCell(professorship.getPerson().getUsername());
                            row.setCell(questionAnswer.getInquiryAnswer().getCode().toString());
                            row.setCell(questionAnswer.getAnswer() != null ? questionAnswer.getAnswer().replace('\n', ' ')
                                    .replace('\r', ' ') : "");
                        }
                    }
                }
            }
        }
    }
}