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
import pt.ist.fenixedu.quc.domain.TeacherInquiryTemplate;

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
import java.util.Set;
import java.util.stream.Collectors;

@DeclareMessageTemplate(
        id = "quc.teacher.warning.template",
        description = "quc.teacher.warning.description",
        subject = "quc.teacher.warning.subject",
        text = "quc.teacher.warning.text",
        parameters = {
                @TemplateParameter(id = "teacherName", description = "quc.teacher.warning.parameter.teacherName"),
                @TemplateParameter(id = "executionCoursesNames", description = "quc.teacher.warning.parameter.executionCoursesNames"),
                @TemplateParameter(id = "endDateString", description = "quc.teacher.warning.parameter.endDateString"),
                @TemplateParameter(id = "replyToAddress", description = "quc.teacher.warning.parameter.replyToAddress"),
                @TemplateParameter(id = "semesterQualifiedName", description = "quc.teacher.warning.parameter.semesterQualifiedName"),
        },
        bundle = "resources.InquiriesResources"
)

@SpringApplication(hint = "Pedagogical Council", group = "role(PEDAGOGICAL_COUNCIL)", path = "view-quc-teachers-status", title = "title.inquiries.teachers.status")
@SpringFunctionality(accessGroup = "role(PEDAGOGICAL_COUNCIL)", app = QucTeachersStatusController.class, title = "title.inquiries.teachers.status")
@RequestMapping("/view-quc-teachers-status")
public class QucTeachersStatusController {

    @RequestMapping(method = RequestMethod.GET)
    public String home(Model model){
        final TeacherInquiryTemplate teacherInquiryTemplate =
                TeacherInquiryTemplate.getTemplateByExecutionPeriod(ExecutionSemester.readActualExecutionSemester()
                        .getPreviousExecutionPeriod());
        if (teacherInquiryTemplate != null) {
            model.addAttribute("teacherInquiry", teacherInquiryTemplate);
        }
        return "fenixedu-ist-quc/pedagogicalCouncil/inquiries/viewQucTeachersStatus";
    }

