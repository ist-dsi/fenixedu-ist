/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
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
