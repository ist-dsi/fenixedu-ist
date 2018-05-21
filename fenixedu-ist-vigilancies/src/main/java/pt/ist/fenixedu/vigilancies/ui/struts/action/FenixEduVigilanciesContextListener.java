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
package pt.ist.fenixedu.vigilancies.ui.struts.action;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.WrittenEvaluation;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.service.services.manager.MergeExecutionCourses;
import org.fenixedu.academic.service.services.resourceAllocationManager.exams.EditWrittenEvaluation.EditWrittenEvaluationEvent;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.messaging.core.domain.Message;
import org.joda.time.DateTime;

import pt.ist.fenixedu.vigilancies.domain.Vigilancy;
import pt.ist.fenixedu.vigilancies.domain.VigilantGroup;
import pt.ist.fenixframework.FenixFramework;

@WebListener
public class FenixEduVigilanciesContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        FenixFramework.getDomainModel().registerDeletionListener(WrittenEvaluation.class, (writtenEvaluation) -> {
            for (Vigilancy vigilancy : writtenEvaluation.getVigilanciesSet()) {
                vigilancy.delete();
            }
        });
        FenixFramework.getDomainModel().registerDeletionListener(ExecutionCourse.class, (executionCourse) -> {
            executionCourse.setVigilantGroup(null);
        });
        FenixFramework.getDomainModel().registerDeletionBlockerListener(Unit.class, (unit, blockers) -> {
            if (!(unit.getVigilantGroupsSet().isEmpty() && unit.getExamCoordinatorsSet().isEmpty())) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.unit.cannot.be.deleted"));
            }
        });
        MergeExecutionCourses.registerMergeHandler(FenixEduVigilanciesContextListener::copyVigilantGroups);
        FenixFramework.getDomainModel().registerDeletionListener(WrittenEvaluation.class,
                FenixEduVigilanciesContextListener::notifyVigilantsOfDeletedEvaluation);
        Signal.register("academic.writtenevaluation.edited",
                FenixEduVigilanciesContextListener::notifyVigilantsOfEditedEvaluation);
    }

    private static void copyVigilantGroups(ExecutionCourse executionCourseFrom, ExecutionCourse executionCourseTo) {
        if (executionCourseTo.getVigilantGroup() == null) {
            executionCourseTo.setVigilantGroup(executionCourseFrom.getVigilantGroup());
        }
    }

    private static void notifyVigilantsOfDeletedEvaluation(WrittenEvaluation writtenEvaluation) {
        if (!writtenEvaluation.getVigilanciesSet().isEmpty()) {
            final Set<Person> tos = new HashSet<>();

            for (VigilantGroup group : VigilantGroup.getAssociatedVigilantGroups(writtenEvaluation)) {
                tos.clear();
                DateTime date = writtenEvaluation.getBeginningDateTime();
                String time = writtenEvaluation.getBeginningDateHourMinuteSecond().toString();
                String beginDateString = date.getDayOfMonth() + "-" + date.getMonthOfYear() + "-" + date.getYear();

                String subject =
                        BundleUtil.getString("resources.VigilancyResources", "email.convoke.subject",
                                writtenEvaluation.getName(), group.getName(), beginDateString, time);
                String body =
                        BundleUtil.getString("resources.VigilancyResources", "label.writtenEvaluationDeletedMessage",
                                writtenEvaluation.getName(), beginDateString, time);
                for (Vigilancy vigilancy : writtenEvaluation.getVigilanciesSet()) {
                    Person person = vigilancy.getVigilantWrapper().getPerson();
                    tos.add(person);
                }
                Message.fromSystem()
                        .replyTo(group.getContactEmail())
                        .to(Person.convertToUserGroup(tos))
                        .subject(subject)
                        .textBody(body)
                        .send();
            }
        }
    }

    private static void notifyVigilantsOfEditedEvaluation(EditWrittenEvaluationEvent event) {
        WrittenEvaluation writtenEvaluation = event.getInstance();
        Date dayDate = event.getDayDate();
        Date beginDate = event.getBeginDate();
        if (!writtenEvaluation.getVigilanciesSet().isEmpty()
                && (dayDate != writtenEvaluation.getDayDate() || timeModificationIsBiggerThanFiveMinutes(beginDate,
                        writtenEvaluation.getBeginningDate()))) {
            final HashSet<Person> tos = new HashSet<>();

            // VigilantGroup group =
            // writtenEvaluation.getAssociatedVigilantGroups().iterator().next();
            for (VigilantGroup group : VigilantGroup.getAssociatedVigilantGroups(writtenEvaluation)) {
                tos.clear();
                DateTime date = writtenEvaluation.getBeginningDateTime();
                String time = writtenEvaluation.getBeginningDateHourMinuteSecond().toString();
                String beginDateString = date.getDayOfMonth() + "-" + date.getMonthOfYear() + "-" + date.getYear();

                String subject =
                        String.format("[ %s - %s - %s %s ]", writtenEvaluation.getName(), group.getName(), beginDateString, time);
                String body =
                        String.format(
                                "Caro Vigilante,\n\nA prova de avaliação: %1$s %2$s - %3$s foi alterada para  %4$td-%4$tm-%4$tY - %5$tH:%5$tM.",
                                writtenEvaluation.getName(), beginDateString, time, dayDate, beginDate);

                for (Vigilancy vigilancy : writtenEvaluation.getVigilanciesSet()) {
                    Person person = vigilancy.getVigilantWrapper().getPerson();
                    tos.add(person);
                }
                Message.fromSystem()
                        .replyTo(group.getContactEmail())
                        .to(Person.convertToUserGroup(tos))
                        .subject(subject)
                        .textBody(body)
                        .send();
            }
        }
    }

    private static boolean timeModificationIsBiggerThanFiveMinutes(Date writtenEvaluationStartTime, Date beginningDate) {
        int hourDiference = Math.abs(writtenEvaluationStartTime.getHours() - beginningDate.getHours());
        int minuteDifference = Math.abs(writtenEvaluationStartTime.getMinutes() - beginningDate.getMinutes());

        return hourDiference > 0 || minuteDifference > 5;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
