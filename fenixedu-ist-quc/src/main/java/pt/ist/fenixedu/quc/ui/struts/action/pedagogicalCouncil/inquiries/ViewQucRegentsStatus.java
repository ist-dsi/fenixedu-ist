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
package pt.ist.fenixedu.quc.ui.struts.action.pedagogicalCouncil.inquiries;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.academic.ui.struts.action.pedagogicalCouncil.PedagogicalCouncilApp.PedagogicalControlApp;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;

import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.InquiryResultComment;
import pt.ist.fenixedu.quc.domain.RegentInquiryTemplate;
import pt.ist.fenixframework.FenixFramework;

@StrutsFunctionality(app = PedagogicalControlApp.class, path = "view-quc-regents-status",
        titleKey = "title.inquiries.regents.status", bundle = "InquiriesResources")
@Mapping(path = "/qucRegentsStatus", module = "pedagogicalCouncil")
@Forwards({ @Forward(name = "viewQucRegentsState", path = "/pedagogicalCouncil/inquiries/viewQucRegentsStatus.jsp") })
public class ViewQucRegentsStatus extends FenixDispatchAction {

    @EntryPoint
    public ActionForward prepare(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        final RegentInquiryTemplate regentInquiryTemplate =
                RegentInquiryTemplate.getTemplateByExecutionPeriod(ExecutionSemester.readActualExecutionSemester()
                        .getPreviousExecutionPeriod());
        if (regentInquiryTemplate != null) {
            request.setAttribute("regentInquiryOID", regentInquiryTemplate.getExternalId());
        }
        return mapping.findForward("viewQucRegentsState");
    }

    public ActionForward dowloadReport(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        final RegentInquiryTemplate regentInquiryTemplate =
                FenixFramework.getDomainObject(getFromRequest(request, "regentInquiryOID").toString());

        final ExecutionSemester executionPeriod = regentInquiryTemplate.getExecutionPeriod();

        final List<RegentBean> regentsList = new ArrayList<RegentBean>();
        for (Professorship professorship : Bennu.getInstance().getProfessorshipsSet()) {
            if (professorship.getExecutionCourse().getExecutionPeriod() == executionPeriod) {
                Person regent = professorship.getPerson();
                boolean isToAnswer = RegentInquiryTemplate.hasToAnswerRegentInquiry(professorship);
                if (isToAnswer) {
                    boolean hasMandatoryCommentsToMake =
                            InquiryResultComment.hasMandatoryCommentsToMakeAsResponsible(professorship);
                    Department department = null;
                    if (regent.getEmployee() != null) {
                        department =
                                regent.getEmployee().getLastDepartmentWorkingPlace(
                                        regentInquiryTemplate.getExecutionPeriod().getBeginDateYearMonthDay(),
                                        regentInquiryTemplate.getExecutionPeriod().getEndDateYearMonthDay());
                    }
                    int questionsToAnswer =
                            professorship.getInquiryRegentAnswer() != null ? regentInquiryTemplate.getNumberOfQuestions()
                                    - professorship.getInquiryRegentAnswer().getNumberOfAnsweredQuestions() : regentInquiryTemplate
                                    .getNumberOfQuestions();
                    int mandatoryQuestionsToAnswer =
                            professorship.getInquiryRegentAnswer() != null ? regentInquiryTemplate.getNumberOfRequiredQuestions()
                                    - professorship.getInquiryRegentAnswer().getNumberOfAnsweredRequiredQuestions() : regentInquiryTemplate
                                    .getNumberOfRequiredQuestions();
                    RegentBean regentBean = new RegentBean(department, regent, professorship);
                    //there are conditions that make appear a new set of questions and some can be mandatory, 
                    //thus the number of mandatory answered questions can be greater than the default number of mandatory questions
                    regentBean.setMandatoryQuestionsToAnswer(Math.abs(mandatoryQuestionsToAnswer));
                    regentBean.setQuestionsToAnswer(questionsToAnswer);
                    regentBean.setCommentsToMake(hasMandatoryCommentsToMake);
                    regentsList.add(regentBean);
                }
            }
        }

        Spreadsheet spreadsheet = createReport(regentsList);
        StringBuilder filename = new StringBuilder("Relatório_preenchimento_Regentes_");
        filename.append(new DateTime().toString("yyyy_MM_dd_HH_mm"));

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-disposition", "attachment; filename=" + filename + ".xls");

        OutputStream outputStream = response.getOutputStream();
        spreadsheet.exportToXLSSheet(outputStream);
        outputStream.flush();
        outputStream.close();
        return null;
    }

