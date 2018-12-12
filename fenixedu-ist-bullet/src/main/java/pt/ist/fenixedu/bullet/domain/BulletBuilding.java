package pt.ist.fenixedu.bullet.domain;

import java.util.LinkedHashMap;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.spaces.domain.Space;

public class BulletBuilding extends BulletSpace {

    private BulletBuilding(Space space) {
        super(space);
    }

    public static Stream<BulletBuilding> all(final DumpContext context) {
        return Bennu.getInstance().getSpaceSet().stream().filter(BulletBuilding::isBuilding).map(BulletBuilding::new);
    }

    @Override
    public void populateSlots(final DumpContext context, LinkedHashMap<String, String> slots) {
        super.populateSlots(context, slots);
        slots.put(BulletObjectTag.ZONE.unit(), BulletZone.key(getCampus()));
    }

}
