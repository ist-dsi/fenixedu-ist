package pt.ist.fenixedu.contracts.domain.accessControl;

import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.spaces.domain.Space;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */

@GroupOperator("campusGrantOwner")
public class CampusGrantOwnerGroup extends CampusSapGroup {

    private static final String[] SAP_GROUPS = new String[] { " Bolseiros", " Bols. Investigação" };

    public CampusGrantOwnerGroup() {
        super();
    }

    private CampusGrantOwnerGroup(Space campus) {
        super(campus);
    }

    public static Group get(final Space campus) {
        return new CampusGrantOwnerGroup(campus);
    }

    @Override
    public String[] getSapGroups() {
        return SAP_GROUPS;
    }

    @Override
    public PersistentGroup toPersistentGroup() {
        return PersistentCampusGrantOwnerGroup.getInstance(campus);
    }
}