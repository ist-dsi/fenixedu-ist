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
package pt.ist.fenixedu.integration.service.services.manager.organizationalStructureManagement;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.YearMonthDay;

import pt.ist.fenixedu.contracts.domain.organizationalStructure.Function;
import pt.ist.fenixedu.contracts.domain.organizationalStructure.FunctionType;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class CreateFunction {

    @Atomic
    public static void run(LocalizedString functionName, YearMonthDay begin, YearMonthDay end, FunctionType type,
            String unitID) throws FenixServiceException, DomainException {
        Unit unit = (Unit) FenixFramework.getDomainObject(unitID);
        if (unit == null) {
            throw new FenixServiceException("error.function.no.unit");
        }

        new Function(functionName, begin, end, type, unit);
    }
}