package pt.ist.fenixedu.bullet.domain;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CurricularSemester;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.degreeStructure.RegimeType;

public class BulletPlan extends BulletObject {
    protected static final String CODE_DELIM = "/", NAME_DELIM = " - ";
    private DegreeCurricularPlan plan;
    private CurricularSemester semester;

    private BulletPlan(DegreeCurricularPlan plan, CurricularSemester semester) {
        //XXX Plans are not divided by specialization
        this.plan = plan;
        this.semester = semester;
    }

    public static Stream<BulletPlan> all(final DumpContext context) {
        return context.plans.stream()
                .flatMap(plan -> context.semesters.stream()
                        .filter(semester -> plan.getDegreeModuleScopes().stream()
                                .map(scope -> CurricularSemester.readBySemesterAndYear(scope.getCurricularSemester(), scope.getCurricularYear()))
                                .collect(Collectors.toSet()).contains(semester))
                        .map(semester -> new BulletPlan(plan, semester)));
    }

    protected String key() {
        return String.join(CODE_DELIM, plan.getName(), semester.getCurricularYear().getYear().toString(), semester.getSemester().toString());
    }

    protected String getName() {
        return String.join(" - ", plan.getPresentationName(), semester.getCurricularYear().getYear() + "º Ano", semester.getSemester() + "º Semestre");
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
}
