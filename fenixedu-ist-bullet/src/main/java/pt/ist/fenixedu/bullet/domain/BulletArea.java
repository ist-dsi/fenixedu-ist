package pt.ist.fenixedu.bullet.domain;

import java.util.LinkedHashMap;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.bennu.core.domain.Bennu;

public class BulletArea extends BulletObject {
    Department department;

    private BulletArea(Department department) {
        this.department = department;
    }

    public static Stream<BulletArea> all(final DumpContext context) {
        //XXX Not including pre-bolonha department. No need to add an empty area for departmentless courses.
        return Bennu.getInstance().getDepartmentsSet().stream().filter(d -> !d.getName().contains("Bolonha")).map(BulletArea::new);
    }

    public static String key(Department department) {
        return department.getName();
    }

    @Override
    public void populateSlots(final DumpContext context, LinkedHashMap<String, String> slots) {
        slots.put(BulletObjectTag.NAME.unit(), key(department));
    }
}
