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
package pt.ist.fenixedu.integration.task.updateData.inquiries;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import pt.ist.fenixedu.quc.domain.CoordinatorInquiryTemplate;
import pt.ist.fenixedu.quc.domain.InquiryBlock;

public class CreateCoordinatorInquiry extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionSemester currentExecutionSemester = ExecutionSemester.readActualExecutionSemester();
        DateTime begin = new DateTime(2012, 5, 16, 0, 0, 0, 0);
        DateTime end = new DateTime(2012, 5, 20, 0, 0, 0, 0);

        CoordinatorInquiryTemplate newCoordinatorInquiryTemplate = new CoordinatorInquiryTemplate(begin, end, true);
        newCoordinatorInquiryTemplate.setExecutionPeriod(currentExecutionSemester);

        CoordinatorInquiryTemplate previousCoordinatorInquiryTemplate =
                CoordinatorInquiryTemplate.getTemplateByExecutionPeriod(currentExecutionSemester.getPreviousExecutionPeriod());
        for (InquiryBlock inquiryBlock : previousCoordinatorInquiryTemplate.getInquiryBlocksSet()) {
            newCoordinatorInquiryTemplate.addInquiryBlocks(inquiryBlock);
        }
    }
}
