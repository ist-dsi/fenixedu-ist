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

import java.io.Serializable;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.tutorship.domain.TutorshipSummaryPeriod;

public class TutorshipSummaryPeriodBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private ExecutionSemester executionSemester;
    private LocalDate beginDate;
    private LocalDate endDate;

    public TutorshipSummaryPeriodBean() {
    }

    public ExecutionSemester getExecutionSemester() {
        return executionSemester;
    }

    public void setExecutionSemester(ExecutionSemester executionSemester) {
        this.executionSemester = executionSemester;

        if (executionSemester != null && executionSemester.getTutorshipSummaryPeriod() != null) {
            setBeginDate(executionSemester.getTutorshipSummaryPeriod().getBeginDate());
            setEndDate(executionSemester.getTutorshipSummaryPeriod().getEndDate());
        }
    }

    public LocalDate getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(LocalDate beginDate) {
        this.beginDate = beginDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isValid() {
        return getExecutionSemester() != null && getBeginDate() != null && getEndDate() != null;
    }

    public void save() {
        if (getExecutionSemester().getTutorshipSummaryPeriod() != null) {
            getExecutionSemester().getTutorshipSummaryPeriod().update(this);
        } else {
            getExecutionSemester().setTutorshipSummaryPeriod(TutorshipSummaryPeriod.create(this));
        }
    }
}
