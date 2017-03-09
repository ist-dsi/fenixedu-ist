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
package pt.ist.fenixedu.integration.api;

import static org.fenixedu.academic.dto.SummariesManagementBean.SummaryType.NORMAL_SUMMARY;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.fenixedu.academic.domain.AdHocEvaluation;
import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.Coordinator;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeInfo;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.Evaluation;
import org.fenixedu.academic.domain.Exam;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Grouping;
import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.academic.domain.LessonInstance;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Photograph;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.Project;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.Summary;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.WrittenEvaluation;
import org.fenixedu.academic.domain.WrittenEvaluationEnrolment;
import org.fenixedu.academic.domain.accessControl.ActiveStudentsGroup;
import org.fenixedu.academic.domain.accessControl.ActiveTeachersGroup;
import org.fenixedu.academic.domain.accessControl.AllAlumniGroup;
import org.fenixedu.academic.domain.accounting.Entry;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.paymentCodes.AccountingEventPaymentCode;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.WebAddress;
import org.fenixedu.academic.domain.degreeStructure.BibliographicReferences.BibliographicReference;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.space.SpaceUtils;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.academic.domain.util.icalendar.CalendarFactory;
import org.fenixedu.academic.domain.util.icalendar.ClassEventBean;
import org.fenixedu.academic.domain.util.icalendar.EvaluationEventBean;
import org.fenixedu.academic.domain.util.icalendar.EventBean;
import org.fenixedu.academic.dto.InfoExam;
import org.fenixedu.academic.dto.InfoExecutionCourse;
import org.fenixedu.academic.dto.InfoLesson;
import org.fenixedu.academic.dto.InfoLessonInstance;
import org.fenixedu.academic.dto.InfoOccupation;
import org.fenixedu.academic.dto.InfoShowOccupation;
import org.fenixedu.academic.dto.InfoSiteRoomTimeTable;
import org.fenixedu.academic.dto.InfoWrittenEvaluation;
import org.fenixedu.academic.dto.InfoWrittenTest;
import org.fenixedu.academic.dto.SummariesManagementBean;
import org.fenixedu.academic.service.factory.RoomSiteComponentBuilder;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.service.services.exceptions.InvalidArgumentsServiceException;
import org.fenixedu.academic.service.services.student.EnrolStudentInWrittenEvaluation;
import org.fenixedu.academic.service.services.student.UnEnrollStudentInWrittenEvaluation;
import org.fenixedu.academic.service.services.teacher.CreateSummary;
import org.fenixedu.academic.service.services.teacher.DeleteSummary;
import org.fenixedu.academic.ui.struts.action.ICalendarSyncPoint;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.ContentType;
import org.fenixedu.academic.util.EvaluationType;
import org.fenixedu.academic.util.HourMinuteSecond;
import org.fenixedu.academic.util.MultiLanguageString;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.oauth.annotation.OAuthEndpoint;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.stream.StreamUtils;
import org.fenixedu.spaces.domain.BlueprintFile;
import org.fenixedu.spaces.domain.Space;
import org.fenixedu.spaces.services.SpaceBlueprintsDWGProcessor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Partial;
import org.joda.time.TimeOfDay;
import org.joda.time.YearMonthDay;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.fortuna.ical4j.model.Calendar;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;
import pt.ist.fenixedu.integration.FenixEduIstIntegrationConfiguration;
import pt.ist.fenixedu.integration.api.beans.FenixCalendar;
import pt.ist.fenixedu.integration.api.beans.FenixCalendar.FenixCalendarEvent;
import pt.ist.fenixedu.integration.api.beans.FenixCalendar.FenixClassEvent;
import pt.ist.fenixedu.integration.api.beans.FenixCalendar.FenixEvaluationEvent;
import pt.ist.fenixedu.integration.api.beans.FenixCourse;
import pt.ist.fenixedu.integration.api.beans.FenixCurricularGroup;
import pt.ist.fenixedu.integration.api.beans.FenixCurriculum;
import pt.ist.fenixedu.integration.api.beans.FenixPayment;
import pt.ist.fenixedu.integration.api.beans.FenixPayment.PaymentEvent;
import pt.ist.fenixedu.integration.api.beans.FenixPayment.PendingEvent;
import pt.ist.fenixedu.integration.api.beans.FenixPerson;
import pt.ist.fenixedu.integration.api.beans.FenixPerson.FenixPhoto;
import pt.ist.fenixedu.integration.api.beans.FenixPerson.FenixRole;
import pt.ist.fenixedu.integration.api.beans.FenixPersonCourses;
import pt.ist.fenixedu.integration.api.beans.FenixPersonCourses.FenixEnrolment;
import pt.ist.fenixedu.integration.api.beans.publico.FenixAbout;
import pt.ist.fenixedu.integration.api.beans.publico.FenixCourseEvaluation;
import pt.ist.fenixedu.integration.api.beans.publico.FenixCourseExtended;
import pt.ist.fenixedu.integration.api.beans.publico.FenixCourseExtended.FenixCompetence;
import pt.ist.fenixedu.integration.api.beans.publico.FenixCourseExtended.FenixCompetence.BiblioRef;
import pt.ist.fenixedu.integration.api.beans.publico.FenixCourseGroup;
import pt.ist.fenixedu.integration.api.beans.publico.FenixCourseStudents;
import pt.ist.fenixedu.integration.api.beans.publico.FenixDegree;
import pt.ist.fenixedu.integration.api.beans.publico.FenixDegreeExtended;
import pt.ist.fenixedu.integration.api.beans.publico.FenixDegreeExtended.FenixDegreeInfo;
import pt.ist.fenixedu.integration.api.beans.publico.FenixDegreeExtended.FenixTeacher;
import pt.ist.fenixedu.integration.api.beans.publico.FenixDomainModel;
import pt.ist.fenixedu.integration.api.beans.publico.FenixExecutionCourse;
import pt.ist.fenixedu.integration.api.beans.publico.FenixPeriod;
import pt.ist.fenixedu.integration.api.beans.publico.FenixRoomEvent;
import pt.ist.fenixedu.integration.api.beans.publico.FenixSchedule;
import pt.ist.fenixedu.integration.api.beans.publico.FenixSpace;
import pt.ist.fenixedu.integration.api.infra.FenixAPIFromExternalServer;
import pt.ist.fenixedu.integration.dto.PersonInformationBean;
import pt.ist.fenixedu.integration.service.services.externalServices.CreatePreEnrolment;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.FenixFramework;

@Path("/fenix/v1")
public class FenixAPIv1 {

    private static final Logger logger = LoggerFactory.getLogger(FenixAPIv1.class);

    public final static String PERSONAL_SCOPE = "INFO";
    public final static String SCHEDULE_SCOPE = "SCHEDULE";
    public final static String EVALUATIONS_SCOPE = "EVALUATIONS";
    public final static String CURRICULAR_SCOPE = "CURRICULAR";
    public final static String PAYMENTS_SCOPE = "PAYMENTS";
    public final static String DEGREE_CURRICULAR_MANAGEMENT = "DEGREE_CURRICULAR_MANAGEMENT";

    public final static String JSON_UTF8 = "application/json; charset=utf-8";

    public final static String dayPattern = "dd/MM/yyyy";
    public final static String dayHourPattern = "dd/MM/yyyy HH:mm";
    public final static String hourPattern = "HH:mm";
    public final static String dayHourSecondPattern = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter formatDayHour = DateTimeFormat.forPattern(dayHourPattern);
    public static final DateTimeFormatter formatDay = DateTimeFormat.forPattern(dayPattern);
    public static final DateTimeFormatter formatHour = DateTimeFormat.forPattern(hourPattern);
    public static final DateTimeFormatter formatDayHourSecond = DateTimeFormat.forPattern(dayHourSecondPattern);

    private static Gson gson;

    private static final String ENROL = "yes";
    private static final String UNENROL = "no";

    public final static String ROLE_TEACHER = "TEACHER";
    public final static String ROLE_STUDENT = "STUDENT";
    public final static String ROLE_EMPLOYEE = "EMPLOYEE";
    public final static String ROLE_ALUMNI = "ALUMNI";

