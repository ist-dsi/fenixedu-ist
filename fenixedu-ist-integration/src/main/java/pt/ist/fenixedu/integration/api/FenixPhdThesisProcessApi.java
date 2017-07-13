package pt.ist.fenixedu.integration.api;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.phd.ExternalPhdProgram;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcessNumber;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcessState;
import org.fenixedu.academic.domain.phd.PhdParticipant;
import org.fenixedu.academic.domain.phd.PhdProcessState;
import org.fenixedu.academic.domain.phd.PhdProgram;
import org.fenixedu.academic.domain.phd.PhdProgramFocusArea;
import org.fenixedu.academic.domain.phd.PhdProgramProcessDocument;
import org.fenixedu.academic.domain.phd.PhdProgramProcessState;
import org.fenixedu.academic.domain.phd.PhdStudyPlan;
import org.fenixedu.academic.domain.phd.PhdStudyPlanEntry;
import org.fenixedu.academic.domain.phd.ThesisSubjectOrder;
import org.fenixedu.academic.domain.phd.candidacy.PhdCandidacyReferee;
import org.fenixedu.academic.domain.phd.candidacy.PhdCandidacyRefereeLetter;
import org.fenixedu.academic.domain.phd.candidacy.PhdProgramCandidacyProcess;
import org.fenixedu.academic.domain.phd.conclusion.PhdConclusionProcess;
import org.fenixedu.academic.domain.phd.seminar.PublicPresentationSeminarProcess;
import org.fenixedu.academic.domain.phd.thesis.PhdThesisProcess;
import org.fenixedu.academic.domain.phd.thesis.ThesisJuryElement;
import org.fenixedu.academic.domain.phd.thesis.meeting.PhdMeeting;
import org.fenixedu.academic.domain.phd.thesis.meeting.PhdMeetingSchedulingProcess;
import org.fenixedu.academic.domain.student.PrecedentDegreeInformation;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.curriculum.ConclusionProcess;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.oauth.annotation.OAuthEndpoint;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Path("/fenix/v1/phdThesisProcesses")
public class FenixPhdThesisProcessApi {

	private static final String DATE_FORMAT = "yyyy-MM-dd";
	private static final String DATETIME_FORMAT = DATE_FORMAT + " HH:mm:ss";

    @GET
    @Produces(FenixAPIv1.JSON_UTF8)
    @OAuthEndpoint(FenixAPIv1.DEGREE_CURRICULAR_MANAGEMENT)
    public String list(@QueryParam("academicTerm") final String academicTerm, @QueryParam("department") final String department,
            @QueryParam("username") final String username) {
        if (!Strings.isNullOrEmpty(username)) {
            return forUser(username);
        }
        final ExecutionYear executionYear = getAcademicInterval(academicTerm);
        final Stream<PhdIndividualProgramProcess> stream = executionYear.getPhdIndividualProgramProcessesSet().stream()
                .filter(p -> isForDepartment(p, department));
        return list(stream);
	}

    private String forUser(@PathParam("username") final String username) {
        final User userForStudent = User.findByUsername(username);
        final Person person = userForStudent == null ? null : userForStudent.getPerson();
        final Stream<PhdIndividualProgramProcess> stream = person == null ? Stream.empty()
                : person.getPhdIndividualProgramProcessesSet().stream();
        return list(stream);
    }

    private String list(final Stream<PhdIndividualProgramProcess> phdProcesses) {
        final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
        final User user = Authenticate.getUser();
        final JsonArray array = phdProcesses
            .filter(p -> isDegreeCoordinator(p, user, currentExecutionYear))
            .map(p -> toJson(this::fillPhdIndividualProgramProcess, p))
            .collect(toJsonArray());
        return array.toString();
    }

    private ExecutionYear getAcademicInterval(final String academicTerm) {
        if (Strings.isNullOrEmpty(academicTerm)) {
            return ExecutionYear.readCurrentExecutionYear();
        }
        final ExecutionInterval interval = ExecutionInterval.getExecutionInterval(academicTerm);
        if (interval == null) {
            throw newApplicationError(Status.NOT_FOUND, "resource_not_found", "Can't find the academic term : " + academicTerm);
        }
        return interval instanceof ExecutionSemester ? ((ExecutionSemester) interval).getExecutionYear() : (ExecutionYear) interval;
    }

    private WebApplicationException newApplicationError(Status status, String error, String description) {
        JsonObject errorObject = new JsonObject();
        errorObject.addProperty("error", error);
        errorObject.addProperty("description", description);
        return new WebApplicationException(Response.status(status).entity(errorObject.toString()).build());
    }

