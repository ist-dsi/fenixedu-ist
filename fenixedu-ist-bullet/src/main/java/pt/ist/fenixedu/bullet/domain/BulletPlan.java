package pt.ist.fenixedu.bullet.domain;

import org.fenixedu.academic.domain.*;
import org.fenixedu.academic.domain.degreeStructure.RegimeType;
import org.fenixedu.academic.domain.organizationalStructure.ScientificAreaUnit;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BulletPlan extends BulletObject {
    protected static final String CODE_DELIM = "/", NAME_DELIM = " - ";
    private DegreeCurricularPlan plan;
    private CurricularSemester semester;
    private String courseGroup;

    private BulletPlan(DegreeCurricularPlan plan, CurricularSemester semester, String courseGroup) {
        //XXX Plans are not divided by specialization
        this.plan = plan;
        this.semester = semester;
        this.courseGroup = courseGroup;
    }

    public static Stream<BulletPlan> all(final DumpContext context) {
        return context.plans.stream().flatMap(dcp -> toPlansFor(context.baseSemester, dcp));
    }

    private static Stream<BulletPlan> toPlansFor(final ExecutionSemester executionSemester, final DegreeCurricularPlan degreeCurricularPlan) {
        final Set<String> set = new HashSet<>();
        final Set<BulletPlan> result = new HashSet<>();
        for (final CurricularCourse curricularCourse : degreeCurricularPlan.getCurricularCoursesSet()) {
            final CompetenceCourse competenceCourse = curricularCourse.getCompetenceCourse();
            final ScientificAreaUnit scientificAreaUnit = competenceCourse == null ? null : competenceCourse.getScientificAreaUnit(executionSemester);
            final String courseGroup = scientificAreaUnit == null ? null : scientificAreaUnit.getAcronym();
            for (final DegreeModuleScope degreeModuleScope : curricularCourse.getDegreeModuleScopes()) {
                final CurricularSemester curricularSemester = CurricularSemester.readBySemesterAndYear(
                        degreeModuleScope.getCurricularSemester(), degreeModuleScope.getCurricularYear());
                final String key = (courseGroup == null ? "" : courseGroup)
                        + curricularSemester.getCurricularYear().getYear()
                        + curricularSemester.getSemester();
                if (!set.contains(key)) {
                    set.add(key);
                    result.add(new BulletPlan(degreeCurricularPlan, curricularSemester, courseGroup));
                }
            }
        }
        return result.stream();
    }

    protected String key() {
        return courseGroup == null ?
            String.join(CODE_DELIM, plan.getName(), semester.getCurricularYear().getYear().toString(), semester.getSemester().toString()) :
            String.join(CODE_DELIM, plan.getName(), semester.getCurricularYear().getYear().toString(), semester.getSemester().toString(), courseGroup);
    }

    protected String getName() {
        return courseGroup == null ? String.join(" - ", plan.getPresentationName(),
                semester.getCurricularYear().getYear() + "º Ano",
                semester.getSemester() + "º Semestre") :
                String.join(" - ", plan.getPresentationName(),
                semester.getCurricularYear().getYear() + "º Ano",
                semester.getSemester() + "º Semestre",
                courseGroup);
    }

    public Stream<ExecutionCourse> getTargetExecutions(final DumpContext context) {
        //XXX It's safe to assume that multiple executions per semester and curricular are a thing of the past
        return plan.getExecutionCoursesByExecutionPeriod(context.baseSemester).stream()
                .filter(ec -> ec.getCurricularCourseFor(plan).getParentContextsSet().stream().anyMatch(c -> c.isOpen(context.baseSemester)
                        && c.containsSemesterAndCurricularYear(semester.getSemester(), semester.getCurricularYear().getYear(), RegimeType.SEMESTRIAL)));
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
