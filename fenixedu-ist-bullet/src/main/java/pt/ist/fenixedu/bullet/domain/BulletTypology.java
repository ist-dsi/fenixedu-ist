package pt.ist.fenixedu.bullet.domain;

import java.util.LinkedHashMap;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ShiftType;

public class BulletTypology extends BulletObject {
    
    private ShiftType type;

    private BulletTypology(ShiftType type) {
        this.type = type;
    }

    public static Stream<BulletTypology> all(final DumpContext context) {
        return Stream.of(ShiftType.values()).map(BulletTypology::new);
    }

    public static String key(ShiftType type) {
        return type.getSiglaTipoAula();
    }

    @Override
    public void populateSlots(final DumpContext context, final LinkedHashMap<String, String> slots) {
        slots.put(BulletObjectTag.NAME.unit(), key(type));
        slots.put(BulletObjectTag.DESCRIPTION.unit(), type.getFullNameTipoAula());
    }
}
