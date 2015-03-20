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

import org.fenixedu.academic.domain.Person;

import pt.ist.fenixedu.vigilancies.domain.Vigilancy;
import pt.ist.fenixedu.vigilancies.domain.VigilantWrapper;
import pt.ist.fenixframework.Atomic;

public class AddIncompatiblePerson {

    @Atomic
    public static void run(VigilantWrapper vigilantWrapper, Person person) {
        if (Vigilancy.getIncompatibleVigilantPerson(vigilantWrapper.getPerson()) != null) {
            Vigilancy.setIncompatibleVigilantPerson(Vigilancy.getIncompatibleVigilantPerson(vigilantWrapper.getPerson()), null);
        }
        final Person person1 = person;
        Vigilancy.setIncompatibleVigilantPerson(vigilantWrapper.getPerson(), person1);
        Vigilancy.setIncompatibleVigilantPerson(person, vigilantWrapper.getPerson());
    }

}