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
package pt.ist.fenixedu.vigilancies.service;

import java.util.List;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.messaging.core.domain.Message;
import org.joda.time.DateTime;

import pt.ist.fenixedu.vigilancies.domain.UnavailablePeriod;
import pt.ist.fenixedu.vigilancies.domain.VigilantGroup;
import pt.ist.fenixframework.Atomic;

public class CreateUnavailablePeriod {

    @Atomic
    public static void run(Person person, DateTime begin, DateTime end, String justification) {

        CreateUnavailable(person, begin, end, justification);
        sendEmail(person, begin, end, justification,
                VigilantGroup.getVigilantGroupsForExecutionYear(person, ExecutionYear.readCurrentExecutionYear()));
    }

    private static void CreateUnavailable(Person person, DateTime begin, DateTime end, String justification) {
        new UnavailablePeriod(begin, end, justification, person);
    }

    private static void sendEmail(Person person, DateTime begin, DateTime end, String justification, List<VigilantGroup> groups) {
        for (VigilantGroup group : groups) {
            String beginDate =
                    begin.getDayOfMonth() + "/" + begin.getMonthOfYear() + "/" + begin.getYear() + " - "
                            + String.format("%02d", begin.getHourOfDay()) + ":" + String.format("%02d", begin.getMinuteOfHour())
                            + "h";
            String endDate =
                    end.getDayOfMonth() + "/" + end.getMonthOfYear() + "/" + end.getYear() + " - "
                            + String.format("%02d", end.getHourOfDay()) + ":" + String.format("%02d", end.getMinuteOfHour())
                            + "h";
            String message =
                    BundleUtil.getString("resources.VigilancyResources", "email.convoke.unavailablePeriod",
                            person.getName(), beginDate, endDate, justification);

            String subject =
                    BundleUtil.getString("resources.VigilancyResources", "email.convoke.unavailablePeriod.subject",
                            group.getName());

            Message.fromSystem()
                    .replyTo(group.getContactEmail())
                    .singleBcc(group.getContactEmail())
                    .subject(subject)
                    .textBody(message)
                    .send();
        }
    }
}