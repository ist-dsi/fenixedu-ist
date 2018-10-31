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
package pt.ist.fenixedu.integration.task.updateData.student;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.function.Predicate;

import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.events.AccountingEventsManager;
import org.fenixedu.academic.domain.accounting.events.gratuity.DfaGratuityEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEventWithPaymentPlan;
import org.fenixedu.academic.domain.accounting.events.gratuity.SpecializationDegreeGratuityEvent;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.util.InvocationResult;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.commons.i18n.I18N;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

@Task(englishTitle = "CreateGratuityEvents", readOnly = true)
public class CreateGratuityEvents extends CronTask {

    private final Predicate<DegreeType> acceptedDegreeTypesForGratuityEvent = DegreeType.oneOf(DegreeType::isPreBolonhaDegree,
            DegreeType::isBolonhaDegree, DegreeType::isBolonhaMasterDegree, DegreeType::isIntegratedMasterDegree,
            DegreeType::isAdvancedFormationDiploma, DegreeType::isSpecializationDegree);

    private int GratuityEvent_TOTAL_CREATED = 0;

    private void generateGratuityEventsForAllStudents(final ExecutionYear executionYear) {
        for (final ExecutionDegree executionDegree : executionYear.getExecutionDegreesSet()) {
            for (final StudentCurricularPlan studentCurricularPlan : executionDegree.getDegreeCurricularPlan()
                    .getStudentCurricularPlansSet()) {
                generateGratuityEvents(executionYear, studentCurricularPlan);
            }
        }
    }

    private void generateGratuityEvents(final ExecutionYear executionYear, final StudentCurricularPlan studentCurricularPlan) {

        if (!studentCurricularPlan.isBolonhaDegree() || !studentCurricularPlan.hasRegistration()) {
            return;
        }

        if (!acceptedDegreeTypesForGratuityEvent.test(studentCurricularPlan.getDegreeType())) {
            return;
        }

        if (studentCurricularPlan.getDegreeType().isAdvancedFormationDiploma()) {
            if (studentCurricularPlan.getRegistration().hasGratuityEvent(executionYear, DfaGratuityEvent.class)) {
                return;
            }
        } else if (studentCurricularPlan.getDegreeType().isSpecializationDegree()) {
            if (studentCurricularPlan.getRegistration().hasGratuityEvent(executionYear, SpecializationDegreeGratuityEvent.class)) {
                return;
            }
        } else if(!studentCurricularPlan.getDegreeType().isEmpty()) {
            if (studentCurricularPlan.getRegistration().hasGratuityEvent(executionYear, GratuityEventWithPaymentPlan.class)) {
                return;
            }
        }

        generateGratuityEvent(studentCurricularPlan,executionYear);
    }

    private void generateGratuityEvent(StudentCurricularPlan studentCurricularPlan, ExecutionYear executionYear) {
        try {
            generateGratuityEventAtomic(executionYear, studentCurricularPlan);
        } catch (Exception e) {
            taskLog("Exception on student curricular plan with oid : %s\n", studentCurricularPlan.getExternalId());
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            taskLog(writer.toString());
        }

    }

    @Atomic(mode = TxMode.WRITE)
    private void generateGratuityEventAtomic(ExecutionYear executionYear, StudentCurricularPlan studentCurricularPlan) {
        final AccountingEventsManager manager = new AccountingEventsManager();
        final InvocationResult result = manager.createGratuityEvent(studentCurricularPlan, executionYear);
        if (result.isSuccess()) {
            GratuityEvent_TOTAL_CREATED++;
        }
    }

    @Override
    public void runTask() {
        I18N.setLocale(new Locale("PT", "pt"));
        generateGratuityEventsForAllStudents(ExecutionYear.readCurrentExecutionYear());
        taskLog("Created %s GratuityEvent events\n", GratuityEvent_TOTAL_CREATED);
    }
}
