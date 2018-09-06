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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.EntryPhase;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.QueueJob;
import org.fenixedu.academic.domain.QueueJobResult;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.candidacy.Candidacy;
import org.fenixedu.academic.domain.candidacy.CandidacySituation;
import org.fenixedu.academic.domain.candidacy.CandidacySituationType;
import org.fenixedu.academic.domain.candidacy.DegreeCandidacy;
import org.fenixedu.academic.domain.candidacy.IMDCandidacy;
import org.fenixedu.academic.domain.candidacy.StandByCandidacySituation;
import org.fenixedu.academic.domain.candidacy.StudentCandidacy;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Accountability;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.organizationalStructure.UnitUtils;
import org.fenixedu.academic.domain.student.PrecedentDegreeInformation;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.spaces.domain.Space;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.tutorship.domain.TutorshipIntention;
import pt.ist.fenixframework.Atomic;

public class DgesStudentImportationProcess extends DgesStudentImportationProcess_Base {

    private static final Logger logger = LoggerFactory.getLogger(DgesStudentImportationProcess.class);

    protected DgesStudentImportationProcess() {
        super();
    }

    protected DgesStudentImportationProcess(final ExecutionYear executionYear, final Space campus, final EntryPhase entryPhase,
            DgesStudentImportationFile dgesStudentImportationFile) {
        this();

        init(executionYear, campus, entryPhase, dgesStudentImportationFile);
    }

    protected void init(final ExecutionYear executionYear, final Space campus, final EntryPhase entryPhase,
            DgesStudentImportationFile dgesStudentImportationFile) {
        super.init(executionYear, entryPhase);
        String[] args = new String[0];

        if (campus == null) {
            throw new DomainException("error.DgesStudentImportationProcess.campus.is.null", args);
        }
        String[] args1 = {};
        if (dgesStudentImportationFile == null) {
            throw new DomainException("error.DgesStudentImportationProcess.importation.file.is.null", args1);
        }

        setDgesStudentImportationForCampus(campus);
        setDgesStudentImportationFile(dgesStudentImportationFile);
    }

    @Override
    public QueueJobResult execute() throws Exception {

        ByteArrayOutputStream stream = null;
        PrintWriter LOG_WRITER = null;
        try {
            stream = new ByteArrayOutputStream();
            LOG_WRITER = new PrintWriter(new BufferedOutputStream(stream));

            importCandidates(LOG_WRITER);
        } catch (Throwable a) {
            logger.error(a.getMessage(), a);
            throw new RuntimeException(a);
        }

        finally {
            if (LOG_WRITER != null) {
                LOG_WRITER.close();
            }
            stream.close();
        }

        final QueueJobResult queueJobResult = new QueueJobResult();
        queueJobResult.setContentType("text/plain");

        queueJobResult.setContent(stream.toByteArray());

        stream.close();
        return queueJobResult;
    }

    @Override
    public String getFilename() {
        return "DgesStudentImportationProcess_result_" + getExecutionYear().getName().replaceAll("/", "-") + ".txt";
    }

    public void importCandidates(final PrintWriter LOG_WRITER) {

        final List<DegreeCandidateDTO> degreeCandidateDTOs =
                parseDgesFile(getDgesStudentImportationFile().getContent(), getUniversityAcronym(), getEntryPhase());

        final Employee employee = AdministrativeOffice.readDegreeAdministrativeOffice().getCoordinator().getPerson().getEmployee();

        LOG_WRITER.println(String.format("DGES Entries for %s : %s", getDgesStudentImportationForCampus().getName(),
                degreeCandidateDTOs.size()));

        createDegreeCandidacies(LOG_WRITER, employee, degreeCandidateDTOs);
        distributeTutorshipIntentions(degreeCandidateDTOs);
    }

