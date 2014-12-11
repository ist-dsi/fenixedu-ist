package pt.ist.fenixedu.delegates.domain.accessControl;

import java.util.Optional;

import org.fenixedu.academic.domain.Degree;

public class PersistentDegreeDelegatesGroup extends PersistentDegreeDelegatesGroup_Base {

    public PersistentDegreeDelegatesGroup(Degree degree) {
        super();
        setDegree(degree);
        if (degree != null) {
            setRootForFenixPredicate(null);
        }
    }

    @Override
    public org.fenixedu.bennu.core.groups.Group toGroup() {
        return DegreeDelegatesGroup.get(getDegree());
    }

    @Override
    protected void gc() {
        setDegree(null);
        super.gc();
    }

    public static PersistentDegreeDelegatesGroup getInstance() {
        return getInstance(null);
    }

    public static PersistentDegreeDelegatesGroup getInstance(Degree degree) {
        return singleton(
                () -> degree == null ? find(PersistentDegreeDelegatesGroup.class) : Optional.ofNullable(degree
                        .getDegreeDelegatesGroup()), () -> new PersistentDegreeDelegatesGroup(degree));
    }

}
