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
package pt.ist.fenixedu.teacher.evaluation.domain.credits;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.QueueJobResult;
import org.joda.time.DateTime;

public class CloseCreditsQueueJob extends CloseCreditsQueueJob_Base {

    public CloseCreditsQueueJob(ExecutionYear executionYear) {
        super();
        setExecutionYear(executionYear);
    }

    @Override
    public QueueJobResult execute() throws Exception {
        AnnualCreditsState annualCreditsState = AnnualCreditsState.getAnnualCreditsState(getExecutionYear());
        if (!annualCreditsState.getIsCreditsClosed()) {
            if (!annualCreditsState.getIsFinalCreditsCalculated()) {
                CalculateCreditsQueueJob calculateCreditsQueueJob = new CalculateCreditsQueueJob(getExecutionYear());
                calculateCreditsQueueJob.execute();
                calculateCreditsQueueJob.setDone(true);
                calculateCreditsQueueJob.setRootDomainObjectQueueUndone(null);
                calculateCreditsQueueJob.setJobEndTime(new DateTime());
            }
            annualCreditsState.setIsCreditsClosed(true);
        }
        return null;
    }

}
