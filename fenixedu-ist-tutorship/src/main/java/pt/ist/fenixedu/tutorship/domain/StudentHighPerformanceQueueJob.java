/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Tutorship.
 *
 * FenixEdu IST Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.domain;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.QueueJobResult;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.spreadsheet.SheetData;
import org.fenixedu.commons.spreadsheet.SpreadsheetBuilder;
import org.fenixedu.commons.spreadsheet.WorkbookExportFormat;
import org.joda.time.DateTime;

public class StudentHighPerformanceQueueJob extends StudentHighPerformanceQueueJob_Base {
    public StudentHighPerformanceQueueJob() {
        super();
    }

    @Override
    public QueueJobResult execute() throws Exception {
        final ExecutionSemester semester = (ExecutionSemester) ExecutionSemester.getExecutionInterval(getExecutionInterval());

        Collection<Registration> highPerformants =
                Bennu.getInstance().getRegistrationsSet().stream().filter(r -> r.hasActiveLastState(semester)).filter(r -> {
                    Collection<Enrolment> enrols = r.getEnrolments(semester);
                    return enrols.isEmpty() ? false : enrols.stream().allMatch(e -> e.isApproved());
                }).collect(Collectors.toSet());

        SheetData<Registration> data = new SheetData<Registration>(highPerformants) {
            @Override
            protected void makeLine(Registration item) {
                addCell("istId", item.getPerson().getUsername());
                addCell("Nome", item.getPerson().getName());
                double totalEcts = 0;
                Collection<Enrolment> enrols = item.getEnrolments(semester);
                for (Enrolment enrolment : enrols) {
                    totalEcts += enrolment.getEctsCredits();
                }
                addCell("Créditos Inscritos", totalEcts);
                addCell("Curso", item.getDegree().getNameFor(semester));
                CycleType cycleType = item.getCycleType(semester.getExecutionYear());
                addCell("Ciclo", cycleType != null ? cycleType.getDescription() : null);
                addCell("Email", item.getPerson().getEmailForSendingEmails());
                Tutorship activeTutorship = Tutorship.getActiveTutorship(item.getLastStudentCurricularPlan());
                addCell("Tutor", activeTutorship != null ? activeTutorship.getPerson().getName() : null);
                addCell("Email Tutor", activeTutorship != null ? activeTutorship.getPerson().getEmailForSendingEmails() : null);
            }
        };

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new SpreadsheetBuilder().addSheet("elevado rendimento", data).build(WorkbookExportFormat.EXCEL, stream);

        QueueJobResult result = new QueueJobResult();
        result.setContentType("application/xls");
        result.setContent(stream.toByteArray());
        return result;
    }

    @Override
    public String getFilename() {
        return "Students_" + new DateTime().toString("yyyy-MM-dd") + ".xls";
    }

}