    static {
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    private String mls(MultiLanguageString mls) {
        if (mls == null) {
            return "";
        }
        return mls.getContent();
    }

    private FenixPhoto getPhoto(final Person person) {
        FenixPhoto photo = null;
        try {
            final Photograph personalPhoto = person.getPersonalPhoto();
            if (person.isPhotoAvailableToCurrentUser()) {
                final byte[] avatar = personalPhoto.getDefaultAvatar();
                String type = ContentType.PNG.getMimeType();
                String data = Base64.getEncoder().encodeToString(avatar);
                photo = new FenixPhoto(type, data);
            }
        } catch (Exception npe) {
        }
        return photo;
    }

    private Person getPerson() {
        User user = Authenticate.getUser();
        if (user != null) {
            return user.getPerson();
        }
        return null;
    }

    /**
     * It will return name, istid, campus, email, photo and contacts
     *

     * @return only public contacts and photo are available

     */
    @GET
    @Produces(JSON_UTF8)
    @Path("person")
    @OAuthEndpoint(PERSONAL_SCOPE)
    public FenixPerson person() {

        final Person person = getPerson();
        final User user = person.getUser();
        PersonInformationBean pib = new PersonInformationBean(person, true);
        final Set<FenixRole> roles = new HashSet<FenixRole>();

        if (new ActiveTeachersGroup().isMember(user)) {
            roles.add(new FenixPerson.TeacherFenixRole(pib.getTeacherDepartment()));
        }

        if (new ActiveStudentsGroup().isMember(user)) {
            roles.add(new FenixPerson.StudentFenixRole(pib.getStudentRegistrations()));
        }

        if (new AllAlumniGroup().isMember(user)) {
            ArrayList<Registration> concludedRegistrations = new ArrayList<>();
            if (person.getStudent() != null) {
                concludedRegistrations.addAll(person.getStudent().getConcludedRegistrations());
            }
            roles.add(new FenixPerson.AlumniFenixRole(concludedRegistrations));
        }

        if (new ActiveEmployees().isMember(user)) {
            roles.add(new FenixPerson.EmployeeFenixRole());
        }

        final String name = pib.getName();
        final String gender = person.getGender().name();
        final String birthday = person.getDateOfBirthYearMonthDay().toString(formatDay);
        final String username = person.getUsername();
        final String campus = pib.getCampus();
        final String email = pib.getEmail();
        final List<String> personalEmails = pib.getPersonalEmails();
        final List<String> workEmails = pib.getWorkEmails();
        List<String> personalWebAdresses = pib.getPersonalWebAdresses();
        List<String> workWebAdresses = pib.getWorkWebAdresses();
        final FenixPhoto photo = getPhoto(person);

        return new FenixPerson(campus, roles, photo, name, gender, birthday, username, email, personalEmails, workEmails,
                personalWebAdresses, workWebAdresses);
    }

    /**
     * Person courses (enrolled if student and teaching if teacher)
     *

     * @param academicTerm
     * @return enrolled courses and teaching courses

     */
    @OAuthEndpoint(CURRICULAR_SCOPE)
    @GET
    @Produces(JSON_UTF8)
    @Path("person/courses/")
    public FenixPersonCourses personCourses(@QueryParam("academicTerm") String academicTerm) {

        final Person person = getPerson();

        Set<ExecutionSemester> semesters = getExecutionSemesters(academicTerm);

        List<FenixEnrolment> enrolments = new ArrayList<FenixEnrolment>();
        List<FenixCourse> teachingCourses = new ArrayList<FenixCourse>();
        List<FenixCourse> attendingCourses = new ArrayList<>();

        for (ExecutionSemester executionSemester : semesters) {
            fillEnrolments(person, enrolments, executionSemester);
            fillTeachingCourses(person, teachingCourses, executionSemester);
            fillAttendingCourses(person, attendingCourses, executionSemester);
        }

        return new FenixPersonCourses(enrolments, teachingCourses, attendingCourses);
    }

    public Set<ExecutionSemester> getExecutionSemesters(String academicTerm) {
        ExecutionInterval executionInterval = ExecutionInterval.getExecutionInterval(getAcademicInterval(academicTerm));

        Set<ExecutionSemester> semesters = new HashSet<ExecutionSemester>();

        if (executionInterval instanceof ExecutionYear) {
            semesters.addAll(((ExecutionYear) executionInterval).getExecutionPeriodsSet());
        } else if (executionInterval instanceof ExecutionSemester) {
            semesters.add((ExecutionSemester) executionInterval);
        }
        return semesters;
    }

    public void fillTeachingCourses(final Person person, List<FenixCourse> teachingCourses, ExecutionSemester executionSemester) {
        for (final Professorship professorship : person.getProfessorships(executionSemester)) {
            final ExecutionCourse executionCourse = professorship.getExecutionCourse();
            if (executionCourse.getExecutionPeriod().equals(executionSemester)) {
                teachingCourses.add(new FenixCourse(executionCourse));

            }
        }
    }

    public void fillEnrolments(final Person person, List<FenixEnrolment> enrolments, ExecutionSemester executionSemester) {
        final Student foundStudent = person.getStudent();

        if(foundStudent != null) {
            for (Registration registration : foundStudent.getAllRegistrations()) {
                for (Enrolment enrolment : registration.getEnrolments(executionSemester)) {
                    final ExecutionCourse executionCourse = enrolment.getExecutionCourseFor(executionSemester);
                    String grade = enrolment.getGrade().getValue();
                    double ects = enrolment.getEctsCredits();
                    enrolments.add(new FenixEnrolment(executionCourse, grade, ects));
                }
            }
        }
    }

    public void fillAttendingCourses(final Person person, List<FenixCourse> attendingCourses, ExecutionSemester executionSemester) {
        final Student foundStudent = person.getStudent();

        if(foundStudent != null) {
            foundStudent.getRegistrationsSet().stream()
                    .flatMap(r -> r.getAttendingExecutionCoursesFor(executionSemester).stream())
                    .map(FenixCourse::new)
                    .forEach(attendingCourses::add);
        }
    }

    /* CALENDARS */

    private enum EventType {
        CLASS, EVALUATION;
    }

    private FenixCalendar getFenixCalendar(String academicTerm, List<EventBean> eventBeans, EventType eventType) {

        List<FenixCalendarEvent> events = new ArrayList<FenixCalendarEvent>();
        for (EventBean eventBean : eventBeans) {

            String start = formatDayHour.print(eventBean.getBegin());
            String end = formatDayHour.print(eventBean.getEnd());

            FenixPeriod eventPeriod = new FenixPeriod(start, end);
            String title = eventBean.getOriginalTitle();

            Set<FenixSpace> rooms = new HashSet<>();
            if (eventBean.getRooms() != null) {
                for (Space room : eventBean.getRooms()) {
                    if (room != null) {
                        rooms.add(FenixSpace.getSimpleSpace(room));
                    }
                }
            }

            FenixCalendarEvent event = null;

            switch (eventType) {
            case CLASS:
                final Shift classShift = ((ClassEventBean) eventBean).getClassShift();
                event = new FenixClassEvent(eventPeriod, rooms, title, new FenixCourse(classShift.getExecutionCourse()));
                break;

            case EVALUATION:
                Set<FenixCourse> fenixCourses =
                        FluentIterable.from(((EvaluationEventBean) eventBean).getCourses())
                                .transform(new Function<ExecutionCourse, FenixCourse>() {

                                    @Override
                                    public FenixCourse apply(ExecutionCourse course) {
                                        return new FenixCourse(course);
                                    }

                                }).toSet();

                FenixSpace assignedRoom = FenixSpace.getSimpleSpace(((EvaluationEventBean) eventBean).getAssignedRoom());

                event = new FenixEvaluationEvent(eventPeriod, assignedRoom, rooms, title, fenixCourses);
                break;
            }

            events.add(event);
        }
        return new FenixCalendar(academicTerm, events);
    }

    private FenixCalendar getFenixClassCalendar(String academicTerm, List<EventBean> eventBeans) {
        return getFenixCalendar(academicTerm, eventBeans, EventType.CLASS);
    }

    private FenixCalendar getFenixEvaluationsCalendar(String academicTerm, List<EventBean> eventBeans) {
        return getFenixCalendar(academicTerm, eventBeans, EventType.EVALUATION);
    }

    private String evaluationCalendarICal(Person person) {
        ICalendarSyncPoint calendarSyncPoint = new ICalendarSyncPoint();

        List<EventBean> listEventBean = calendarSyncPoint.getExams(person.getUser());
        listEventBean.addAll(calendarSyncPoint.getTeachingExams(person.getUser()));

        Calendar createCalendar = CalendarFactory.createCalendar(listEventBean);

        return createCalendar.toString();
    }

    private FenixCalendar evaluationCalendarJson(Person person) {
        final User user = person.getUser();

        String academicTerm = ExecutionYear.readCurrentExecutionYear().getQualifiedName();

        ICalendarSyncPoint calendarSyncPoint = new ICalendarSyncPoint();
        List<EventBean> listEventBean = calendarSyncPoint.getExams(user);
        listEventBean.addAll(calendarSyncPoint.getTeachingExams(user));

        return getFenixEvaluationsCalendar(academicTerm, listEventBean);
    }

    /**
     * calendar of all written evaluations (tests and exams). Available for
     * students and teachers.
     *

     * @param format
     *            ("calendar" or "json")
     * @return If format is "calendar", returns iCal format. If not returns the
     *         following json.

     */
    @OAuthEndpoint(SCHEDULE_SCOPE)
    @GET
    @Path("person/calendar/evaluations")
    public Response calendarEvaluation(@QueryParam("format") String format) {
        validateFormat(format);
        final Person person = getPerson();

        if (!new ActiveStudentsGroup().isMember(person.getUser())) {
            return Response.status(Status.OK).header(HttpHeaders.CONTENT_TYPE, JSON_UTF8).entity("{}").build();
        }

        if ("calendar".equals(format)) {
            String evaluationCalendarICal = evaluationCalendarICal(person);
            return Response.ok(evaluationCalendarICal, "text/calendar;charset=UTF-8").build();
        } else {
            return Response.status(Status.OK).header(HttpHeaders.CONTENT_TYPE, JSON_UTF8).entity(evaluationCalendarJson(person))
                    .build();
        }
    }

    private String classesCalendarICal(Person person) {
        ICalendarSyncPoint calendarSyncPoint = new ICalendarSyncPoint();

        List<EventBean> listEventBean = calendarSyncPoint.getClasses(person.getUser());
        listEventBean.addAll(calendarSyncPoint.getTeachingClasses(person.getUser()));

        Calendar createCalendar = CalendarFactory.createCalendar(listEventBean);

        return createCalendar.toString();
    }

    private FenixCalendar classesCalendarJson(Person person) {

        final User user = person.getUser();
        String academicTerm = ExecutionYear.readCurrentExecutionYear().getQualifiedName();

        ICalendarSyncPoint calendarSyncPoint = new ICalendarSyncPoint();
        List<EventBean> listEventBean = calendarSyncPoint.getClasses(user);
        listEventBean.addAll(calendarSyncPoint.getTeachingClasses(user));

        return getFenixClassCalendar(academicTerm, listEventBean);
    }

    /**
     * calendar of all lessons of students and teachers
     *

     * @param format
     *            ("calendar" or "json")
     * @return If format is "calendar", returns iCal format. If not returns the
     *         following json.

     */
    @OAuthEndpoint(SCHEDULE_SCOPE)
    @GET
    @Path("person/calendar/classes")
    public Response calendarClasses(@QueryParam("format") String format) {
        validateFormat(format);
        Person person = getPerson();
        if ("calendar".equals(format)) {
            String classesCalendarICal = classesCalendarICal(person);
            return Response.ok(classesCalendarICal, "text/calendar; charset=UTF-8").build();
        } else {
            return Response.status(Status.OK).header(HttpHeaders.CONTENT_TYPE, JSON_UTF8).entity(classesCalendarJson(person))
                    .build();
        }
    }

    /**
     * Complete curriculum (for students)
     *
     * @return
     */
    @OAuthEndpoint(CURRICULAR_SCOPE)
    @GET
    @Path("person/curriculum")
    @Produces(JSON_UTF8)
    public List<FenixCurriculum> personCurriculum() {
        Person person = getPerson();
        List<FenixCurriculum> curriculum = new ArrayList<>();

        if (person.getStudent() != null) {
            curriculum = getStudentStatistics(person.getStudent().getRegistrationsSet());
        }
        return curriculum;
    }

    private List<FenixCurriculum> getStudentStatistics(Set<Registration> registrationsList) {

        final List<FenixCurriculum> curriculums = new ArrayList<FenixCurriculum>();

        for (Registration registration : registrationsList) {
            String start = registration.getFirstStudentCurricularPlan().getStartDateYearMonthDay().toString(formatDay);

            StudentCurricularPlan lastCurricularPlan = registration.getLastStudentCurricularPlan();

            String end = null;
            if (lastCurricularPlan .getEndDate() != null) {
                end = lastCurricularPlan.getEndDate().toString(formatDay);
            }

            ICurriculum icurriculum = lastCurricularPlan.getCurriculum(new DateTime(), null);

            final Integer curricularYear = icurriculum.getCurricularYear();
            BigDecimal credits = icurriculum.getSumEctsCredits();
            BigDecimal average = icurriculum.getRawGrade().getNumericValue();

            Integer calculatedAverage = icurriculum.getFinalGrade().getIntegerValue();

            boolean isFinished = lastCurricularPlan.isConcluded();
            final List<FenixCurriculum.ApprovedCourse> courseInfos = new ArrayList<>();

            for (StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
                Stream<CurriculumGroup> curriculumGroups = getAllGroupsForConclusion(studentCurricularPlan);

                curriculumGroups.forEach(curriculumGroup -> {
                    for (ICurriculumEntry iCurriculumEntry : curriculumGroup.getCurriculum().getCurriculumEntries()) {

                        String entryGradeValue = iCurriculumEntry.getGradeValue();
                        BigDecimal entryEcts = iCurriculumEntry.getEctsCreditsForCurriculum();

                        FenixCourse course = null;
                        if (iCurriculumEntry instanceof Enrolment) {
                            Enrolment enrolment = (Enrolment) iCurriculumEntry;
                            ExecutionCourse executionCourse = enrolment.getExecutionCourseFor(enrolment.getExecutionPeriod());
                            if (executionCourse != null) {
                                course = new FenixCourse(executionCourse);
                            } else {
                                CurricularCourse curricularCourse = enrolment.getCurricularCourse();
                                String entryName = mls(iCurriculumEntry.getPresentationName());
                                course = new FenixCourse(curricularCourse.getExternalId(), curricularCourse.getAcronym(), entryName);
                            }

                        } else if (iCurriculumEntry instanceof Dismissal) {
                            Dismissal dismissal = (Dismissal) iCurriculumEntry;
                            CurricularCourse curricularCourse = dismissal.getCurricularCourse();
                            String entryName = mls(iCurriculumEntry.getPresentationName());
                            course = new FenixCourse(curricularCourse.getExternalId(), curricularCourse.getAcronym(), entryName);
                        } else {
                            continue;
                        }

                        courseInfos.add(new FenixCurriculum.ApprovedCourse(course, entryGradeValue, entryEcts));

                    }
                });
            }

            curriculums.add(new FenixCurriculum(new FenixDegree(lastCurricularPlan.getDegree()), start, end, credits, average,
                    calculatedAverage, isFinished, curricularYear, courseInfos));
        }
        return curriculums;
    }

    protected Stream<CurriculumGroup> getAllGroupsForConclusion(StudentCurricularPlan studentCurricularPlan) {
        final Stream<ProgramConclusion> conclusions = ProgramConclusion.conclusionsFor(studentCurricularPlan);

        Stream<CurriculumGroup> curriculumGroups =
                conclusions.map(pc -> pc.groupFor(studentCurricularPlan)).filter(Optional::isPresent).map(Optional::get);
        return curriculumGroups;
    }

    private List<Event> calculateNotPayedEvents(final Person person) {

        final List<Event> result = new ArrayList<Event>();

        result.addAll(person.getNotPayedEventsPayableOn(null, false));
        result.addAll(person.getNotPayedEventsPayableOn(null, true));

        return result;
    }

    /**
     * Information about gratuity payments (payed and not payed)
     *
     * @return
     */
    @OAuthEndpoint(PAYMENTS_SCOPE)
    @GET
    @Path("person/payments")
    @Produces(JSON_UTF8)
    public FenixPayment personPayments() {

        Person person = getPerson();

        List<PaymentEvent> payed = new ArrayList<>();

        for (Entry entry : person.getPayments()) {
            String id = entry.getExternalId();
            String amount = entry.getOriginalAmount().getAmountAsString();
            String name = entry.getPaymentMode().getName();
            String description = entry.getDescription().toString();
            String shortDescription = getShortDescriptionForEntry(entry);
            String date = formatDay.print(entry.getWhenRegistered());
            payed.add(new PaymentEvent(id, amount, name, description, shortDescription, date));
        }

        List<Event> notPayedEvents = calculateNotPayedEvents(person);
        List<PendingEvent> notPayed = new ArrayList<>();

        for (Event event : notPayedEvents) {

            for (AccountingEventPaymentCode accountingEventPaymentCode : event.getNonProcessedPaymentCodes()) {
                String id = accountingEventPaymentCode.getExternalId();
                String description = accountingEventPaymentCode.getDescription();
                String startDate = formatDay.print(accountingEventPaymentCode.getStartDate()) + " 00:00";
                String endDate = formatDay.print(accountingEventPaymentCode.getEndDate()) + " 23:59";
                String entity = accountingEventPaymentCode.getEntityCode();
                String reference = accountingEventPaymentCode.getFormattedCode();
                String amount = accountingEventPaymentCode.getMinAmount().getAmountAsString();
                notPayed.add(new PendingEvent(id, description, new FenixPeriod(startDate, endDate), entity, reference, amount));
            }
        }

        return new FenixPayment(payed, notPayed);
    }

    private String getShortDescriptionForEntry(Entry entry) {
        return BundleUtil.getString(Bundle.ENUMERATION, entry.getEntryType().getName());
    }

    /**
     * Written evaluations for students
     *

     * @return enrolled and not enrolled student's evaluations

     */
    @OAuthEndpoint(EVALUATIONS_SCOPE)
    @GET
    @Path("person/evaluations")
    @Produces(JSON_UTF8)
    public List<FenixCourseEvaluation.WrittenEvaluation> evaluations(@Context HttpServletResponse response,
            @Context HttpServletRequest request, @Context ServletContext context) {

        Person person = getPerson();
        final Student student = person.getStudent();

        if (!new ActiveStudentsGroup().isMember(person.getUser()) || student == null) {
            return new ArrayList<FenixCourseEvaluation.WrittenEvaluation>();
        }

        List<FenixCourseEvaluation.WrittenEvaluation> evaluations = new ArrayList<>();

        ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();

        for (Registration registration : student.getRegistrationsSet()) {
            for (ExecutionSemester executionSemester : executionYear.getExecutionPeriodsSet()) {

                List<ExecutionCourse> studentExecutionCourses = registration.getAttendingExecutionCoursesFor(executionSemester);

                List<Evaluation> unenroledEvaluation = new ArrayList<Evaluation>();
                unenroledEvaluation.addAll(registration.getUnenroledExams(executionSemester));
                unenroledEvaluation.addAll(registration.getUnenroledWrittenTests(executionSemester));

                List<Evaluation> enroledEvaluation = new ArrayList<Evaluation>();
                enroledEvaluation.addAll(registration.getEnroledExams(executionSemester));
                enroledEvaluation.addAll(registration.getEnroledWrittenTests(executionSemester));

                for (Evaluation evaluation : unenroledEvaluation) {
                    Set<ExecutionCourse> examExecutionCourses =
                            new HashSet<ExecutionCourse>(evaluation.getAssociatedExecutionCoursesSet());
                    examExecutionCourses.retainAll(studentExecutionCourses);
                    evaluations.addAll(processEvaluation(evaluation, examExecutionCourses, false, student, executionSemester));
                }

                for (Evaluation evaluation : enroledEvaluation) {
                    Set<ExecutionCourse> examExecutionCourses =
                            new HashSet<ExecutionCourse>(evaluation.getAssociatedExecutionCoursesSet());
                    examExecutionCourses.retainAll(studentExecutionCourses);
                    evaluations.addAll(processEvaluation(evaluation, examExecutionCourses, true, student, executionSemester));
                }
            }
        }

        return evaluations;
    }

    private List<FenixCourseEvaluation.WrittenEvaluation> processEvaluation(Evaluation evaluation,
            Set<ExecutionCourse> executionCourses, final Boolean isEnrolled, final Student student,
            final ExecutionSemester executionSemester) {

        List<FenixCourseEvaluation.WrittenEvaluation> evaluations = new ArrayList<FenixCourseEvaluation.WrittenEvaluation>();

        if (!executionSemester.equals(ExecutionSemester.readActualExecutionSemester())) {
            if (!(evaluation instanceof Exam) || !((Exam) evaluation).isSpecialSeason()) {
                return evaluations;
            }
        }

        for (ExecutionCourse executionCourse : executionCourses) {
            if (evaluation instanceof Exam) {
                Exam exam = (Exam) evaluation;
                if (!exam.isExamsMapPublished()) {
                    continue;
                }
            }
            evaluations.add(getWrittenEvaluationJSON((WrittenEvaluation) evaluation, executionCourse, isEnrolled, student));
        }
        return evaluations;
    }

    /**
     * Enrols in evaluation represented by oid if enrol is "yes". unenrols
     * evaluation if enrol is "no".
     *

     * @param oid
     *            evaluations id
     * @param enrol
     *            ( "yes" or "no")
     * @return all evaluations

     */
    @OAuthEndpoint(EVALUATIONS_SCOPE)
    @PUT
    @Produces(JSON_UTF8)
    @Path("person/evaluations/{id}")
    public List<FenixCourseEvaluation.WrittenEvaluation> evaluations(@PathParam("id") String oid,
            @QueryParam("enrol") String enrol, @Context HttpServletResponse response, @Context HttpServletRequest request,
            @Context ServletContext context) {
        validateEnrol(enrol);
        WrittenEvaluation eval = getDomainObject(oid, WrittenEvaluation.class);
        try {
            if (enrol != null && !Strings.isNullOrEmpty(enrol.trim())) {
                if (enrol.equalsIgnoreCase(ENROL)) {
                    EnrolStudentInWrittenEvaluation.runEnrolStudentInWrittenEvaluation(getPerson().getUsername(),
                            eval.getExternalId());
                } else if (enrol.equalsIgnoreCase(UNENROL)) {
                    UnEnrollStudentInWrittenEvaluation.runUnEnrollStudentInWrittenEvaluation(getPerson().getUsername(),
                            eval.getExternalId());
                }
            }
            return evaluations(response, request, context);

        } catch (InvalidArgumentsServiceException e) {
            if (e.getMessage() == null) {
                final Set<ExecutionCourse> associatedExecutionCoursesSet = eval.getAssociatedExecutionCoursesSet();
                for (final Registration registration : getPerson().getStudent().getRegistrationsSet()) {
                    for (final Attends attends : registration.getAssociatedAttendsSet()) {
                        final ExecutionCourse executionCourse = attends.getExecutionCourse();
                        if (associatedExecutionCoursesSet.contains(executionCourse)) {
                            throw newApplicationError(Status.PRECONDITION_FAILED, "invalid_registration",
                                    "student registration is " + registration.getActiveStateType().name());
                        }
                    }
                }
                throw newApplicationError(Status.PRECONDITION_FAILED, "student_not_registered",
                        "the student does not have a registration in a degree associated with this evaluation");

            }
            throw newApplicationError(Status.PRECONDITION_FAILED, e.getMessage(), e.getMessage());
        } catch (FenixServiceException e) {
            throw newApplicationError(Status.PRECONDITION_FAILED, e.getMessage(), e.getMessage());
        }
    }

    private final <T extends DomainObject> T getDomainObject(final String externalId, final Class<T> clazz) {
        try {
            T domainObject = FenixFramework.getDomainObject(externalId);
            if (!FenixFramework.isDomainObjectValid(domainObject) || !clazz.isAssignableFrom(domainObject.getClass())) {
                throw newApplicationError(Status.NOT_FOUND, "id not found", "id not found");
            }
            return domainObject;
        } catch (Exception nfe) {
            throw newApplicationError(Status.NOT_FOUND, "id not found", "id not found");
        }
    }

    private WebApplicationException newApplicationError(Status status, String error, String description) {
        JsonObject errorObject = new JsonObject();
        errorObject.addProperty("error", error);
        errorObject.addProperty("description", description);
        return new WebApplicationException(Response.status(status).entity(errorObject.toString()).build());
    }

    /**
     * generic information about the institution
     *

     * @return news and events rss
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("about")
    public FenixAbout about() {
        return FenixAbout.getInstance();
    }

    /**
     * Academic Terms
     *

     * @return all the academic terms available to be used in other endpoints as academicTerm query parameter.
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("academicterms")
    public String academicTerms() {
        JsonObject obj = new JsonObject();
        for (ExecutionYear year : Bennu.getInstance().getExecutionYearsSet()) {
            JsonArray sems = new JsonArray();
            for (ExecutionSemester semester : year.getExecutionPeriodsSet()) {
                sems.add(new JsonPrimitive(semester.getQualifiedName()));
            }
            obj.add(year.getQualifiedName(), sems);
        }
        return gson.toJson(obj);
    }

    private List<String> getTeacherPublicMail(Teacher teacher) {
        final List<String> emails = new ArrayList<>();
        if (teacher != null) {
            for (EmailAddress emailAddress : teacher.getPerson().getEmailAddresses()) {
                if (emailAddress.getVisibleToPublic()) {
                    emails.add(emailAddress.getPresentationValue());
                }
            }
        }
        return emails;
    }

    private List<String> getTeacherPublicWebAddress(Teacher teacher) {
        final List<String> urls = new ArrayList<>();
        if (teacher != null) {
            for (WebAddress webAddress : teacher.getPerson().getWebAddresses()) {
                if (webAddress.getVisibleToPublic()) {
                    urls.add(webAddress.getUrl());
                }
            }
        }
        return urls;
    }

    /**
     * All information about degrees available
     *

     * @param academicTerm
     * @see academicTerms
     *
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("degrees")
    public List<FenixDegreeExtended> degrees(@QueryParam("academicTerm") String academicTerm) {

        AcademicInterval academicInterval = getAcademicInterval(academicTerm);

        List<FenixDegreeExtended> fenixDegrees = new ArrayList<>();

        for (final ExecutionDegree executionDegree : ExecutionDegree.filterByAcademicInterval(academicInterval)) {
            fenixDegrees.add(getFenixDegree(executionDegree));
        }
        return fenixDegrees;
    }

    @GET
    @Produces(JSON_UTF8)
    @Path("canteen")
    public String canteen(@QueryParam("day") String day, @QueryParam("name") String canteenName) {
        validateDay(day);
        if (day == null || Strings.isNullOrEmpty(day.trim())) {
            day = formatDay.print(new DateTime());
        }
        if (canteenName == null || Strings.isNullOrEmpty(canteenName.trim())) {
            canteenName = FenixEduIstIntegrationConfiguration.getConfiguration().getFenixAPICanteenDefaultName();
        }
        return FenixAPIFromExternalServer.getCanteen(day, canteenName);
    }

    @GET
    @Produces(JSON_UTF8)
    @Path("contacts")
    public String contacts() {
        return FenixAPIFromExternalServer.getContacts();
    }

    @GET
    @Produces(JSON_UTF8)
    @Path("shuttle")
    public String shuttle() {
        return FenixAPIFromExternalServer.getShuttle();
    }

    @GET
    @Produces(JSON_UTF8)
    @Path("degrees/all")
    public Set<FenixDegree> degreesAll() {

        Set<FenixDegree> degrees = new HashSet<FenixDegree>();

        for (final Degree degree : Degree.readNotEmptyDegrees()) {
            degrees.add(new FenixDegree(degree, true));
        }
        return degrees;
    }

    private FenixDegreeExtended getFenixDegree(ExecutionDegree executionDegree) {
        final Degree degree = executionDegree.getDegree();
        List<FenixSpace> degreeCampus = new ArrayList<>();
        ExecutionYear executionYear = executionDegree.getExecutionYear();
        String name = degree.getNameI18N(executionYear).getContent(I18N.getLocale());

        String type = degree.getDegreeTypeName();
        String typeName = degree.getDegreeType().getName().getContent();
        String degreeUrl = degree.getSiteUrl();

        for (Space campus : degree.getCampus(executionYear)) {
            degreeCampus.add(FenixSpace.getSimpleSpace(campus));
        }

        FenixDegreeExtended.FenixDegreeInfo fenixDegreeInfo = null;

        DegreeInfo degreeInfo = degree.getDegreeInfoFor(executionYear);
        if (degreeInfo == null) {
            degreeInfo = degree.getMostRecentDegreeInfo(executionYear);
        }

        if (degreeInfo != null) {

            String description = mls(degreeInfo.getDescription());
            String objectives = mls(degreeInfo.getObjectives());
            String designFor = mls(degreeInfo.getDesignedFor());
            String requisites = mls(degreeInfo.getDegreeInfoCandidacy().getAccessRequisites());
            String profissionalExits = mls(degreeInfo.getProfessionalExits());
            String history = mls(degreeInfo.getHistory());
            String operationRegime = mls(degreeInfo.getOperationalRegime());
            String gratuity = mls(degreeInfo.getGratuity());
            String links = mls(degreeInfo.getLinks());
            fenixDegreeInfo =
                    new FenixDegreeInfo(description, objectives, designFor, requisites, profissionalExits, history,
                            operationRegime, gratuity, links);
        }

        final List<FenixTeacher> teachers = new ArrayList<>();
        final Collection<Teacher> responsibleCoordinatorsTeachers = degree.getResponsibleCoordinatorsTeachers(executionYear);

        for (Teacher teacher : responsibleCoordinatorsTeachers) {
            String teacherName = teacher.getPerson().getName();
            String istId = teacher.getPerson().getUsername();
            List<String> mails = getTeacherPublicMail(teacher);
            List<String> urls = getTeacherPublicWebAddress(teacher);
            teachers.add(new FenixTeacher(teacherName, istId, mails, urls));
        }

        FenixDegreeExtended fenixDegree =
                new FenixDegreeExtended(executionYear.getQualifiedName(), name, degree, type, typeName, degreeUrl, degreeCampus,
                        fenixDegreeInfo, teachers);
        return fenixDegree;
    }

    /**
     *
     * Retrieves information about degree with id
     *

     * @param oid
     *            degree id
     * @param academicTerm
     *            ("yyyy/yyyy")
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("degrees/{id}")
    public FenixDegree degreesByOid(@PathParam("id") String oid, @QueryParam("academicTerm") String academicTerm) {
        Degree degree = getDomainObject(oid, Degree.class);
        final AcademicInterval academicInterval = getAcademicInterval(academicTerm, true);
        List<ExecutionDegree> executionDegrees = degree.getExecutionDegrees(academicInterval);
        if (!executionDegrees.isEmpty()) {
            final ExecutionDegree max = Ordering.from(ExecutionDegree.EXECUTION_DEGREE_COMPARATORY_BY_YEAR).max(executionDegrees);
            if (max != null) {
                return getFenixDegree(max);
            }
        }

        throw newApplicationError(Status.NOT_FOUND, "resource_not_found", "No degree information found for " + degree.getName()
                + " on " + academicInterval.getPresentationName());
    }

    /**
     * Courses for degree with id
     *

     * @param oid
     *            degree id
     * @param academicTerm
     *            ("yyyy/yyyy")
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("degrees/{id}/courses")
    public List<FenixExecutionCourse> coursesByODegreesId(@PathParam("id") String oid,
            @QueryParam("academicTerm") String academicTerm) {

        Degree degree = getDomainObject(oid, Degree.class);

        ExecutionSemester[] executionSemesters = getExecutionSemesters(academicTerm).toArray(new ExecutionSemester[0]);

        final Set<ExecutionCourse> executionCourses = new HashSet<ExecutionCourse>();
        degree.getDegreeCurricularPlansSet().stream().filter(DegreeCurricularPlan::isActive)
                .forEach(plan -> addExecutionCourses(plan.getRoot(), executionCourses, executionSemesters));

        List<FenixExecutionCourse> fenixExecutionCourses = new ArrayList<>();

        for (ExecutionCourse executionCourse : executionCourses) {
            String sigla = executionCourse.getSigla();
            String credits = getCredits(executionCourse, degree);
            String name = executionCourse.getName();
            String id = executionCourse.getExternalId();
            String academicTermValue = executionCourse.getExecutionPeriod().getQualifiedName();

            fenixExecutionCourses.add(new FenixExecutionCourse(sigla, credits, name, id, academicTermValue));
        }
        return fenixExecutionCourses;
    }

    /**
     * Set the previously chosen courses for students
     *

     * @param oid degree id
     * @param academicTerm
     * @param preEnrolments
     * 
     */
    @POST
    @Produces(JSON_UTF8)
    @Path("degrees/{id}/preEnrolmentsCurricularGroups")
    @OAuthEndpoint(value = DEGREE_CURRICULAR_MANAGEMENT)
    public Response defaultEnrolments(@PathParam("id") String oid, @QueryParam("academicTerm") String academicTerm,
            JsonObject preEnrolments) {

        AcademicInterval academicInterval = getAcademicInterval(academicTerm, true);
        if (academicInterval == null) {
            throw newApplicationError(Status.BAD_REQUEST, "format_error", "academicTerm parameter must not be null");
        }

        ExecutionInterval executionInterval = ExecutionInterval.getExecutionInterval(academicInterval);
        if (!(executionInterval instanceof ExecutionSemester)) {
            throw newApplicationError(Status.BAD_REQUEST, "format_error", "academicTerm parameter must be a semester");
        }
        ExecutionSemester executionSemester = (ExecutionSemester) executionInterval;

        Degree degree = getDomainObject(oid, Degree.class);
        final Person coordinator = Authenticate.getUser().getPerson();
        boolean isCoordinator =
                degree.getResponsibleCoordinators(executionSemester.getExecutionYear()).stream().map(Coordinator::getPerson)
                        .anyMatch(p -> p == coordinator);
        if (!isCoordinator) {
            throw newApplicationError(Status.BAD_REQUEST, "notAllowed",
                    "must be a responsible coordinator of the degree in the given semester");
        }

        JsonObject report = new JsonObject();
        report.add("errors", new JsonArray());

        JsonArray jArray = preEnrolments.getAsJsonArray("defaultEnrolments");
        for (JsonElement jsonElement : jArray) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            String userId = jsonObject.get("student").getAsString();
            User user = User.findByUsername(userId);
            if (user == null) {
                report.get("errors").getAsJsonArray()
                        .add(new JsonPrimitive("User " + userId + " not found. Pre enrolments for this user were not processed"));
                break;
            }
            for (JsonElement jsonPreEnrol : jsonObject.get("preEnrolments").getAsJsonArray()) {
                JsonObject jsonEnrolLine = jsonPreEnrol.getAsJsonObject();
                String curricularCourseId = jsonEnrolLine.get("course").getAsString();
                String groupId = jsonEnrolLine.get("group").getAsString();

                CurricularCourse curricularCourse = null;
                try {
                    curricularCourse = FenixFramework.getDomainObject(curricularCourseId);
                } catch (Exception e) {
                    report.get("errors").getAsJsonArray()
                            .add(new JsonPrimitive("Curricular course " + curricularCourseId + " not found for user " + userId));
                    break;
                }
                CourseGroup courseGroup = null;
                try {
                    courseGroup = FenixFramework.getDomainObject(groupId);
                } catch (Exception e) {
                    report.get("errors").getAsJsonArray()
                            .add(new JsonPrimitive("Course group " + groupId + " not found for user " + userId));
                    break;
                }

                try {
                    CreatePreEnrolment.create(executionSemester, degree, user, curricularCourse, courseGroup);
                } catch (Exception e) {
                    report.get("errors")
                            .getAsJsonArray()
                            .add(new JsonPrimitive("An error occured for user " + userId + " course group " + groupId
                                    + " and curricular course " + curricularCourseId
                                    + " - this pre enrolment was not successful - Exception: " + e.getMessage()));
                }
            }
        }
        return Response.ok(report).build();
    }

