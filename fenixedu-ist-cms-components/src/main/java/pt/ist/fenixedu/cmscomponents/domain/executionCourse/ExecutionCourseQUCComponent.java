/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST CMS Components.
 *
 * FenixEdu IST CMS Components is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST CMS Components is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST CMS Components.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.cmscomponents.domain.executionCourse;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.component.ComponentType;
import org.fenixedu.cms.rendering.TemplateContext;
import org.fenixedu.learning.domain.executionCourse.ExecutionCourseSite;
import org.fenixedu.learning.domain.executionCourse.components.BaseExecutionCourseComponent;
import org.joda.time.DateTime;

import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.TeacherInquiryTemplate;

@ComponentType(description = "Shows QUC Results on Public Pages", name = "QUC Results")
public class ExecutionCourseQUCComponent extends BaseExecutionCourseComponent {

    @Override
    public void handle(Page page, TemplateContext componentContext, TemplateContext globalContext) {
        ExecutionCourse executionCourse = ((ExecutionCourseSite) page.getSite()).getExecutionCourse();

        ExecutionSemester executionPeriod = executionCourse.getExecutionPeriod();
        ExecutionSemester oldQucExecutionSemester = ExecutionSemester.readBySemesterAndExecutionYear(2, "2009/2010");
        if (executionPeriod.isAfter(oldQucExecutionSemester)) {
            TeacherInquiryTemplate teacherInquiryTemplate = TeacherInquiryTemplate.getTemplateByExecutionPeriod(executionPeriod);
            if (teacherInquiryTemplate == null
                    || teacherInquiryTemplate.getResponsePeriodBegin().plusDays(7).isAfter(DateTime.now())
                    || executionCourse.getInquiryResultsSet().isEmpty()) {
                return;
            }

            Map<Professorship, Set<ShiftType>> professorships = new HashMap<Professorship, Set<ShiftType>>();
            for (Professorship professorship : executionCourse.getProfessorshipsSet()) {
                Collection<InquiryResult> professorshipResults = professorship.getInquiryResultsSet();
                if (!professorshipResults.isEmpty()) {
                    professorships.put(professorship, getShiftTypes(professorshipResults));
                }
            }

            globalContext.put("executionCourse", executionCourse);
            globalContext.put("professorships", professorships);
        }
    }

    private Set<ShiftType> getShiftTypes(Collection<InquiryResult> professorshipResults) {
        Set<ShiftType> shiftTypes = new HashSet<ShiftType>();
        for (InquiryResult inquiryResult : professorshipResults) {
            shiftTypes.add(inquiryResult.getShiftType());
        }
        return shiftTypes;
    }
}
