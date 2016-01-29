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
package pt.ist.fenixedu.integration.domain.accounting.events.export;

import java.util.List;

import org.fenixedu.academic.domain.QueueJob;
import org.fenixedu.academic.domain.QueueJobResult;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class SIBSOutgoingPaymentQueueJob extends SIBSOutgoingPaymentQueueJob_Base {

    public SIBSOutgoingPaymentQueueJob(DateTime lastSuccessfulSentPaymentFileDate) {
        super();
        setLastSuccessfulSentPaymentFileDate(lastSuccessfulSentPaymentFileDate);
    }

    @Override
    public QueueJobResult execute() throws Exception {
        new SIBSOutgoingPaymentFile(getLastSuccessfulSentPaymentFileDate());
        return new QueueJobResult();
    }

    @Atomic
    public static SIBSOutgoingPaymentQueueJob launchJob(DateTime lastSuccessfulSentPaymentFileDate) {
        return new SIBSOutgoingPaymentQueueJob(lastSuccessfulSentPaymentFileDate);
    }

    public static List<SIBSOutgoingPaymentQueueJob> readAllSIBSOutgoingPaymentQueueJobs() {
        return Lists.newArrayList(Iterables.filter(Bennu.getInstance().getJobsSet(), SIBSOutgoingPaymentQueueJob.class));
    }

    public static SIBSOutgoingPaymentQueueJob getQueueJobNotDoneAndNotCancelled() {
        return readAllSIBSOutgoingPaymentQueueJobs().stream().filter(QueueJob::getIsNotDoneAndNotCancelled).findFirst()
                .orElse(null);
    }

    public static boolean hasExportationQueueJobToRun() {
        return getQueueJobNotDoneAndNotCancelled() != null;
    }

}
