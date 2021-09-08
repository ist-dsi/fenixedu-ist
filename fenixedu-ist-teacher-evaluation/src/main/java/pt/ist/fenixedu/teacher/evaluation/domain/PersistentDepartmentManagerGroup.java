package pt.ist.fenixedu.teacher.evaluation.domain;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.joda.time.DateTime;
import pt.ist.fenixframework.dml.runtime.Relation;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

public class PersistentDepartmentManagerGroup extends PersistentDepartmentManagerGroup_Base {
    PersistentDepartmentManagerGroup(Department department) {
        super();
        setDepartment(department);
    }

    @Override
    public Group toGroup() {
        return new DepartmentManagerGroup(getDepartment());
    }

    @Override
    public Stream<User> getMembers() {
        return getDepartment().getAssociatedPersonsSet().stream().map(Person::getUser);
    }

    @Override
    public Stream<User> getMembers(DateTime when) {
        return getMembers();
    }

    @Override
    public boolean isMember(User user) {
        return user != null && user.getPerson() != null &&
                getDepartment().getAssociatedPersonsSet().stream().anyMatch(p -> p.getUser().equals(user));
    }

    @Override
    public boolean isMember(User user, DateTime when) {
        return isMember(user);
    }

    @Override
    protected Collection<Relation<?,?>> getContextRelations() {
        return Collections.singleton(getRelationPersistentDepartmentManagerGroupDepartment());
    }

    public static PersistentDepartmentManagerGroup getInstance(Department department) {
        return singleton(() -> getGroupFromDepartment(department),
                () -> new PersistentDepartmentManagerGroup(department));
    }

    public static Optional<PersistentDepartmentManagerGroup> getGroupFromDepartment(Department department) {
        PersistentDepartmentManagerGroup group = department.getDepartmentManagerGroup();
        return group == null ? Optional.empty() : Optional.of(group);
    }
}
