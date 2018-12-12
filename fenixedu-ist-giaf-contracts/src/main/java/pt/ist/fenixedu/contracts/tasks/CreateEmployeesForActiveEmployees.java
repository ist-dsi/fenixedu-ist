package pt.ist.fenixedu.contracts.tasks;

import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;

import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;

@Task(englishTitle = "CreateEmployeesForActiveEmployees")
public class CreateEmployeesForActiveEmployees extends CronTask {

    @Override
    public void runTask() {
        new ActiveEmployees().getMembers().forEach(user -> {
            if (user.getPerson() != null && user.getPerson().getEmployee() == null) {
                new Employee(user.getPerson(), Employee.getNextEmployeeNumber());
            }
        });
    }
}