package pt.ist.fenixedu.bullet.domain;

import org.fenixedu.bennu.core.domain.Bennu;
import pt.ist.fenixframework.Atomic;

public class BttRoot extends BttRoot_Base {
    
    private BttRoot() {
        setCounter(700000000);
        setBennu(Bennu.getInstance());
    }

    static BttRoot getInstance() {
        final BttRoot root = Bennu.getInstance().getBttRoot();
        return root == null ? createInstance() : root;
    }

    @Atomic
    static private BttRoot createInstance() {
        final BttRoot root = Bennu.getInstance().getBttRoot();
        return root == null ? new BttRoot() : root;
    }

    int nextBttId() {
        final int counter = 1 + getCounter();
        if(counter >= 1000000000) {
            throw new Error("Exceeded max BTT_Id value");
        }
        setCounter(counter);
        return counter;
    }

    @Atomic
    void resetCounter() {
        getBttTypeSet().stream()
            .flatMap(t -> t.getBttObjectSet().stream())
            .forEach(o -> o.delete());

        setCounter(700000000);
    }
}
