package pt.ist.fenixedu.bullet.domain;

public class BttType extends BttType_Base {
    
    public BttType(String name) {
        if(name == null) {
            throw new NullPointerException("BTT Type cannot be null");
        }
        if(BttRoot.getInstance().getBttTypeSet().stream().anyMatch(t -> t.getName().equals(name))) {
            throw new Error("Type " + name + " already exists");
        }
        setName(name);
        setBttRoot(BttRoot.getInstance());
    }

    static BttType getType(String name) {
        return BttRoot.getInstance().getBttTypeSet().stream().filter(t -> t.getName().equals(name)).findAny().orElseGet(() -> new BttType(name));
    }

}
