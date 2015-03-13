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
package pt.ist.fenixedu.tutorship.ui.Action.pedagogicalCouncil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Teacher;

import pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyConverter;
import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixedu.tutorship.domain.TutorshipSummary;

public class TutorSummaryBean extends TutorSearchBean {

    private static final long serialVersionUID = 1L;

    private ExecutionSemester executionSemester;
    private Degree degree;

    public boolean isAbleToCreateSummary() {
        for (TutorshipSummary ts : getPastSummaries()) {
            if (ts.getSemester().isCurrent()) {
                return false;
            }
        }

        return true;
    }

    public ExecutionSemester getActiveSemester() {
        return ExecutionSemester.readActualExecutionSemester();
    }

    public List<CreateSummaryBean> getAvailableSummaries() {

        /* each CreateSummaryBean must have a unique degree */
        List<CreateSummaryBean> result = new ArrayList<CreateSummaryBean>();
        if (getTeacher() != null && (isSearchType() || getExecutionSemester() == null)) {
            /* add active - already created - summaries */
            for (TutorshipSummary ts : getTeacher().getTutorshipSummariesSet()) {
                if (ts.isActive()) {
                    CreateSummaryBean createSummaryBean = new CreateSummaryBean(ts);
                    result.add(createSummaryBean);
                }
            }
            /* add - not created - available summaries */
            Set<ExecutionSemester> activePeriods = TutorshipSummary.getActivePeriods();

            for (Tutorship t : getTeacher().getTutorshipsSet()) {
                boolean addDegree = true;
                Degree studentDegree = t.getStudent().getDegree();

                /* check if degree is already added to the result */
                for (CreateSummaryBean createSummaryBean : result) {
                    if (createSummaryBean.getDegree().equals(studentDegree)) {
                        addDegree = false;
                        break;
                    }
                }
                if (addDegree) {
                    for (ExecutionSemester semester : activePeriods) {
                        if (t.isActive()) {
                            CreateSummaryBean createSummaryBean = new CreateSummaryBean(getTeacher(), semester, studentDegree);
                            result.add(createSummaryBean);
                        }
                    }
                }
            }
        } else if (getExecutionSemester() != null) {
            if (isSearchType()) {
                if (getDepartment() != null) {
                    for (Teacher teacher : getDepartment().getAllCurrentTeachers()) {
                        if (!teacher.getTutorshipsSet().isEmpty()) {
                            for (TutorshipSummary ts : teacher.getTutorshipSummariesSet()) {
                                if ((ts.isActive()) && ts.getSemester().equals(getExecutionSemester())) {
                                    result.add(new CreateSummaryBean(ts));
                                }
                            }
                        }
                    }
                }
            } else {
                if (getDegree() != null && getExecutionSemester() != null) {
                    for (TutorshipSummary ts : getDegree().getTutorshipSummariesSet()) {
                        if (ts.isActive() && ts.getSemester().equals(getExecutionSemester())) {
                            result.add(new CreateSummaryBean(ts));
                        }
                    }
                } else if (getDegree() != null && getExecutionSemester() == null) {
                    for (TutorshipSummary ts : getDegree().getTutorshipSummariesSet()) {
                        if (ts.isActive()) {
                            result.add(new CreateSummaryBean(ts));
                        }
                    }
                }
            }
        }

        return result;
    }

    public List<TutorshipSummary> getPastSummaries() {

        List<TutorshipSummary> result = new ArrayList<TutorshipSummary>();

        if (isSearchType()) {
            if (getDepartment() != null && getTeacher() == null && getExecutionSemester() == null) {
                for (Teacher teacher : getDepartment().getAllCurrentTeachers()) {
                    if (!teacher.getTutorshipsSet().isEmpty()) {
                        for (TutorshipSummary ts : teacher.getTutorshipSummariesSet()) {
                            if ((!ts.isActive())) {
                                result.add(ts);
                            }
                        }
                    }
                }
            } else if (getDepartment() != null && getTeacher() == null && getExecutionSemester() != null) {
                for (Teacher teacher : getDepartment().getAllCurrentTeachers()) {
                    if (!teacher.getTutorshipsSet().isEmpty()) {
                        for (TutorshipSummary ts : teacher.getTutorshipSummariesSet()) {
                            if ((!ts.isActive()) && ts.getSemester().equals(getExecutionSemester())) {
                                result.add(ts);
                            }
                        }
                    }
                }
            } else {
                if (getTeacher() != null && getExecutionSemester() != null) {
                    for (TutorshipSummary ts : getTeacher().getTutorshipSummariesSet()) {
                        if ((!ts.isActive()) && ts.getSemester().equals(getExecutionSemester())) {
                            result.add(ts);
                        }
                    }
                } else {
                    if (getTeacher() != null) {
                        for (TutorshipSummary ts : getTeacher().getTutorshipSummariesSet()) {
                            if (!ts.isActive()) {
                                result.add(ts);
                            }
                        }

                    } else {
                        if (getExecutionSemester() != null) {
                            for (TutorshipSummary ts : getExecutionSemester().getTutorshipSummariesSet()) {
                                if (!ts.isActive()) {
                                    result.add(ts);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (getDegree() != null && getExecutionSemester() != null) {
                for (TutorshipSummary ts : getDegree().getTutorshipSummariesSet()) {
                    if (!ts.isActive() && ts.getSemester().equals(getExecutionSemester())) {
                        result.add(ts);
                    }
                }
            } else if (getDegree() != null && getExecutionSemester() == null) {
                for (TutorshipSummary ts : getDegree().getTutorshipSummariesSet()) {
                    if (!ts.isActive()) {
                        result.add(ts);
                    }
                }
            } else if (getExecutionSemester() != null) {
                for (TutorshipSummary ts : getExecutionSemester().getTutorshipSummariesSet()) {
                    if (!ts.isActive()) {
                        result.add(ts);
                    }
                }
            }
        }

        return result;
    }

    public ExecutionSemester getExecutionSemester() {
        return executionSemester;
    }

    public void setExecutionSemester(ExecutionSemester executionSemester) {
        this.executionSemester = executionSemester;
    }

    public Degree getDegree() {
        return degree;
    }

    public void setDegree(Degree degree) {
        this.degree = degree;
    }

    public static class DegreesProvider implements DataProvider {

        @Override
        public Object provide(Object source, Object currentValue) {

            final SortedSet<Degree> result = new TreeSet<Degree>(Degree.COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_ID);
            final TutorSummaryBean chooseDegreeBean = (TutorSummaryBean) source;

            if (chooseDegreeBean.getExecutionSemester() != null) {
                for (final ExecutionDegree executionDegree : chooseDegreeBean.getExecutionSemester().getExecutionYear()
                        .getExecutionDegreesSet()) {
                    if (executionDegree.getDegreeType().isFirstCycle()) {
                        result.add(executionDegree.getDegree());
                    }
                }
            }

            return result;

        }

        @Override
        public Converter getConverter() {
            return new DomainObjectKeyConverter();
        }
    }
}