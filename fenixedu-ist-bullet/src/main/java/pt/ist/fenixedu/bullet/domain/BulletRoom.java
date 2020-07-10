package pt.ist.fenixedu.bullet.domain;

import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.spaces.domain.Space;
import org.fenixedu.spaces.domain.SpaceClassification;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BulletRoom extends BulletSpace {

    public BulletRoom(Space space) {
        super(space);
    }

    public static Stream<BulletRoom> all(final DumpContext context) {
        //XXX SpaceUtils forEducation method is user relative
        final User GOPUser = User.findByUsername("ist22986"); // Suzana Visenjou
        Authenticate.mock(GOPUser, "Script");
/*
        Stream<BulletRoom> rooms = SpaceUtils.allocatableSpacesForEducation()
                .filter(s -> s.getAllocatableCapacity() != 0)
                .filter(s -> isForEducation(s))
                .map(BulletRoom::new);
 */

        Stream<BulletRoom> rooms = Stream.of(context.baseSemester.getPreviousExecutionPeriod(), context.baseSemester)
                .flatMap(es -> es.getAssociatedExecutionCoursesSet().stream())
                .flatMap(ec -> ec.getCourseLoadsSet().stream())
                .flatMap(cl -> cl.getShiftsSet().stream())
                .flatMap(s -> s.getAssociatedLessonsSet().stream())
                .flatMap(l -> roomStream(l))
                .distinct()
                .map(BulletRoom::new);

        Authenticate.unmock();
        return rooms;
    }

    private static Stream<Space> roomStream(final Lesson lesson) {
        return Stream.concat(lesson.getLessonInstancesSet().stream().map(i -> i.getRoom()).filter(r -> r != null),
                Stream.of(lesson).map(l -> l.getRoomOccupation()).filter(o -> o != null).map(o -> o.getRoom()));
    }

    private static boolean isForEducation(final Space space) {
        final SpaceClassification spaceClassification = space.getClassification();
        final String code = spaceClassification == null ? null : spaceClassification.getAbsoluteCode();
        return code != null && (code.startsWith("1.") || code.startsWith("2."));
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

    public String roomFullPath() {
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