    private Spreadsheet createReport(List<RegentBean> regentsList) throws IOException {
        Spreadsheet spreadsheet = new Spreadsheet("Regentes em falta");
        spreadsheet.setHeader("Departamento");
        spreadsheet.setHeader("Regente");
        spreadsheet.setHeader("Nº Mec");
        spreadsheet.setHeader("Telefone");
        spreadsheet.setHeader("Email");
        spreadsheet.setHeader("Comentários obrigatórios por fazer");
        spreadsheet.setHeader("Perguntas obrigatórias por responder");
        spreadsheet.setHeader("Perguntas por responder");
        spreadsheet.setHeader("Disciplina");
        spreadsheet.setHeader("Disciplina sujeita auditoria?");

        for (RegentBean regentBean : regentsList) {
            Row row = spreadsheet.addRow();
            row.setCell(regentBean.getDepartment() != null ? regentBean.getDepartment().getName().getContent(
                    Locale.forLanguageTag("pt")) : "-");
            row.setCell(regentBean.getRegent().getName());
            row.setCell(regentBean.getRegent().getUsername());
            row.setCell(regentBean.getRegent().getDefaultMobilePhoneNumber());
            row.setCell(regentBean.getRegent().getDefaultEmailAddressValue());
            row.setCell(regentBean.isCommentsToMake() ? "Sim" : "Não");
            row.setCell(regentBean.getMandatoryQuestionsToAnswer());
            row.setCell(regentBean.getQuestionsToAnswer());
            row.setCell(regentBean.getProfessorship().getExecutionCourse().getName());
            row.setCell(InquiryResult.canBeSubjectToQucAudit(regentBean.getProfessorship().getExecutionCourse()) ? "Sim" : "Não");
        }

        return spreadsheet;
    }

    class RegentBean {
        private Department department;
        private Person regent;
        private Professorship professorship;
        private boolean commentsToMake;
        private int questionsToAnswer;
        private int mandatoryQuestionsToAnswer;

        public RegentBean(Department department, Person regent, Professorship professorship) {
            setDepartment(department);
            setRegent(regent);
            setProfessorship(professorship);
        }

        public void setDepartment(Department department) {
            this.department = department;
        }

        public Department getDepartment() {
            return department;
        }

        public void setRegent(Person regent) {
            this.regent = regent;
        }

        public Person getRegent() {
            return regent;
        }

        public void setCommentsToMake(boolean commentsToMake) {
            this.commentsToMake = commentsToMake;
        }

        public boolean isCommentsToMake() {
            return commentsToMake;
        }

        public int getQuestionsToAnswer() {
            return questionsToAnswer;
        }

        public void setQuestionsToAnswer(int questionsToAnswer) {
            this.questionsToAnswer = questionsToAnswer;
        }

        public int getMandatoryQuestionsToAnswer() {
            return mandatoryQuestionsToAnswer;
        }

        public void setMandatoryQuestionsToAnswer(int mandatoryQuestionsToAnswer) {
            this.mandatoryQuestionsToAnswer = mandatoryQuestionsToAnswer;
        }

        public Professorship getProfessorship() {
            return professorship;
        }

        public void setProfessorship(Professorship professorship) {
            this.professorship = professorship;
        }
    }
}
