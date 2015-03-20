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
/**
 * 
 */
package pt.ist.fenixedu.quc.ui.renderers;

import pt.ist.fenixWebFramework.renderers.components.HtmlLink;
import pt.ist.fenixWebFramework.renderers.components.HtmlTableCell;
import pt.ist.fenixWebFramework.renderers.components.HtmlTableRow;
import pt.ist.fenixedu.quc.dto.BlockResumeResult;
import pt.ist.fenixedu.quc.dto.TeacherShiftTypeGroupsResumeResult;

/**
 * @author - Ricardo Rodrigues (ricardo.rodrigues@ist.utl.pt)
 * 
 */
public class InquiryTeacherShiftTypeResumeRenderer extends InquiryBlocksResumeRenderer {

    @Override
    protected void createFinalCells(HtmlTableRow tableRow, BlockResumeResult blockResumeResult) {

        HtmlTableCell linksCell = tableRow.createCell();
        String resultsParameters = buildParametersForResults(blockResumeResult);

        HtmlLink link = new HtmlLink();
        link.setModule("/publico");
        link.setUrl("/viewTeacherResults.do?" + resultsParameters);
        link.setTarget("_blank");
        link.setText("Resultados");

        linksCell.setBody(link);
        linksCell.setClasses("col-actions");
    }

    private String buildParametersForResults(BlockResumeResult blocksResumeResult) {
        TeacherShiftTypeGroupsResumeResult teacherShiftResume = (TeacherShiftTypeGroupsResumeResult) blocksResumeResult;
        StringBuilder builder = new StringBuilder();
        builder.append("professorshipOID=").append(teacherShiftResume.getProfessorship().getExternalId());
        builder.append("&shiftType=").append(teacherShiftResume.getShiftType().name());
        return builder.toString();
    }

}
