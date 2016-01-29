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
import java.util.List;

import org.fenixedu.academic.domain.WrittenEvaluation;

import pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyArrayConverter;
import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixedu.vigilancies.domain.Vigilancy;
import pt.ist.fenixedu.vigilancies.domain.VigilantGroup;
import pt.ist.fenixedu.vigilancies.domain.VigilantWrapper;
import pt.ist.fenixedu.vigilancies.ui.struts.action.vigilancy.ConvokeBean;
import pt.ist.fenixedu.vigilancies.ui.struts.action.vigilancy.VigilantGroupBean;

public class VigilantsForGivenVigilantGroup implements DataProvider {

    @Override
    public Object provide(Object source, Object currentValue) {

        VigilantGroupBean bean = (VigilantGroupBean) source;
        VigilantGroup vigilantGroup = bean.getSelectedVigilantGroup();
        List<VigilantWrapper> vigilants = new ArrayList<VigilantWrapper>();

        if (source instanceof ConvokeBean) {
            ConvokeBean convokeBean = (ConvokeBean) bean;
            vigilants.addAll(convokeBean.getVigilantsSugestion());
            WrittenEvaluation evaluation = convokeBean.getWrittenEvaluation();
            if (evaluation != null && evaluation.getVigilanciesSet().size() > 0) {
                for (Vigilancy convoke : evaluation.getVigilanciesSet()) {
                    vigilants.remove(convoke.getVigilantWrapper());
                }
            }
        } else {
            vigilants.addAll(vigilantGroup.getVigilantWrappersSet());
            Collections.sort(vigilants, VigilantWrapper.CATEGORY_COMPARATOR.thenComparing(VigilantWrapper.USERNAME_COMPARATOR));
        }

        return vigilants;

    }

    @Override
    public Converter getConverter() {
        return new DomainObjectKeyArrayConverter();
    }

}