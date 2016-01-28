package pt.ist.fenixedu.integration.dto;

import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.ShiftType;

import pt.ist.fenixedu.quc.domain.InquiryResultType;
import pt.ist.fenixedu.teacher.evaluation.service.external.ProfessorshipEvaluation;

import com.google.common.base.Strings;

public class QucProfessorshipEvaluation implements ProfessorshipEvaluation {

    @Override
    public Double getProfessorshipEvaluation(Professorship professorship, ShiftType shiftType) {
        return professorship
                .getInquiryResultsSet()
                .stream()
                .filter(ir -> ir.getShiftType().equals(shiftType) && ir.getResultType() != null
                        && ir.getResultType().equals(InquiryResultType.TEACHER_EVALUATION)
                        && !Strings.isNullOrEmpty(ir.getValue())).map(ir -> Double.valueOf(ir.getValue())).findAny().orElse(null);
    }

}
