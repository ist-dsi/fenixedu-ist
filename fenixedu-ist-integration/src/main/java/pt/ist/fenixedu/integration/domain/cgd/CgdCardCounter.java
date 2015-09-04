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

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;

import pt.ist.fenixframework.Atomic;

public class CgdCardCounter extends CgdCardCounter_Base {

    private CgdCardCounter(final int year) {
        setBennu(Bennu.getInstance());
        setYear(year);
        setCount(0);
    }

    @Atomic
    public static String getNextSerialNumber(final User user) {
        final int year = Year.now().getValue();
        final CgdCardCounter counter = getCounterForYear(year);
        return counter.nextSerialNumber(user);
    }

    private String nextSerialNumber(final User user) {
        return user.getCgdCardSet().stream().filter(c -> c.getCgdCardCounter() == this)
                .findAny()
                .orElseGet(() -> createNewSerialNumber(user)).getSerialNumberForCard();
    }

    CgdCard createNewSerialNumber(User user) {
        final int count = getCount() + 1;
        setCount(count);
        return new CgdCard(this, user, count);
    }

    static CgdCardCounter getCounterForYear(final int year) {
        CgdCardCounter counter = findCounterForYear(year);
        return counter == null ? new CgdCardCounter(year) : counter;
    }

    static CgdCardCounter findCounterForYear(final int year) {
        for (final CgdCardCounter counter : Bennu.getInstance().getCgdCardCounterSet()) {
            if (counter.getYear() == year) {
                return counter;
            }
        }
        return null;
    }

}