    private void distributeTutorshipIntentions(List<DegreeCandidateDTO> degreeCandidateDTOs) {
        if (getEntryPhase().equals(EntryPhase.FIRST_PHASE)) {
            HashMap<ExecutionDegree, Integer> studentsPerExecution = new HashMap<ExecutionDegree, Integer>();
            for (final DegreeCandidateDTO degreeCandidateDTO : degreeCandidateDTOs) {
                final ExecutionDegree executionDegree =
                        degreeCandidateDTO.getExecutionDegree(getExecutionYear(), getDgesStudentImportationForCampus());
                Integer numberOfStudents = studentsPerExecution.get(executionDegree);
                if (numberOfStudents != null) {
                    numberOfStudents++;
                } else {
                    numberOfStudents = 1;
                }
                studentsPerExecution.put(executionDegree, numberOfStudents);
            }

            for (ExecutionDegree executionDegree : studentsPerExecution.keySet()) {
                int numberOfStudents = studentsPerExecution.get(executionDegree);
                int numberOfTutors = TutorshipIntention.getTutorshipIntentions(executionDegree).size();
                if (numberOfTutors > 0) {
                    int exceedingStudents = numberOfStudents % numberOfTutors;
                    int studentPerTutor = numberOfStudents / numberOfTutors;
                    for (TutorshipIntention tutorshipIntention : TutorshipIntention.getTutorshipIntentions(executionDegree)) {
                        tutorshipIntention.setMaxStudentsToTutor(studentPerTutor);
                        if (exceedingStudents > 0) {
                            tutorshipIntention.setMaxStudentsToTutor(tutorshipIntention.getMaxStudentsToTutor() + 1);
                            exceedingStudents--;
                        }
                    }
                }
            }
        }
    }

    private void createDegreeCandidacies(final PrintWriter LOG_WRITER, final Employee employee,
            final List<DegreeCandidateDTO> degreeCandidateDTOs) {
        final Map<String,Unit> highSchoolCache = loadExternalInstitutionUnits();
        int processed = 0;
        for (final DegreeCandidateDTO degreeCandidateDTO : degreeCandidateDTOs) {

            if (++processed % 150 == 0) {
                logger.info("Processed :" + processed);
            }

            logCandidate(LOG_WRITER, degreeCandidateDTO);

            Person person = null;
            try {
                person = degreeCandidateDTO.getMatchingPerson();
            } catch (DegreeCandidateDTO.NotFoundPersonException e) {
                person = degreeCandidateDTO.createPerson();
                logCreatedPerson(LOG_WRITER, person);
            } catch (DegreeCandidateDTO.TooManyMatchedPersonsException e) {
                logTooManyMatchsForCandidate(LOG_WRITER, degreeCandidateDTO);
                continue;
            } catch (DegreeCandidateDTO.MatchingPersonException e) {
                throw new RuntimeException(e);
            }

            if (person.getStudent() != null && !person.getStudent().getRegistrationsSet().isEmpty()) {
                logCandidateIsStudentWithRegistrationAlreadyExists(LOG_WRITER, degreeCandidateDTO, person);
                continue;
            }

            if (person.getTeacher() != null) {
                logCandidateIsTeacher(LOG_WRITER, degreeCandidateDTO, person);
                continue;
            }

            if (person.getEmployee() != null) {
                logCandidateIsEmployee(LOG_WRITER, degreeCandidateDTO, person);
            }

            int studentNumber = Student.generateStudentNumber();
            if (person.getStudent() == null) {
                // Ensure that the same student number is created
                new Student(person, studentNumber);
                logCreatedStudent(LOG_WRITER, person.getStudent());
            }

            person.ensureOpenUserAccount();

            voidPreviousCandidacies(person,
                    degreeCandidateDTO.getExecutionDegree(getExecutionYear(), getDgesStudentImportationForCampus()));

            final StudentCandidacy studentCandidacy = createCandidacy(employee, degreeCandidateDTO, person, highSchoolCache);
            new StandByCandidacySituation(studentCandidacy, employee.getPerson());

        }
    }

    private void logCreatedStudent(final PrintWriter LOG_WRITER, final Student student) {
        LOG_WRITER.println("Created student");
    }

    private void logCreatedPerson(final PrintWriter LOG_WRITER, final Person person) {
        LOG_WRITER.println("Created person");
    }

    private void logCandidate(final PrintWriter LOG_WRITER, DegreeCandidateDTO degreeCandidateDTO) {
        LOG_WRITER.println("-------------------------------------------------------------------");
        LOG_WRITER.println("Processing: " + degreeCandidateDTO.toString());
    }

