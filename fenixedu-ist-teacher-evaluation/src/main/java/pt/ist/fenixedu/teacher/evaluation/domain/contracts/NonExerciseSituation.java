package pt.ist.fenixedu.teacher.evaluation.domain.contracts;

import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.Interval;

public class NonExerciseSituation extends NonExerciseSituation_Base {

    public NonExerciseSituation() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public void delete() {
        setPerson(null);
        setRootDomainObject(null);
        deleteDomainObject();
    }

    public static Set<NonExerciseSituation> getNonExerciseSituationSet(Teacher teacher, ExecutionSemester executionSemester) {
        Set<NonExerciseSituation> nonExerciseSituationSet = new HashSet<NonExerciseSituation>();
        teacher.getPerson().getNonExerciseSituationSet().forEach(nes -> {
            if (nes.isActive(executionSemester)) {
                nonExerciseSituationSet.add(nes);
            }
        });
        return nonExerciseSituationSet;
    }

    private boolean isActive(ExecutionSemester executionSemester) {
        if (getEndDate() == null) {
            return !getBeginDate().isAfter(executionSemester.getEndDateYearMonthDay().toLocalDate());
        }
        Interval semesterInterval =
                new Interval(executionSemester.getBeginDateYearMonthDay().toLocalDate().toDateTimeAtStartOfDay(), executionSemester
                        .getEndDateYearMonthDay().toLocalDate().toDateTimeAtStartOfDay());
        return new Interval(getBeginDate().toDateTimeAtStartOfDay(), getEndDate().plusDays(1).toDateTimeAtStartOfDay()).overlaps(semesterInterval);
    }
}
