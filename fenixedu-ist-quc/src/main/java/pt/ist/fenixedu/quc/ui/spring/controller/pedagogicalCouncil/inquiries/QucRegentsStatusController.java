package pt.ist.fenixedu.quc.ui.spring.controller.pedagogicalCouncil.inquiries;

import com.google.common.collect.Lists;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.messaging.core.domain.Message;
import org.fenixedu.messaging.core.domain.Sender;
import org.fenixedu.messaging.core.template.DeclareMessageTemplate;
import org.fenixedu.messaging.core.template.TemplateParameter;

import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.InquiryResultComment;
import pt.ist.fenixedu.quc.domain.RegentInquiryTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@DeclareMessageTemplate(
        id = "quc.regent.warning.template",
        description = "quc.regent.warning.description",
        subject = "quc.regent.warning.subject",
        text = "quc.regent.warning.text",
        parameters = {
                @TemplateParameter(id = "regentName", description = "quc.regent.warning.parameter.regentName"),
                @TemplateParameter(id = "executionCoursesNames", description = "quc.regent.warning.parameter.executionCoursesNames"),
                @TemplateParameter(id = "endDateString", description = "quc.regent.warning.parameter.endDateString"),
                @TemplateParameter(id = "replyToAddress", description = "quc.regent.warning.parameter.replyToAddress"),
                @TemplateParameter(id = "semesterQualifiedName", description = "quc.regent.warning.parameter.semesterQualifiedName"),
        },
        bundle = "resources.InquiriesResources"
)

@SpringApplication(hint = "Pedagogical Council", group = "role(PEDAGOGICAL_COUNCIL)", path = "view-quc-regents-status", title = "title.inquiries.regents.status")
@SpringFunctionality(accessGroup = "role(PEDAGOGICAL_COUNCIL)", app = QucRegentsStatusController.class, title = "title.inquiries.regents.status")
@RequestMapping("/view-quc-regents-status")
public class QucRegentsStatusController {
    @RequestMapping(method = RequestMethod.GET)
    public String home(Model model){
        final RegentInquiryTemplate regentInquiryTemplate =
                RegentInquiryTemplate.getTemplateByExecutionPeriod(ExecutionSemester.readActualExecutionSemester()
                        .getPreviousExecutionPeriod());
        if (regentInquiryTemplate != null) {
            model.addAttribute("regentInquiry", regentInquiryTemplate);
        }
        return "fenixedu-ist-quc/pedagogicalCouncil/inquiries/viewQucRegentsStatus";
    }

