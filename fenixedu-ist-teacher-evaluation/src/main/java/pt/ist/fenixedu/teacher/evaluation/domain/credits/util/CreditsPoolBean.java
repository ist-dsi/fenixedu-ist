package pt.ist.fenixedu.teacher.evaluation.domain.credits.util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionYear;

import pt.ist.fenixedu.teacher.evaluation.domain.DepartmentCreditsPool;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.AnnualCreditsState;

public class CreditsPoolBean implements Serializable {

    protected ExecutionYear executionYear;
    protected List<CreditsPoolByDepartmentBean> creditsPoolByDepartments;

    public CreditsPoolBean() {
        setExecutionYear(ExecutionYear.readCurrentExecutionYear());
        updateValues();
    }

    public void updateValues() {
        setCreditsPoolByDepartments(new ArrayList<CreditsPoolByDepartmentBean>());
        Department.readActiveDepartments()
                .forEach(department -> creditsPoolByDepartments.add(new CreditsPoolByDepartmentBean(department)));
    }

    public ExecutionYear getExecutionYear() {
        return executionYear;
    }

    public void setExecutionYear(ExecutionYear executionYear) {
        this.executionYear = executionYear;
    }

    public List<CreditsPoolByDepartmentBean> getCreditsPoolByDepartments() {
        return creditsPoolByDepartments;
    }

    public void setCreditsPoolByDepartments(List<CreditsPoolByDepartmentBean> creditsPoolByDepartments) {
        this.creditsPoolByDepartments = creditsPoolByDepartments;
    }

    public Boolean getCanEditCreditsPool() {
        if (getExecutionYear() != null) {
            AnnualCreditsState annualCreditsState = AnnualCreditsState.getAnnualCreditsState(getExecutionYear());
            return !annualCreditsState.getIsFinalCreditsCalculated() && !annualCreditsState.getIsCreditsClosed();
        }
        return false;
    }

    public class CreditsPoolByDepartmentBean implements Serializable {
        protected Department department;
        protected BigDecimal originalCreditsPool = BigDecimal.ZERO;
        protected BigDecimal creditsPool = BigDecimal.ZERO;

        public CreditsPoolByDepartmentBean() {
        }

        public CreditsPoolByDepartmentBean(Department department) {
            setDepartment(department);
            DepartmentCreditsPool departmentCreditsPool =
                    DepartmentCreditsPool.getDepartmentCreditsPool(department, executionYear);
            if (departmentCreditsPool != null) {
                setOriginalCreditsPool(departmentCreditsPool.getOriginalCreditsPool());
                setCreditsPool(departmentCreditsPool.getCreditsPool());
            }
        }

        public Department getDepartment() {
            return department;
        }

        public void setDepartment(Department department) {
            this.department = department;
        }

        public BigDecimal getOriginalCreditsPool() {
            return originalCreditsPool;
        }

        public void setOriginalCreditsPool(BigDecimal originalCreditsPool) {
            this.originalCreditsPool = originalCreditsPool;
        }

        public BigDecimal getCreditsPool() {
            return creditsPool;
        }

        public void setCreditsPool(BigDecimal creditsPool) {
            this.creditsPool = creditsPool;
        }

    }
}