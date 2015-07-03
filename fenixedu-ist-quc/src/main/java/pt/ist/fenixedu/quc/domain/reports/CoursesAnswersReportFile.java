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

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.QueueJobResult;
import org.fenixedu.academic.domain.reports.GepReportFile;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.InquiryCourseAnswer;
import pt.ist.fenixedu.quc.domain.InquiryStudentTeacherAnswer;
import pt.ist.fenixedu.quc.domain.QuestionAnswer;
import pt.ist.fenixedu.quc.domain.StudentInquiryTemplate;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

public class CoursesAnswersReportFile extends CoursesAnswersReportFile_Base {

    private static final Logger logger = LoggerFactory.getLogger(GepReportFile.class);

    public String getJobName() {
        return "Relatório respostas alunos";
    }

    protected String getPrefix() {
        return "studentAnswers";
    }

    @Override
    public String getFilename() {
        return getReportName(null, null).replace(' ', '_') + ".zip";
    }

    private String getReportName(ExecutionSemester executionSemester, InquiryBlock inquiryBlock) {

        final StringBuilder result = new StringBuilder();
        result.append(getRequestDate().toString("yyyy_MM_dd_HH_mm")).append("_");
        result.append(getPrefix()).append("_");
        result.append(getExecutionYear().getName());
        if (executionSemester != null) {
            result.append("_").append(executionSemester.getName()).append("_");
            result.append(inquiryBlock.getInquiryQuestionHeader().getTitle().getContent());
        }
        return StringNormalizer.normalizeAndRemoveAccents(result.toString().replace('/', '_').replace("º", ""));
    }

    @Override
    public void renderReport(Spreadsheet spreadsheet) throws Exception {
        // not used
    }

    @Override
    public QueueJobResult execute() throws Exception {
        Table<ExecutionSemester, InquiryBlock, Spreadsheet> reports = HashBasedTable.create();
        renderReport(reports);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bout);
        for (Cell<ExecutionSemester, InquiryBlock, Spreadsheet> cell : reports.cellSet()) {
            if (cell.getValue().getRows().size() > 1) {
                zip.putNextEntry(new ZipEntry(getReportName(cell.getRowKey(), cell.getColumnKey()) + ".csv"));
                ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
                cell.getValue().exportToCSV(byteArrayOS, "\t");
                zip.write(byteArrayOS.toByteArray());
                zip.closeEntry();
            }
        }
        zip.close();

        final QueueJobResult queueJobResult = new QueueJobResult();
        queueJobResult.setContentType("application/zip");
        queueJobResult.setContent(bout.toByteArray());

        logger.info("Job " + getFilename() + " completed");

        return queueJobResult;
    }

    public void renderReport(Table<ExecutionSemester, InquiryBlock, Spreadsheet> reports) throws Exception {
        initializeReports(reports);

        for (ExecutionSemester executionSemester : getExecutionYear().getExecutionPeriodsSet()) {
            for (InquiryCourseAnswer inquiryAnswer : executionSemester.getInquiryCourseAnswersSet()) {
                for (QuestionAnswer questionAnswer : inquiryAnswer.getQuestionAnswersSet()) {
                    Spreadsheet spreadsheet =
                            reports.get(executionSemester, questionAnswer.getInquiryQuestion().getInquiryGroupQuestion()
                                    .getInquiryBlock());
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
                        Spreadsheet spreadsheet =
                                reports.get(executionSemester, questionAnswer.getInquiryQuestion().getInquiryGroupQuestion()
                                        .getInquiryBlock());
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

    private void initializeReports(Table<ExecutionSemester, InquiryBlock, Spreadsheet> reports) {
        for (ExecutionSemester executionSemester : getExecutionYear().getExecutionPeriodsSet()) {
            for (StudentInquiryTemplate inquiryTemplate : StudentInquiryTemplate
                    .getInquiryTemplatesByExecutionPeriod(executionSemester)) {
                for (InquiryBlock inquiryBlock : inquiryTemplate.getInquiryBlocksSet()) {
                    reports.put(executionSemester, inquiryBlock, createSpreasheet(executionSemester, inquiryBlock));
                }
            }
        }
    }

    private Spreadsheet createSpreasheet(ExecutionSemester executionSemester, InquiryBlock inquiryBlock) {
        Spreadsheet spreadsheet = new Spreadsheet(getReportName(executionSemester, inquiryBlock));
        spreadsheet.setHeader("Código Período Execução");
        spreadsheet.setHeader("Código Resposta");
        spreadsheet.setHeader("Código Curso Execução");
        spreadsheet.setHeader("Código Disciplina Execução");
        spreadsheet.setHeader("Código Professorship");
        spreadsheet.setHeader("Tipo Turno");
        spreadsheet.setHeader("Código Pergunta");
        spreadsheet.setHeader("Resposta");
        spreadsheet.setHeader("Código Resposta Disciplina");
        return spreadsheet;
    }

}
