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
import org.fenixedu.bennu.core.domain.User;

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
        Stream<User> stream = search.isEmpty() ? null : search.stream().map(p -> p.getUser());
        Stream<User> matches = filterEmployeeNumber(stream);
        return matches == null ? null : matches.map(u -> u.getPerson()).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Stream<User> filterEmployeeNumber(Stream<User> matches) {
        if (employeeNumber != null) {
            if (matches == null) {
                return Stream.of(Employee.readByNumber(employeeNumber)).filter(Objects::nonNull).map(Employee::getPerson)
                        .map(Person::getUser).filter(Objects::nonNull);
            } else {
                return matches.filter(p -> Stream.of(p.getPerson()).map(Person::getEmployee).filter(Objects::nonNull)
                        .filter(a -> a.getEmployeeNumber().equals(employeeNumber)).findAny().isPresent());
            }
        }
        return matches;
    }
}