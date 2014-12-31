package pt.ist.fenixedu.delegates.domain.accessControl;

import java.util.Optional;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.bennu.core.groups.Group;

import com.google.common.base.Objects;

public class PersistentDelegateGroup extends PersistentDelegateGroup_Base {

    protected PersistentDelegateGroup(Degree degree, Boolean yearDelegate) {
        super();
        setDegree(degree);
        setYearDelegate(yearDelegate);
        if (degree != null) {
            setRootForFenixPredicate(null);
        }
    }

    @Override
    public Group toGroup() {
        return DelegateGroup.get(getDegree(), getYearDelegate());
    }

    @Override
    protected void gc() {
        setDegree(null);
        super.gc();
    }

    public static PersistentDelegateGroup getInstance(Degree degree, Boolean yearDelegate) {
        return singleton(() -> select(degree, yearDelegate), () -> new PersistentDelegateGroup(degree, yearDelegate));
    }

    private static Optional<PersistentDelegateGroup> select(Degree degree, Boolean yearDelegate) {
        if (degree != null) {
            return degree.getDelegatesGroupSet().stream().filter(g -> Objects.equal(g.getYearDelegate(), yearDelegate)).findAny();
        }
        return filter(PersistentDelegateGroup.class).filter(g -> Objects.equal(g.getYearDelegate(), yearDelegate)).findAny();
    }
}