    /**
     * Courses group structure for degree with id
     *

     * @param oid
     *            degree id
     * @param academicTerm
     * 
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("degrees/{id}/curricularGroups")
    public List<FenixCurricularGroup> curricularGroupsByDegreeId(@PathParam("id") String oid,
            @QueryParam("academicTerm") String academicTerm) {

        Degree degree = getDomainObject(oid, Degree.class);
        Set<ExecutionSemester> executionSemesters = getExecutionSemesters(academicTerm);
        ExecutionYear executionYear = executionSemesters.iterator().next().getExecutionYear();

        Set<FenixCurricularGroup> result = new HashSet<FenixCurricularGroup>();
        degree.getDegreeCurricularPlansForYear(executionYear).stream().filter(DegreeCurricularPlan::isActive).forEach(dcp -> {
            result.add(new FenixCurricularGroup(dcp.getRoot(), executionYear));
            addCurricularGroup(executionYear, result, dcp.getRoot());
        });
        return new ArrayList<FenixCurricularGroup>(result);
    }

    private void addCurricularGroup(ExecutionYear executionYear, Set<FenixCurricularGroup> result, CourseGroup group) {
        for (DegreeModule degreeModule : group.getChildDegreeModulesValidOn(executionYear)) {
            if (degreeModule.isCourseGroup()) {
                result.add(new FenixCurricularGroup((CourseGroup) degreeModule, executionYear));
                addCurricularGroup(executionYear, result, (CourseGroup) degreeModule);
            }
        }
    }

    /**
     * Curricular courses for degree with id
     *

     * @param oid
     *            degree id
     * @param academicTerm
     * 
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("degrees/{id}/curricularCourses")
    public String curricularCoursesByDegreeId(@PathParam("id") String oid, @QueryParam("academicTerm") String academicTerm) {

        Degree degree = getDomainObject(oid, Degree.class);

        Set<ExecutionSemester> executionSemesters = getExecutionSemesters(academicTerm);
        ExecutionYear executionYear = executionSemesters.iterator().next().getExecutionYear();

        final Set<CurricularCourse> curricularCourses = new HashSet<CurricularCourse>();

        degree.getDegreeCurricularPlansForYear(executionYear).stream().filter(DegreeCurricularPlan::isActive)
                .forEach(plan -> addCurricularCourses(plan.getRoot(), curricularCourses, executionSemesters));

        return curricularCourses.stream().map(cc -> {
            JsonObject object = new JsonObject();
            object.addProperty("id", cc.getExternalId());
            object.addProperty("name", cc.getName());
            return object;
        }).collect(StreamUtils.toJsonArray()).toString();
    }

    private void addCurricularCourses(final CourseGroup courseGroup, final Set<CurricularCourse> curricularCourses,
            final Set<ExecutionSemester> executionSemesters) {
        for (ExecutionSemester executionSemester : executionSemesters) {
            for (DegreeModule degreeModule : courseGroup.getChildDegreeModulesValidOn(executionSemester)) {
                if (degreeModule.isLeaf()) {
                    final CurricularCourse curricularCourse = (CurricularCourse) degreeModule;
                    curricularCourses.add(curricularCourse);
                } else {
                    final CourseGroup childCourseGroup = (CourseGroup) degreeModule;
                    addCurricularCourses(childCourseGroup, curricularCourses, executionSemesters);
                }
            }
        }
    }

    @GET
    @Produces(JSON_UTF8)
    @Path("degrees/{id}/students")
    @OAuthEndpoint(value = CURRICULAR_SCOPE, serviceOnly = true)
    public String enrolledByDegreeAndCurricularYear(@PathParam("id") String degreeId, @QueryParam("curricularYear") Integer year) {
        Degree degree = getDomainObject(degreeId, Degree.class);
        Stream<Registration> registrations =
                degree.getActiveDegreeCurricularPlans().stream().flatMap(dcp -> dcp.getStudentCurricularPlansSet().stream())
                        .filter(StudentCurricularPlan::isActive).map(StudentCurricularPlan::getRegistration);
        if (year != null) {
            registrations = registrations.filter(r -> r.getCurricularYear() == year);
        }
        return registrations.map(r -> {
            JsonObject object = new JsonObject();
            object.addProperty("username", r.getPerson().getUsername());
            object.addProperty("name", r.getPerson().getUser().getProfile().getDisplayName());
            return object;
        }).collect(StreamUtils.toJsonArray()).toString();
    }

    private void addExecutionCourses(final CourseGroup courseGroup, final Collection<ExecutionCourse> executionCourseViews,
            final ExecutionSemester... executionPeriods) {
        for (final org.fenixedu.academic.domain.degreeStructure.Context context : courseGroup.getChildContextsSet()) {
            for (final ExecutionSemester executionSemester : executionPeriods) {
                if (context.isValid(executionSemester)) {
                    final DegreeModule degreeModule = context.getChildDegreeModule();
                    if (degreeModule.isLeaf()) {
                        final CurricularCourse curricularCourse = (CurricularCourse) degreeModule;
                        for (final ExecutionCourse executionCourse : curricularCourse.getAssociatedExecutionCoursesSet()) {
                            if (executionCourse.getExecutionPeriod() == executionSemester) {
                                executionCourseViews.add(executionCourse);
                            }
                        }
                    } else {
                        final CourseGroup childCourseGroup = (CourseGroup) degreeModule;
                        addExecutionCourses(childCourseGroup, executionCourseViews, executionPeriods);
                    }
                }
            }
        }
    }

    private String getCredits(ExecutionCourse ec, Degree degree) {
        for (CurricularCourse curricularCourse : ec.getAssociatedCurricularCoursesSet()) {
            if (degree.equals(curricularCourse.getDegree())) {
                return curricularCourse.getEctsCredits().toString();
            }
        }
        return "N/A";
    }

    /**
     * Detailed information about course
     *

     * @param oid
     *            course id
     * @return
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("courses/{id}/")
    public FenixCourseExtended coursesByOid(@PathParam("id") String oid) {
        ExecutionCourse executionCourse = getDomainObject(oid, ExecutionCourse.class);

        String acronym = executionCourse.getSigla();
        String name = executionCourse.getName();
        String evaluationMethod = executionCourse.getEvaluationMethodText();
        String academicTerm = executionCourse.getExecutionPeriod().getQualifiedName();
        String courseUrl = executionCourse.getSiteUrl();

        if (courseUrl == null) {
            courseUrl = "";
        }

        Map<CompetenceCourse, Set<CurricularCourse>> curricularCourses =
                executionCourse.getCurricularCoursesIndexedByCompetenceCourse();

        List<FenixCourseExtended.FenixCompetence> moreInfo = new ArrayList<>();

        for (Map.Entry<CompetenceCourse, Set<CurricularCourse>> entry : curricularCourses.entrySet()) {

            List<FenixCourseExtended.FenixCompetence.BiblioRef> biblios = new ArrayList<>();
            final CompetenceCourse competenceCourse = entry.getKey();
            for (BibliographicReference bibliographicReference : competenceCourse.getBibliographicReferences()
                    .getBibliographicReferencesSortedByOrder()) {

                String author = bibliographicReference.getAuthors();
                String reference = bibliographicReference.getReference();
                String title = bibliographicReference.getTitle();
                String bibYear = bibliographicReference.getYear();
                String type = bibliographicReference.getType().getName();
                String url = bibliographicReference.getUrl();

                biblios.add(new BiblioRef(author, reference, title, bibYear, type, url));
            }

            List<FenixCourseExtended.FenixCompetence.Degree> degrees = new ArrayList<>();
            for (CurricularCourse curricularCourse : entry.getValue()) {
                String id = curricularCourse.getDegree().getExternalId();
                String dName = curricularCourse.getDegree().getPresentationName();
                String dacronym = curricularCourse.getAcronym();

                degrees.add(new FenixCourseExtended.FenixCompetence.Degree(id, dName, dacronym));
            }

            String program = competenceCourse.getProgram();

            moreInfo.add(new FenixCompetence(competenceCourse.getExternalId(), program, biblios, degrees));

        }

        int numberOfStudents = executionCourse.getAttendsSet().size();

        List<FenixTeacher> teachers = new ArrayList<>();
        for (Professorship professorship : executionCourse.getProfessorshipsSet()) {

            String tname = professorship.getPerson().getName();
            String istid = professorship.getPerson().getUsername();
            List<String> mail = getTeacherPublicMail(professorship.getTeacher());
            List<String> url = getTeacherPublicWebAddress(professorship.getTeacher());

            teachers.add(new FenixTeacher(tname, istid, mail, url));
        }

        String summaryLink = courseUrl.concat("/rss/summary");
        String announcementLink = courseUrl.concat("/rss/announcement");

        return new FenixCourseExtended(acronym, name, evaluationMethod, academicTerm, numberOfStudents, summaryLink,
                announcementLink, courseUrl, moreInfo, teachers);
    }

    /**
     * Retrieve groups for course given by oid
     *

     * @param oid
     *            course id
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("courses/{id}/groups")
    public List<FenixCourseGroup> groupsCoursesByOid(@PathParam("id") final String oid) {

        final ExecutionCourse executionCourse = getDomainObject(oid, ExecutionCourse.class);

        final List<FenixCourseGroup> groupings = new ArrayList<>();

        for (final Grouping grouping : executionCourse.getGroupings()) {
            groupings.add(new FenixCourseGroup(grouping));
        }

        return groupings;

    }

    /**
     * Returns summaries for course by id
     *

     * @param oid
     *            course id
     * @return
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("courses/{id}/summaries")
    @OAuthEndpoint(PERSONAL_SCOPE)
    public List<FenixLessonSummary> summariesCoursesByOid(@PathParam("id") String oid) {
        final ExecutionCourse executionCourse = getDomainObject(oid, ExecutionCourse.class);
        List<FenixLessonSummary> summaries = new ArrayList<>();

        for (Shift shift : executionCourse.getAssociatedShifts()) {
            for (Lesson lesson : shift.getAssociatedLessonsSet()) {
                lesson.getAllLessonDates().stream().sorted().map(ld -> {
                    LessonInstance lessonInstance = lesson.getLessonInstanceFor(ld);
                    HourMinuteSecond time = lesson.getBeginHourMinuteSecond();
                    TimeOfDay lessonTime = new TimeOfDay(time.getHour(), time.getMinuteOfHour(), time.getSecondOfMinute());
                    return new FenixLessonSummary(shift, ld.toDateTime(lessonTime), lesson.getSala(), lessonInstance);
                }).forEach(summaries::add);
            }
        }

        return summaries;
    }

    /**
     * Adds a summary for a lesson in a course by id
     *

     * @param oid
     *            course id
     * @param summaryInfo
     *            JSON object containing the summary info:
     *              {
     *                  "shift": "shift_id",
     *                  "date": "yyyy-MM-dd HH:mm:ss",
     *                  "room": "room_id",
     *                  "taught": true/false,
     *                  "attendance": 0,
     *                  "title": {
     *                      "pt-PT": "pt title",
     *                      "en-GB": "en title"
     *                  },
     *                  "content": {
     *                      "pt-PT": "pt content",
     *                      "en-GB": "en content"
     *                  }
     *              }
     * @return
     */
    @POST
    @Produces(JSON_UTF8)
    @Path("courses/{id}/summaries")
    @OAuthEndpoint(PERSONAL_SCOPE)
    public FenixLessonSummary createSummaryCourseByOid(@PathParam("id") String oid, JsonObject summaryInfo) {
        return addOrUpdateSummary(oid, summaryInfo, (bean) -> {
            try {
                CreateSummary.runCreateSummary(bean);
            } catch (Exception e) {
                throw newApplicationError(Status.PRECONDITION_FAILED, "validation error", e.getMessage());
            }
        });
    }