	private boolean isDegreeCoordinator(final PhdIndividualProgramProcess p, final User user, final ExecutionYear executionYear) {
		final PhdProgram program = p.getPhdProgram();
		final Degree degree = program == null ? null : program.getDegree();
		return degree != null && degree.isCoordinator(user.getPerson(), executionYear);
	}

	private boolean isForDepartment(final PhdIndividualProgramProcess p, final String department) {
	    if (Strings.isNullOrEmpty(department)) {
	        return true;
	    }
	    final PhdProgram program = p.getPhdProgram();
	    final Degree degree = program == null ? null : program.getDegree();
	    return degree != null && degree.getDepartmentsSet().stream().anyMatch(d -> department.equals(d.getAcronym()));
	}

    public void fillPhdIndividualProgramProcess(final JsonObject result, final PhdIndividualProgramProcess pipp) {
		final ExecutionYear executionYear = pipp.getExecutionYear();
		result.addProperty("academicTerm", executionYear.getQualifiedName());

		final PhdIndividualProgramProcessNumber processNumber = pipp.getPhdIndividualProcessNumber();
		result.addProperty("processNumber", processNumber.getFullProcessNumber());

		final Person person = pipp.getPerson();
		result.add("student", toJson(this::fillPerson, person));

		final PhdProgram phdProgram = pipp.getPhdProgram();
		result.add("phdProgram", toJson(this::fillPhdProgram, phdProgram));

		final PhdIndividualProgramProcessState activeState = pipp.getActiveState();
		result.addProperty("currentState", activeState.getLocalizedName());

		final PhdProgramFocusArea focusArea = pipp.getPhdProgramFocusArea();
		if (focusArea != null) {
			result.add("focusArea", toJson(this::fillMLS, focusArea.getName()));
		}

		result.addProperty("otherCollaborationType", pipp.getOtherCollaborationType());

		result.add("qualificationExams", toJson(this::qualificationExams, pipp));

		result.add("thesisTitle", toJson(this::fillThesisTitle, pipp));

		if (pipp.getWhenFormalizedRegistration() != null) {
			result.addProperty("whenFormalizedRegistration", toJson(pipp.getWhenFormalizedRegistration()));
		}
		if (pipp.getWhenStartedStudies() != null) {
			result.addProperty("whenStartedStudies", toJson(pipp.getWhenStartedStudies()));
		}

		result.add("states", toJsonArray(this::fillPhdProgramProcessState, pipp.getStatesSet()));
		
		result.addProperty("collaborationType", pipp.getCollaborationType().getLocalizedName());


		result.add("candidacyProcess", toJson(this::fillPhdProgramCandidacyProcess, pipp.getCandidacyProcess()));		

		if (pipp.getDestiny() != null) {
			result.add("destiny", toJson(this::fillPhdIndividualProgramProcess, pipp.getDestiny()));
		}

		if (pipp.getThesisProcess() != null) {
			result.add("thesisProcess", toJson(this::fillPhdThesisProcess, pipp.getThesisProcess()));				
		}

		result.add("participantsSet", toJsonArray(this::fillPhdParticipant, pipp.getParticipantsSet()));

		if (pipp.getSeminarProcess() != null) {
			result.add("seminarProcess", toJson(this::fillPublicPresentationSeminarProcess, pipp.getSeminarProcess()));
		}

		result.add("documents", toJsonArray(this::fillPhdProgramProcessDocument, pipp.getDocumentsSet()));

		if (pipp.getStudyPlan() != null) {
			result.add("studyPlan", toJson(this::fillPhdStudyPlan, pipp.getStudyPlan()));
		}

		if (pipp.getRegistration() != null) {
			result.add("registration", toJson(this::fillRegistration, pipp.getRegistration()));
		}

		if (pipp.getExternalPhdProgram() != null) {
			result.add("externalPhdProgram", toJson(this::fillExternalPhdProgram, pipp.getExternalPhdProgram()));
		}

		result.add("guidings", toJsonArray(this::fillPhdParticipant, pipp.getGuidingsSet()));

		result.add("assistantGuidings", toJsonArray(this::fillPhdParticipant, pipp.getAssistantGuidingsSet()));

		result.add("phdConclusionProcesses", toJsonArray(this::fillPhdConclusionProcess, pipp.getPhdConclusionProcessesSet()));

		result.add("thesisSubjects", toJsonArray(this::fillThesisSubjectOrder, pipp.getThesisSubjectOrdersSet()));

		result.add("precedentDegreeInformations", toJsonArray(this::fillPrecedentDegreeInformation, pipp.getPrecedentDegreeInformationsSet()));
	}

