/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Evaluation.
 *
 * FenixEdu IST Teacher Evaluation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Evaluation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Evaluation.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.teacher.evaluation.domain;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.annotation.GroupArgument;
import org.fenixedu.bennu.core.annotation.GroupArgumentParser;
import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.ArgumentParser;
import org.fenixedu.bennu.core.groups.CustomGroup;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;

import java.util.stream.Stream;

@GroupOperator("departmentManager")
public class DepartmentManagerGroup extends CustomGroup {
    @GroupArgumentParser
    public class DepartmentManagerArgumentParser implements ArgumentParser<Department> {
        @Override
        public Department parse(String argument) {
            return Department.find(argument);
        }

        @Override
        public String serialize(Department argument) {
            return argument.getAcronym();
        }

        @Override
        public Class<Department> type() {
            return Department.class;
        }
    }

    @GroupArgument("")
    private Department department;

    DepartmentManagerGroup(){
        super();
    }

    public DepartmentManagerGroup(Department department){
        this();
        this.department = department;
    }

    @Override
    public String getPresentationName() {
        return BundleUtil.getString("resources.TeacherCreditsSheetResources", "label.group.DepartmentManagerGroup");
    }

    @Override
    public PersistentGroup toPersistentGroup() {
        return PersistentDepartmentManagerGroup.getInstance(department);
    }

    @Override
    public Stream<User> getMembers() {
        return department.getAssociatedPersonsSet().stream().map(Person::getUser);
    }

    @Override
    public Stream<User> getMembers(DateTime when) {
        return getMembers();
    }

    @Override
    public boolean isMember(User user) {
        return user != null && user.getPerson() != null &&
                department.getAssociatedPersonsSet().stream().anyMatch(p -> p.getUser().equals(user));
    }

    @Override
    public boolean isMember(User user, DateTime when) {
        return isMember(user);
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof DepartmentManagerGroup) {
            return department.equals(((DepartmentManagerGroup) object).department);
        }
        return false;
    }

    @Override
    public int hashCode(){
        return department.hashCode();
    }
}