    /**
     * Edits a summary for a lesson in a course by id
     *

     * @param oid
     *            course id
     * @param summaryInfo
     *            JSON object containing the summary info:
     *              {
     *                  "shift": "shift_id",
     *                  "date": "yyyy-MM-dd HH:mm:ss",
     *                  "room": "room_id",
     *                  "taught": true/false,
     *                  "attendance": 0,
     *                  "title": {
     *                      "pt-PT": "pt title",
     *                      "en-GB": "en title"
     *                  },
     *                  "content": {
     *                      "pt-PT": "pt content",
     *                      "en-GB": "en content"
     *                  }
     *              }
     * @return
     */
    @PUT
    @Produces(JSON_UTF8)
    @Path("courses/{id}/summaries")
    @OAuthEndpoint(PERSONAL_SCOPE)
    public FenixLessonSummary updateSummaryCourseByOid(@PathParam("id") String oid, JsonObject summaryInfo) {
        return addOrUpdateSummary(oid, summaryInfo, (bean) -> {
            try {
                CreateSummary.runEditSummary(bean);
            } catch (Exception e) {
                throw newApplicationError(Status.PRECONDITION_FAILED, "validation error", e.getMessage());
            }
        });
    }

    private FenixLessonSummary addOrUpdateSummary(String courseOid, JsonObject summaryInfo, Consumer<SummariesManagementBean> service)  {
        final String room, shiftId, date;
        final Boolean taught;
        final int attendance;
        final JsonObject title, content;

        try {
            room = summaryInfo.get("room").getAsString();
            shiftId = summaryInfo.get("shift").getAsString();
            date = summaryInfo.get("date").getAsString();
        } catch (Exception ignored) {
            throw newApplicationError(Status.PRECONDITION_FAILED, "invalid parameters", "missing keys 'room', 'shift' or 'date'");
        }

        try {
            taught = summaryInfo.get("taught").getAsBoolean();
            attendance = summaryInfo.get("attendance").getAsInt();
        } catch (Exception ignored) {
            throw newApplicationError(Status.PRECONDITION_FAILED, "invalid parameters", "missing keys 'taught' or 'attendance'");
        }

        try {
            title = summaryInfo.getAsJsonObject("title");
            content = summaryInfo.getAsJsonObject("content");
        } catch (Exception ignored) {
            throw newApplicationError(Status.PRECONDITION_FAILED, "invalid parameters", "missing keys 'title' or 'content'");
        }


        final ExecutionCourse executionCourse = getDomainObject(courseOid, ExecutionCourse.class);
        final Space space = getDomainObject(room, Space.class);
        final Shift shift = getDomainObject(shiftId, Shift.class);

        if (!executionCourse.getAssociatedShifts().contains(shift)) {
            throw newApplicationError(Status.PRECONDITION_FAILED, "invalid parameters", "invalid shift for this execution course");
        }

        SummariesManagementBean bean =
                fillSummaryManagementBean(executionCourse, shift, space, date, title, content, taught, attendance);
        service.accept(bean);

        Lesson lesson = bean.getLesson();
        DateTime lessonDateTime = DateTimeFormat.forPattern(dayHourSecondPattern).parseDateTime(date);
        LessonInstance lessonInstance = lesson.getLessonInstanceFor(lessonDateTime.toYearMonthDay());
        return new FenixLessonSummary(shift, lessonDateTime, space, lessonInstance);
    }

