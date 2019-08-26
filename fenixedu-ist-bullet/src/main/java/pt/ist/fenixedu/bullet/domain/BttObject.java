package pt.ist.fenixedu.bullet.domain;

import org.fenixedu.academic.domain.Lesson;
import pt.ist.fenixframework.Atomic;

public class BttObject extends BttObject_Base {
    
    private BttObject(String type) {
        setBttType(BttType.getType(type));
        setBttId(BttRoot.getInstance().nextBttId());
    }

    @Atomic
    static int getBttId(Lesson lesson) {
        BttObject bttObject = lesson.getBttObject();
        if(bttObject == null) {
            bttObject = new BttObject("Lesson");
            lesson.setBttObject(bttObject);
        }
        return bttObject.getBttId();
    }

    static Lesson getLesson(int bttId) {
        return BttRoot.getInstance().getBttTypeSet().stream()
                .filter(t -> t.getName().equals("Lesson"))
                .flatMap(t -> t.getBttObjectSet().stream())
                .filter(o -> o.getBttId() == bttId)
                .findAny().map(o -> o.getLesson()).orElse(null);
    }
}
