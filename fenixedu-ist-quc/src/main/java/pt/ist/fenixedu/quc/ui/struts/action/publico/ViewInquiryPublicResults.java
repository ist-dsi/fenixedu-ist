/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST QUC.
 *
 * FenixEdu IST QUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST QUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST QUC.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.quc.ui.struts.action.publico;

import java.util.Collection;
import java.util.List;

import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;

import pt.ist.fenixedu.quc.domain.GroupResultType;
import pt.ist.fenixedu.quc.domain.InquiryBlock;
import pt.ist.fenixedu.quc.domain.InquiryGroupQuestion;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.dto.GroupResultsSummaryBean;

public abstract class ViewInquiryPublicResults extends FenixDispatchAction {

    protected static GroupResultsSummaryBean getGeneralResults(List<InquiryResult> results,
            Collection<InquiryBlock> resultsBlocks, GroupResultType groupResultType) {
        for (InquiryBlock inquiryBlock : resultsBlocks) {
            for (InquiryGroupQuestion groupQuestion : inquiryBlock.getInquiryGroupsQuestionsSet()) {
                if (groupResultType.equals(groupQuestion.getGroupResultType())) {
                    return new GroupResultsSummaryBean(groupQuestion, results, null, null);
                }
            }
        }
        return null;
    }
}
