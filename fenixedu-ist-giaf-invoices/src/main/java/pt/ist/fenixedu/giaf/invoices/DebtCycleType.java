package pt.ist.fenixedu.giaf.invoices;

import org.fenixedu.academic.domain.degreeStructure.CycleType;

public enum DebtCycleType {

    FIRST_CYCLE, SECOND_CYCLE, INTEGRATED_MASTER, THIRD_CYCLE;

    static DebtCycleType valueOf(CycleType cycleType) {
        switch (cycleType) {
        case FIRST_CYCLE:
            return FIRST_CYCLE;
        case SECOND_CYCLE:
            return SECOND_CYCLE;
        case THIRD_CYCLE:
            return THIRD_CYCLE;
        default:
            return null;
        }
    }

    public String getDescription() {
        switch (this) {
        case FIRST_CYCLE:
            return CycleType.FIRST_CYCLE.getDescription();
        case SECOND_CYCLE:
            return CycleType.SECOND_CYCLE.getDescription();
        case THIRD_CYCLE:
            return CycleType.THIRD_CYCLE.getDescription();
        default:
//            return CycleType.SECOND_CYCLE.getDescription();
//            return DegreeType.BOLONHA_INTEGRATED_MASTER_DEGREE.getLocalizedName();
            return "Mestrado Integrado";
        }
    }

}
