package pt.ist.fenixedu.bullet.domain;

import java.util.LinkedHashMap;
import java.util.NoSuchElementException;

import org.fenixedu.spaces.domain.Space;

public class BulletSpace extends BulletObject {

    protected Space space;

    protected BulletSpace(Space space) {
        this.space = space;
    }


    protected static boolean isCampus(final Space space) {
        try {
            return "Campus".equals(space.getClassification().getName().getContent());
        } catch (NoSuchElementException ex) {
            return false;
        }
    }

    protected static Space campusFor(final Space space) {
        return space == null || isCampus(space) ? space : campusFor(space.getParent());
    }

    protected static boolean isBuilding(final Space space) {
        try {
            final String name = space.getClassification().getName().getContent();
            return "Building".equals(name) || "Edifício".equals(name);
        } catch (NoSuchElementException ex) {
            return false;
        }
    }

    private static Space buildingFor(final Space space) {
        return space == null || isBuilding(space) ? space : buildingFor(space.getParent());
    }

    protected static boolean isLevel(final Space space) {
        try {
            final String name = space.getClassification().getName().getContent();
            return "Floor".equals(name) || "Piso".equals(name);
        } catch (NoSuchElementException ex) {
            return false;
        }
    }

    private static Space levelFor(final Space space) {
        return space == null || isLevel(space) ? space : levelFor(space.getParent());
    }

    protected static String levelName(final Space space) {
        return space == null ? "" : levelName(space.getParent(), space.getName());
    }

    private static String levelName(final Space space, final String childName) {
        if (space == null) {
            return childName;
        }
        final String name = isLevel(space) ? space.getName() + '.' + childName : childName;
        return levelName(space.getParent(), name);
    }

    protected Space getCampus() {
        return campusFor(space);
    }

    protected Space getBuilding() {
        return buildingFor(space);
    }

    protected Space getLevel() {
        return levelFor(space);
    }

    public static String key(Space space) {
        return space.getName();
    }

    @Override
    public void populateSlots(final DumpContext context, final LinkedHashMap<String, String> slots) {
        slots.put(BulletObjectTag.NAME.unit(), key(space));
    }

}