    private StudentCandidacy createCandidacy(final Employee employee, final DegreeCandidateDTO degreeCandidateDTO,
            final Person person, final Map<String,Unit> highSchoolCache) {
        final ExecutionDegree executionDegree =
                degreeCandidateDTO.getExecutionDegree(getExecutionYear(), getDgesStudentImportationForCampus());
        StudentCandidacy candidacy = null;

        if (executionDegree.getDegree().getDegreeType().isBolonhaDegree()) {
            candidacy =
                    new DegreeCandidacy(person, executionDegree, employee.getPerson(), degreeCandidateDTO.getEntryGrade(),
                            degreeCandidateDTO.getContigent(), degreeCandidateDTO.getIngressionType(),
                            degreeCandidateDTO.getEntryPhase(), degreeCandidateDTO.getPlacingOption());

        } else if (executionDegree.getDegree().getDegreeType().isIntegratedMasterDegree()) {
            candidacy =
                    new IMDCandidacy(person, executionDegree, employee.getPerson(), degreeCandidateDTO.getEntryGrade(),
                            degreeCandidateDTO.getContigent(), degreeCandidateDTO.getIngressionType(),
                            degreeCandidateDTO.getEntryPhase(), degreeCandidateDTO.getPlacingOption());

        } else {
            throw new RuntimeException("Unexpected degree type from DGES file");
        }

        candidacy.setHighSchoolType(degreeCandidateDTO.getHighSchoolType());
        candidacy.setFirstTimeCandidacy(true);
        createPrecedentDegreeInformation(candidacy, degreeCandidateDTO, highSchoolCache);
        candidacy.setDgesStudentImportationProcess(this);

        return candidacy;
    }

    private void createPrecedentDegreeInformation(final StudentCandidacy studentCandidacy,
            final DegreeCandidateDTO degreeCandidateDTO, final Map<String,Unit> highSchoolCache) {
        final PrecedentDegreeInformation precedentDegreeInformation = studentCandidacy.getPrecedentDegreeInformation();
        precedentDegreeInformation.setStudentCandidacy(studentCandidacy);

        precedentDegreeInformation.setConclusionGrade(degreeCandidateDTO.getHighSchoolFinalGrade());
        precedentDegreeInformation.setDegreeDesignation(degreeCandidateDTO.getHighSchoolDegreeDesignation());
        precedentDegreeInformation.setInstitution(highSchoolCache.get(degreeCandidateDTO.getHighSchoolName()));
    }

    private Map<String, Unit> loadExternalInstitutionUnits() {
        final Map<String, Unit> cache = new HashMap<>();
        loadExternalInstitutionUnits(cache, UnitUtils.readExternalInstitutionUnit());
        return cache;
    }

    private void loadExternalInstitutionUnits(final Map<String, Unit> cache, final Unit unit) {
        final String unitName = unit.getName();
        if (!cache.containsKey(unitName)) {
            cache.put(unitName, unit);
        }
        for (final Accountability acc : unit.getChildsSet()) {
            final Party child = acc.getChildParty();
            if (child instanceof Unit) {
                final Unit childUnit = (Unit) child;
                loadExternalInstitutionUnits(cache, childUnit);
            }
        }
    }

    private void voidPreviousCandidacies(Person person, ExecutionDegree executionDegree) {
        for (Candidacy candidacy : person.getCandidaciesSet()) {
            if (candidacy instanceof StudentCandidacy) {
                StudentCandidacy studentCandidacy = (StudentCandidacy) candidacy;
                if (studentCandidacy.getExecutionDegree().getExecutionYear() == executionDegree.getExecutionYear()
                        && !studentCandidacy.isConcluded()) {
                    studentCandidacy.cancelCandidacy();
                }
            }
        }
    }

    private void logCandidateIsEmployee(final PrintWriter LOG_WRITER, DegreeCandidateDTO degreeCandidateDTO, Person person) {
        LOG_WRITER.println(String.format("CANDIDATE WITH ID %s IS EMPLOYEE WITH NUMBER %s",
                degreeCandidateDTO.getDocumentIdNumber(), person.getEmployee().getEmployeeNumber()));

    }

    private void logCandidateIsTeacher(final PrintWriter LOG_WRITER, DegreeCandidateDTO degreeCandidateDTO, Person person) {
        LOG_WRITER.println(String.format("CANDIDATE WITH ID %s IS TEACHER WITH USERNAME %s",
                degreeCandidateDTO.getDocumentIdNumber(), person.getUsername()));
    }