    private SummariesManagementBean fillSummaryManagementBean(ExecutionCourse executionCourse, Shift shift, Space space,
            String date, JsonObject title, JsonObject content, Boolean taught, int attendance) {

        MultiLanguageString titleMLS, contentMLS;
        try {
            titleMLS = MultiLanguageString.fromLocalizedString(LocalizedString.fromJson(title));
            contentMLS = MultiLanguageString.fromLocalizedString(LocalizedString.fromJson(content));
        } catch (Exception e) {
            throw newApplicationError(Status.PRECONDITION_FAILED, "invalid parameters", "'title' or 'content' is not a valid multi-language string");
        }

        DateTime lessonDateTime = DateTimeFormat.forPattern(dayHourSecondPattern).parseDateTime(date);
        YearMonthDay lessonDate = lessonDateTime.toYearMonthDay();
        Partial lessonTime = new Partial()
                .with(DateTimeFieldType.hourOfDay(), lessonDateTime.getHourOfDay())
                .with(DateTimeFieldType.minuteOfHour(), lessonDateTime.getMinuteOfHour())
                .with(DateTimeFieldType.secondOfDay(), lessonDateTime.getSecondOfMinute());


        Optional<Lesson> lesson = shift.getAssociatedLessonsSet().stream()
                .filter(l -> l.getAllLessonDates().stream().anyMatch(lessonDate::equals))
                .filter(l -> l.getSala().equals(space))
                .findFirst();

        if (!lesson.isPresent()) {
            throw newApplicationError(Status.PRECONDITION_FAILED, "invalid parameters", "invalid lesson date or room");
        }

        Person person = getPerson();
        Professorship professorship = executionCourse.getProfessorship(person);
        String teacherName = person.getName();
        LessonInstance lessonInstance = lesson.get().getLessonInstanceFor(lessonDate);
        Summary summary = lessonInstance != null ? lessonInstance.getSummary() : null;
        ShiftType shiftType = shift.getSortedTypes().first();

        return new SummariesManagementBean(titleMLS, contentMLS, attendance, NORMAL_SUMMARY, professorship,
                teacherName, null, shift, lesson.get(), lessonDate, space, lessonTime, summary, professorship, shiftType, taught);
    }

