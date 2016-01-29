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
package pt.ist.fenixedu.quc.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.Person;

import pt.ist.fenixedu.quc.domain.InquiryGlobalComment;
import pt.ist.fenixedu.quc.domain.InquiryResultComment;
import pt.ist.fenixedu.quc.domain.ResultPersonCategory;

public class DepartmentUCResultsBean extends CoordinatorResultsBean {

    private static final long serialVersionUID = 1L;

    public DepartmentUCResultsBean(ExecutionCourse executionCourse, ExecutionDegree executionDegree, Person president,
            boolean backToResume) {
        super(executionCourse, executionDegree, president, backToResume);
    }

    public List<InquiryResultComment> getAllCourseComments() {
        List<InquiryResultComment> commentsMade = new ArrayList<InquiryResultComment>();
        for (InquiryGlobalComment globalComment : getExecutionCourse().getInquiryGlobalCommentsSet()) {
            commentsMade.addAll(globalComment.getInquiryResultCommentsSet());
        }
        Collections.sort(commentsMade,
                Comparator.comparing(InquiryResultComment::getPerson, Comparator.comparing(Person::getName)));
        return commentsMade;
    }

    @Override
    protected ResultPersonCategory getPersonCategory() {
        return ResultPersonCategory.DEPARTMENT_PRESIDENT;
    }
}
