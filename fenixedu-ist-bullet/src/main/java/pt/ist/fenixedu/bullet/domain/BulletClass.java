package pt.ist.fenixedu.bullet.domain;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionCourse;

public class BulletClass extends BulletObject {
    private BulletPlan plan;
    private String id;
    private int students;

    private BulletClass(BulletPlan plan, String id, int students) {
        this.plan = plan;
        this.id = id;
        this.students = students;
    }

    public static Stream<BulletClass> all(final DumpContext context) {
        //XXX we're estimating based on number of shifts however we should also regard number of enrolments with respect to the class's students limit
        return context.all(BulletPlan.class).stream().flatMap(plan -> {
            Set<ExecutionCourse> courses = plan.getTargetExecutions(context).collect(Collectors.toSet());
            int totalClasses = courses.stream().flatMap(ec -> ec.getShiftTypes().stream().map(ec::getNumberOfShifts)).mapToInt(Integer::intValue).max().orElse(0);
            if (totalClasses == 0) {
                //XXX usually happens for master's last semester and third cycle plans and these shouldn't have any loads either, so it should be ok to have no classes at generation time
                return Stream.of();
            }
            double totalStudents = courses.stream().map(ExecutionCourse::getEnrolmentCount).mapToInt(Integer::intValue).max().getAsInt();
            int expectedPerClass = new Double(Math.ceil(totalStudents / totalClasses)).intValue();
            return IntStream.range(1, totalClasses + 1).mapToObj(id -> new BulletClass(plan, String.valueOf(id), expectedPerClass));
        });
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