    private void logCandidateIsStudentWithRegistrationAlreadyExists(final PrintWriter LOG_WRITER,
            DegreeCandidateDTO degreeCandidateDTO, Person person) {
        LOG_WRITER.println(String.format("CANDIDATE WITH ID %s IS THE STUDENT %s WITH REGISTRATIONS",
                degreeCandidateDTO.getDocumentIdNumber(), person.getStudent().getStudentNumber()));

    }

    private void logTooManyMatchsForCandidate(final PrintWriter LOG_WRITER, DegreeCandidateDTO degreeCandidateDTO) {
        LOG_WRITER.println(String.format("CANDIDATE WITH ID %s HAS MANY PERSONS", degreeCandidateDTO.getDocumentIdNumber()));
    }

    String getUniversityAcronym() {
        return "ALAMEDA".equals(getDgesStudentImportationForCampus().getName()) ? ALAMEDA_UNIVERSITY : TAGUS_UNIVERSITY;
    }

    public static boolean canRequestJob() {
        return QueueJob.getUndoneJobsForClass(DgesStudentImportationProcess.class).isEmpty();
    }

    public static List<DgesStudentImportationProcess> readAllJobs(final ExecutionYear executionYear) {
        return executionYear.getDgesBaseProcessSet().stream().filter(p -> p instanceof DgesStudentImportationProcess)
                .map(p -> (DgesStudentImportationProcess) p).collect(Collectors.toList());
    }

    public static List<DgesStudentImportationProcess> readDoneJobs(final ExecutionYear executionYear) {
        return executionYear.getDgesBaseProcessSet().stream().filter(p -> p instanceof DgesStudentImportationProcess)
                .filter(QueueJob::getDone).map(p -> (DgesStudentImportationProcess) p).collect(Collectors.toList());
    }

    public static List<DgesStudentImportationProcess> readUndoneJobs(final ExecutionYear executionYear) {
        return executionYear.getDgesBaseProcessSet().stream().filter(p -> p instanceof DgesStudentImportationProcess)
                .filter(j -> !j.getDone()).map(p -> (DgesStudentImportationProcess) p).collect(Collectors.toList());
    }

    public static List<DgesStudentImportationProcess> readPendingJobs(final ExecutionYear executionYear) {
        return executionYear.getDgesBaseProcessSet().stream().filter(p -> p instanceof DgesStudentImportationProcess)
                .filter(QueueJob::getIsNotDoneAndNotCancelled).map(p -> (DgesStudentImportationProcess) p)
                .collect(Collectors.toList());
    }

    @Atomic
    public static void cancelStandByCandidaciesFromPreviousYears(final ExecutionYear executionYear) {
        final ExecutionYear previous = executionYear.getPreviousExecutionYear();
        if (previous != null) {
            previous.getExecutionDegreesSet().stream()
                .flatMap(ed -> ed.getStudentCandidaciesSet().stream())
                .filter(sc -> !sc.getCandidacySituationsSet().isEmpty() && isToRemove(sc))
                .forEach(StudentCandidacy::cancelCandidacy);

            cancelStandByCandidaciesFromPreviousYears(previous);
        }
    }

    private static boolean isToRemove(final StudentCandidacy candidacy) {
        return (candidacy instanceof DegreeCandidacy || candidacy instanceof IMDCandidacy) && isInStandBy(candidacy);
    }

    private static boolean isInStandBy(final StudentCandidacy candidacy) {
        final CandidacySituation situation = candidacy.getActiveCandidacySituation();
        return situation != null && situation.getCandidacySituationType() == CandidacySituationType.STAND_BY;
    }

    public static long countStandByCandidaciesFromPreviousYear(final ExecutionYear executionYear) {
        final ExecutionYear previous = executionYear.getPreviousExecutionYear();
        if (previous != null) {
            return previous.getExecutionDegreesSet().stream()
                .flatMap(ed -> ed.getStudentCandidaciesSet().stream())
                .filter(sc -> !sc.getCandidacySituationsSet().isEmpty() && isToRemove(sc))
                .count();
        }
        return 0l;
    }

}
