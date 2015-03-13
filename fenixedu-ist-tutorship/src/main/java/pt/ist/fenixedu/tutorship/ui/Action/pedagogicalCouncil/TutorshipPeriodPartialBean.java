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

import org.fenixedu.academic.domain.ExecutionDegree;
import org.joda.time.Partial;

import pt.ist.fenixedu.tutorship.domain.Tutorship;

public class TutorshipPeriodPartialBean extends TeacherTutorshipCreationBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Partial endDate;
    private Tutorship tutorship;

    public TutorshipPeriodPartialBean(Tutorship tutorship, ExecutionDegree executionDegree) {
        super(executionDegree);
        this.tutorship = tutorship;
    }

    public Partial getEndDate() {
        return endDate;
    }

    public void setEndDate(Partial endDate) {
        this.endDate = endDate;
    }

    public Tutorship getTutorship() {
        return tutorship;
    }

    public void setTutorship(Tutorship tutorship) {
        this.tutorship = tutorship;
    }

}
