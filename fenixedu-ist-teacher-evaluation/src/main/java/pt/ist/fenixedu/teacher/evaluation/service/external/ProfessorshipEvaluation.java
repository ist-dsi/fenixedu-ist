package pt.ist.fenixedu.teacher.evaluation.service.external;

import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.ShiftType;

public interface ProfessorshipEvaluation {

    public Double getProfessorshipEvaluation(Professorship professorship, ShiftType shiftType);
}
