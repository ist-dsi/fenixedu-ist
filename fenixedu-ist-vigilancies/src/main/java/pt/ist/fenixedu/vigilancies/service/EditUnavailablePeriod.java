/**
 * Copyright © 2011 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Exam Vigilancies.
 *
 * FenixEdu Exam Vigilancies is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Exam Vigilancies is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Exam Vigilancies.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.vigilancies.service;

import org.joda.time.DateTime;

import pt.ist.fenixedu.vigilancies.domain.UnavailablePeriod;
import pt.ist.fenixframework.Atomic;

public class EditUnavailablePeriod {

    @Atomic
    public static void run(UnavailablePeriod unavailablePeriod, DateTime begin, DateTime end, String justification) {

        unavailablePeriod.edit(begin, end, justification);

    }

}