    /**
     * Deletes a summary in a lesson of a course by id
     *

     * @param oid
     *            course id
     * @param summaryInfo
     *            JSON object containing the summary info:
     *              {
     *                  "shift": "shift_id",
     *                  "date": "yyyy-MM-dd HH:mm:ss",
     *                  "room": "room_id",
     *              }
     * @return
     */
    @DELETE
    @Produces(JSON_UTF8)
    @Path("courses/{id}/summaries")
    @OAuthEndpoint(PERSONAL_SCOPE)
    public Response deleteSummaryCourseByOid(@PathParam("id") String oid, JsonObject summaryInfo) {
        final String shiftId, date, room;
        try {
            shiftId = summaryInfo.get("shift").getAsString();
            date = summaryInfo.get("date").getAsString();
            room = summaryInfo.get("room").getAsString();
        } catch (Exception e) {
            throw newApplicationError(Status.PRECONDITION_FAILED, "invalid parameters", "missing keys 'shift', 'date' or 'room'");
        }

        final ExecutionCourse executionCourse = getDomainObject(oid, ExecutionCourse.class);
        final Space space = getDomainObject(room, Space.class);
        final Shift shift = getDomainObject(shiftId, Shift.class);

        DateTime lessonDateTime = DateTimeFormat.forPattern(dayHourSecondPattern).parseDateTime(date);
        YearMonthDay lessonDate = lessonDateTime.toYearMonthDay();

        Optional<LessonInstance> lessonInstance = shift.getAssociatedLessonsSet().stream()
                                                       .map(l -> l.getLessonInstanceFor(lessonDate))
                                                       .filter(l -> l != null && l.getRoom().equals(space))
                                                       .findFirst();
        if (!lessonInstance.isPresent()) {
            throw newApplicationError(Status.PRECONDITION_FAILED, "invalid parameters", "invalid lesson date or room");
        }

        final Person person = getPerson();
        Summary summary = lessonInstance.get().getSummary();
        Professorship professorship = executionCourse.getProfessorship(person);

        try {
            DeleteSummary.runDeleteSummary(executionCourse, summary, professorship);
        } catch (Exception e) {
            throw newApplicationError(Status.PRECONDITION_FAILED, "validation error", e.getMessage());
        }

        return Response.noContent().build();
    }

