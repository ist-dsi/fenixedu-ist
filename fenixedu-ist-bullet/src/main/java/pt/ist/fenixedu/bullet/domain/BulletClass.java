package pt.ist.fenixedu.bullet.domain;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.SchoolClass;

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
        final long numbberOfStudents = schoolClass.getAssociatedShiftsSet().stream()
                .flatMap(shift -> shift.getShiftEnrolmentsSet().stream())
                .map(shiftEnrolment -> shiftEnrolment.getRegistration().getStudent())
                .distinct()
                .count();
        return Math.toIntExact(numbberOfStudents);
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
