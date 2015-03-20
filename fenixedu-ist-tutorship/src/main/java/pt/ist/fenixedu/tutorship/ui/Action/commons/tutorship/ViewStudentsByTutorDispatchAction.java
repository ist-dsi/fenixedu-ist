/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Tutorship.
 *
 * FenixEdu IST Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.ui.Action.commons.tutorship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanComparator;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;

import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixedu.tutorship.dto.teacher.tutor.StudentsByTutorBean;
import pt.ist.fenixedu.tutorship.dto.teacher.tutor.TutorTutorshipsHistoryBean;
import pt.ist.fenixedu.tutorship.dto.teacher.tutor.TutorshipBean;

public abstract class ViewStudentsByTutorDispatchAction extends FenixDispatchAction {

    protected void getTutorships(HttpServletRequest request, final Teacher teacher) {

        if (!teacher.getTutorshipsSet().isEmpty()) {
            TutorTutorshipsHistoryBean tutorshipHistory = new TutorTutorshipsHistoryBean(teacher);

            List<StudentsByTutorBean> activeTutorshipsByEntryYear =
                    getTutorshipsByEntryYear(Tutorship.getActiveTutorships(teacher));
            tutorshipHistory.setActiveTutorshipsByEntryYear(activeTutorshipsByEntryYear);

            List<StudentsByTutorBean> pastTutorshipsByEntryYear = getTutorshipsByEntryYear(Tutorship.getPastTutorships(teacher));
            tutorshipHistory.setPastTutorshipsByEntryYear(pastTutorshipsByEntryYear);

            request.setAttribute("tutorshipHistory", tutorshipHistory);
        }
    }

    private List<StudentsByTutorBean> getTutorshipsByEntryYear(List<Tutorship> tutorships) {
        Map<ExecutionYear, StudentsByTutorBean> tutorshipsMapByEntryYear = new HashMap<ExecutionYear, StudentsByTutorBean>();

        for (final Tutorship tutorship : tutorships) {
            ExecutionYear entryYear =
                    ExecutionYear.getExecutionYearByDate(tutorship.getStudentCurricularPlan().getRegistration().getStartDate());
            if (!tutorship.getStudentCurricularPlan().getRegistration().isCanceled()) {
                if (tutorshipsMapByEntryYear.containsKey(entryYear)) {
                    StudentsByTutorBean studentsByTutorBean = tutorshipsMapByEntryYear.get(entryYear);
                    studentsByTutorBean.getStudentsList().add(new TutorshipBean(tutorship));
                    Collections.sort(studentsByTutorBean.getStudentsList(), TutorshipBean.TUTORSHIP_COMPARATOR_BY_STUDENT_NUMBER);
                } else {
                    List<Tutorship> studentsByEntryYearList = new ArrayList<Tutorship>();
                    studentsByEntryYearList.add(tutorship);
                    StudentsByTutorBean studentsByTutorBean =
                            new StudentsByTutorBean(tutorship.getTeacher(), entryYear, studentsByEntryYearList);
                    tutorshipsMapByEntryYear.put(entryYear, studentsByTutorBean);
                }
            }
        }

        List<StudentsByTutorBean> tutorshipsByEntryYear = new ArrayList<StudentsByTutorBean>(tutorshipsMapByEntryYear.values());
        Collections.sort(tutorshipsByEntryYear, new BeanComparator("studentsEntryYear"));
        Collections.reverse(tutorshipsByEntryYear);

        return tutorshipsByEntryYear;
    }

}
