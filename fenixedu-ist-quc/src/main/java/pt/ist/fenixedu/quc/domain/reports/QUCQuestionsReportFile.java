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

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

import pt.ist.fenixedu.quc.domain.InquiryQuestion;
import pt.ist.fenixedu.quc.domain.InquiryQuestionHeader;

public class QUCQuestionsReportFile extends QUCQuestionsReportFile_Base {

    public String getJobName() {
        return "Relatório perguntas QUC";
    }

    protected String getPrefix() {
        return "qucQuestions";
    }

    public void renderReport(Spreadsheet spreadsheet) throws Exception {

        spreadsheet.setHeader("Código Pergunta");
        spreadsheet.setHeader("Pergunta");
        spreadsheet.setHeader("Ordem");
        spreadsheet.setHeader("Grupo/Bloco");

        for (InquiryQuestion inquiryQuestion : Bennu.getInstance().getInquiryQuestionsSet()) {
            Row row = spreadsheet.addRow();
            row.setCell(String.valueOf(inquiryQuestion.getCode()));
            row.setCell(inquiryQuestion.getLabel().getContent());
            row.setCell(inquiryQuestion.getQuestionOrder());
            InquiryQuestionHeader inquiryQuestionHeader = inquiryQuestion.getInquiryGroupQuestion().getInquiryQuestionHeader();
            if (inquiryQuestionHeader != null && inquiryQuestionHeader.getTitle() != null) {
                row.setCell(inquiryQuestionHeader.getTitle().getContent());
            } else {
                if (inquiryQuestion.getInquiryGroupQuestion().getInquiryBlock() != null) {
                    row.setCell(inquiryQuestion.getInquiryGroupQuestion().getInquiryBlock().getInquiryQuestionHeader().getTitle()
                            .getContent());
                }
            }
        }
    }
}
