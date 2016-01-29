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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.fenixedu.academic.domain.Person;

import pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyConverter;
import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixedu.vigilancies.domain.VigilantGroup;
import pt.ist.fenixedu.vigilancies.domain.VigilantWrapper;
import pt.ist.fenixedu.vigilancies.ui.struts.action.vigilancy.UnavailablePeriodBean;

public class SelectVigilantForUnavailable implements DataProvider {

    @Override
    public Object provide(Object source, Object currentValue) {

        UnavailablePeriodBean bean = (UnavailablePeriodBean) source;
        VigilantGroup vigilantGroup = bean.getSelectedVigilantGroup();

        List<VigilantWrapper> vigilantWrappers = new ArrayList<VigilantWrapper>();
        if (vigilantGroup != null) {
            vigilantWrappers.addAll(vigilantGroup.getVigilantWrappersSet());
            Collections.sort(vigilantWrappers,
                    Comparator.comparing(VigilantWrapper::getPerson, Comparator.comparing(Person::getName)));
        }
        return vigilantWrappers;

    }

    @Override
    public Converter getConverter() {
        return new DomainObjectKeyConverter();
    }

}