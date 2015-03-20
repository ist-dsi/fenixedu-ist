/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Vigilancies.
 *
 * FenixEdu IST Vigilancies is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Vigilancies is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Vigilancies.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.vigilancies.ui.renderers.providers;

import java.util.Collection;

import pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyArrayConverter;
import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixedu.vigilancies.domain.ExamCoordinator;
import pt.ist.fenixedu.vigilancies.domain.VigilantGroup;
import pt.ist.fenixedu.vigilancies.ui.struts.action.vigilancy.VigilantGroupBean;

public class VigilantGroupsOfExamCoordinatorSelectMany implements DataProvider {

    @Override
    public Object provide(Object source, Object currentValue) {

        VigilantGroupBean bean = (VigilantGroupBean) source;
        ExamCoordinator coordinator = bean.getExamCoordinator();

        Collection<VigilantGroup> vigilantGroups = coordinator.getVigilantGroupsSet();

        return vigilantGroups;

    }

    @Override
    public Converter getConverter() {
        return new DomainObjectKeyArrayConverter();
    }

}