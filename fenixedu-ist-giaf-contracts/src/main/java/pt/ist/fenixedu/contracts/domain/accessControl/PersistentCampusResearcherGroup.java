package pt.ist.fenixedu.contracts.domain.accessControl;

import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.spaces.domain.Space;

import java.util.Optional;

/**
 * @author Tiago Pinho
 */
public class PersistentCampusResearcherGroup extends PersistentCampusResearcherGroup_Base {

    public PersistentCampusResearcherGroup(Space campus) {
        super();
        setCampus(campus);
    }

    @Override
    public Group toGroup() {
        return CampusResearcherGroup.get(getCampus());
    }

    @Override
    protected void gc() {
        setCampus(null);
        super.gc();
    }

    public static PersistentCampusResearcherGroup getInstance(Space campus) {
        return singleton(() -> Optional.ofNullable(campus.getCampusResearcherGroup()), () -> new PersistentCampusResearcherGroup(
                campus));
    }

}