    @RequestMapping(method = RequestMethod.GET, path = "/downloadReport/{regentInquiry}")
    public String downloadReport(@PathVariable RegentInquiryTemplate regentInquiry,  HttpServletResponse response) throws IOException {
        final ExecutionSemester executionPeriod = regentInquiry.getExecutionPeriod();

        final List<RegentBean> regentsList = new ArrayList<>();
        for (Professorship professorship : Bennu.getInstance().getProfessorshipsSet()) {
            if (professorship.getExecutionCourse().getExecutionPeriod() == executionPeriod) {
                Person regent = professorship.getPerson();
                boolean isToAnswer = RegentInquiryTemplate.hasToAnswerRegentInquiry(professorship);
                if (isToAnswer) {
                    boolean hasMandatoryCommentsToMake =
                            InquiryResultComment.hasMandatoryCommentsToMakeAsResponsible(professorship);
                    Department department = null;
                    if (regent.getEmployee() != null) {
                        department = regent.getEmployee().getLastDepartmentWorkingPlace(
                                        regentInquiry.getExecutionPeriod().getBeginDateYearMonthDay(),
                                        regentInquiry.getExecutionPeriod().getEndDateYearMonthDay());
                    }
                    int questionsToAnswer =
                            professorship.getInquiryRegentAnswer() != null ? regentInquiry.getNumberOfQuestions()
                                    - professorship.getInquiryRegentAnswer().getNumberOfAnsweredQuestions() : regentInquiry
                                    .getNumberOfQuestions();
                    int mandatoryQuestionsToAnswer =
                            professorship.getInquiryRegentAnswer() != null ? regentInquiry.getNumberOfRequiredQuestions()
                                    - professorship.getInquiryRegentAnswer().getNumberOfAnsweredRequiredQuestions() : regentInquiry
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


    @RequestMapping(method = RequestMethod.GET, path = "/sendRegentsMail/{regentInquiry}")
    public String prepareSendTeachersMail(Model model, @PathVariable RegentInquiryTemplate regentInquiry) {
        model.addAttribute("regentInquiry", regentInquiry);
        model.addAttribute("executionSemesterName", regentInquiry.getExecutionPeriod().getQualifiedName());
        return "fenixedu-ist-quc/pedagogicalCouncil/inquiries/confirmSendRegentsQucWarningEmail";
    }

    @RequestMapping(method = RequestMethod.POST, path = "/sendRegentsMail")
    public String sendRegentsMail(Model model, @RequestParam RegentInquiryTemplate regentInquiry,
            @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date replyEndDate) {
        final Sender sender = getCPSender();
        final DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.forLanguageTag("pt"));
        getRegents(regentInquiry).forEach((regent, executionCourses) -> {
            if (regent.getDefaultEmailAddressValue() != null) {
                sendMessage(regentInquiry, df.format(replyEndDate), sender, regent, executionCourses);
            }
        });
        model.addAttribute("senderName", sender.getName());
        return "fenixedu-ist-quc/pedagogicalCouncil/inquiries/sentRegentsQucWarningEmails";
    }

    private void sendMessage(RegentInquiryTemplate regentInquiry, String replyEndDate, Sender sender, Person regent, List<ExecutionCourse> executionCourses) {
        Message.from(sender)
                .template("quc.regent.warning.template")
                    .parameter("regentName", regent.getName())
                    .parameter("executionCoursesNames", Lists.transform(executionCourses, ExecutionCourse::getName))
                    .parameter("endDateString", replyEndDate)
                    .parameter("replyToAddress", sender.getReplyTo())
                    .parameter("semesterQualifiedName", regentInquiry.getExecutionPeriod().getQualifiedName())
                .and()
                .replyToSender()
                .singleBcc(regent.getDefaultEmailAddressValue())
                .wrapped()
                .send();
    }

    private Map<Person, List<ExecutionCourse>> getRegents(RegentInquiryTemplate regentInquiryTemplate) {
        final Map<Person, List<ExecutionCourse>> regentsMap = new HashMap<>();
        if (regentInquiryTemplate != null) {
            final ExecutionSemester executionPeriod = regentInquiryTemplate.getExecutionPeriod();
            Map<Person, List<ExecutionCourse>> allExecutionCoursesMap = new HashMap<>();

            for (Professorship professorship : Bennu.getInstance().getProfessorshipsSet()) {
                if (professorship.getExecutionCourse().getExecutionPeriod() == executionPeriod) {
                    if (RegentInquiryTemplate.hasToAnswerRegentInquiry(professorship)) {
                        allExecutionCoursesMap.computeIfAbsent(professorship.getPerson(), k -> new ArrayList<>());
                        allExecutionCoursesMap.get(professorship.getPerson()).add(professorship.getExecutionCourse());

                        if (professorship.getInquiryRegentAnswer() == null
                                || professorship.getInquiryRegentAnswer().hasRequiredQuestionsToAnswer(regentInquiryTemplate)
                                || InquiryResultComment.hasMandatoryCommentsToMakeAsResponsible(professorship)) {
                            regentsMap.computeIfAbsent(professorship.getPerson(), k -> new ArrayList<>());
                            regentsMap.get(professorship.getPerson()).add(professorship.getExecutionCourse());
                        }
                    }
                }
            }
            for (Person regent : allExecutionCoursesMap.keySet()) {
                List<ExecutionCourse> allExecutionCourses = allExecutionCoursesMap.get(regent);
                List<ExecutionCourse> executionCoursesToAnswer = regentsMap.get(regent);
                if (executionCoursesToAnswer != null) {
                    executionCoursesToAnswer.forEach(ec -> {
                        if (!allExecutionCourses.remove(ec)) {
                            allExecutionCourses.add(ec);
                        }
                    });
                }
                for (ExecutionCourse executionCourse : allExecutionCourses) {
                    if (InquiryResultComment.hasMandatoryCommentsToMakeAsRegentInUC(regent, executionCourse)) {
                        if (executionCoursesToAnswer == null) {
                            executionCoursesToAnswer = new ArrayList<>();
                            regentsMap.put(regent, executionCoursesToAnswer);
                        }
                        executionCoursesToAnswer.add(executionCourse);
                    }
                }
            }
        }
        return regentsMap;
    }

    private Sender getCPSender() throws NoSuchElementException {
        return Sender.all().stream()
                .filter(sender -> sender.getName().equalsIgnoreCase("Técnico Lisboa (Conselho Pedagógico)"))
                .findFirst().orElseThrow(() -> new NoSuchElementException("No sender available for given name"));
    }

    private Spreadsheet createReport(List<RegentBean> regentsList) {
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
            Spreadsheet.Row row = spreadsheet.addRow();
            row.setCell(regentBean.getDepartment() != null ? regentBean.getDepartment().getName() : "-");
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
