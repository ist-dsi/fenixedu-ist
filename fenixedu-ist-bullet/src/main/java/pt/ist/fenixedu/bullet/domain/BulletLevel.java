package pt.ist.fenixedu.bullet.domain;

import java.util.LinkedHashMap;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.spaces.domain.Space;

public class BulletLevel extends BulletSpace {

    String name;

    private BulletLevel(Space space) {
        super(space);
        this.name = levelName(space);
    }

    public static Stream<BulletLevel> all(final DumpContext context) {
        return Bennu.getInstance().getSpaceSet().stream().filter(BulletLevel::isLevel).map(BulletLevel::new).distinct();
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof BulletLevel && ((BulletLevel) o).name.equals(this.name);
    }

    public static String key(Space space) {
        return levelName(space);
    }

    @Override
    public void populateSlots(final DumpContext context, final LinkedHashMap<String, String> slots) {
        slots.put(BulletObjectTag.NAME.unit(), this.name);
    }

}
