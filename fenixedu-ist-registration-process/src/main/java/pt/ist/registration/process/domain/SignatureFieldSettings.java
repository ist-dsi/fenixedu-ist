package pt.ist.registration.process.domain;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
public class SignatureFieldSettings {
    private final int llx;
    private final int lly;
    private final int urx;
    private final int ury;
    private final String name;
    private final int page;

    public SignatureFieldSettings(int llx, int lly, int urx, int ury, String name, int page) {
        this.llx = llx;
        this.lly = lly;
        this.urx = urx;
        this.ury = ury;
        this.name = name;
        this.page = page;
    }

    public int getLlx() {
        return llx;
    }

    public int getLly() {
        return lly;
    }

    public int getUrx() {
        return urx;
    }

    public int getUry() {
        return ury;
    }

    public String getName() {
        return name;
    }

    public int getPage() { return page; }
}
