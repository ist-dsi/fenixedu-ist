package pt.ist.fenixedu.tutorship.dto.teacher.tutor;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.beanutils.BeanComparator;
import org.fenixedu.academic.domain.student.PersonalIngressionData;
import org.fenixedu.academic.domain.student.Registration;

import pt.ist.fenixedu.tutorship.domain.Tutorship;

public class TutorshipBean implements Serializable {

    public static final Comparator<TutorshipBean> TUTORSHIP_COMPARATOR_BY_STUDENT_NUMBER = new BeanComparator(
            "tutorship.studentCurricularPlan.registration.number");

    private Tutorship tutorship;

    public TutorshipBean(Tutorship tutorship) {
        setTutorship(tutorship);
    }

    public Tutorship getTutorship() {
        return tutorship;
    }

    public void setTutorship(Tutorship tutorship) {
        this.tutorship = tutorship;
    }

    public Boolean getIsDislocatedFromPermanentResidence() {
        Registration registration = getTutorship().getStudentCurricularPlan().getRegistration();
        PersonalIngressionData personalIngressionData =
                registration.getStudent().getPersonalIngressionDataByExecutionYear(
                        registration.getStudentCandidacy().getExecutionYear());
        return personalIngressionData != null ? personalIngressionData.getDislocatedFromPermanentResidence() : null;
    }
}
