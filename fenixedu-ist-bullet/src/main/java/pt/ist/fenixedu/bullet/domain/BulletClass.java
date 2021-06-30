package pt.ist.fenixedu.bullet.domain;

import org.fenixedu.academic.domain.*;

import java.util.LinkedHashMap;
import java.util.stream.Stream;

public class BulletClass extends BulletObject {
    private BulletPlan plan;
    private String id;
    private int students;

    private BulletClass(BulletPlan plan, String id, int students) {
        this.plan = plan;
        this.id = id;
        this.students = students;
    }

    private static ExecutionSemester getExecutionSemester(final DumpContext context) {
        final ExecutionSemester previous = context.baseSemester;
        return previous == null ? null : previous.getPreviousExecutionPeriod();
    }

    private static int numberOfStudentsInClass(final SchoolClass schoolClass) {
        final int numbberOfStudents = schoolClass.getAssociatedShiftsSet().stream()
                .filter(shift -> !shift.getTypes().contains(ShiftType.TEORICA))
                .mapToInt(shift -> Math.round(factorFor(schoolClass, shift) * Math.abs(shift.getLotacao().intValue())))
                .filter(capacity -> capacity > 0)
                .min().orElse(0);
        return numbberOfStudents;
    }

    private static float factorFor(final SchoolClass schoolClass, final Shift shift) {
        final int otherStudentCount = shift.getAssociatedClassesSet().stream()
                .map(someClass -> someClass.getExecutionDegree())
                .distinct()
                .filter(executionDegree -> executionDegree != schoolClass.getExecutionDegree())
                .mapToInt(executionDegree -> studentCount(executionDegree))
                .sum();
        final int studentCount = studentCount(schoolClass.getExecutionDegree());
        return otherStudentCount == 0 ? 1 : studentCount == 0 ? 0 : (studentCount / (studentCount + otherStudentCount));
    }

    private static int studentCount(final ExecutionDegree executionDegree) {
        final DegreeCurricularPlan dcp = executionDegree.getDegreeCurricularPlan();
        return Math.toIntExact(dcp.getStudentCurricularPlansSet().stream()
                .map(scp -> scp.getRegistration())
                .filter(r -> r.isActive())
                .count());
    }

    public static Stream<BulletClass> all(final DumpContext context) {
        final ExecutionSemester targetSemester = getExecutionSemester(context);
        if (targetSemester == null) {
            return Stream.empty();
        }
        return context.all(BulletPlan.class).stream().flatMap(plan ->
            plan.getDegreeCurricularPlan().getExecutionDegreesSet().stream()
                    .filter(ed -> ed.getExecutionYear().getExecutionPeriodsSet().contains(targetSemester))
                    .flatMap(ed -> ed.getSchoolClassesSet().stream())
                    .filter(schoolClass -> schoolClass.getExecutionPeriod() == targetSemester)
                    .map(schoolClass -> new BulletClass(plan, schoolClass.getNome(), numberOfStudentsInClass(schoolClass))));
    }

    @Override
    public void populateSlots(final DumpContext context, final LinkedHashMap<String, String> slots) {
        slots.put(BulletObjectTag.NAME.unit(), String.join(BulletPlan.NAME_DELIM, plan.getName(), id));
        slots.put(BulletObjectTag.PLAN_CODE.unit(), plan.key()); //FIXME urgh this is coexisting with a contract of a static key method in BS objects
        slots.put(BulletObjectTag.NUMBER_STUDENTS.unit(), String.valueOf(students));
        slots.put(BulletObjectTag.MAX_LIMIT.unit(), String.valueOf(context.classHoursLimit));
        slots.put(BulletObjectTag.CONSECUTIVE_LIMIT.unit(), String.valueOf(context.consecutiveClassHoursLimit));
    }

}