	private void fillPrecedentDegreeInformation(final JsonObject result, final PrecedentDegreeInformation p) {
		result.addProperty("candidacyInternal", p.getCandidacyInternal());
		result.addProperty("conclusionGrade", p.getConclusionGrade());
		if (p.getInstitution() != null || p.getPrecedentInstitution() != null) {
			result.addProperty("degreeAndInstitutionName", p.getDegreeAndInstitutionName());
		}
		result.addProperty("degreeDesignation", p.getDegreeDesignation());
		result.addProperty("institutionName", p.getInstitutionName());
		result.addProperty("otherPrecedentSchoolLevel", p.getOtherPrecedentSchoolLevel());
		result.addProperty("otherSchoolLevel", p.getOtherSchoolLevel());
		result.addProperty("precedentDegreeDesignation", p.getPrecedentDegreeDesignation());
		result.addProperty("approvedEcts", p.getApprovedEcts());
		result.addProperty("conclusionYear", p.getConclusionYear());
		result.addProperty("enroledEcts", p.getEnroledEcts());
		result.addProperty("executionYear", p.getExecutionYear().getQualifiedName());
		//result.addProperty("", p.getIndividualCandidacy());
		result.addProperty("gradeSum", p.getGradeSum());
		result.addProperty("numberOfApprovedCurricularCourses", p.getNumberOfApprovedCurricularCourses());
		result.addProperty("numberOfEnroledCurricularCourses", p.getNumberOfEnroledCurricularCourses());
		result.addProperty("numberOfEnrolmentsInPreviousDegrees", p.getNumberOfEnrolmentsInPreviousDegrees());
		if (p.getConclusionDate() != null) {
			result.addProperty("conclusionDate", toJson(p.getConclusionDate()));
		}
		if (p.getCountry() != null) {
			result.add("country", toJson(this::fillCountry, p.getCountry()));
		}
		if (p.getCountryHighSchool() != null) {
			result.add("countryHighSchool", toJson(this::fillCountry, p.getCountryHighSchool()));
		}
		if (p.getCycleType() != null) {
			result.addProperty("cycleType", p.getCycleType().getDescription());
		}
		if (p.getInstitution() != null || p.getPrecedentInstitution() != null) {
			result.addProperty("degreeAndInstitutionName", p.getDegreeAndInstitutionName());
		}
		result.addProperty("degreeDesignation", p.getDegreeDesignation());
		if (p.getMobilityProgramDuration() != null) {
			result.addProperty("mobilityProgramDuration", p.getMobilityProgramDuration().name());
		}
		if (p.getPrecedentCountry() != null) {
			result.add("precedentCountry", toJson(this::fillCountry, p.getPrecedentCountry()));
		}
		if (p.getPrecedentInstitution() != null) {
			result.add("precedentInstitution", toJson(this::fillUnit, p.getPrecedentInstitution()));
		}
		if (p.getPrecedentSchoolLevel() != null) {
			result.addProperty("precedentSchoolLevel", p.getPrecedentSchoolLevel().getLocalizedName());
		}
		if (p.getRegistration() != null) {
			result.add("registration", toJson(this::fillRegistration, p.getRegistration()));
		}
		if (p.getSchoolLevel() != null) {
			result.addProperty("schoolLevel", p.getSchoolLevel().getLocalizedName());
		}
		if (p.getSourceInstitution() != null) {
			result.add("sourceInstitution", toJson(this::fillUnit, p.getSourceInstitution()));
		}
	}

	private void fillUnit(final JsonObject result, final Unit unit) {
		result.addProperty("acronym", unit.getAcronym());
		result.add("name", toJson(this::fillMLS, unit.getNameI18n()));
	}

	private void fillCountry(final JsonObject result, final Country country) {
		result.addProperty("twoLetterCode", country.getCode());
		result.addProperty("threeLetterCode", country.getThreeLetterCode());
		result.addProperty("name", country.getName());
	}

	private void fillThesisSubjectOrder(final JsonObject result, final ThesisSubjectOrder p) {
		result.addProperty("order", p.getSubjectOrder());
		result.add("subject", toJson(this::fillMLS, p.getThesisSubject().getName()));
		if (p.getThesisSubject().getDescription() != null) {
			result.add("description", toJson(this::fillMLS, p.getThesisSubject().getDescription()));
		}
		result.add("teacher", toJson(this::fillPerson, p.getThesisSubject().getTeacher().getPerson()));
		result.addProperty("externalAdvisorName", p.getThesisSubject().getExternalAdvisorName());
		result.add("focusAres", toJson(this::fillMLS, p.getThesisSubject().getPhdProgramFocusArea().getName()));
	}

