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
package pt.ist.fenixedu.integration.api.beans.publico;

import org.joda.time.LocalDate;

import pt.ist.fenixedu.integration.api.FenixAPIv1;

public class FenixPeriod {

    String start;
    String end;

    public FenixPeriod() {
        this((String) null, (String) null);
    }

    public FenixPeriod(final String start, final String end) {
        this.start = start == null ? "" : start;
        this.end = end == null ? "" : end;
    }

    public FenixPeriod(final LocalDate start, final LocalDate end) {
        this.start = start == null ? "" : start.toDateTimeAtStartOfDay().toString(FenixAPIv1.dayHourPattern);
        this.end = end == null ? "" : end.toDateTimeAtStartOfDay().toString(FenixAPIv1.dayHourPattern);
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

}
