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

import org.apache.commons.beanutils.BeanComparator;
import org.fenixedu.academic.domain.WrittenEvaluation;
import org.joda.time.YearMonthDay;

import pt.ist.fenixWebFramework.rendererExtensions.converters.DomainObjectKeyConverter;
import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixedu.vigilancies.domain.VigilantGroup;
import pt.ist.fenixedu.vigilancies.ui.struts.action.vigilancy.ConvokeBean;

public class WrittenEvaluationForGivenVigilantGroup implements DataProvider {

    @Override
    public Object provide(Object source, Object currentValue) {

        ConvokeBean bean = (ConvokeBean) source;
        VigilantGroup selectedGroup = bean.getSelectedVigilantGroup();
        List<WrittenEvaluation> evaluationsAfterCurrentDate = new ArrayList<WrittenEvaluation>();
        if (selectedGroup != null) {
            List<WrittenEvaluation> writtenEvaluationsForExecutionCourse = selectedGroup.getAllAssociatedWrittenEvaluations();
            List<WrittenEvaluation> writtenEvaluations = getOpenWrittenEvaluations(writtenEvaluationsForExecutionCourse);
            evaluationsAfterCurrentDate.addAll(writtenEvaluations);
            Collections.sort(evaluationsAfterCurrentDate, new BeanComparator("name"));
        }

        return evaluationsAfterCurrentDate;

    }

    private List<WrittenEvaluation> getOpenWrittenEvaluations(List<WrittenEvaluation> writtenEvaluations) {
        YearMonthDay currentDate = new YearMonthDay();
        List<WrittenEvaluation> evaluations = new ArrayList<WrittenEvaluation>(writtenEvaluations);
        List<WrittenEvaluation> evaluationsAfterCurrentDate = new ArrayList<WrittenEvaluation>();

        for (WrittenEvaluation writtenEvaluation : evaluations) {
            if (writtenEvaluation.getDayDateYearMonthDay().isAfter(currentDate)) {
                evaluationsAfterCurrentDate.add(writtenEvaluation);
            }
        }
        return evaluationsAfterCurrentDate;
    }

    @Override
    public Converter getConverter() {
        return new DomainObjectKeyConverter();
    }

}