	private void fillPhdConclusionProcess(final JsonObject result, final PhdConclusionProcess p) {
		result.addProperty("studyPlanEctsCredits", p.getStudyPlanEctsCredits());
		result.addProperty("thesisEctsCredits", p.getThesisEctsCredits());
		result.addProperty("totalEctsCredits", p.getTotalEctsCredits());
		result.addProperty("version", p.getVersion());
		result.addProperty("conclusionDate", toJson(p.getConclusionDate()));
		result.addProperty("finalGrade", p.getFinalGrade().getLocalizedName());
	}

	private void fillExternalPhdProgram(final JsonObject result, final ExternalPhdProgram program) {
		result.addProperty("acronym", program.getAcronym());
		result.add("name", toJson(this::fillMLS, program.getName()));
		result.addProperty("collaborationType", program.getForCollaborationType().getLocalizedName());
	}

	private void fillRegistration(final JsonObject result, final Registration registration) {
		result.addProperty("degreeCurricularPlan", registration.getDegreeCurricularPlanName());
		result.add("states", toJsonArray(this::fillRegistrationState, registration.getRegistrationStatesSet()));
		result.add("conclusionProcesses", toJsonProgramConclusions(registration));
		result.add("curriculum", toJsonArray(this::fillStudentCurricularPlan, registration.getStudentCurricularPlansSet()));
	}

	private void fillStudentCurricularPlan(final JsonObject result, final StudentCurricularPlan scp) {
		result.addProperty("name", scp.getName());
		result.addProperty("approvedEctsCredits", scp.getApprovedEctsCredits());
		result.add("enrolments", toJsonArray(this::fillEnrolment, scp.getAllEnrollments()));
	}

	private void fillEnrolment(final JsonObject result, final Enrolment e) {
		result.add("degreeModule", toJson(this::fillMLS, e.getDegreeModule().getNameI18N(e.getExecutionPeriod())));
		result.addProperty("code", e.getCode());
		result.addProperty("description", e.getDescription());
		result.addProperty("state", e.getEnrollmentState().getDescription());
		result.addProperty("type", e.getEnrolmentTypeName());
		result.addProperty("condition", e.getEnrolmentCondition().getDescription());
		result.addProperty("ects", e.getEctsCreditsForCurriculum());
		result.addProperty("weight", e.getWeigthForCurriculum());
		result.addProperty("executionPeriod", e.getExecutionPeriod().getQualifiedName());
		result.addProperty("isApproved", e.isApproved());
		if (e.getGradeScale() != null) {
			result.addProperty("gradeScale", e.getGradeScale().getDescription());
		}
		if (e.getGrade() != null) {
			result.addProperty("gradeValue", e.getGradeValue());
		}
		result.addProperty("isFirstTime", e.getIsFirstTime());
	}

	private JsonArray toJsonProgramConclusions(final Registration registration) {
		final Stream<ProgramConclusion> conclusions = ProgramConclusion.conclusionsFor(registration);
		final Stream<ConclusionProcess> stream = conclusions.map(pc -> pc.groupFor(registration).orElse(null))
                .filter(cg -> cg != null).map(cg -> cg.getConclusionProcess()).filter(cp -> cp != null);
		return toJsonArray(this::fillConclusionProcess, stream);
	}

	private void fillConclusionProcess(final JsonObject result, final ConclusionProcess cp) {
		result.addProperty("name", cp.getName().getContent());
		result.addProperty("credits", cp.getCredits());
		result.addProperty("finalGrade", cp.getFinalGrade().getValue());
		if (cp.getDescriptiveGrade() != null) {
			result.addProperty("descriptiveGrade", cp.getDescriptiveGrade().getValue());
		}
		result.addProperty("rawGrade", cp.getRawGrade().getValue());
		result.addProperty("conclusionDate", toJson(cp.getConclusionDate()));
		result.addProperty("conclusionYear", cp.getConclusionYear().getQualifiedName());
		result.addProperty("notes", cp.getNotes());
	}

	private void fillRegistrationState(final JsonObject result, final RegistrationState s) {
		result.addProperty("courseDescription", s.getExecutionYear().getQualifiedName());
		result.addProperty("courseDescription", s.getStateType().getDescription());
		result.addProperty("courseDescription", toJson(s.getStateDate()));
		if (s.getEndDate() != null) {
			result.addProperty("courseDescription", toJson(s.getEndDate()));
		}
		result.addProperty("courseDescription", s.getRemarks());
	}

	private void fillPhdStudyPlan(final JsonObject result, final PhdStudyPlan plan) {
		result.add("degree", toJson(this::fillDegree, plan.getDegree()));
		result.addProperty("description", plan.getDescription());
		result.add("entries", toJsonArray(this::fillPhdStudyPlanEntry, plan.getEntriesSet()));
		result.addProperty("exempted", plan.getExempted());
		result.add("extraCurricularEntries", toJsonArray(this::fillPhdStudyPlanEntry, plan.getExtraCurricularEntries()));
		result.add("normalEntries", toJsonArray(this::fillPhdStudyPlanEntry, plan.getNormalEntries()));
		result.add("propaedeuticEntries", toJsonArray(this::fillPhdStudyPlanEntry, plan.getPropaedeuticEntries()));
	}

