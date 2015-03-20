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

import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.ResultsInquiryTemplate;

public class CreateCourseResultInquiry extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionSemester currentExecutionSemester = ExecutionSemester.readActualExecutionSemester();

        ResultsInquiryTemplate newResultsInquiryTemplate = new ResultsInquiryTemplate();
        newResultsInquiryTemplate.setExecutionPeriod(currentExecutionSemester);

        ResultsInquiryTemplate previousResultsInquiryTemplate =
                ResultsInquiryTemplate.getTemplateByExecutionPeriod(currentExecutionSemester.getPreviousExecutionPeriod());
        for (InquiryBlock inquiryBlock : previousResultsInquiryTemplate.getInquiryBlocksSet()) {
            newResultsInquiryTemplate.addInquiryBlocks(inquiryBlock);
        }
    }
}
