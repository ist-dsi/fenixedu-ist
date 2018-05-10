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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.WrittenEvaluation;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.messaging.core.domain.Message;
import org.joda.time.DateTime;

import pt.ist.fenixedu.vigilancies.domain.ExamCoordinator;
import pt.ist.fenixedu.vigilancies.domain.VigilantGroup;
import pt.ist.fenixedu.vigilancies.domain.VigilantWrapper;
import pt.ist.fenixedu.vigilancies.domain.VigilantWrapper_Base;
import pt.ist.fenixframework.Atomic;

public class CreateConvokes {

    @Atomic
    public static void run(List<VigilantWrapper> vigilants, WrittenEvaluation writtenEvaluation, VigilantGroup group,
            ExamCoordinator coordinator, String emailMessage) {
        group.convokeVigilants(vigilants, writtenEvaluation);

        Set<Person> receivers;

        if (emailMessage.length() != 0) {
            Person person = coordinator.getPerson();
            receivers = vigilants.stream().map(VigilantWrapper_Base::getPerson).collect(Collectors.toSet());
            receivers.addAll(writtenEvaluation.getTeachers());

            String bccs = Optional.ofNullable(group.getContactEmail()).orElse(null);
            String replyTo = Optional.ofNullable(group.getContactEmail()).orElse(person.getEmail());

            DateTime date = writtenEvaluation.getBeginningDateTime();
            String beginDateString = String.format("%d/%d/%d", date.getDayOfMonth(), date.getMonthOfYear(), date.getYear());

            String subject = BundleUtil.getString("resources.VigilancyResources", "email.convoke.subject",
                            group.getEmailSubjectPrefix(), writtenEvaluation.getName(), group.getName(), beginDateString);

            Message.from(person.getSender())
                    .replyTo(replyTo)
                    .to(Person.convertToUserGroup(receivers))
                    .singleBcc(bccs)
                    .subject(subject)
                    .textBody(emailMessage)
                    .send();
        }
    }
}