package pt.ist.fenixedu.integration.api.internal;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentPeriod;
import org.fenixedu.academic.domain.EnrolmentPeriodInCurricularCourses;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academic.service.services.administrativeOffice.enrolment.MultiSemesterEnrolmentReporter;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.rest.BennuRestResource;
import org.fenixedu.bennu.oauth.annotation.OAuthEndpoint;
import org.joda.time.DateTime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@Path("/_internal/analytics/enrolment")
public class EnrolmentStatisticsResource extends BennuRestResource {

    private static boolean isDegree(final DegreeCurricularPlan degreeCurricularPlan) {
        final DegreeType degreeType = degreeCurricularPlan.getDegreeType();
        return degreeType.isBolonhaDegree() || degreeType.isBolonhaMasterDegree() || degreeType.isIntegratedMasterDegree();
    }

    @GET
    @Path("/by-degree")
    @OAuthEndpoint("_internal")
    public String statisticsByDegree() {
        accessControl(Group.managers());

        final Map<StudentCurricularPlan, Set<Enrolment>> studentsEnrolments = new HashMap<>();
        final Map<DegreeCurricularPlan, GenericPair<AtomicInteger, AtomicInteger>> degreesMap =
                new TreeMap<>(DegreeCurricularPlan.COMPARATOR_BY_NAME);
        final Map<Integer, AtomicInteger> enrolmentsNumber = new TreeMap<>();
        int totalEnrolments = 0;

        final ExecutionSemester executionPeriod = ExecutionSemester.readActualExecutionSemester();
        for (final Enrolment enrolment : executionPeriod.getEnrolmentsSet()) {
            final StudentCurricularPlan studentCurricularPlan = enrolment.getStudentCurricularPlan();
            if (isDegree(studentCurricularPlan.getDegreeCurricularPlan())) {
                studentsEnrolments.computeIfAbsent(studentCurricularPlan, k -> new HashSet<>()).add(enrolment);
            }
        }

        for (final Entry<StudentCurricularPlan, Set<Enrolment>> studentEnrolmentEntry : studentsEnrolments.entrySet()) {
            final Set<Enrolment> enrolments = studentEnrolmentEntry.getValue();
            totalEnrolments += enrolments.size();

            enrolmentsNumber.computeIfAbsent(enrolments.size(), k -> new AtomicInteger()).incrementAndGet();

            final StudentCurricularPlan studentCurricularPlan = studentEnrolmentEntry.getKey();
            GenericPair<AtomicInteger, AtomicInteger> genericPair = degreesMap
                    .computeIfAbsent(studentCurricularPlan.getDegreeCurricularPlan(),
                            k -> new GenericPair<>(new AtomicInteger(), new AtomicInteger()));
            genericPair.getLeft().incrementAndGet();
            genericPair.getRight().addAndGet(enrolments.size());
        }

        JsonObject json = new JsonObject();
        json.addProperty("semester", executionPeriod.getQualifiedName());
        json.addProperty("totalEnrolments", totalEnrolments);

        JsonObject enrolmentDistribution = new JsonObject();
        enrolmentsNumber.entrySet().stream()
                .forEach(entry -> enrolmentDistribution.addProperty(String.valueOf(entry.getKey()), entry.getValue().get()));
        json.add("enrolmentDistribution", enrolmentDistribution);
        json.addProperty("totalEnroledStudents", studentsEnrolments.keySet().size());

        JsonArray enrolmentsByDegree = new JsonArray();

        degreesMap.entrySet().stream().forEach(entry -> {
            JsonObject degreeJson = new JsonObject();
            degreeJson.addProperty("name", entry.getKey().getName());
            degreeJson.addProperty("enroledStudents", entry.getValue().getLeft().get());
            degreeJson.addProperty("totalEnrolments", entry.getValue().getRight().get());
            enrolmentsByDegree.add(degreeJson);
        });

        json.add("enrolmentsByDegree", enrolmentsByDegree);

        return json.toString();
    }

    @GET
    @Path("/over-time")
    @OAuthEndpoint("_internal")
    public String statisticsOverTime(@DefaultValue("1") @QueryParam("hoursToReport") int hoursToReport,
            @DefaultValue("10") @QueryParam("yearsToReport") int yearsToReport) {
        accessControl(Group.managers());
        ExecutionSemester semester = ExecutionSemester.readActualExecutionSemester();
        final MultiSemesterEnrolmentReporter reporter = new MultiSemesterEnrolmentReporter(hoursToReport, semester.getSemester());

        while (yearsToReport > 0 && semester != null) {
            DateTime enrolmentStartTime = findEnrolmentStartTimeFor(semester);
            if (enrolmentStartTime != null) {
                reporter.report(enrolmentStartTime.getYear(), enrolmentStartTime.getMonthOfYear(),
                        enrolmentStartTime.getDayOfMonth(), enrolmentStartTime.getHourOfDay());
            }
            yearsToReport--;
            // Go back two semesters
            semester = semester.getPreviousExecutionPeriod();
            semester = semester == null ? null : semester.getPreviousExecutionPeriod();
        }

        return reporter.getStats().toString();
    }

    private DateTime findEnrolmentStartTimeFor(ExecutionSemester semester) {
        Map<DateTime, List<EnrolmentPeriod>> dates = semester.getEnrolmentPeriodSet().stream()
                .filter(period -> period.getClass().equals(EnrolmentPeriodInCurricularCourses.class))
                .collect(Collectors.groupingBy(EnrolmentPeriod::getStartDateDateTime));
        return dates.entrySet().stream().max(Comparator
                .comparing(entry -> entry.getValue().stream().filter(p -> isDegree(p.getDegreeCurricularPlan())).count()))
                .map(Entry::getKey).orElse(null);
    }

}
