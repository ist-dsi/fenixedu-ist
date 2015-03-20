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
package pt.ist.fenixedu.tutorship.ui.renderers.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.comparators.ReverseComparator;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyConverter;
import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixedu.tutorship.dto.teacher.tutor.StudentsPerformanceInfoBean;

public class TutorshipMonitoringExecutionYearProvider implements DataProvider {

    @Override
    public Object provide(Object source, Object currentValue) {
        StudentsPerformanceInfoBean bean = (StudentsPerformanceInfoBean) source;
        return getExecutionYears(bean);
    }

    public static List<ExecutionYear> getExecutionYears(StudentsPerformanceInfoBean bean) {
        List<ExecutionYear> executionYears = new ArrayList<ExecutionYear>();
        for (ExecutionYear year : Bennu.getInstance().getExecutionYearsSet()) {
            if (year.isAfterOrEquals(bean.getStudentsEntryYear())) {
                executionYears.add(year);
            }
        }
        Collections.sort(executionYears, new ReverseComparator());
        return executionYears;
    }

    @Override
    public Converter getConverter() {
        return new DomainObjectKeyConverter();
    }

}
