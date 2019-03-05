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
package pt.ist.fenixedu.quc.ui.struts.action.gep.inquiries;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.QueueJob;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;

import com.google.common.io.ByteStreams;
import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.quc.domain.ResultsImportationProcess;
import pt.ist.fenixedu.quc.domain.exceptions.FenixEduQucDomainException;
import pt.ist.fenixedu.quc.dto.ResultsFileBean;

/**
 * @author - Ricardo Rodrigues (ricardo.rodrigues@ist.utl.pt)
 * 
 */
@StrutsFunctionality(app = GepInquiriesApp.class, path = "upload-results", titleKey = "link.inquiries.uploadResults")
@Mapping(path = "/uploadInquiriesResults", module = "gep")
@Forwards(@Forward(name = "prepareUploadPage", path = "/gep/inquiries/uploadInquiriesResults.jsp"))
public class UploadInquiriesResultsDA extends FenixDispatchAction {

    private final int MAX_QUEUE_JOB_LIST_SIZE = 5;

    @EntryPoint
    public ActionForward prepare(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        List<ResultsImportationProcess> lastQueueJobs =
                Bennu.getInstance().getQueueJobSet().stream().filter(q -> q instanceof ResultsImportationProcess)
                        .map(q -> (ResultsImportationProcess) q).sorted((q1, q2) -> q2.getResultsImportationFile()
                                .getCreationDate().compareTo(q1.getResultsImportationFile().getCreationDate()))
                        .limit(5)
                        .collect(Collectors.toList());
        QueueJob.getLastJobsForClassOrSubClass(ResultsImportationProcess.class, MAX_QUEUE_JOB_LIST_SIZE);
        request.setAttribute("uploadFileBean", new ResultsFileBean());
        request.setAttribute("queueJobList", lastQueueJobs);
        return mapping.findForward("prepareUploadPage");
    }

    public ActionForward submitResultsFile(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {
        ResultsFileBean resultsBean = getRenderedObject("uploadFileBean");
        RenderUtils.invalidateViewState("uploadFileBean");
        try {
            byte[] content = ByteStreams.toByteArray(resultsBean.getInputStream());
            resultsBean.createImportationProcess(content);
            request.setAttribute("success", "true");
        } catch (IOException e) {
            addErrorMessage(request, e);
        }
        return prepare(mapping, actionForm, request, response);
    }
}
