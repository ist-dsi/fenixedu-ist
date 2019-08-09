package pt.ist.fenixedu.contracts.domain.accessControl;

import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.spaces.domain.Space;

/**
 * @author Tiago Pinho
 */
@GroupOperator("campusResearcher")
public class CampusResearcherGroup extends CampusSapGroup {

    private static final String[] SAP_GROUPS = new String[] { " Investigadores" };

    public CampusResearcherGroup() {
        super();
    }

    private CampusResearcherGroup(Space campus) {
        super(campus);
    }

    public static Group get(final Space campus) {
        return new CampusResearcherGroup(campus);
    }

    @Override
    public String[] getSapGroups() {
        return SAP_GROUPS;
    }

    @Override
    public PersistentGroup toPersistentGroup() {
        return PersistentCampusResearcherGroup.getInstance(campus);
    }
}
