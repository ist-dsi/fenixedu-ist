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
package pt.ist.fenixedu.contracts.tasks;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;

import pt.ist.fenixedu.contracts.service.UpdateTeacherAuthorizationsForSemesterFromSap;

@Task(englishTitle = "UpdateTeacherAuthorizations")
public class UpdateTeacherAuthorizations extends CronTask {

	private static final ExecutionYear FIRST_EXECUTION_YEAR = ExecutionYear.readExecutionYearByName("2018/2019");

	@Override
	public void runTask() {
		for (ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear(); executionYear != null
				&& executionYear
						.isAfterOrEquals(FIRST_EXECUTION_YEAR); executionYear = executionYear.getNextExecutionYear()) {
			taskLog(executionYear.getQualifiedName());
			executionYear.getExecutionPeriodsSet().stream().sorted(ExecutionSemester.COMPARATOR_BY_SEMESTER_AND_YEAR)
					.filter(executionSemester -> executionSemester.getTeacherAuthorizationStream().findAny()
							.isPresent())
					.forEach(executionSemester -> {
						taskLog(new UpdateTeacherAuthorizationsForSemesterFromSap()
								.updateTeacherAuthorization(executionSemester));
					});

		}
	}
}