	private void fillPhdStudyPlanEntry(final JsonObject result, final PhdStudyPlanEntry s) {
		result.addProperty("courseDescription", s.getCourseDescription());
		result.addProperty("type", s.getType().name());
	}

	private void fillDegree(final JsonObject result, final Degree degree) {
		result.add("name", toJson(this::fillMLS, degree.getNameI18N()));
		result.addProperty("type", degree.getDegreeType().getName().getContent());
	}

	private void fillPublicPresentationSeminarProcess(final JsonObject result, final PublicPresentationSeminarProcess p) {
		result.addProperty("activeStateRemarks", p.getActiveStateRemarks());
		result.addProperty("displayName", p.getDisplayName());
		result.addProperty("activeState", p.getActiveState().getLocalizedName());
		if (p.getComissionDocument() != null) {
			result.add("comissionDocument", toJson(this::fillPhdProgramProcessDocument, p.getComissionDocument()));
		}
		result.add("documents", toJsonArray(this::fillPhdProgramProcessDocument, p.getDocumentsSet()));
		if (p.getPresentationDate() != null) {
			result.addProperty("presentationDate", toJson(p.getPresentationDate()));
		}
		if (p.getPresentationRequestDate() != null) {
			result.addProperty("presentationRequestDate", toJson(p.getPresentationRequestDate()));
		}
		if (p.getReportDocument() != null) {
			result.add("reportDocument", toJson(this::fillPhdProgramProcessDocument, p.getReportDocument()));
		}
		result.add("states", toJsonArray(this::fillPhdProcessState, p.getStatesSet()));
	}

