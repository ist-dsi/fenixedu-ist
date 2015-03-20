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
package pt.ist.fenixedu.vigilancies.domain.accessControl.arguments;

import org.fenixedu.academic.domain.accessControl.arguments.DomainObjectArgumentParser;
import org.fenixedu.bennu.core.annotation.GroupArgumentParser;

import pt.ist.fenixedu.vigilancies.domain.Vigilancy;

@GroupArgumentParser
public class VigilancyArgument extends DomainObjectArgumentParser<Vigilancy> {
    @Override
    public Class<Vigilancy> type() {
        return Vigilancy.class;
    }
}
