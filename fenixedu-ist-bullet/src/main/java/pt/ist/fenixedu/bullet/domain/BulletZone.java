package pt.ist.fenixedu.bullet.domain;

import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.spaces.domain.Space;

public class BulletZone extends BulletSpace {

    private BulletZone(Space space) {
        super(space);
    }

    public static Stream<BulletZone> all(final DumpContext context) {
        return Bennu.getInstance().getSpaceSet().stream().filter(BulletZone::isCampus).map(BulletZone::new);
    }

}
