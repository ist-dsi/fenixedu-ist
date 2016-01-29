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
import org.fenixedu.bennu.core.domain.User;

import com.qubit.solution.fenixedu.integration.cgd.webservices.resolver.memberid.IMemberIDAdapter;

public class NumberObfuscationAdaptor implements IMemberIDAdapter {

    @Override
    public String retrieveMemberID(final Person person) {
        final User user = person.getUser();
        return CgdCardCounter.getNextSerialNumber(user);
    }

    @Override
    public Person readPerson(final String memberId) {
        final CgdCardCounter counter = CgdCardCounter.findCounterForYear(CgdCard.yearFor(memberId));
        final int serialNumber = CgdCard.serialNumberFor(memberId);
        return counter == null ? null : counter.getCgdCardSet().stream().filter(c -> c.getSerialNumber() == serialNumber)
                .map(c -> c.getUser().getPerson()).findAny().orElse(null);
    }

    @Override
    public boolean isAllowedAccessToMember(final Person person) {
        final User user = person.getUser();
        if (user != null) {
            final int year = Year.now().getValue();
            for (final CgdCard card : user.getCgdCardSet()) {
                if (card.getAllowSendDetails() && card.getCgdCardCounter().getYear() == year) {
                    return true;
                }
            }
        }
        return false;
    }

}
