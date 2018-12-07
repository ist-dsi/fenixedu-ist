package pt.ist.fenixedu.bullet.domain;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.Teacher;

public class BulletCourse extends BulletObject {
    private ExecutionCourse course;

    private BulletCourse(ExecutionCourse course) {
        this.course = course;
    }

    public static Stream<BulletCourse> all(final DumpContext context) {
        return context.executions.stream().map(BulletCourse::new);
    }

    public static String key(ExecutionCourse course) {
        return course.getSigla();
    }

    @Override
    public void populateSlots(final DumpContext context, final LinkedHashMap<String, String> slots) {
        slots.put(BulletObjectTag.NAME.unit(), course.getName());
        slots.put(BulletObjectTag.ACRONYM.unit(), course.getSigla());
        slots.put(BulletObjectTag.CODE.unit(), key(course));
        //XXX Importance is assumed to be the same for now
        slots.put(BulletObjectTag.IMPORTANCE.unit(), String.valueOf(context.courseImportance));

        Set<Department> departments = course.getDepartments();
        if (departments.size() > 1) {
            Set<Department> teacherDepartments = course.getProfessorshipsSet().stream()
                    .map(Professorship::getTeacher).map(Teacher::getDepartment).collect(Collectors.toSet());
            departments.retainAll(teacherDepartments);
        }
        slots.put(BulletObjectTag.AREA.unit(), departments.isEmpty() ? "" : BulletArea.key(departments.iterator().next()));
    }
}
