/**
 * Copyright © 2011 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Exam Vigilancies.
 *
 * FenixEdu Exam Vigilancies is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Exam Vigilancies is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Exam Vigilancies.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.vigilancies.ui.renderers.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.beanutils.BeanComparator;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.organizationalStructure.Unit;

import pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyConverter;
import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixedu.vigilancies.ui.struts.action.vigilancy.VigilantGroupBean;

public class UnitsOfDepartmentForVigilantGroup implements DataProvider {

    @Override
    public Object provide(Object source, Object currentValue) {
        VigilantGroupBean bean = (VigilantGroupBean) source;
        Department department = bean.getSelectedDepartment();

        List<Unit> unitsOfDepartment = new ArrayList<Unit>();

        if (department != null) {
            for (Unit unit : department.getDepartmentUnit().getScientificAreaUnits()) {
                unitsOfDepartment.add(unit);
            }
            unitsOfDepartment.add(department.getDepartmentUnit());
        }

        Collections.sort(unitsOfDepartment, new BeanComparator("name"));
        return unitsOfDepartment;
    }

    @Override
    public Converter getConverter() {
        return new DomainObjectKeyConverter();
    }

}