    /**
     * All students for course by id
     *

     * @param oid
     *            course id
     * @return
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("courses/{id}/students")
    public FenixCourseStudents studentsCoursesByOid(@PathParam("id") String oid) {
        ExecutionCourse executionCourse = getDomainObject(oid, ExecutionCourse.class);
        return new FenixCourseStudents(executionCourse);
    }

    /**
     *
     * Returns evaluations for course by id (Test, Exams, Project, AdHoc,
     * OnlineTest)
     *

     * @param oid
     * @return
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("courses/{id}/evaluations")
    public List<FenixCourseEvaluation> evaluationCoursesByOid(@PathParam("id") String oid) {

        ExecutionCourse executionCourse = getDomainObject(oid, ExecutionCourse.class);

        final List<FenixCourseEvaluation> evals = new ArrayList<>();

        for (Evaluation evaluation : executionCourse.getAssociatedEvaluationsSet()) {
            if (evaluation instanceof WrittenEvaluation) {
                evals.add(getWrittenEvaluationJSON((WrittenEvaluation) evaluation, executionCourse));
            } else if (evaluation instanceof Project) {
                evals.add(getProjectEvaluationJSON((Project) evaluation));
            } else if (evaluation instanceof AdHocEvaluation) {
                evals.add(getAdhocEvaluationJSON((AdHocEvaluation) evaluation));
            } else {
                evals.add(getGenericTestJSON(evaluation));
            }
        }
        return evals;
    }

    private FenixCourseEvaluation.AdHocEvaluation getAdhocEvaluationJSON(AdHocEvaluation adHocEvaluation) {

        return new FenixCourseEvaluation.AdHocEvaluation(adHocEvaluation.getPresentationName(), adHocEvaluation.getDescription());
    }

    private FenixCourseEvaluation.GenericTest getGenericTestJSON(Evaluation evaluation) {
        String type = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, evaluation.getClass().getSimpleName());
        return new FenixCourseEvaluation.GenericTest(evaluation.getPresentationName(), type);
    }

    private FenixCourseEvaluation.Project getProjectEvaluationJSON(Project projectEvaluation) {

        String name = projectEvaluation.getPresentationName();

        String start = null;
        String end = null;

        if (projectEvaluation.getProjectBeginDateTime() != null) {
            start = formatDayHour.print(projectEvaluation.getProjectBeginDateTime());
        }
        if (projectEvaluation.getProjectEndDateTime() != null) {
            end = formatDayHour.print(projectEvaluation.getProjectEndDateTime());
        }
        return new FenixCourseEvaluation.Project(name, new FenixPeriod(start, end));
    }

    private FenixCourseEvaluation.WrittenEvaluation getWrittenEvaluationJSON(WrittenEvaluation writtenEvaluation,
            ExecutionCourse executionCourse) {
        return getWrittenEvaluationJSON(writtenEvaluation, executionCourse, null, null);
    }

    private FenixCourseEvaluation.WrittenEvaluation getWrittenEvaluationJSON(WrittenEvaluation writtenEvaluation,
            ExecutionCourse executionCourse, Boolean isEnrolled, Student student) {
        String name = writtenEvaluation.getPresentationName();
        EvaluationType type = writtenEvaluation.getEvaluationType();

        String day = formatDay.print(writtenEvaluation.getDay().getTimeInMillis());
        String beginningTime = formatHour.print(writtenEvaluation.getBeginning().getTimeInMillis());
        String endTime = formatHour.print(writtenEvaluation.getEnd().getTimeInMillis());

        FenixPeriod evaluationPeriod =
                new FenixPeriod(Joiner.on(" ").join(day, beginningTime), Joiner.on(" ").join(day, endTime));

        boolean isEnrolmentPeriod = writtenEvaluation.getIsInEnrolmentPeriod();

        final DateTime start = writtenEvaluation.getEnrolmentPeriodStart();
        final DateTime end = writtenEvaluation.getEnrolmentPeriodEnd();

        final String enrollmentPeriodStart = start == null ? null : formatDayHourSecond.print(start);
        final String enrollmentPeriodEnd = end == null ? null : formatDayHourSecond.print(end);

        String writtenEvaluationId = writtenEvaluation.getExternalId();
        if (student != null) {
            Space assignedRoom = null;

            final WrittenEvaluationEnrolment evalEnrolment = writtenEvaluation.getWrittenEvaluationEnrolmentFor(student);
            if (evalEnrolment != null) {
                assignedRoom = evalEnrolment.getRoom();
            }

            if (type.equals(EvaluationType.EXAM_TYPE)) {
                return new FenixCourseEvaluation.Exam(writtenEvaluationId, name, evaluationPeriod, isEnrolmentPeriod,
                        enrollmentPeriodStart, enrollmentPeriodEnd, writtenEvaluation.getAssociatedRooms(), isEnrolled,
                        Collections.singleton(executionCourse), assignedRoom);
            } else {
                return new FenixCourseEvaluation.Test(writtenEvaluationId, name, evaluationPeriod, isEnrolmentPeriod,
                        enrollmentPeriodStart, enrollmentPeriodEnd, writtenEvaluation.getAssociatedRooms(), isEnrolled,
                        Collections.singleton(executionCourse), assignedRoom);
            }
        }

        if (type.equals(EvaluationType.EXAM_TYPE)) {
            return new FenixCourseEvaluation.Exam(writtenEvaluationId, name, evaluationPeriod, isEnrolmentPeriod,
                    enrollmentPeriodStart, enrollmentPeriodEnd, writtenEvaluation.getAssociatedRooms(), isEnrolled,
                    Collections.singleton(executionCourse));
        } else {
            return new FenixCourseEvaluation.Test(writtenEvaluationId, name, evaluationPeriod, isEnrolmentPeriod,
                    enrollmentPeriodStart, enrollmentPeriodEnd, writtenEvaluation.getAssociatedRooms(), isEnrolled,
                    Collections.singleton(executionCourse));
        }

    }

    /**
     * All lessons and lesson period of course by id
     *

     * @param oid
     * @return
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("courses/{id}/schedule")
    public FenixSchedule scheduleCoursesByOid(@PathParam("id") String oid) {
        ExecutionCourse executionCourse = getDomainObject(oid, ExecutionCourse.class);
        return new FenixSchedule(executionCourse);
    }

    /**
     * Campus spaces
     *

     * @return
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("spaces")
    public List<FenixSpace> spaces() {

        List<FenixSpace> campi = new ArrayList<>();

        for (Space campus : Space.getTopLevelSpaces()) {
            campi.add(FenixSpace.getSimpleSpace(campus));
        }
        return campi;
    }

    /**
     * Space information regarding space type (Campus, Building, Floor or Room)
     *
     * @param oid
     * @param day
     *            ("dd/mm/yyyy")
     * @return
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("spaces/{id}")
    public FenixSpace spacesByOid(@PathParam("id") String oid, @QueryParam("day") String day) {

        Space space = getDomainObject(oid, Space.class);
        if (SpaceUtils.isRoom(space)) {
            return getFenixRoom(space, getRoomDay(day));
        }
        return FenixSpace.getSpace(space);
    }

    /**
     * Returns the blueprint of this space
     *
     * @param oid
     * @param format
     *            ("dd/mm/yyyy")
     * @return
     */
    @GET
    @Path("spaces/{id}/blueprint")
    public Response spaceBlueprint(@PathParam("id") String oid, final @QueryParam("format") String format) {

        final boolean isDwgFormat = format != null && format.equals("dwg");
        final Space space = getDomainObject(oid, Space.class);

        if (space == null) {
            return Response.noContent().build();
        }

        StreamingOutput stream;

        if (isDwgFormat) {
            Optional<BlueprintFile> optional = space.getBlueprintFile();
            if (!optional.isPresent()) {
                optional = SpaceBlueprintsDWGProcessor.getSuroundingSpaceMostRecentBlueprint(space).getBlueprintFile();
            }
            final InputStream inputStream = optional.get().getStream();
            stream = new StreamingOutput() {
                @Override
                public void write(OutputStream output) throws IOException, WebApplicationException {
                    ByteStreams.copy(inputStream, output);
                }
            };
        } else {
            stream = new StreamingOutput() {
                @Override
                public void write(OutputStream os) throws IOException, WebApplicationException {
                    Boolean isToViewOriginalSpaceBlueprint = false;
                    Boolean viewBlueprintNumbers = true;
                    Boolean isToViewIdentifications = true;
                    Boolean isToViewDoorNumbers = false;
                    BigDecimal scalePercentage = new BigDecimal(100);
                    DateTime now = new DateTime();
                    try {
                        SpaceBlueprintsDWGProcessor.writeBlueprint(space, now, isToViewOriginalSpaceBlueprint,
                                viewBlueprintNumbers, isToViewIdentifications, isToViewDoorNumbers, scalePercentage, os);
                    } catch (UnavailableException e) {
                        throw newApplicationError(Status.BAD_REQUEST, "problem found", "problem found");
                    }
                    os.flush();
                }
            };
        }
        final String contentType = isDwgFormat ? "application/dwg" : "image/jpeg";
        final String filename = space.getExternalId() + (isDwgFormat ? ".dwg" : ".jpg");
        return Response.ok(stream, contentType).header("Content-Disposition", "attachment; filename=" + filename).build();
    }