    @RequestMapping(method = RequestMethod.GET, path = "/downloadReport/{teacherInquiry}")
    public String downloadReport(@PathVariable TeacherInquiryTemplate teacherInquiry,  HttpServletResponse response) throws IOException {
        final ExecutionSemester executionPeriod = teacherInquiry.getExecutionPeriod();

        final List<TeacherBean> teachersList = new ArrayList<>();
        final Set<Professorship> professorships = Bennu.getInstance().getProfessorshipsSet().stream()
                .filter(professorship -> professorship.getExecutionCourse().getExecutionPeriod() == executionPeriod)
                .filter(professorship -> TeacherInquiryTemplate.hasToAnswerTeacherInquiry(professorship.getPerson(), professorship))
                .collect(Collectors.toSet());
        for (Professorship professorship : professorships) {
            Person person = professorship.getPerson();
            boolean hasMandatoryCommentsToMake = InquiryResultComment.hasMandatoryCommentsToMake(professorship);
            Department department = null;
            if (person.getEmployee() != null) {
                department = person.getEmployee().getLastDepartmentWorkingPlace(
                        executionPeriod.getBeginDateYearMonthDay(), executionPeriod.getEndDateYearMonthDay());
            }
            TeacherBean
                    teacherBean = new TeacherBean(department, person, professorship);
            teacherBean.setCommentsToMake(hasMandatoryCommentsToMake);
            int questionsToAnswer =
                    professorship.getInquiryTeacherAnswer() != null ? teacherInquiry.getNumberOfQuestions()
                            - professorship.getInquiryTeacherAnswer().getNumberOfAnsweredQuestions() : teacherInquiry
                            .getNumberOfQuestions();
            int mandatoryQuestionsToAnswer =
                    professorship.getInquiryTeacherAnswer() != null ? teacherInquiry
                            .getNumberOfRequiredQuestions()
                            - professorship.getInquiryTeacherAnswer().getNumberOfAnsweredRequiredQuestions() : teacherInquiry
                            .getNumberOfRequiredQuestions();
            //there are conditions that make appear a new set of questions and some can be mandatory,
            //thus the number of mandatory answered questions can be greater than the default number of mandatory questions
            teacherBean.setMandatoryQuestionsToAnswer(Math.abs(mandatoryQuestionsToAnswer));
            teacherBean.setQuestionsToAnswer(questionsToAnswer);
            teachersList.add(teacherBean);
        }
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
                                        teacherInquiry.getExecutionPeriod().getBeginDateYearMonthDay(),
                                        teacherInquiry.getExecutionPeriod().getEndDateYearMonthDay());
                    }
                    TeacherBean
                            teacherBean = new TeacherBean(department, person, professorship);
                    teacherBean.setCommentsToMake(hasMandatoryCommentsToMake);
                    int questionsToAnswer =
                            professorship.getInquiryTeacherAnswer() != null ? teacherInquiry.getNumberOfQuestions()
                                    - professorship.getInquiryTeacherAnswer().getNumberOfAnsweredQuestions() : teacherInquiry
                                    .getNumberOfQuestions();
                    int mandatoryQuestionsToAnswer =
                            professorship.getInquiryTeacherAnswer() != null ? teacherInquiry
                                    .getNumberOfRequiredQuestions()
                                    - professorship.getInquiryTeacherAnswer().getNumberOfAnsweredRequiredQuestions() : teacherInquiry
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

    @RequestMapping(method = RequestMethod.GET, path = "/sendTeachersMail/{teacherInquiry}")
    public String prepareSendTeachersMail(Model model, @PathVariable TeacherInquiryTemplate teacherInquiry) {
        model.addAttribute("teacherInquiry", teacherInquiry);
        model.addAttribute("executionSemesterName", teacherInquiry.getExecutionPeriod().getQualifiedName());
        return "fenixedu-ist-quc/pedagogicalCouncil/inquiries/confirmSendTeachersQucWarningEmail";
    }

    @RequestMapping(method = RequestMethod.POST, path = "/sendTeachersMail")
    public String sendTeachersMail(Model model, @RequestParam TeacherInquiryTemplate teacherInquiry,
            @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date replyEndDate) {
        final Sender sender = getCPSender();
        final DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.forLanguageTag("pt"));
        getTeachers(teacherInquiry).forEach((teacher, executionCourses) -> {
            if (teacher.getDefaultEmailAddressValue() != null) {
                sendMessage(teacherInquiry, df.format(replyEndDate), sender, teacher, executionCourses);
            }
        });
        model.addAttribute("senderName", sender.getName());
        return "fenixedu-ist-quc/pedagogicalCouncil/inquiries/sentTeachersQucWarningEmails";
    }

    private void sendMessage(TeacherInquiryTemplate teacherInquiry, String replyEndDate, Sender sender, Person teacher, List<ExecutionCourse> executionCourses) {
        Message.from(sender)
                .template("quc.teacher.warning.template")
                    .parameter("teacherName", teacher.getName())
                    .parameter("executionCoursesNames", Lists.transform(executionCourses, ExecutionCourse::getName))
                    .parameter("endDateString", replyEndDate)
                    .parameter("replyToAddress", sender.getReplyTo())
                    .parameter("semesterQualifiedName", teacherInquiry.getExecutionPeriod().getQualifiedName())
                .and()
                .replyToSender()
                .singleBcc(teacher.getDefaultEmailAddressValue())
                .wrapped()
                .send();
    }

    private Map<Person, List<ExecutionCourse>> getTeachers(TeacherInquiryTemplate teacherInquiryTemplate) {
        final Map<Person, List<ExecutionCourse>> teachersMap = new HashMap<>();
        if (teacherInquiryTemplate != null) {
            final ExecutionSemester executionPeriod = teacherInquiryTemplate.getExecutionPeriod();
            for (Professorship professorship : Bennu.getInstance().getProfessorshipsSet()) {
                if (professorship.getExecutionCourse().getExecutionPeriod() == executionPeriod) {
                    boolean isToAnswer =
                            TeacherInquiryTemplate.hasToAnswerTeacherInquiry(professorship.getPerson(), professorship);
                    if (isToAnswer
                            && ((professorship.getInquiryTeacherAnswer() == null || professorship.getInquiryTeacherAnswer()
                            .hasRequiredQuestionsToAnswer(teacherInquiryTemplate)) || InquiryResultComment
                            .hasMandatoryCommentsToMake(professorship))) {
                        teachersMap.computeIfAbsent(professorship.getPerson(), k -> new ArrayList<>());
                        teachersMap.get(professorship.getPerson()).add(professorship.getExecutionCourse());
                    }
                }
            }
        }
        return teachersMap;
    }

    private Sender getCPSender() throws NoSuchElementException {
        return Sender.all().stream()
                .filter(sender -> sender.getName().equalsIgnoreCase("Técnico Lisboa (Conselho Pedagógico)"))
                .findFirst().orElseThrow(() -> new NoSuchElementException("No sender available for given name"));
    }

    private Spreadsheet createReport(List<TeacherBean> teachersList) {
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
            Spreadsheet.Row row = spreadsheet.addRow();
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
