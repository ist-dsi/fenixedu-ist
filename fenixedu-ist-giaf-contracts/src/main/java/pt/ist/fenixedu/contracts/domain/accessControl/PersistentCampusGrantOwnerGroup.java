package pt.ist.fenixedu.contracts.domain.accessControl;

import java.util.Optional;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.spaces.domain.Space;

public class PersistentCampusGrantOwnerGroup extends PersistentCampusGrantOwnerGroup_Base {
    
    public PersistentCampusGrantOwnerGroup(Space campus) {
        super();
        setCampus(campus);
    }

    @Override
    public Group toGroup() {
        return CampusGrantOwnerGroup.get(getCampus());
    }

    @Override
    protected void gc() {
        setCampus(null);
        super.gc();
    }

    public static PersistentCampusGrantOwnerGroup getInstance(Space campus) {
        return singleton(() -> Optional.ofNullable(campus.getCampusGrantOwnerGroup()), () -> new PersistentCampusGrantOwnerGroup(
                campus));
    }
}
