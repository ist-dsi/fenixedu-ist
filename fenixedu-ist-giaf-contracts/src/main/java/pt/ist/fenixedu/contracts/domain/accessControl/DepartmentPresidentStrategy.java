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
package pt.ist.fenixedu.contracts.domain.accessControl;

import java.util.Objects;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.FenixGroupStrategy;
import org.fenixedu.academic.domain.organizationalStructure.Accountability;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.joda.time.DateTime;

import pt.ist.fenixedu.contracts.domain.organizationalStructure.Function;
import pt.ist.fenixedu.contracts.domain.organizationalStructure.FunctionType;
import pt.ist.fenixedu.contracts.domain.organizationalStructure.PersonFunction;

@GroupOperator("departmentPresident")
public class DepartmentPresidentStrategy extends FenixGroupStrategy {

    private static final long serialVersionUID = -3153992434314606564L;

    @Override
    public Stream<User> getMembers() {
        return Bennu.getInstance().getDepartmentsSet().stream().flatMap(d -> getCurrentDepartmentPresidents(d))
                .filter(Objects::nonNull).map(p -> p.getUser());
    }

    @Override
    public boolean isMember(User user) {
        return user != null && user.getPerson() != null && user.getPerson().getEmployee() != null
                && user.getPerson().getEmployee().getCurrentDepartmentWorkingPlace() != null
                && isPersonCurrentDepartmentPresident(user.getPerson(),
                        user.getPerson().getEmployee().getCurrentDepartmentWorkingPlace());
    }

    @Override
    public Stream<User> getMembers(DateTime when) {
        return getMembers();
    }

    @Override
    public boolean isMember(User user, DateTime when) {
        return isMember(user);
    }

    @Deprecated
    public static Person getCurrentDepartmentPresident(Department department) {
        for (final Accountability accountability : department.getDepartmentUnit().getChildsSet()) {
            if (accountability instanceof PersonFunction && accountability.isActive()) {
                final PersonFunction personFunction = (PersonFunction) accountability;
                final Function function = personFunction.getFunction();
                if (function != null && function.getFunctionType() == FunctionType.PRESIDENT) {
                    final Party childParty = accountability.getChildParty();
                    if (childParty != null && childParty.isPerson()) {
                        return (Person) childParty;
                    }
                }
            }
        }
        return null;
    }

    private static Stream<Person> getCurrentDepartmentPresidents(Department department) {
        return department.getDepartmentUnit().getChildsSet().stream()
                .filter(a -> a instanceof PersonFunction && a.isActive() && ((PersonFunction) a).getFunction() != null
                        && ((PersonFunction) a).getFunction().getFunctionType() == FunctionType.PRESIDENT
                        && a.getChildParty() != null && a.getChildParty().isPerson())
                .map(a -> (Person) a.getChildParty());
    }

    public static boolean isPersonCurrentDepartmentPresident(Person person, Department department) {
        return getCurrentDepartmentPresidents(department).anyMatch(p -> p.equals(person));
    }

    public static boolean isCurrentUserCurrentDepartmentPresident(Department department) {
        final Person person = AccessControl.getPerson();
        return person == null ? false : isPersonCurrentDepartmentPresident(person, department);
    }
}
