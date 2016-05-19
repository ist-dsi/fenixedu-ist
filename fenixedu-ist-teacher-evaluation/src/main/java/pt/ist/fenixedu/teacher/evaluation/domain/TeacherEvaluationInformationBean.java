/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Evaluation.
 *
 * FenixEdu IST Teacher Evaluation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Evaluation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Evaluation.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.teacher.evaluation.domain;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.TeacherAuthorization;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.phd.InternalPhdParticipant;
import org.fenixedu.academic.domain.phd.thesis.ThesisJuryElement;
import org.fenixedu.academic.domain.thesis.Thesis;
import org.fenixedu.academic.domain.thesis.ThesisEvaluationParticipant;
import org.fenixedu.academic.domain.thesis.ThesisParticipationType;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.contracts.domain.organizationalStructure.PersonFunction;
import pt.ist.fenixedu.teacher.evaluation.service.external.SotisPublications;
import pt.ist.fenixedu.teacher.evaluation.service.external.SotisPublications.Publication;

import com.google.common.base.Joiner;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TeacherEvaluationInformationBean implements Serializable {

    private static final String DIR_NAME = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/projectInfoForTeacherEvaluation";
    final static String[] institutions = new String[] { "IST", "ADIST", "IST-ID" };
    private static final BigDecimal MANAGEMENT_FUNCTION_COEFICIENT = BigDecimal.valueOf(1.5);

    private TeacherEvaluationProcess teacherEvaluationProcess;
    private TeacherAuthorization teacherAuthorization;
    private SortedSet<ProfessorshipEvaluationBean> professorships = new TreeSet<ProfessorshipEvaluationBean>();
    private SortedSet<OrientationBean> orientations = new TreeSet<OrientationBean>();
    private SortedSet<Publication> publications = new TreeSet<Publication>();
    private SortedSet<OrientationBean> functions = new TreeSet<OrientationBean>();

    public TeacherEvaluationInformationBean(TeacherEvaluationProcess teacherEvaluationProcess) {
        this.teacherEvaluationProcess = teacherEvaluationProcess;
        if (teacherEvaluationProcess.getEvaluee().getTeacher() != null) {
            LocalDate beginDate =
                    new LocalDate(teacherEvaluationProcess.getFacultyEvaluationProcess().getBeginEvaluationYear(), 1, 1);
            LocalDate endDate =
                    new LocalDate(teacherEvaluationProcess.getFacultyEvaluationProcess().getEndEvaluationYear(), 12, 31);
            setTeacherAuthorization(teacherEvaluationProcess
                    .getEvaluee()
                    .getTeacher()
                    .getLatestTeacherAuthorizationInInterval(
                            new Interval(beginDate.toDateTimeAtStartOfDay(), endDate.toDateTimeAtStartOfDay())).orElse(null));

            for (Professorship professorship : getTeacherEvaluationProcess().getEvaluee().getTeacher().getProfessorships()) {
                if (professorship.getExecutionCourse().getExecutionPeriod().getBeginLocalDate().getYear() >= getTeacherEvaluationProcess()
                        .getFacultyEvaluationProcess().getBeginEvaluationYear()
                        && professorship.getExecutionCourse().getExecutionPeriod().getBeginLocalDate().getYear() <= getTeacherEvaluationProcess()
                                .getFacultyEvaluationProcess().getEndEvaluationYear()) {
                    professorships.addAll(ProfessorshipEvaluationBean.getProfessorshipEvaluationBeanSet(professorship));
                }
            }

            for (ThesisEvaluationParticipant participant : teacherEvaluationProcess.getEvaluee()
                    .getThesisEvaluationParticipantsSet()) {
                Thesis thesis = participant.getThesis();
                if (thesis.isEvaluated()
                        && thesis.hasFinalEnrolmentEvaluation()
                        && thesis.getDiscussed().getYear() >= getTeacherEvaluationProcess().getFacultyEvaluationProcess()
                                .getBeginEvaluationYear()
                        && thesis.getDiscussed().getYear() <= getTeacherEvaluationProcess().getFacultyEvaluationProcess()
                                .getEndEvaluationYear()) {
                    if (participant.getType() == ThesisParticipationType.ORIENTATOR
                            || participant.getType() == ThesisParticipationType.COORIENTATOR) {
                        orientations.add(new OrientationBean(participant));
                    }
                    if (participant.getType() == ThesisParticipationType.VOWEL
                            || participant.getType() == ThesisParticipationType.PRESIDENT) {

                        String descrition =
                                Joiner.on(", ").join(
                                        BundleUtil.getString("resources.TeacherEvaluationResources", participant.getClass()
                                                .getName() + "." + ThesisParticipationType.PRESIDENT.name()),
                                        participant.getThesis().getTitle(),
                                        participant.getThesis().getStudent().getPerson().getName(),
                                        participant.getThesis().getStudent().getNumber(),
                                        thesis.getDiscussed().toLocalDate().toString());

                        functions.add(new OrientationBean(descrition, thesis.getDiscussed().toLocalDate()));
                    }

                }
            }

            for (InternalPhdParticipant internalPhdParticipant : teacherEvaluationProcess.getEvaluee()
                    .getInternalParticipantsSet()) {
                LocalDate conclusionDate = internalPhdParticipant.getIndividualProcess().getConclusionDate();
                if (conclusionDate != null
                        && conclusionDate.getYear() >= getTeacherEvaluationProcess().getFacultyEvaluationProcess()
                                .getBeginEvaluationYear()
                        && conclusionDate.getYear() <= getTeacherEvaluationProcess().getFacultyEvaluationProcess()
                                .getEndEvaluationYear()) {
                    if (internalPhdParticipant.getProcessForGuiding() != null
                            || internalPhdParticipant.getProcessForAssistantGuiding() != null) {
                        orientations.add(new OrientationBean(internalPhdParticipant));
                    }
                    ThesisJuryElement thesisJuryElement = getThesisJuryElement(internalPhdParticipant);
                    if (thesisJuryElement != null) {
                        String descrition =
                                Joiner.on(", ").join(
                                        BundleUtil.getString("resources.TeacherEvaluationResources", internalPhdParticipant
                                                .getClass().getName() + "." + ThesisParticipationType.PRESIDENT.name()),
                                        internalPhdParticipant.getIndividualProcess().getThesisTitle(),
                                        internalPhdParticipant.getIndividualProcess().getStudent().getPerson().getName(),
                                        internalPhdParticipant.getIndividualProcess().getStudent().getNumber(),
                                        internalPhdParticipant.getIndividualProcess().getConclusionDate().toString());

                        functions.add(new OrientationBean(descrition, internalPhdParticipant.getIndividualProcess()
                                .getConclusionDate()));
                    }
                }
            }

            publications.addAll(new SotisPublications().getPublications(getTeacherEvaluationProcess().getEvaluee().getUser(),
                    teacherEvaluationProcess.getFacultyEvaluationProcess().getBeginEvaluationYear(), teacherEvaluationProcess
                            .getFacultyEvaluationProcess().getEndEvaluationYear()));

            for (PersonFunction personFunction : (Collection<PersonFunction>) teacherEvaluationProcess.getEvaluee()
                    .getParentAccountabilities(AccountabilityTypeEnum.MANAGEMENT_FUNCTION, PersonFunction.class)) {
                if (personFunction.getBeginDate().getYear() >= getTeacherEvaluationProcess().getFacultyEvaluationProcess()
                        .getBeginEvaluationYear()
                        && personFunction.getBeginDate().getYear() <= getTeacherEvaluationProcess().getFacultyEvaluationProcess()
                                .getEndEvaluationYear() && !personFunction.getFunction().isVirtual()) {
                    functions.add(new OrientationBean(personFunction));
                }
            }
        }
    }

    private ThesisJuryElement getThesisJuryElement(InternalPhdParticipant internalPhdParticipant) {
        for (final ThesisJuryElement element : internalPhdParticipant.getThesisJuryElementsSet()) {
            if (element.isPresident() || (element.getProcess() != null
                    && element.isFor(internalPhdParticipant.getIndividualProcess().getThesisProcess()))) {
                return element;
            }
        }
        return null;
    }

    public TeacherEvaluationProcess getTeacherEvaluationProcess() {
        return teacherEvaluationProcess;
    }

    public void setTeacherEvaluationProcess(TeacherEvaluationProcess teacherEvaluationProcess) {
        this.teacherEvaluationProcess = teacherEvaluationProcess;
    }

    public TeacherAuthorization getTeacherAuthorization() {
        return teacherAuthorization;
    }

    public void setTeacherAuthorization(TeacherAuthorization teacherAuthorization) {
        this.teacherAuthorization = teacherAuthorization;
    }

    public SortedSet<ProfessorshipEvaluationBean> getProfessorships() {
        return professorships;
    }

    public void setProfessorships(SortedSet<ProfessorshipEvaluationBean> professorships) {
        this.professorships = professorships;
    }

    public SortedSet<OrientationBean> getOrientations() {
        return orientations;
    }

    public void setOrientations(SortedSet<OrientationBean> orientations) {
        this.orientations = orientations;
    }

    public SortedSet<Publication> getPublications() {
        return publications;
    }

    public void setPublications(SortedSet<Publication> publications) {
        this.publications = publications;
    }

    public SortedSet<OrientationBean> getFunctions() {
        return functions;
    }

    public void setFunctions(SortedSet<OrientationBean> functions) {
        this.functions = functions;
    }

    public JsonArray getScientificProjects() {
        JsonArray result = new JsonArray();
        for (String institution : institutions) {
            for (JsonElement jsonElement : readProjectInfoFor(teacherEvaluationProcess.getEvaluee().getUser(), institution)) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                jsonObject.addProperty("institution", institution);
                result.add(jsonElement);
            }
        }
        return result;
    }

    private static JsonArray readProjectInfoFor(final User user, final String institution) {
        final String pathForUser = pathForUser(user);
        final String filename = pathForUser + File.separatorChar + institution + ".json";
        final File file = new File(filename);
        if (file.exists()) {
            try {
                final String content = new String(Files.readAllBytes(file.toPath()));
                return new JsonParser().parse(content).getAsJsonArray();
            } catch (final IOException e) {
                throw new Error(e);
            }
        }
        return new JsonArray();
    }

    private static String pathForUser(final User user) {
        final String username = user.getUsername();
        if (username != null && !username.isEmpty() && username.length() > 6) {
            final int l = username.length();
            return DIR_NAME + File.separatorChar + username.substring(l - 2, l) + File.separatorChar
                    + username.substring(l - 4, l - 2) + File.separatorChar + username.substring(l - 6, l - 4)
                    + File.separatorChar + username;
        }
        throw new Error("Bad username format for: " + username);
    }

    public class OrientationBean implements Serializable, Comparable<OrientationBean> {

        private String participationType;
        private String responsabilityType;
        private Integer coorientationNumber;
        private Double credits;
        private String description;
        private LocalDate date;

        public OrientationBean(ThesisEvaluationParticipant participant) {
            participationType = participant.getClass().getName();
            responsabilityType = participant.getClass().getName() + "." + participant.getType().name();
            coorientationNumber =
                    (int) participant.getThesis().getParticipationsSet().stream()
                            .filter(p -> p.getType() == ThesisParticipationType.COORIENTATOR).count();
            credits = participant.getThesis().getEnrolment().getEctsCredits();
            date = participant.getThesis().getDiscussed().toLocalDate();
            this.description =
                    Joiner.on(", ").join(
                            BundleUtil.getString(Bundle.ENUMERATION, participant.getType().name()) + " ("
                                    + participant.getPercentageDistribution() + "%)", participant.getThesis().getTitle(),
                            participant.getThesis().getStudent().getPerson().getName(),
                            participant.getThesis().getStudent().getNumber(), date.toString());
        }

        public OrientationBean(InternalPhdParticipant internalPhdParticipant) {
            participationType = internalPhdParticipant.getClass().getName();
            if (internalPhdParticipant.getIndividualProcess().isGuider(internalPhdParticipant.getPerson())) {
                responsabilityType = internalPhdParticipant.getClass().getName() + ".guiding";
                System.out.println(responsabilityType);
            }
            if (internalPhdParticipant.getIndividualProcess().isAssistantGuider(internalPhdParticipant.getPerson())) {
                responsabilityType = internalPhdParticipant.getClass().getName() + ".assistant.guiding";
                System.out.println(responsabilityType);
            }

            coorientationNumber = internalPhdParticipant.getIndividualProcess().getAssistantGuidingsSet().size();
            date = internalPhdParticipant.getIndividualProcess().getConclusionDate();
            this.description =
                    Joiner.on(", ").join(internalPhdParticipant.getRoleOnProcess(),
                            internalPhdParticipant.getIndividualProcess().getThesisTitle(),
                            internalPhdParticipant.getIndividualProcess().getStudent().getPerson().getName(),
                            internalPhdParticipant.getIndividualProcess().getStudent().getNumber(), date.toString());
        }

        public OrientationBean(PersonFunction personFunction) {
            participationType = personFunction.getFunction().getName();
            credits =
                    MANAGEMENT_FUNCTION_COEFICIENT.multiply(BigDecimal.valueOf(personFunction.getCredits()))
                            .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            date = personFunction.getBeginDate().toLocalDate();
            this.description =
                    Joiner.on(", ").join(personFunction.getFunction().getName(), personFunction.getUnit().getName(),
                            personFunction.getBeginDate().toString(), personFunction.getEndDate().toString());

        }

        public OrientationBean(String descrition, LocalDate date) {
            this.participationType = descrition.substring(0, descrition.indexOf(","));
            this.description = descrition;
            this.date = date;
        }

        public String getParticipationType() {
            return participationType;
        }

        public void setParticipationType(String participationType) {
            this.participationType = participationType;
        }

        public String getResponsabilityType() {
            return responsabilityType;
        }

        public void setResponsabilityType(String responsabilityType) {
            this.responsabilityType = responsabilityType;
        }

        public Integer getCoorientationNumber() {
            return coorientationNumber;
        }

        public void setCoorientationNumber(Integer coorientationNumber) {
            this.coorientationNumber = coorientationNumber;
        }

        public Double getCredits() {
            return credits;
        }

        public void setCredits(Double credits) {
            this.credits = credits;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public int compareTo(OrientationBean o) {
            int compare = date.compareTo(o.date);
            if (compare == 0) {
                compare = getParticipationType().compareTo(o.getParticipationType());
            }
            return compare == 0 ? getDescription().compareTo(o.getDescription()) : compare;
        }

    }

}
