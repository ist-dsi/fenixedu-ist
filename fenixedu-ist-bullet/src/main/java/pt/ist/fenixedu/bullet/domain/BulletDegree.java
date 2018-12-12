package pt.ist.fenixedu.bullet.domain;

import java.util.LinkedHashMap;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Degree;

public class BulletDegree extends BulletObject {

    private Degree degree;

    private BulletDegree(Degree degree) {
        this.degree = degree;
    }

    public static Stream<BulletDegree> all(final DumpContext context) {
        return context.degrees.stream().map(BulletDegree::new);
    }

    public static String key(Degree degree) {
        return degree.getCode();
    }

    @Override
    public void populateSlots(final DumpContext context, final LinkedHashMap<String, String> slots) {
        slots.put(BulletObjectTag.NAME.unit(), degree.getNameI18N().getContent());
        slots.put(BulletObjectTag.ACRONYM.unit(), degree.getSigla());
        slots.put(BulletObjectTag.CODE.unit(), key(degree));
    }

}
