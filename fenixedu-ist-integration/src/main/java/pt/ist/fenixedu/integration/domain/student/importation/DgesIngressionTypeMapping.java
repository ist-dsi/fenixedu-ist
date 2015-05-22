package pt.ist.fenixedu.integration.domain.student.importation;

import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.bennu.core.domain.Bennu;

public class DgesIngressionTypeMapping extends DgesIngressionTypeMapping_Base {

    public DgesIngressionTypeMapping() {
        super();
    }

    public static IngressionType getIngressionType(String dgesCode) {
        return Bennu.getInstance().getDgesIngressionTypeMappingSet().stream().filter(dc -> dc.getDgesCode().equals(dgesCode))
                .findAny().get().getIngressionType();
    }
}