	private void fillPhdThesisProcess(final JsonObject result, final PhdThesisProcess thesisProcess) {
		result.addProperty("activeStateRemarks", thesisProcess.getActiveStateRemarks());
		result.addProperty("discussionPlace", thesisProcess.getDiscussionPlace());
		result.addProperty("displayName", thesisProcess.getDisplayName());
		result.addProperty("meetingPlace", thesisProcess.getMeetingPlace());
		result.addProperty("processNumber", thesisProcess.getProcessNumber());
		result.addProperty("ratificationEntityCustomMessage", thesisProcess.getRatificationEntityCustomMessage());
		result.addProperty("activeState", thesisProcess.getActiveState().getLocalizedName());
		if (thesisProcess.getConclusionDate() != null) {
			result.addProperty("conclusionDate", toJson(thesisProcess.getConclusionDate()));
		}
		if (thesisProcess.getDiscussionDate() != null) {
			result.addProperty("discussionDate", toJson(thesisProcess.getDiscussionDate()));
		}
		result.add("documents", toJsonArray(this::fillPhdProgramProcessDocument, thesisProcess.getDocumentsSet()));
		if (thesisProcess.getFinalGrade() != null) {
			result.addProperty("finalGrade", thesisProcess.getFinalGrade().getLocalizedName());
		}
		if (thesisProcess.getFinalThesisDocument() != null) {
			result.add("finalThesisDocument", toJson(this::fillPhdProgramProcessDocument, thesisProcess.getFinalThesisDocument()));
		}
		if (thesisProcess.getJuryElementsDocument() != null) {
			result.add("juryElementsDocument", toJson(this::fillPhdProgramProcessDocument, thesisProcess.getJuryElementsDocument()));
		}
		if (thesisProcess.getJuryMeetingMinutesDocument() != null) {
			result.add("juryMeetingMinutesDocument", toJson(this::fillPhdProgramProcessDocument, thesisProcess.getJuryMeetingMinutesDocument()));
		}
		if (thesisProcess.getJuryPresidentDocument() != null) {
			result.add("juryPresidentDocument", toJson(this::fillPhdProgramProcessDocument, thesisProcess.getJuryPresidentDocument()));
		}
		if (thesisProcess.getMeetingDate() != null) {
			result.addProperty("meetingDate", toJson(thesisProcess.getMeetingDate()));
		}
		if (thesisProcess.getMeetingProcess() != null) {
			result.add("meetingProcess", toJson(this::fillPhdMeetingSchedulingProcess, thesisProcess.getMeetingProcess()));
		}
		result.addProperty("phdJuryElementsRatificationEntity", thesisProcess.getPhdJuryElementsRatificationEntity().getLocalizedName());
		if (thesisProcess.getPhdMeetingSchedulingActiveState() != null) {
			result.addProperty("getPhdMeetingSchedulingActiveState", thesisProcess.getPhdMeetingSchedulingActiveState().getLocalizedName());
		}
		if (thesisProcess.getPresidentJuryElement() != null) {
			result.add("getPresidentJuryElement", toJson(this::fillThesisJuryElement, thesisProcess.getPresidentJuryElement()));
		}
		result.add("getPresidentTitle", toJson(this::fillMLS, thesisProcess.getPresidentTitle()));
		result.add("getProvisionalThesisDocument", toJson(this::fillPhdProgramProcessDocument, thesisProcess.getProvisionalThesisDocument()));
		result.add("getReportThesisJuryElementDocuments", toJsonArray(this::fillPhdProgramProcessDocument, thesisProcess.getReportThesisJuryElementDocuments()));
		result.add("getReportThesisJuryElements", toJsonArray(this::fillThesisJuryElement, thesisProcess.getReportThesisJuryElements()));
		result.add("getStatesSet", toJsonArray(this::fillPhdProcessState, thesisProcess.getStatesSet()));
		result.add("getThesisDocumentsToFeedback", toJsonArray(this::fillPhdProgramProcessDocument, thesisProcess.getThesisDocumentsToFeedback()));
		result.add("getThesisJuryElementsSet", toJsonArray(this::fillThesisJuryElement, thesisProcess.getThesisJuryElementsSet()));
		result.add("thesisRequirementDocument", toJson(this::fillPhdProgramProcessDocument, thesisProcess.getThesisRequirementDocument()));
		if (thesisProcess.getWhenFinalThesisRatified() != null) {
			result.addProperty("whenFinalThesisRatified", toJson(thesisProcess.getWhenFinalThesisRatified()));
		}
		if (thesisProcess.getWhenJuryDesignated() != null) {
			result.addProperty("whenJuryDesignated", toJson(thesisProcess.getWhenJuryDesignated()));
		}
		if (thesisProcess.getWhenJuryRequired() != null) {
			result.addProperty("whenJuryRequired", toJson(thesisProcess.getWhenJuryRequired()));
		}
		if (thesisProcess.getWhenJuryValidated() != null) {
			result.addProperty("whenJuryValidated", toJson(thesisProcess.getWhenJuryValidated()));
		}
		if (thesisProcess.getWhenReceivedJury() != null) {
			result.addProperty("whenReceivedJury", toJson(thesisProcess.getWhenReceivedJury()));
		}
		if (thesisProcess.getWhenRequestedJuryReviews() != null) {
			result.addProperty("whenRequestedJuryReviews", toJson(thesisProcess.getWhenRequestedJuryReviews()));
		}
		if (thesisProcess.getWhenRequestJury() != null) {
			result.addProperty("whenRequestJury", toJson(thesisProcess.getWhenRequestJury()));
		}
		if (thesisProcess.getWhenThesisDiscussionRequired() != null) {
			result.addProperty("whenThesisDiscussionRequired", toJson(thesisProcess.getWhenThesisDiscussionRequired()));
		}
	}

	private void fillThesisJuryElement(final JsonObject result, final ThesisJuryElement element) {
		result.addProperty("getName", element.getName());
		result.addProperty("getNameWithTitle", element.getNameWithTitle());
		if (element.getProcess() != null) {
			result.addProperty("getNameWithTitleAndRoleOnProcess", element.getNameWithTitleAndRoleOnProcess());
		}
		result.addProperty("getTitle", element.getTitle());
		result.addProperty("getCategory", element.getCategory());
		result.addProperty("getInstitution", element.getInstitution());
		result.addProperty("getWorkLocation", element.getWorkLocation());
		result.addProperty("getEmail", element.getEmail());
		result.addProperty("getQualification", element.getQualification());
		result.addProperty("getExpert", element.getExpert());
		result.addProperty("getAddress", element.getAddress());
		result.addProperty("getPhone", element.getPhone());
		result.addProperty("getReporter", element.getReporter());
		result.addProperty("getElementOrder", element.getElementOrder());
		result.add("getFeedbackDocumentsSet", toJsonArray(this::fillPhdProgramProcessDocument, element.getFeedbackDocumentsSet()));
		result.add("getParticipant", toJson(this::fillPhdParticipant, element.getParticipant()));
	}

	private void fillPhdParticipant(final JsonObject result, final PhdParticipant participant) {
		if (participant.getAcceptanceLetter() != null) {
			result.add("acceptanceLetter", toJson(this::fillPhdProgramProcessDocument, participant.getAcceptanceLetter()));
		}
		result.addProperty("address", participant.getAddress());
		result.addProperty("category", participant.getCategory());
		result.addProperty("institution", participant.getInstitution());
		result.addProperty("name", participant.getName());
		result.addProperty("nameWithTitle", participant.getNameWithTitle());
		result.addProperty("phone", participant.getPhone());
		result.addProperty("qualification", safe(participant));
		result.addProperty("title", participant.getTitle());
		result.addProperty("workLocation", participant.getWorkLocation());
	}