    private FenixSpace.Room getFenixRoom(Space room, java.util.Calendar rightNow) {

        InfoSiteRoomTimeTable bodyComponent = RoomSiteComponentBuilder.getInfoSiteRoomTimeTable(rightNow, room, null);
        List<FenixRoomEvent> roomEvents = new ArrayList<FenixRoomEvent>();

        try {
            for (Object occupation : bodyComponent.getInfoShowOccupation()) {
                InfoShowOccupation showOccupation = (InfoShowOccupation) occupation;
                DateTime date = new DateTime(rightNow);
                DateTime newDate = date.withDayOfWeek(showOccupation.getDiaSemana().getDiaSemanaInDayOfWeekJodaFormat());
                String day = formatDay.print(newDate);

                FenixRoomEvent roomEvent = null;

                if (showOccupation instanceof InfoLesson || showOccupation instanceof InfoLessonInstance) {
                    InfoShowOccupation lesson = showOccupation;
                    InfoExecutionCourse infoExecutionCourse = lesson.getInfoShift().getInfoDisciplinaExecucao();

                    String start = formatHour.print(lesson.getInicio().getTimeInMillis());
                    String end = formatHour.print(lesson.getFim().getTimeInMillis());
                    String weekday = lesson.getDiaSemana().getDiaSemanaString();

                    FenixPeriod period = new FenixPeriod(day + " " + start, day + " " + end);

                    String info = lesson.getInfoShift().getShiftTypesCodePrettyPrint();

                    FenixCourse course = new FenixCourse(infoExecutionCourse.getExecutionCourse());

                    roomEvent = new FenixRoomEvent.LessonEvent(start, end, weekday, day, period, info, course);

                } else if (showOccupation instanceof InfoWrittenEvaluation) {
                    InfoWrittenEvaluation infoWrittenEvaluation = (InfoWrittenEvaluation) showOccupation;

                    List<FenixCourse> courses = new ArrayList<>();

                    for (int iterEC = 0; iterEC < infoWrittenEvaluation.getAssociatedExecutionCourse().size(); iterEC++) {
                        InfoExecutionCourse infoEC = infoWrittenEvaluation.getAssociatedExecutionCourse().get(iterEC);
                        courses.add(new FenixCourse(infoEC.getExecutionCourse()));
                    }

                    String start = null;
                    String end = null;
                    String weekday = null;

                    if (infoWrittenEvaluation instanceof InfoExam) {
                        InfoExam infoExam = (InfoExam) infoWrittenEvaluation;
                        start = infoExam.getBeginningHour();
                        end = infoExam.getEndHour();
                        weekday = infoWrittenEvaluation.getDiaSemana().getDiaSemanaString();

                        FenixPeriod period = new FenixPeriod(day + " " + start, day + " " + end);

                        Integer season = infoExam.getSeason().getSeason();

                        roomEvent =
                                new FenixRoomEvent.WrittenEvaluationEvent.ExamEvent(start, end, weekday, day, period, courses,
                                        season);

                    } else if (infoWrittenEvaluation instanceof InfoWrittenTest) {
                        InfoWrittenTest infoWrittenTest = (InfoWrittenTest) infoWrittenEvaluation;
                        String description = infoWrittenTest.getDescription();
                        start = formatHour.print(infoWrittenTest.getInicio().getTimeInMillis());
                        end = formatHour.print(infoWrittenTest.getFim().getTimeInMillis());
                        weekday = infoWrittenTest.getDiaSemana().getDiaSemanaString();

                        FenixPeriod period = new FenixPeriod(day + " " + start, day + " " + end);

                        roomEvent =
                                new FenixRoomEvent.WrittenEvaluationEvent.TestEvent(start, end, weekday, day, period, courses,
                                        description);
                    }

                } else if (showOccupation instanceof InfoOccupation) {

                    InfoOccupation infoGenericEvent = (InfoOccupation) showOccupation;
                    String description = infoGenericEvent.getDescription();
                    String title = infoGenericEvent.getTitle();
                    String start = formatHour.print(infoGenericEvent.getInicio().getTimeInMillis());
                    String end = formatHour.print(infoGenericEvent.getFim().getTimeInMillis());;
                    String weekday = infoGenericEvent.getDiaSemana().getDiaSemanaString();
                    FenixPeriod period = new FenixPeriod(day + " " + start, day + " " + end);

                    roomEvent = new FenixRoomEvent.GenericEvent(start, end, weekday, day, period, description, title);
                }

                if (roomEvent != null) {
                    roomEvents.add(roomEvent);
                }

            }

            return new FenixSpace.Room(room, roomEvents);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw newApplicationError(Status.INTERNAL_SERVER_ERROR, "berserk!", "something went wrong");
        }
    }

    private java.util.Calendar getRoomDay(String day) {
        validateDay(day);
        java.util.Calendar rightNow = java.util.Calendar.getInstance();
        Date date = null;
        if (day != null && !Strings.isNullOrEmpty(day.trim())) {
            date = formatDay.parseDateTime(day).toDate();
            rightNow.setTime(date);
        }
        return rightNow;
    }

    private AcademicInterval getDefaultAcademicTerm() {
        return ExecutionSemester.readActualExecutionSemester().getAcademicInterval();
    }

    private AcademicInterval getAcademicInterval(String academicTerm) {
        return getAcademicInterval(academicTerm, false);
    }

    private AcademicInterval getAcademicInterval(String academicTerm, Boolean nullDefault) {

        if (Strings.isNullOrEmpty(academicTerm)) {
            return nullDefault ? null : getDefaultAcademicTerm();
        }

        ExecutionInterval interval = ExecutionInterval.getExecutionInterval(academicTerm);

        if (interval == null) {
            throw newApplicationError(Status.NOT_FOUND, "resource_not_found", "Can't find the academic term : " + academicTerm);
        }

        return interval.getAcademicInterval();

    }

    private void validateDay(String day) {
        if (day != null && !Strings.isNullOrEmpty(day.trim())) {
            boolean invalid = false;
            try {
                DateTime parse = formatDay.parseDateTime(day);
                invalid = parse == null;
            } catch (IllegalArgumentException | UnsupportedOperationException e) {
                invalid = true;
            } finally {
                if (invalid) {
                    throw newApplicationError(Status.BAD_REQUEST, "format_error", "day must be " + dayPattern);
                }
            }
        }
    }

    private void validateFormat(String format) {
        if (format != null && !Strings.isNullOrEmpty(format.trim())) {
            if (!("calendar".equals(format) || "json".equals(format))) {
                throw newApplicationError(Status.BAD_REQUEST, "format_error", "format must be calendar or json");
            }
        }
    }

    private void validateEnrol(String enrol) {
        if (enrol == null || Strings.isNullOrEmpty(enrol.trim()) || !(ENROL.equals(enrol) || UNENROL.equals(enrol))) {
            throw newApplicationError(Status.BAD_REQUEST, "format_error", "enrol must be yes or no");
        }
    }

    /**
     * information about the domain model implemented by this application
     *

     * @return domain model
     */
    @GET
    @Produces(JSON_UTF8)
    @Path("domainModel")
    public String domainModel() {
        return new FenixDomainModel().toJSONString();
    }

}