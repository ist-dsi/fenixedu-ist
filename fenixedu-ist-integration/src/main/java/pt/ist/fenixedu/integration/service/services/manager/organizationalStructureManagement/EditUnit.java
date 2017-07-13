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

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.organizationalStructure.UnitClassification;
import org.fenixedu.academic.service.ServiceMonitoring;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.YearMonthDay;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class EditUnit {

    @Atomic
    public static void run(String unitID, LocalizedString unitName, String unitNameCard, String unitCostCenter,
            String acronym, YearMonthDay begin, YearMonthDay end, String departmentID, String degreeID,
            String administrativeOfficeID, String webAddress, UnitClassification classification,
            Boolean canBeResponsibleOfSpaces, String campusID) throws FenixServiceException, DomainException {
        ServiceMonitoring.logService(EditUnit.class, unitID, unitName, unitNameCard, unitCostCenter, acronym, begin, end,
                departmentID, degreeID, administrativeOfficeID, webAddress, classification, canBeResponsibleOfSpaces, campusID);

        Unit unit = (Unit) FenixFramework.getDomainObject(unitID);
        if (unit == null) {
            throw new FenixServiceException("error.noUnit");
        }

        Integer costCenterCode = getCostCenterCode(unitCostCenter);

        Degree degree = FenixFramework.getDomainObject(degreeID);
        Department department = FenixFramework.getDomainObject(departmentID);
        AdministrativeOffice administrativeOffice = FenixFramework.getDomainObject(administrativeOfficeID);
        Space campus = (Space) FenixFramework.getDomainObject(campusID);

        unit.edit(unitName, unitNameCard, costCenterCode, acronym, begin, end, webAddress, classification, department, degree,
                administrativeOffice, canBeResponsibleOfSpaces, campus);
    }

    private static Integer getCostCenterCode(String unitCostCenter) {
        Integer costCenterCode = null;
        if (unitCostCenter != null && !unitCostCenter.equals("")) {
            costCenterCode = (Integer.valueOf(unitCostCenter));
        }
        return costCenterCode;
    }
}