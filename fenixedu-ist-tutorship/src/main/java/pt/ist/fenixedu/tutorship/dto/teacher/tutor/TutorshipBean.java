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
package pt.ist.fenixedu.tutorship.dto.teacher.tutor;

import java.io.Serializable;
import java.util.Comparator;

import org.fenixedu.academic.domain.student.PersonalIngressionData;
import org.fenixedu.academic.domain.student.Registration;

import pt.ist.fenixedu.tutorship.domain.Tutorship;

public class TutorshipBean implements Serializable {

    public static final Comparator<TutorshipBean> TUTORSHIP_COMPARATOR_BY_STUDENT_NUMBER = Comparator.comparing(
            TutorshipBean::getTutorship, Tutorship.TUTORSHIP_COMPARATOR_BY_STUDENT_NUMBER);

    private Tutorship tutorship;

    public TutorshipBean(Tutorship tutorship) {
        setTutorship(tutorship);
    }

    public Tutorship getTutorship() {
        return tutorship;
    }

    public void setTutorship(Tutorship tutorship) {
        this.tutorship = tutorship;
    }

    public Boolean getIsDislocatedFromPermanentResidence() {
        Registration registration = getTutorship().getStudentCurricularPlan().getRegistration();
        PersonalIngressionData personalIngressionData =
                registration.getStudent().getPersonalIngressionDataByExecutionYear(
                        registration.getRegistrationYear());
        return personalIngressionData != null ? personalIngressionData.getDislocatedFromPermanentResidence() : null;
    }
}
