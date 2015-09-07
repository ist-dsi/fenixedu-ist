/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.domain.cgd;

import java.time.Year;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;

import com.qubit.solution.fenixedu.integration.cgd.services.form43.CgdForm43Sender;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class CgdCard extends CgdCard_Base {

    public CgdCard(final CgdCardCounter counter, final User user, final int count) {
        setCgdCardCounter(counter);
        setUser(user);
        setSerialNumber(count);
    }

    public String getSerialNumberForCard() {
        return Integer.toString(getCgdCardCounter().getYear() % 100)
                + fillLeftString(Integer.toString(getSerialNumber()), '0', 6);
    }

    protected static String fillLeftString(final String string, final char c, final int fillTo) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = string.length(); i < fillTo; i++) {
            stringBuilder.append(c);
        }
        stringBuilder.append(string);
        return stringBuilder.toString();
    }

    static int yearFor(final String cardSerialNumber) {
        return Integer.parseInt(cardSerialNumber.substring(0, 2));
    }

    static int serialNumberFor(final String memberId) {
        return Integer.parseInt(memberId.substring(2));
    }

    @Atomic
    public static CgdCard setGrantAccess(final boolean allowAccess) {
        final User user = Authenticate.getUser();
        if (user != null) {
            final int year = Year.now().getValue();
            final CgdCard card = findCardFor(user, year, allowAccess);
            if (card != null) {
                card.setAllowSendDetails(allowAccess);
                if (allowAccess) {
                    return card;
                }
            }
        }
        return null;
    }

    public static boolean getGrantAccess() {
        final User user = Authenticate.getUser();
        if (user != null) {
            final int year = Year.now().getValue();
            final CgdCard card = findCardFor(user, year, false);
            return card != null && card.getAllowSendDetails();
        }
        return false;
    }

    private static CgdCard findCardFor(final User user, final int year, final boolean createIfNotExists) {
        final CgdCard card = user.getCgdCardSet().stream().filter(c -> c.getCgdCardCounter().getYear() == year).findAny().orElse(null);
        return card == null && createIfNotExists ? CgdCardCounter.findCounterForYear(year).createNewSerialNumber(user) : card;
    }

    public static boolean hasCGDAccessResponse() {
        final User user = Authenticate.getUser();
        final int year = Year.now().getValue();
        return user != null && user.getCgdCardSet().stream().anyMatch(c -> c.getCgdCardCounter().getYear() == year);
    }

    private static class Sender extends Thread {

        private final String cardId;
        public Sender(final String externalId) {
            this.cardId = externalId;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            doit();
        }

        @Atomic
        private void doit() {
            final CgdCard card = FenixFramework.getDomainObject(cardId);
            final Person person = card.getUser().getPerson();
            if (person != null) {
                final Student student = person.getStudent();
                if (student != null) {
                    for (final Registration registration : student.getRegistrationsSet()) {
                        if (registration.isActive()) {
                            new CgdForm43Sender().sendForm43For(registration);
                        }
                    }
                }
            }            
        }

    }

    public void send() {
        new Sender(this.getExternalId()).start();
    }

}
