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
import pt.ist.fenixedu.quc.domain.TeacherInquiryTemplate;
import pt.ist.fenixframework.FenixFramework;

@StrutsFunctionality(app = PedagogicalControlApp.class, path = "view-quc-teacher-status",
        titleKey = "title.inquiries.teachers.status", bundle = "InquiriesResources")
@Mapping(path = "/qucTeachersStatus", module = "pedagogicalCouncil")
@Forwards({ @Forward(name = "viewQucTeachersState", path = "/pedagogicalCouncil/inquiries/viewQucTeachersStatus.jsp") })
public class ViewQucTeacherStatus extends FenixDispatchAction {

    @EntryPoint
    public ActionForward prepare(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        final TeacherInquiryTemplate teacherInquiryTemplate =
                TeacherInquiryTemplate.getTemplateByExecutionPeriod(ExecutionSemester.readActualExecutionSemester()
                        .getPreviousExecutionPeriod());
        if (teacherInquiryTemplate != null) {
            request.setAttribute("teacherInquiryOID", teacherInquiryTemplate.getExternalId());
        }
        return mapping.findForward("viewQucTeachersState");
    }

    public ActionForward dowloadReport(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        final TeacherInquiryTemplate teacherInquiryTemplate =
                FenixFramework.getDomainObject(getFromRequest(request, "teacherInquiryOID").toString());

        final ExecutionSemester executionPeriod = teacherInquiryTemplate.getExecutionPeriod();

        final List<TeacherBean> teachersList = new ArrayList<TeacherBean>();
        for (Professorship professorship : Bennu.getInstance().getProfessorshipsSet()) {
            if (professorship.getExecutionCourse().getExecutionPeriod() == executionPeriod) {
                Person person = professorship.getPerson();
                boolean isToAnswer = TeacherInquiryTemplate.hasToAnswerTeacherInquiry(person, professorship);
                if (isToAnswer) {
                    boolean hasMandatoryCommentsToMake = InquiryResultComment.hasMandatoryCommentsToMake(professorship);
                    Department department = null;
                    if (person.getEmployee() != null) {
                        department =
                                person.getEmployee().getLastDepartmentWorkingPlace(
                                        teacherInquiryTemplate.getExecutionPeriod().getBeginDateYearMonthDay(),
                                        teacherInquiryTemplate.getExecutionPeriod().getEndDateYearMonthDay());
                    }
                    TeacherBean teacherBean = new TeacherBean(department, person, professorship);
                    teacherBean.setCommentsToMake(hasMandatoryCommentsToMake);
                    int questionsToAnswer =
                            professorship.getInquiryTeacherAnswer() != null ? teacherInquiryTemplate.getNumberOfQuestions()
                                    - professorship.getInquiryTeacherAnswer().getNumberOfAnsweredQuestions() : teacherInquiryTemplate
                                    .getNumberOfQuestions();
                    int mandatoryQuestionsToAnswer =
                            professorship.getInquiryTeacherAnswer() != null ? teacherInquiryTemplate
                                    .getNumberOfRequiredQuestions()
                                    - professorship.getInquiryTeacherAnswer().getNumberOfAnsweredRequiredQuestions() : teacherInquiryTemplate
                                    .getNumberOfRequiredQuestions();
                    //there are conditions that make appear a new set of questions and some can be mandatory, 
                    //thus the number of mandatory answered questions can be greater than the default number of mandatory questions
                    teacherBean.setMandatoryQuestionsToAnswer(Math.abs(mandatoryQuestionsToAnswer));
                    teacherBean.setQuestionsToAnswer(questionsToAnswer);
                    teachersList.add(teacherBean);
                }
            }
        }

        Spreadsheet spreadsheet = createReport(teachersList);
        StringBuilder filename = new StringBuilder("Relatório_preenchimento_Docentes_");
        filename.append(new DateTime().toString("yyyy_MM_dd_HH_mm"));

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-disposition", "attachment; filename=" + filename + ".xls");

        OutputStream outputStream = response.getOutputStream();
        spreadsheet.exportToXLSSheet(outputStream);
        outputStream.flush();
        outputStream.close();
        return null;
    }

    private Spreadsheet createReport(List<TeacherBean> teachersList) throws IOException {
        Spreadsheet spreadsheet = new Spreadsheet("Relatório Docentes QUC");
        spreadsheet.setHeader("Departamento");
        spreadsheet.setHeader("Docente");
        spreadsheet.setHeader("Nº Mec");
        spreadsheet.setHeader("Telefone");
        spreadsheet.setHeader("Email");
        spreadsheet.setHeader("Comentários obrigatórios por fazer");
        spreadsheet.setHeader("Perguntas obrigatórias por responder");
        spreadsheet.setHeader("Perguntas por responder");
        spreadsheet.setHeader("Disciplina");
        spreadsheet.setHeader("Disciplina sujeita auditoria?");

        for (TeacherBean teacherBean : teachersList) {
            Row row = spreadsheet.addRow();
            row.setCell(teacherBean.getDepartment() != null ? teacherBean.getDepartment().getName() : "-");
            row.setCell(teacherBean.getTeacher().getName());
            row.setCell(teacherBean.getTeacher().getUsername());
            row.setCell(teacherBean.getTeacher().getDefaultMobilePhoneNumber());
            row.setCell(teacherBean.getTeacher().getDefaultEmailAddressValue());
            row.setCell(teacherBean.isCommentsToMake() ? "Sim" : "Não");
            row.setCell(teacherBean.getMandatoryQuestionsToAnswer());
            row.setCell(teacherBean.getQuestionsToAnswer());
            row.setCell(teacherBean.getProfessorship().getExecutionCourse().getName());
            row.setCell(InquiryResult.canBeSubjectToQucAudit(teacherBean.getProfessorship().getExecutionCourse()) ? "Sim" : "Não");
        }

        return spreadsheet;
    }

    class TeacherBean {
        private Department department;
        private Person teacher;
        private Professorship professorship;
        private boolean commentsToMake;
        private int questionsToAnswer;
        private int mandatoryQuestionsToAnswer;

        public TeacherBean(Department department, Person teacher, Professorship professorship) {
            setDepartment(department);
            setTeacher(teacher);
            setProfessorship(professorship);
        }

        public void setDepartment(Department department) {
            this.department = department;
        }

        public Department getDepartment() {
            return department;
        }

        public void setTeacher(Person teacher) {
            this.teacher = teacher;
        }

        public Person getTeacher() {
            return teacher;
        }

        public Professorship getProfessorship() {
            return professorship;
        }

        public void setProfessorship(Professorship professorship) {
            this.professorship = professorship;
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
    }
}
