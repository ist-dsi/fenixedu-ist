package pt.ist.fenixedu.bullet.domain;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.space.SpaceUtils;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.spaces.domain.Space;

public class BulletRoom extends BulletSpace {

    private BulletRoom(Space space) {
        super(space);
    }

    public static Stream<BulletRoom> all(final DumpContext context) {
        //XXX SpaceUtils forEducation method is user relative
        User GOPUser = User.findByUsername("ist22986"); // Suzana Visenjou
        Authenticate.mock(GOPUser, "Script");
        Stream<BulletRoom> rooms = SpaceUtils.allocatableSpacesForEducation().filter(s -> s.getAllocatableCapacity() != 0).map(BulletRoom::new);
        Authenticate.unmock();
        return rooms;
    }

    @Override
    public void populateSlots(final DumpContext context, final LinkedHashMap<String, String> slots) {
        super.populateSlots(context, slots);
        slots.put(BulletObjectTag.BUILDING.unit(), BulletBuilding.key(getBuilding()));
        slots.put(BulletObjectTag.LEVEL.unit(), BulletLevel.key(getLevel()));
        slots.put(BulletObjectTag.ROOM_FULL_PATH.unit(), roomFullPath());
        slots.put(BulletObjectTag.CAPACITY.unit(), String.valueOf(space.getAllocatableCapacity()));
        slots.put(BulletObjectTag.EXAM_CAPACITY.unit(), String.valueOf(space.<Integer>getMetadata("examCapacity").orElse(0)));
        slots.put(BulletObjectTag.ACCEPTANCE_MARGIN.unit(), String.valueOf(space.getAllocatableCapacity() / context.acceptanceMarginPercent));
        slots.put(BulletObjectTag.CHARACTERISTIC.unit(), BulletCharacteristic.getCharacteristics(space).collect(Collectors.joining(", ")));
    }

    private String roomFullPath() {
        return BulletZone.key(topLevelSpace(space))
                + " > " + BulletBuilding.key(getBuilding())
                + " > " + BulletLevel.key(getLevel())
                + " > " + space.getName();
    }

    private Space topLevelSpace(final Space space) {
        final Space parent = space.getParent();
        return parent == null ? space : topLevelSpace(parent);
    }

}
