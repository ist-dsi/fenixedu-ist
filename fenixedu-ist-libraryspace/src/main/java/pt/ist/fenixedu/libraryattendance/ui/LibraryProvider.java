/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Library Attendance.
 *
 * FenixEdu IST Library Attendance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Library Attendance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Library Attendance.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.libraryattendance.ui;

import java.util.stream.Collectors;

import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;

public class LibraryProvider implements DataProvider {

    @Override
    public Converter getConverter() {
        return null;
    }

    @Override
    public Object provide(Object arg0, Object arg1) {
        return Bennu.getInstance().getLibrariesSet().stream().filter(s -> s.isActive()).collect(Collectors.toSet());
    }

}