	private String safe(final PhdParticipant participant) {
		try {
			return participant.getQualification();
		} catch (NullPointerException npe) {
			return "?";
		}
	}

	private void fillPhdMeetingSchedulingProcess(final JsonObject result, final PhdMeetingSchedulingProcess meetingProcess) {
		result.addProperty("activeStateRemarks", meetingProcess.getActiveStateRemarks());
		result.addProperty("displayName", meetingProcess.getDisplayName());
		result.addProperty("activeState", meetingProcess.getActiveState().getLocalizedName());
		result.add("documents", toJsonArray(this::fillPhdProgramProcessDocument, meetingProcess.getDocumentsSet()));
		result.add("meetings", toJsonArray(this::fillPhdMeeting, meetingProcess.getMeetingsSet()));
		result.add("states", toJsonArray(this::fillPhdProcessState, meetingProcess.getStatesSet()));
	}

	private void fillPhdMeeting(final JsonObject result, final PhdMeeting m) {
		result.addProperty("meetingPlace", m.getMeetingPlace());
		result.addProperty("versionOfLatestDocumentVersion", m.getVersionOfLatestDocumentVersion());
		result.add("documents", toJsonArray(this::fillPhdProgramProcessDocument, m.getDocumentsSet()));
		result.addProperty("meetingDate", toJson(m.getMeetingDate()));
	}

	private void fillPhdProgramCandidacyProcess(final JsonObject result, final PhdProgramCandidacyProcess candidacyProcess) {
		result.addProperty("processNumber", candidacyProcess.getProcessNumber());
		result.addProperty("candidacyDate", toJson(candidacyProcess.getCandidacyDate()));
		result.addProperty("displayName", candidacyProcess.getDisplayName());
		result.addProperty("validatedByCandidate", candidacyProcess.getValidatedByCandidate());
		if (candidacyProcess.getWhenRatified() != null) {
			result.addProperty("whenRatified", toJson(candidacyProcess.getWhenRatified()));
		}
		if (candidacyProcess.getWhenStartedStudies() != null) {
			result.addProperty("whenStartedStudies", toJson(candidacyProcess.getWhenStartedStudies()));
		}
		result.add("states", toJsonArray(this::fillPhdProcessState, candidacyProcess.getStatesSet()));
		if (candidacyProcess.getPhdProgramLastActiveDegreeCurricularPlan() != null) {
			result.addProperty("lastActiveDegreeCurricularPlan", candidacyProcess.getPhdProgramLastActiveDegreeCurricularPlan().getName());
		}
		result.add("documents", toJsonArray(this::fillPhdProgramProcessDocument, candidacyProcess.getDocumentsSet()));
		result.add("studyPlanRelevantDocuments", toJsonArray(this::fillPhdProgramProcessDocument, candidacyProcess.getStudyPlanRelevantDocuments()));

		//result.add("feedbackRequest", toJson(candidacyProcess.getFeedbackRequest()));
		result.add("referees", toJsonArray(this::fillPhdCandidacyReferee, candidacyProcess.getCandidacyRefereesSet()));
	}

	private void fillPhdCandidacyReferee(final JsonObject result, PhdCandidacyReferee r) {
		result.addProperty("name", r.getName());
		result.addProperty("institution", r.getInstitution());
		result.addProperty("email", r.getEmail());
		result.addProperty("value", r.getValue());
		if (r.getLetter() != null) {
			result.add("letter", toJson(this::fillPhdCandidacyRefereeLetter, r.getLetter()));
		}
	}

	private void fillPhdCandidacyRefereeLetter(final JsonObject result, final PhdCandidacyRefereeLetter letter) {
		result.addProperty("comments", letter.getComments());
		result.addProperty("comparisonGroup", letter.getComparisonGroup());
		result.addProperty("capacity", letter.getCapacity());
		result.addProperty("academicPerformance", letter.getAcademicPerformance().getLocalizedName());
		result.addProperty("howLongKnownApplicant", letter.getHowLongKnownApplicant());
		result.addProperty("rankInClass", letter.getRankInClass());
		result.addProperty("socialAndCommunicationSkills", letter.getSocialAndCommunicationSkills().getLocalizedName());
		result.addProperty("potencialToExcelPhd", letter.getPotencialToExcelPhd().getLocalizedName());

		if (letter.getFile() != null) {
			result.add("file", toJson(this::fillPhdProgramProcessDocument, letter.getFile()));
		}

		result.addProperty("refereeName", letter.getRefereeName());
		result.addProperty("refereeInstitution", letter.getRefereeInstitution());
		result.addProperty("refereePosition", letter.getRefereePosition());
		result.addProperty("rrefereeEmail", letter.getRefereeEmail());

		result.add("refereeAddress", toJson(this::fillAddress, letter));
	}

