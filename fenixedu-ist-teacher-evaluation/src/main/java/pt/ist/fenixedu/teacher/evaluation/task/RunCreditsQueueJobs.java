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
package pt.ist.fenixedu.teacher.evaluation.task;

import org.fenixedu.academic.domain.QueueJob;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.teacher.evaluation.domain.credits.AnnualCreditsState;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.CalculateCreditsQueueJob;
import pt.ist.fenixedu.teacher.evaluation.domain.credits.CloseCreditsQueueJob;

@Task(englishTitle = "RunCreditsQueueJobs", readOnly = false)
public class RunCreditsQueueJobs extends CronTask {

    @Override
    public void runTask() throws Exception {
        LocalDate today = new LocalDate();
        for (AnnualCreditsState annualCreditsState : Bennu.getInstance().getAnnualCreditsStatesSet()) {
            if (!annualCreditsState.getIsFinalCreditsCalculated() && annualCreditsState.getFinalCalculationDate() != null
                    && !today.isBefore(annualCreditsState.getFinalCalculationDate())
                    && !alreadyLaunched(CalculateCreditsQueueJob.class.getName())) {
                new CalculateCreditsQueueJob(annualCreditsState.getExecutionYear());
                taskLog("Lauched CalculateCreditsQueueJob");
            }
            if (!annualCreditsState.getIsCreditsClosed() && annualCreditsState.getCloseCreditsDate() != null
                    && !today.isBefore(annualCreditsState.getCloseCreditsDate())
                    && !alreadyLaunched(CloseCreditsQueueJob.class.getName())) {
                new CloseCreditsQueueJob(annualCreditsState.getExecutionYear());
                taskLog("Lauched CloseCreditsQueueJob");
            }
        }
    }

    private boolean alreadyLaunched(String className) {
        for (QueueJob queueJob : Bennu.getInstance().getQueueJobUndoneSet()) {
            if (queueJob.getClass().getName().equals(className)) {
                return true;
            }
        }
        return false;
    }

}
