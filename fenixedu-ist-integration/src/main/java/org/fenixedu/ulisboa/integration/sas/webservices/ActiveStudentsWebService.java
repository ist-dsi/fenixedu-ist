package org.fenixedu.ulisboa.integration.sas.webservices;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.ulisboa.integration.sas.dto.ActiveStudentBean;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qubit.solution.fenixedu.bennu.webservices.services.server.BennuWebService;
import com.qubit.solution.fenixedu.integration.cgd.domain.idcards.CgdCard;

@WebService
public class ActiveStudentsWebService extends BennuWebService {

    private static Logger logger = LoggerFactory.getLogger(ActiveStudentsWebService.class);

    @WebMethod
    public Collection<ActiveStudentBean> getActiveStudents() {
        return toActiveStudentBeans(Bennu.getInstance().getStudentsSet().stream());
    }

    private boolean isActive(final Registration registration) {
        return registration.isActive() && !registration.getDegreeType().isEmpty();
    }

    @WebMethod
    public Collection<ActiveStudentBean> getDailyRegistration() {
        final LocalDate today = new LocalDate();
        final ExecutionSemester semester = ExecutionSemester.readActualExecutionSemester();
        return toActiveStudentBeans(semester.getEnrolmentsSet().stream()
            .filter(e -> match(today, e.getCreationDateDateTime()))
            .map(e -> e.getStudent())
            .distinct());
    }

    private boolean match(LocalDate ld, DateTime dt) {
        return ld.getYear() == dt.getYear() && ld.getMonthOfYear() == dt.getMonthOfYear() && ld.getDayOfMonth() == dt.getDayOfMonth();
    }

    @WebMethod
    public Collection<ActiveStudentBean> getCurrentDayIssuedCards() {
        return toActiveStudentBeans(getStudentsWithCardsIssuedToday());
    }

    public Collection<ActiveStudentBean> toActiveStudentBeans(final Stream<Student> students) {
        final Stream<Registration> stream = students
                .flatMap(s -> s.getRegistrationsSet().stream())
                .filter(r -> isActive(r));
        return stream.map(s -> populateActiveStudent(s))
            .collect(Collectors.toList());
    }

    private static ActiveStudentBean populateActiveStudent(final Registration registration) {
        try {
            final Student student = registration.getStudent();

            final ActiveStudentBean activeStudentBean = new ActiveStudentBean();
            activeStudentBean.setName(student.getName());
            activeStudentBean.setGender(student.getPerson().getGender().toLocalizedString(Locale.getDefault()));
            //information still not available

            final Optional<CgdCard> card = student.getPerson().getCgdCardsSet().stream().filter(CgdCard::isValid).findAny();
            if (card.isPresent()) {
                String mifareCode = card.get().getMifareCode();
                activeStudentBean.setMifare(mifareCode);
                activeStudentBean.setIsTemporaryCard(Boolean.toString(card.get().getTemporary()));
                activeStudentBean.setCardIssueDate(card.get().getLastMifareModication().toString());
                activeStudentBean.setCardNumber(card.get().getCardNumber());
            }

            activeStudentBean.setIdentificationNumber(student.getPerson().getDocumentIdNumber());
            activeStudentBean.setFiscalCountryCode(student.getPerson().getCountry() != null ? student.getPerson().getCountry().getCode() : "");
            activeStudentBean.setFiscalIdentificationNumber(student.getPerson().getSocialSecurityNumber());
            final YearMonthDay dateOfBirthYearMonthDay = student.getPerson().getDateOfBirthYearMonthDay();
            activeStudentBean.setDateOfBirth(dateOfBirthYearMonthDay != null ? dateOfBirthYearMonthDay.toString() : "");

            final Country country = student.getPerson().getCountry();
            activeStudentBean.setOriginCountry(country != null ? country.getLocalizedName().getContent(Locale.getDefault()) : "");
            activeStudentBean.setOriginCountryCode(country != null ? country.getCode() : "");
            
            activeStudentBean.setStudentCode(Integer.toString(registration.getNumber()));
            if (registration.getDegreeType().isEmpty()) {
                //Consider all courses without school level type mapping as the free course 
                activeStudentBean.setDegreeCode(ActiveDegreesWebService.FREE_COURSES_CODE);
            } else {
                activeStudentBean.setDegreeCode(registration.getDegree().getCode());
                activeStudentBean.setOficialDegreeCode(registration.getDegree().getMinistryCode());
            }
            List<ExecutionYear> sortedExecutionYears = getSortedExecutionYears(registration);
            if (sortedExecutionYears.size() > 0) {
                final ExecutionYear currentExecutionYear = sortedExecutionYears.get(sortedExecutionYears.size() - 1);
                activeStudentBean.setCurrentExecutionYear(currentExecutionYear.getName());
                activeStudentBean.setEnroledECTTotal(Double.toString(ects(registration, currentExecutionYear)));
                LocalDate enrolmentDate = getEnrolmentDate(registration, currentExecutionYear);
                activeStudentBean.setDateOfRegistration(enrolmentDate != null ? enrolmentDate.toString() : "");
                activeStudentBean.setRegime(registration.getRegimeType(currentExecutionYear).toString());
                boolean toPayTuition = registration.hasToPayGratuityOrInsurance();
                activeStudentBean.setIsPayingSchool(toPayTuition);
            }

            if (sortedExecutionYears.size() > 1) {
                final ExecutionYear previousExecutionYear = sortedExecutionYears.get(sortedExecutionYears.size() - 2);
                activeStudentBean.setPreviousExecutionYear(previousExecutionYear.getName());
                activeStudentBean.setEnroledECTTotalInPreviousYear(
                        Double.toString(ects(registration, previousExecutionYear)));
                activeStudentBean
                            .setApprovedECTTotalInPreviousYear(Double.toString(ectsApproved(registration, previousExecutionYear)));
            }

                activeStudentBean.setCurricularYear(Integer.toString(registration.getCurricularYear()));

            return activeStudentBean;
        } catch (Throwable t) {
            logger.error("Problems calculating active students cache for student: " + registration.getNumber());
            return null;
        }
    }

