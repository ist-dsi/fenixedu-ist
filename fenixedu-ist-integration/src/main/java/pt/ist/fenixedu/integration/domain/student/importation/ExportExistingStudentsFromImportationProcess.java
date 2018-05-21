/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.domain.student.importation;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.QueueJob;
import org.fenixedu.academic.domain.QueueJobResult;
import org.fenixedu.academic.domain.person.RoleType;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.fenixedu.spaces.domain.Space;

public class ExportExistingStudentsFromImportationProcess extends ExportExistingStudentsFromImportationProcess_Base {

    public ExportExistingStudentsFromImportationProcess() {
        super();
    }

    @Override
    public QueueJobResult execute() throws Exception {
        Spreadsheet spreadsheet = retrieveStudentsExistingInSystem();
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();

        spreadsheet.exportToCSV(byteArrayOS, ";");
        byteArrayOS.close();

        final QueueJobResult queueJobResult = new QueueJobResult();
        queueJobResult.setContentType("text/csv");
        queueJobResult.setContent(byteArrayOS.toByteArray());

        return queueJobResult;
    }

    private Spreadsheet retrieveStudentsExistingInSystem() {
        Set<Person> personSet = new HashSet<>();

        final Spreadsheet spreadsheet = new Spreadsheet("Shifts");
        spreadsheet.setHeaders("Número de Aluno", "Nome", "BI", "Curso", "Ano", "Campus", "Ficheiro de importacao");

        for (DgesStudentImportationProcess importationProcess : DgesStudentImportationProcess.readDoneJobs(getExecutionYear())) {
            if (!importationProcess.getEntryPhase().equals(getEntryPhase())) {
                continue;
            }

            final List<DegreeCandidateDTO> degreeCandidateDTOs =
                    parseDgesFile(importationProcess.getDgesStudentImportationFile().getContent(),
                            importationProcess.getUniversityAcronym(), getEntryPhase());

            for (DegreeCandidateDTO dto : degreeCandidateDTOs) {
                Person person;
                try {
                    person = dto.getMatchingPerson();
                } catch (DegreeCandidateDTO.NotFoundPersonException | DegreeCandidateDTO.TooManyMatchedPersonsException e) {
                    continue;
                } catch (DegreeCandidateDTO.MatchingPersonException e) {
                    throw new RuntimeException(e);
                }

                if (personSet.contains(person)) {
                    continue;
                }

                if ((person.getStudent() != null && !person.getStudent().getRegistrationsSet().isEmpty())
                        || person.getTeacher() != null || RoleType.TEACHER.isMember(person.getUser())) {
                    addRow(spreadsheet, person.getStudent().getNumber().toString(), person.getName(),
                            person.getDocumentIdNumber(), dto.getExecutionDegree(getExecutionYear(),
                                    importationProcess.getDgesStudentImportationForCampus()), getExecutionYear(),
                            importationProcess.getDgesStudentImportationForCampus(), importationProcess
                                    .getDgesStudentImportationFile().getFilename());

                    personSet.add(person);
                }
            }
        }

        return spreadsheet;
    }

    private void addRow(final Spreadsheet spreadsheet, final String studentNumber, String studentName, String documentIdNumber,
            final ExecutionDegree executionDegree, final ExecutionYear executionYear, final Space campus,
            String importationFilename) {
        final Row row = spreadsheet.addRow();

        row.setCell(0, studentNumber);
        row.setCell(1, studentName);
        row.setCell(2, documentIdNumber);
        row.setCell(3, executionDegree.getDegreeCurricularPlan().getName());
        row.setCell(4, executionYear.getYear());
        row.setCell(5, campus.getName());
        row.setCell(6, importationFilename);
    }

    public static boolean canRequestJob() {
        return QueueJob.getUndoneJobsForClass(ExportExistingStudentsFromImportationProcess.class).isEmpty();
    }

    public static List<ExportExistingStudentsFromImportationProcess> readDoneJobs(final ExecutionYear executionYear) {
        return executionYear.getDgesBaseProcessSet().stream()
                .filter(p -> p instanceof ExportExistingStudentsFromImportationProcess).filter(QueueJob::getDone)
                .map(p -> (ExportExistingStudentsFromImportationProcess) p).collect(Collectors.toList());
    }

    public static List<ExportExistingStudentsFromImportationProcess> readUndoneJobs(final ExecutionYear executionYear) {
        return executionYear.getDgesBaseProcessSet().stream()
                .filter(p -> p instanceof ExportExistingStudentsFromImportationProcess).filter(j -> !j.getDone())
                .map(p -> (ExportExistingStudentsFromImportationProcess) p).collect(Collectors.toList());
    }

    public static List<ExportExistingStudentsFromImportationProcess> readPendingJobs(final ExecutionYear executionYear) {
        return executionYear.getDgesBaseProcessSet().stream()
                .filter(p -> p instanceof ExportExistingStudentsFromImportationProcess)
                .filter(QueueJob::getIsNotDoneAndNotCancelled).map(p -> (ExportExistingStudentsFromImportationProcess) p)
                .collect(Collectors.toList());
    }

}
