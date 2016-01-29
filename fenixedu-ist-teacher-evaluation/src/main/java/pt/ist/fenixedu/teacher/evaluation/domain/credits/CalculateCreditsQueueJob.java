package pt.ist.fenixedu.teacher.evaluation.domain.credits;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.QueueJobResult;
import org.fenixedu.academic.domain.Teacher;

public class CalculateCreditsQueueJob extends CalculateCreditsQueueJob_Base {

    public CalculateCreditsQueueJob(ExecutionYear executionYear) {
        super();
        setExecutionYear(executionYear);
    }

    @Override
    public QueueJobResult execute() throws Exception {
        AnnualCreditsState annualCreditsState = getExecutionYear().getAnnualCreditsState();

        if (annualCreditsState != null && !annualCreditsState.getIsCreditsClosed()) {
            Set<Teacher> teachers = getThisYearTeachers(getExecutionYear());
            calculateFinalCredits(annualCreditsState, teachers);
        }
        return null;
    }

    private void calculateFinalCredits(AnnualCreditsState annualCreditsState, Set<Teacher> teachers) {
        for (Teacher teacher : teachers) {
            AnnualTeachingCredits annualTeachingCredits =
                    AnnualTeachingCredits.readByYearAndTeacher(annualCreditsState.getExecutionYear(), teacher);
            if (annualTeachingCredits == null) {
                annualTeachingCredits = new AnnualTeachingCredits(teacher, annualCreditsState);
            }

            annualTeachingCredits.calculateCredits();
        }
        annualCreditsState.setIsFinalCreditsCalculated(true);
    }

    private Set<Teacher> getThisYearTeachers(ExecutionYear executionYear) {
        Set<Teacher> teachers = new HashSet<Teacher>();
        for (ExecutionSemester executionSemester : getExecutionYear().getExecutionPeriodsSet()) {
            teachers.addAll(executionSemester.getTeacherAuthorizationStream().map(a -> a.getTeacher())
                    .collect(Collectors.toSet()));
        }
        return teachers;
    }
}
