package pt.ist.fenixedu.teacher.evaluation.ui.spring;

import org.springframework.stereotype.Service;

import pt.ist.fenixedu.teacher.evaluation.domain.DepartmentCreditsPool;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.CreditsPoolBean;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.util.CreditsPoolBean.CreditsPoolByDepartmentBean;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

@Service
public class CreditsManagementService {

    @Atomic(mode = TxMode.WRITE)
    public void editCreditsPool(CreditsPoolBean creditsPoolBean) {
        for (CreditsPoolByDepartmentBean creditsPoolByDepartmentBean : creditsPoolBean.getCreditsPoolByDepartments()) {
            DepartmentCreditsPool departmentCreditsPool = DepartmentCreditsPool
                    .getDepartmentCreditsPool(creditsPoolByDepartmentBean.getDepartment(), creditsPoolBean.getExecutionYear());
            if (departmentCreditsPool == null) {
                new DepartmentCreditsPool(creditsPoolByDepartmentBean.getDepartment(), creditsPoolBean.getExecutionYear(),
                        creditsPoolByDepartmentBean.getOriginalCreditsPool(), creditsPoolByDepartmentBean.getCreditsPool());
            } else {
                departmentCreditsPool.setOriginalCreditsPool(creditsPoolByDepartmentBean.getOriginalCreditsPool());
                departmentCreditsPool.setCreditsPool(creditsPoolByDepartmentBean.getCreditsPool());
            }
        }

    }
}