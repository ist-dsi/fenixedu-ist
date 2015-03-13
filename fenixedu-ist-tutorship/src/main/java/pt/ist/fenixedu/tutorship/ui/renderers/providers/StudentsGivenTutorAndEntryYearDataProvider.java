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
package pt.ist.fenixedu.tutorship.ui.renderers.providers;

import java.util.ArrayList;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.Teacher;

import pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyArrayConverter;
import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixedu.tutorship.domain.Tutorship;
import pt.ist.fenixedu.tutorship.dto.coordinator.tutor.TutorshipManagementByEntryYearBean;

public class StudentsGivenTutorAndEntryYearDataProvider implements DataProvider {

    @Override
    public Object provide(Object source, Object currentValue) {
        TutorshipManagementByEntryYearBean bean = (TutorshipManagementByEntryYearBean) source;

        Teacher teacher = bean.getTeacher();

        List<Tutorship> tutorships = new ArrayList<Tutorship>();

        for (Tutorship tutorship : teacher.getTutorshipsSet()) {
            StudentCurricularPlan studentCurricularPlan = tutorship.getStudentCurricularPlan();
            ExecutionYear studentEntryYear =
                    ExecutionYear.getExecutionYearByDate(studentCurricularPlan.getRegistration().getStartDate());
            if (studentEntryYear.equals(bean.getExecutionYear()) && tutorship.isActive()) {
                tutorships.add(tutorship);
            }
        }

        return tutorships;
    }

    @Override
    public Converter getConverter() {
        return new DomainObjectKeyArrayConverter();
    }
}
