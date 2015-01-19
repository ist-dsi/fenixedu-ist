/**
 * Copyright © 2011 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Tutorship.
 *
 * FenixEdu Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.ui.Action.coordinator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.academic.ui.struts.action.coordinator.CoordinatorApplication.CoordinatorManagementApp;
import org.fenixedu.academic.ui.struts.action.coordinator.CoordinatorDegreeManagement;
import org.fenixedu.academic.ui.struts.action.coordinator.DegreeCoordinatorIndex;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.tutorship.domain.ProgramTutoredParticipationLog;
import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixedu.tutorship.domain.TutorshipIntention;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@StrutsFunctionality(app = CoordinatorManagementApp.class, path = "tutorship", titleKey = "link.coordinator.tutorTeachers",
        bundle = "CoordinatorResources")
@Mapping(path = "/tutorTeachers", module = "coordinator")
@Forward(name = "manage", path = "/coordinator/tutors/tutorTeachers.jsp")
@Forward(name = "entry-point", path = "/coordinator/tutors/entryPoint.jsp")
public class TutorTeachersManagementDispatchAction extends FenixDispatchAction {
    public static class TutorshipIntentionSelector implements Serializable {
        private Teacher teacher;

        private Department department;

        private AcademicInterval academicInterval;

        private DegreeCurricularPlan dcp;

        private boolean intending;

        private boolean deletable;

        private final int previousParticipations;

        public TutorshipIntentionSelector(Teacher teacher, Department department, DegreeCurricularPlan dcp,
                AcademicInterval academicInterval) {
            this.teacher = teacher;
            this.department = department;
            this.dcp = dcp;
            this.academicInterval = academicInterval;
            TutorshipIntention intention = TutorshipIntention.readByDcpAndTeacherAndInterval(dcp, teacher, academicInterval);
            if (intention == null) {
                intending = false;
                deletable = true;
            } else {
                intending = true;
                deletable = intention.isDeletable();
            }
            previousParticipations =
                    Tutorship.getActiveTutorships(teacher, academicInterval.getPreviousAcademicInterval()).size();
        }

        public Teacher getTeacher() {
            return teacher;
        }

        public void setTeacher(Teacher teacher) {
            this.teacher = teacher;
        }

        public Department getDepartment() {
            return department;
        }

        public void setDepartment(Department department) {
            this.department = department;
        }

        public AcademicInterval getAcademicInterval() {
            return academicInterval;
        }

        public void setAcademicInterval(AcademicInterval academicInterval) {
            this.academicInterval = academicInterval;
        }

        public DegreeCurricularPlan getDegreeCurricularPlan() {
            return dcp;
        }

        public void setDegreeCurricularPlan(DegreeCurricularPlan dcp) {
            this.dcp = dcp;
        }

        public boolean isIntending() {
            return intending;
        }

        public void setIntending(boolean intending) {
            this.intending = intending;
        }

        public boolean isDeletable() {
            return deletable;
        }

        public void setDeletable(boolean deletable) {
            this.deletable = deletable;
        }

        public int getPreviousParticipations() {
            return previousParticipations;
        }

        public void save() {
            TutorshipIntention intention = TutorshipIntention.readByDcpAndTeacherAndInterval(dcp, teacher, academicInterval);
            ExecutionYear executionYear = (ExecutionYear) ExecutionYear.getExecutionInterval(academicInterval);
            if (intention == null && intending) {
                new TutorshipIntention(dcp, teacher, academicInterval);
                ProgramTutoredParticipationLog.createLog(dcp.getDegree(), executionYear, Bundle.MESSAGING,
                        "log.degree.programtutoredparticipation.addteacher", teacher.getPerson().getPresentationName(), dcp
                                .getDegree().getPresentationName());
            } else if (intention != null && !intending) {
                ProgramTutoredParticipationLog.createLog(dcp.getDegree(), executionYear, Bundle.MESSAGING,
                        "log.degree.programtutoredparticipation.removeteacher", teacher.getPerson().getPresentationName(), dcp
                                .getDegree().getPresentationName());
                intention.delete();
            }
        }

        @Override
        public String toString() {
            return teacher.getPerson().getUsername() + "[" + (intending ? "x" : " ") + "]";
        }
    }

    public static class YearSelection implements Serializable {
        private AcademicInterval executionYear = AcademicInterval.readDefaultAcademicInterval(AcademicPeriod.YEAR)
                .getNextAcademicInterval();

        public YearSelection() {
        }

        public YearSelection(AcademicInterval executionYear) {
            this.executionYear = executionYear;
        }

        public AcademicInterval getExecutionYear() {
            return executionYear;
        }

        public void setExecutionYear(AcademicInterval executionYear) {
            this.executionYear = executionYear;
        }
    }

    @EntryPoint
    public ActionForward entryPoint(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        request.setAttribute("degrees", CoordinatorDegreeManagement.readCoordinatedDegrees());
        return mapping.findForward("entry-point");
    }

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        DegreeCoordinatorIndex.setCoordinatorContext(request);
        return super.execute(mapping, actionForm, request, response);
    }

    public ActionForward prepareTutorSelection(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        AcademicInterval current = AcademicInterval.readDefaultAcademicInterval(AcademicPeriod.YEAR);
        AcademicInterval next = AcademicInterval.readDefaultAcademicInterval(AcademicPeriod.YEAR).getNextAcademicInterval();

        if (next == null) {
            request.setAttribute("academicInterval", current.getResumedRepresentationInStringFormat());
        } else {
            request.setAttribute("academicInterval", next.getResumedRepresentationInStringFormat());
        }
        return selectYear(mapping, actionForm, request, response);
    }

    public ActionForward selectYear(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        YearSelection yearSelection = getYearSelection(request);
        DegreeCurricularPlan dcp = getDegreeCurricularPlan(request);
        List<TutorshipIntentionSelector> selector = new ArrayList<TutorshipIntentionSelector>();
        for (Department department : dcp.getDegree().getDepartmentsSet()) {
            for (Teacher teacher : department.getAllTeachers(yearSelection.getExecutionYear())) {
                selector.add(new TutorshipIntentionSelector(teacher, department, dcp, yearSelection.getExecutionYear()));
            }
        }
        RenderUtils.invalidateViewState();
        request.setAttribute("yearSelection", yearSelection);
        request.setAttribute("selector", selector);
        return mapping.findForward("manage");
    }

    public ActionForward saveChanges(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        YearSelection yearSelection = getYearSelection(request);
        List<TutorshipIntentionSelector> selector = getRenderedObject("selector");
        save(selector);
        request.setAttribute("yearSelection", yearSelection);
        request.setAttribute("selector", selector);
        return selectYear(mapping, actionForm, request, response);
    }

    private YearSelection getYearSelection(HttpServletRequest request) {
        String intervalString = request.getParameter("academicInterval");
        if (intervalString == null) {
            intervalString = (String) request.getAttribute("academicInterval");
        }
        if (intervalString != null) {
            AcademicInterval academicInterval = AcademicInterval.getAcademicIntervalFromResumedString(intervalString);
            return new YearSelection(academicInterval);
        }
        return getRenderedObject("yearSelection");
    }

    private DegreeCurricularPlan getDegreeCurricularPlan(HttpServletRequest request) {
        return FenixFramework.getDomainObject(request.getParameter("degreeCurricularPlanID"));
    }

    @Atomic
    private void save(List<TutorshipIntentionSelector> tutorshipIntentions) {
        for (TutorshipIntentionSelector selector : tutorshipIntentions) {
            selector.save();
        }
    }
}
