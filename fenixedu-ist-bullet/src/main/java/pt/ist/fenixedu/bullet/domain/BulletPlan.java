package pt.ist.fenixedu.bullet.domain;

import org.fenixedu.academic.domain.*;
import org.fenixedu.academic.domain.degreeStructure.RegimeType;
import org.fenixedu.academic.domain.organizationalStructure.ScientificAreaUnit;
import org.fenixedu.academic.domain.student.Registration;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BulletPlan extends BulletObject {
    protected static final String CODE_DELIM = "/", NAME_DELIM = " - ";
    private DegreeCurricularPlan plan;
    private CurricularSemester semester;
    private String courseGroupName;
    private Set<CurricularCourse> courseGroup;

    private BulletPlan(DegreeCurricularPlan plan, CurricularSemester semester, Set<CurricularCourse> courseGroup, String courseGroupName) {
        //XXX Plans are not divided by specialization
        this.plan = plan;
        this.semester = semester;
        this.courseGroup = courseGroup;
        this.courseGroupName = courseGroupName;
    }

    public static Stream<BulletPlan> all(final DumpContext context) {
        return context.plans.stream().flatMap(dcp -> toPlansFor(context.baseSemester, dcp));
    }

    private static Stream<BulletPlan> toPlansFor(final ExecutionSemester executionSemester, final DegreeCurricularPlan degreeCurricularPlan) {
        final Map<CurricularSemester, Set<CurricularCourse>> coursesByYear = new HashMap<>();
/*
        final int numberOfYears = degreeCurricularPlan.getDurationInYears();
        for (int curricularYear = 1; curricularYear <= numberOfYears; curricularYear++) {
            coursesByYear.put(curricularYear, new HashSet<>());
        }
 */
        final Map<CurricularCourse, Set<Registration>> studentsByCourse = new HashMap<>();

        for (final CurricularCourse curricularCourse : degreeCurricularPlan.getCurricularCoursesSet()) {
            for (final DegreeModuleScope degreeModuleScope : curricularCourse.getDegreeModuleScopes()) {
                final CurricularSemester curricularSemester = CurricularSemester.readBySemesterAndYear(
                        degreeModuleScope.getCurricularSemester(), degreeModuleScope.getCurricularYear());
                if (curricularSemester.getSemester().intValue() == executionSemester.getSemester().intValue()) {
                    if (!coursesByYear.containsKey(curricularSemester)) {
                        coursesByYear.put(curricularSemester, new HashSet<>());
                    }
                    coursesByYear.get(curricularSemester).add(curricularCourse);
                }
            }
            final Set<Registration> registrations = curricularCourse.getCurriculumModulesSet().stream()
                    .filter(cm -> cm.isEnrolment())
                    .map(cm -> (Enrolment) cm)
                    .filter(e -> e.getExecutionYear().isCurrent())
                    .map(e -> e.getRegistration())
                    .collect(Collectors.toSet());
            studentsByCourse.put(curricularCourse, registrations);
        }

        final Map<String, Set<CurricularCourse>> ccMap = new HashMap<>();
        studentsByCourse.forEach((cc, set) -> {
            final Set<CurricularCourse> courses = new TreeSet<>((cc1, cc2) -> cc1.getExternalId().compareTo(cc2.getExternalId()));
            courses.add(cc);
            studentsByCourse.forEach((occ, oset) -> {
                if (cc != occ) {
                    final Set<Registration> intersection = new HashSet<>(set);
                    intersection.removeAll(oset);
                    if (intersection.size() >= (set.size() / 2)) {
                        courses.add(occ);
                    }
                }
            });
            if (courses.size() >= 5) {
                final String key = courses.stream()
                        .map(scc -> cc.getExternalId())
                        .collect(Collectors.joining(", "));
                ccMap.put(key, courses);
            }
        });

        final Set<BulletPlan> result = new HashSet<>();
        for (final Set<CurricularCourse> set : ccMap.values()) {
            final CurricularSemester curricularSemester = curricularYearFor(set, coursesByYear);
            if (curricularSemester != null) {
                result.add(new BulletPlan(degreeCurricularPlan, curricularSemester, set, "T" + (result.size() + 1)));
            }
        }
        return result.stream();
    }

    private static CurricularSemester curricularYearFor(final Set<CurricularCourse> set, final Map<CurricularSemester, Set<CurricularCourse>> coursesByYear) {
        for (final CurricularCourse curricularCourse : set) {
            for (final Map.Entry<CurricularSemester, Set<CurricularCourse>> e : coursesByYear.entrySet()) {
                if (e.getValue().contains(curricularCourse)) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    protected String key() {
        return courseGroupName == null ?
            String.join(CODE_DELIM, plan.getName(), semester.getCurricularYear().getYear().toString(), semester.getSemester().toString()) :
            String.join(CODE_DELIM, plan.getName(), semester.getCurricularYear().getYear().toString(), semester.getSemester().toString(), courseGroupName);
    }

    protected String getName() {
        return courseGroupName == null ? String.join(" - ", plan.getName(),
                semester.getCurricularYear().getYear().toString(),
                semester.getSemester().toString()) :
                String.join(" - ", plan.getName(),
                    courseGroupName,
                    semester.getCurricularYear().getYear().toString(),
                    semester.getSemester().toString());
    }

    public Stream<ExecutionCourse> getTargetExecutions(final DumpContext context) {
        //XXX It's safe to assume that multiple executions per semester and curricular are a thing of the past
        return courseGroup.stream()
                .flatMap(cc -> cc.getAssociatedExecutionCoursesSet().stream())
                .filter(ec -> ec.getExecutionPeriod() == context.baseSemester);
    }

    @Override
    public void populateSlots(final DumpContext context, final LinkedHashMap<String, String> slots) {
        slots.put(BulletObjectTag.NAME.unit(), getName());
        slots.put(BulletObjectTag.CODE.unit(), key());
        slots.put(BulletObjectTag.DEGREE_CODE.unit(), BulletDegree.key(plan.getDegree()));
        //XXX BTT concept of optional and mandatory courses do not match well with ours so for now all courses are considered mandatory
        slots.put(BulletObjectTag.MANDATORY_COURSE_CODE.unit(), getTargetExecutions(context).map(BulletCourse::key).collect(Collectors.joining(",")));
        slots.put(BulletObjectTag.OPTIONAL_COURSE_CODE.unit(), "");
    }

    public DegreeCurricularPlan getDegreeCurricularPlan() {
        return plan;
    }
}
