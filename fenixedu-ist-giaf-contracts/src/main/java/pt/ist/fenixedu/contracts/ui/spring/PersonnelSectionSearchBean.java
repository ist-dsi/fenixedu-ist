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
package pt.ist.fenixedu.contracts.ui.spring;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.ui.struts.action.accounts.SearchParametersBean;

import pt.ist.fenixedu.contracts.domain.Employee;

public class PersonnelSectionSearchBean extends SearchParametersBean {

    private Integer employeeNumber;

    public Integer getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(Integer employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    @Override
    public Collection<Person> search() {
        Collection<Person> search = super.search();
        Stream<Person> stream = search.isEmpty() ? null : search.stream();
        Stream<Person> matches = filterEmployeeNumber(stream);
        return matches == null ? null : matches.filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Stream<Person> filterEmployeeNumber(Stream<Person> matches) {
        if (employeeNumber != null) {
            if (matches == null) {
                return Stream.of(Employee.readByNumber(employeeNumber)).filter(Objects::nonNull).map(Employee::getPerson)
                        .filter(Objects::nonNull);
            } else {
                return matches.filter(p -> p.getEmployee() != null && p.getEmployee().getEmployeeNumber().equals(employeeNumber));
            }
        }
        return matches;
    }
}