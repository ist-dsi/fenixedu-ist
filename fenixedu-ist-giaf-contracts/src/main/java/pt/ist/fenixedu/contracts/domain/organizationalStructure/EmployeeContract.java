/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Contracts.
 *
 * FenixEdu IST GIAF Contracts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Contracts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Contracts.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.contracts.domain.organizationalStructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityType;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.joda.time.YearMonthDay;

import pt.ist.fenixedu.contracts.domain.Employee;

public class EmployeeContract extends EmployeeContract_Base {

    public EmployeeContract(Person person, YearMonthDay beginDate, YearMonthDay endDate, Unit unit,
            AccountabilityTypeEnum contractType, Boolean teacherContract) {
        super();
        super.init(person, beginDate, endDate, unit);
        AccountabilityType accountabilityType = AccountabilityType.readByType(contractType);
        setAccountabilityType(accountabilityType);
        setTeacherContract(teacherContract);
    }

    @Override
    public Unit getWorkingUnit() {
        if (getAccountabilityType().getType().equals(AccountabilityTypeEnum.WORKING_CONTRACT)) {
            return getUnit();
        }
        return null;
    }

    @Override
    public Unit getMailingUnit() {
        if (getAccountabilityType().getType().equals(AccountabilityTypeEnum.MAILING_CONTRACT)) {
            return getUnit();
        }
        return null;
    }

    public Boolean isTeacherContract() {
        return getTeacherContract();
    }

    @Override
    public void setAccountabilityType(AccountabilityType accountabilityType) {
        super.setAccountabilityType(accountabilityType);
        if (!accountabilityType.getType().equals(AccountabilityTypeEnum.WORKING_CONTRACT)
                && !accountabilityType.getType().equals(AccountabilityTypeEnum.MAILING_CONTRACT)) {
            throw new DomainException("error.EmployeeContract.invalid.accountabilityType");
        }
    }

    @Override
    public void setChildParty(Party childParty) {
        super.setChildParty(childParty);
    }

    @Override
    public Employee getEmployee() {
        return getPerson().getEmployee();
    }

    public static List<Contract> getWorkingContracts(Unit unit) {
        List<Contract> contracts = new ArrayList<Contract>();
        contracts.addAll((Collection<? extends Contract>) unit.getChildAccountabilities(EmployeeContract.class,
                AccountabilityTypeEnum.WORKING_CONTRACT));
        return contracts;
    }

    public static List<Contract> getWorkingContracts(Unit unit, YearMonthDay begin, YearMonthDay end) {
        List<Contract> contracts = new ArrayList<Contract>();
        for (Contract contract : getWorkingContracts(unit)) {
            if (contract.belongsToPeriod(begin, end)) {
                contracts.add(contract);
            }
        }
        return contracts;
    }

}