    private static double ects(final Registration registration, final ExecutionYear executionYear) {
        return enrolments(registration, executionYear)
                .filter(e -> e.isApproved())
                .mapToDouble(e -> e.getEctsCredits())
                .sum();
    }

    private static double ectsApproved(final Registration registration, final ExecutionYear executionYear) {
        return enrolments(registration, executionYear)
                .mapToDouble(e -> e.getEctsCredits())
                .sum();
    }

    private static Stream<Enrolment> enrolments(final Registration registration, final ExecutionYear executionYear) {
        return registration.getStudentCurricularPlansSet().stream()
                .map(scp -> scp.getRoot())
                .flatMap(cg -> cg.getCurriculumLineStream())
                .filter(cl -> cl.isEnrolment())
                .map(cl -> (Enrolment) cl)
                .filter(e -> e.isValid(executionYear));
    }

    private static LocalDate getEnrolmentDate(Registration firstRegistration, ExecutionYear currentExecutionYear) {
        RegistrationDataByExecutionYear registrationDataByExecutionYear =
                firstRegistration.getRegistrationDataByExecutionYearSet().stream()
                        .filter(rdby -> rdby.getExecutionYear().equals(currentExecutionYear)).findFirst().orElse(null);
        return registrationDataByExecutionYear != null ? registrationDataByExecutionYear.getEnrolmentDate() : null;
    }

    private static List<ExecutionYear> getSortedExecutionYears(final Registration registration) {
        return registration.getStudentCurricularPlansSet().stream()
            .map(scp -> scp.getRoot())
            .flatMap(cg -> cg.getCurriculumLineStream())
            .map(cl -> cl.getExecutionYear())
            .distinct()
            .sorted()
            .collect(Collectors.toList())
            ;
    }

    private Stream<Student> getStudentsWithCardsIssuedToday() {
        // We are comparing the card modification date instead of the card issued date
        // This is required since the card issued date may be some day before the card insertion in the system
        return Bennu.getInstance().getCgdCardsSet().stream()
                .filter(card -> isToday(card.getLastMifareModication()) && card.getPerson().getStudent() != null)
                .map(card -> card.getPerson().getStudent());
    }

    private boolean isToday(LocalDate b) {
        LocalDate now = LocalDate.now();
        return now.year().get() == b.year().get() && now.monthOfYear().get() == b.monthOfYear().get()
                && now.dayOfMonth().get() == b.dayOfMonth().get();
    }

}