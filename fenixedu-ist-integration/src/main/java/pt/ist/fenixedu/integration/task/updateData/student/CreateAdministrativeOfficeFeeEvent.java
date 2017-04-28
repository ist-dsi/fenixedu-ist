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
import java.util.function.Predicate;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.events.AccountingEventsManager;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcessState;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.util.InvocationResult;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

@Task(englishTitle = "CreateAdministrativeOfficeFeeEvent", readOnly = true)
public class CreateAdministrativeOfficeFeeEvent extends CronTask {
    private int AdministrativeOfficeFee_TOTAL_CREATED = 0;
    private int InsuranceEvent_TOTAL_CREATED = 0;

    private final Predicate<DegreeType> acceptedDegreeTypesForAdministrativeOfficeFeeAndInsuranceEvent = DegreeType.oneOf(
            DegreeType::isPreBolonhaDegree, DegreeType::isBolonhaDegree, DegreeType::isBolonhaMasterDegree,
            DegreeType::isIntegratedMasterDegree, DegreeType::isEmpty, DegreeType::isAdvancedFormationDiploma,
            DegreeType::isSpecializationDegree);

    private void createAdministrativeOfficeFeeEvent(StudentCurricularPlan scp, ExecutionYear executionYear) {
        try {
            createAdministrativeOfficeFeeEventAtomic(executionYear, scp);
        } catch (Exception e) {
            taskLog("Exception on student curricular plan with oid : %s\n", scp.getExternalId());
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            taskLog(writer.toString());
        }

    }

    @Atomic(mode = TxMode.WRITE)
    private void createAdministrativeOfficeFeeEventAtomic(ExecutionYear executionYear, StudentCurricularPlan scp) {
        final AccountingEventsManager manager = new AccountingEventsManager();
        final InvocationResult result;
        if (!acceptedDegreeTypesForAdministrativeOfficeFeeAndInsuranceEvent.test(scp.getDegreeType())) {
            return;
        }
        result = manager.createAdministrativeOfficeFeeAndInsuranceEvent(scp, executionYear);
        if (result.isSuccess()) {
            AdministrativeOfficeFee_TOTAL_CREATED++;
        }
    }

    private void createInsuranceEvent(Person person, ExecutionYear executionYear) {
        try {
            createInsuranceEventAtomic(executionYear, person);
        } catch (Exception e) {
            taskLog("Exception on person with oid : %s\n", person.getExternalId());
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            taskLog(writer.toString());
        }

    }

    @Atomic(mode = TxMode.WRITE)
    private void createInsuranceEventAtomic(ExecutionYear executionYear, Person person) {
        final AccountingEventsManager manager = new AccountingEventsManager();
        final InvocationResult result = manager.createInsuranceEvent(person, executionYear);
        if (result.isSuccess()) {
            InsuranceEvent_TOTAL_CREATED++;
        }
    }

    @Override
    public void runTask() {
        ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        for (final Student student : Bennu.getInstance().getStudentsSet()) {
            for (final Registration registration : student.getRegistrationsSet()) {
                final StudentCurricularPlan studentCurricularPlan = registration.getLastStudentCurricularPlan();
                if (studentCurricularPlan != null) {
                    createAdministrativeOfficeFeeEvent(studentCurricularPlan, executionYear);
                }
            }
            if (student.getPerson() != null) {
                for (final PhdIndividualProgramProcess process : student.getPerson().getPhdIndividualProgramProcessesSet()) {
                    if (process.getActiveState() == PhdIndividualProgramProcessState.WORK_DEVELOPMENT) {
                        createInsuranceEvent(student.getPerson(), executionYear);
                        break;
                    }
                }
            }
        }
        taskLog("Created %s AdministrativeOfficeFee events\n", AdministrativeOfficeFee_TOTAL_CREATED);
        taskLog("Created %s InsuranceEvent events\n", InsuranceEvent_TOTAL_CREATED);
    }

}