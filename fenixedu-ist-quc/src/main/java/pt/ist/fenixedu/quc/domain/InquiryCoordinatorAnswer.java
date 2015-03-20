/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST QUC.
 *
 * FenixEdu IST QUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST QUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST QUC.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.quc.domain;

import org.fenixedu.academic.domain.Coordinator;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;

public class InquiryCoordinatorAnswer extends InquiryCoordinatorAnswer_Base {

    public InquiryCoordinatorAnswer(ExecutionDegree executionDegree, ExecutionSemester executionSemester) {
        super();
        setExecutionDegree(executionDegree);
        setExecutionSemester(executionSemester);
    }

    public static InquiryCoordinatorAnswer getInquiryCoordinationAnswers(ExecutionDegree executionDegree,
            ExecutionSemester executionSemester) {
        for (InquiryCoordinatorAnswer inquiryCoordinatorAnswer : executionDegree.getInquiryCoordinationAnswersSet()) {
            if (inquiryCoordinatorAnswer.getExecutionSemester() == executionSemester) {
                return inquiryCoordinatorAnswer;
            }
        }
        return null;
    }

    public static InquiryCoordinatorAnswer getInquiryCoordinatorAnswer(Coordinator coordinator,
            ExecutionSemester executionSemester) {
        for (InquiryCoordinatorAnswer inquiryCoordinatorAnswer : coordinator.getInquiryCoordinatorAnswersSet()) {
            if (inquiryCoordinatorAnswer.getExecutionSemester() == executionSemester) {
                return inquiryCoordinatorAnswer;
            }
        }
        return null;
    }

}