	private void fillAddress(final JsonObject result, final PhdCandidacyRefereeLetter letter) {
	    fillAddress(result, letter.getRefereeAddress(), letter.getRefereeCity(), letter.getRefereeZipCode(), letter.getRefereeCountry());
	}
	
	private void fillAddress(final JsonObject result, final String address, final String city, final String zipCode, final Country country) {
		result.addProperty("address", address);
		result.addProperty("city", city);
		result.addProperty("zipCode", zipCode);
		result.addProperty("country", country.getName());
	}

	private void fillPhdProcessState(final JsonObject result, final PhdProcessState s) {
		result.addProperty("type", s.getType().getLocalizedName());
		result.addProperty("date", toJson(s.getStateDate()));
		result.addProperty("remarks", s.getRemarks());
	}

	private void fillPhdProgramProcessDocument(final JsonObject result, final PhdProgramProcessDocument document) {
		result.addProperty("documentType", document.getDocumentType().getLocalizedName());
		result.addProperty("displayName", document.getDisplayName());
		result.addProperty("documentAccepted", document.getDocumentAccepted());
		result.addProperty("downloadUrl", document.getDownloadUrl());
		result.addProperty("filename", document.getFilename());
		result.addProperty("remarks", document.getRemarks());
		result.addProperty("documentVersion", document.getDocumentVersion());
		result.addProperty("contentType", document.getContentType());
	}

	private void fillPhdProgramProcessState(final JsonObject result, final PhdProgramProcessState state) {
		result.addProperty("date", toJson(state.getStateDate()));
		result.addProperty("type", state.getType().getLocalizedName());
	}

	private String toJson(final LocalDate localDate) {
		return localDate.toString(DATE_FORMAT);
	}

	private String toJson(final DateTime dateTime) {
		return dateTime.toString(DATETIME_FORMAT);
	}

	private void fillThesisTitle(final JsonObject result, final PhdIndividualProgramProcess pipp) {
		result.addProperty("pt", pipp.getThesisTitle());
		result.addProperty("en", pipp.getThesisTitleEn());
	}

    private void qualificationExams(final JsonObject result, final PhdIndividualProgramProcess pipp) {
        result.addProperty("required", pipp.getQualificationExamsRequired());
        result.addProperty("performed", pipp.getQualificationExamsPerformed());
    }

	private void fillPhdProgram(final JsonObject result, final PhdProgram phdProgram) {
		result.addProperty("acronym", phdProgram.getAcronym());
		result.add("name", toJson(this::fillMLS, phdProgram.getName()));
		result.add("degree", toJson(this::fillDegree, phdProgram.getDegree()));
	}
		

	private void fillPerson(final JsonObject result, final Person person) {
		result.addProperty("username", person.getUsername());
		result.addProperty("name", person.getName());
		if (person.getUser() != null) {
			result.addProperty("avatarUrl", person.getUser().getProfile().getAvatarUrl());
		}
	}

	@SuppressWarnings("deprecation")
	private void fillMLS(final JsonObject result, final LocalizedString name) {
	    name.forEach((l, c) -> result.addProperty(l.getLanguage(), c));
	}

	private <T> JsonArray toJsonArray(final BiConsumer<JsonObject, T> filler, final Iterable<T> origins) {
	    return toJsonArray(filler, origins.iterator());
	}

   private <T> JsonArray toJsonArray(final BiConsumer<JsonObject, T> filler, final Stream<T> origins) {
       return toJsonArray(filler, origins.iterator());
   }

   private <T> JsonArray toJsonArray(final BiConsumer<JsonObject, T> filler, final Iterator<T> origins) {
       final JsonArray result = new JsonArray();
       origins.forEachRemaining(s -> result.add(toJson(filler, s)));
       return result;
   }

	private <T> JsonObject toJson(final BiConsumer<JsonObject, T> filler, final T origin) {
        final JsonObject result = new JsonObject();
        filler.accept(result, origin);
        return result;
    }

	private <T extends JsonElement> Collector<T, JsonArray, JsonArray> toJsonArray() {
        return Collector.of(JsonArray::new, (array, element) -> array.add(element), (one, other) -> {
            one.addAll(other);
            return one;
        }, Characteristics.IDENTITY_FINISH);
    }
}
