package pt.ist.fenixedu.integration.task.updateData.student;

import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.ExtraCurricularActivity;
import org.fenixedu.academic.domain.student.curriculum.ExtraCurricularActivityType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Partial;
import org.joda.time.PeriodType;

import pt.ist.fenixedu.delegates.domain.student.DegreeDelegate;
import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.domain.student.YearDelegate;

public class SynchronizeDelegateActivities extends CronTask {

    private static final int MINIMUM_FUNCTION_DAYS = 40;
    private static ExtraCurricularActivityType YEAR_DELEGATE_ACTIVITY = null;
    private static ExtraCurricularActivityType DEGREE_DELEGATE_ACTIVITY = null;

    @Override
    public void runTask() throws Exception {
        YEAR_DELEGATE_ACTIVITY = getDelegateExtraActivityType("Delegado de ano");
        DEGREE_DELEGATE_ACTIVITY = getDelegateExtraActivityType("Delegado de curso e delegado-adjunto de curso");
        StringBuilder report = new StringBuilder();
        int currentYear = new LocalDate().getYear();

        ExecutionYear executionYear = ExecutionYear.readByDateTime(new LocalDate(2015, 10, 22));
        Interval yearInterval =
                new Interval(executionYear.getBeginLocalDate().toDateTimeAtStartOfDay(), executionYear.getEndLocalDate()
                        .toDateTimeAtStartOfDay());
        for (Delegate delegate : Bennu.getInstance().getDelegatesSet()) {
            if (hasMinimumFunctionTime(delegate)
                    && (delegate.getStart().isAfter(executionYear.getBeginLocalDate().toDateTimeAtStartOfDay()) || delegate
                            .getInterval().overlaps(yearInterval))) {
                final ExtraCurricularActivityType activityType = getActivityType(delegate);
                Interval interval = getIntervalForActivity(delegate.getInterval());

                Student student = delegate.getUser().getPerson().getStudent();
                if (student == null) {
                    continue;
                }
                if (!student
                        .getExtraCurricularActivitySet()
                        .stream()
                        .anyMatch(
                                eca -> intervalsIntersect(eca.getActivityInterval(), interval)
                                        && eca.getType().equals(activityType))) {
                    new ExtraCurricularActivity(student, activityType, interval);
                    taskLog("Created a new activity for student \t%s\t with the Activity %s with the dates %s - %s\n", student
                            .getPerson().getUsername(), activityType.getName().getContent(),
                            interval.getStart().toString("dd/MM/yyyy"), interval.getEnd().toString("dd/MM/yyyy"));
                } else {
                    updateActivity(report, currentYear, delegate, activityType, interval, student);
                }
            }
        }

        output("anomalies.txt", report.toString().getBytes());
    }

    private void updateActivity(StringBuilder report, int currentYear, Delegate delegate,
            final ExtraCurricularActivityType activityType, Interval interval, Student student) {
        student.getExtraCurricularActivitySet()
                .stream()
                .filter(eca -> intervalsIntersect(eca.getActivityInterval(), interval) && eca.getType().equals(activityType))
                .forEach(
                        eca -> {
                            int activityYears =
                                    eca.getActivityInterval().getEnd().getYear() - eca.getActivityInterval().getStart().getYear();
                            int delegateYears = delegate.getEnd().getYear() - delegate.getStart().getYear();
                            if (activityYears == delegateYears) {
                                //nothing to do here
                            } else if (activityYears > delegateYears) {
                                //the activity may contain more than one delegate function
                                Set<Delegate> delegatesSet = student.getPerson().getUser().getDelegatesSet();
                                int summedDelegateYears =
                                        (int) delegatesSet
                                                .stream()
                                                .filter(delegateCheck -> eca.getType().equals(getActivityType(delegateCheck))
                                                        && intervalsIntersect(eca.getActivityInterval(),
                                                                getIntervalForActivity(delegateCheck.getInterval())))
                                                .mapToInt(
                                                        delegateCheck -> delegateCheck.getEnd().getYear()
                                                                - delegateCheck.getStart().getYear()).sum();

                                if (summedDelegateYears == activityYears) {
                                    //nothing to worry, there are other delegate functions and all summed up matches the activity
                                } else {
                                    //in the system this user was not delegate for that many years
                                    Stream<Delegate> delegateIntersections =
                                            delegatesSet.stream().filter(
                                                    delegateCheck -> eca.getType().equals(getActivityType(delegateCheck))
                                                            && intervalsIntersect(eca.getActivityInterval(),
                                                                    getIntervalForActivity(delegateCheck.getInterval())));

                                    reportCase(report, delegateIntersections, delegate, student, activityType, eca);
                                    DateTime endDate =
                                            delegatesSet
                                                    .stream()
                                                    .filter(delegateCheck -> eca.getType().equals(getActivityType(delegateCheck))
                                                            && intervalsIntersect(eca.getActivityInterval(),
                                                                    getIntervalForActivity(delegateCheck.getInterval())))
                                                    .map(delegateCheck -> delegateCheck.getEnd()).max(DateTime::compareTo).get();

                                    if (endDate.getYear() < eca.getActivityInterval().getEnd().getYear()) {
                                        //this mean the job was set for x years but in the meantime the person was replaced before that end date
                                        //and another one took his position, so the activity has to be updated
                                        taskLog("Updated activity end date - %s %s %s - %s ; %s %s - %s\n", student.getPerson()
                                                .getUsername(), delegate.getTitle(), delegate.getStart().toString("dd/MM/yyyy"),
                                                delegate.getEnd().toString("dd/MM/yyyy"), eca.getType().getName().getContent(),
                                                eca.getStart(), eca.getEnd());
                                        DateTimeFieldType[] fieldTypes =
                                                new DateTimeFieldType[] { DateTimeFieldType.year(),
                                                        DateTimeFieldType.monthOfYear() };
                                        int values[] = new int[] { endDate.getYear(), endDate.getMonthOfYear() };
                                        eca.setEnd(new Partial(fieldTypes, values));
                                    } else {
                                        //if the year of the function is not smaller it means that the beginning of the activity does not math
                                        //the data in the delegates but in that case this script will not try to make any automatic changes
                                    }
                                }
                            } else { //activityYears < delegateYears
                                //check for other activities inside the same time period that do not end after the delegateYears
                                int ecaInternSum =
                                        student.getExtraCurricularActivitySet()
                                                .stream()
                                                .filter(ecaIntern -> intervalsIntersect(ecaIntern.getActivityInterval(), interval)
                                                        && ecaIntern.getType().equals(activityType) && !ecaIntern.equals(eca))
                                                .mapToInt(
                                                        ecaIntern -> ecaIntern.getActivityInterval().getEnd().getYear()
                                                                - ecaIntern.getActivityInterval().getStart().getYear()).sum();
                                if (ecaInternSum + activityYears != delegateYears) {
                                    if (delegate.getEnd().getYear() <= currentYear) {
                                        taskLog("Updated activity end date - %s %s %s - %s ; %s %s - %s\n", student.getPerson()
                                                .getUsername(), delegate.getTitle(), delegate.getStart().toString("dd/MM/yyyy"),
                                                delegate.getEnd().toString("dd/MM/yyyy"), eca.getType().getName().getContent(),
                                                eca.getStart(), eca.getEnd());

                                        DateTimeFieldType[] fieldTypes =
                                                new DateTimeFieldType[] { DateTimeFieldType.year(),
                                                        DateTimeFieldType.monthOfYear() };
                                        int values[] =
                                                new int[] { delegate.getEnd().getYear(), delegate.getEnd().getMonthOfYear() };
                                        eca.setEnd(new Partial(fieldTypes, values));
                                    } else {
                                        //the delegate function ends in the future and so the activity only reflects the current time
                                    }
                                } else {
                                    //there are other activities that together sum up the delegate function years, nothing to do
                                }
                            }
                        });
    }

    private void reportCase(StringBuilder report, Stream<Delegate> delegateIntersections, Delegate d, Student s,
            ExtraCurricularActivityType activityType, ExtraCurricularActivity eca) {
        report.append("######\n");
        report.append(s.getPerson().getUsername()).append("\t").append(activityType.getName().getContent()).append(" ")
                .append(eca.getStart().toString("MM/yyyy")).append(" ").append(eca.getEnd().toString("MM/yyyy")).append("\n");
        delegateIntersections.forEach(de -> {
            report.append(s.getPerson().getUsername()).append("\t").append(de.getTitle()).append(" ")
                    .append(de.getStart().toString("dd/MM/yyyy")).append(" ").append(de.getEnd().toString("dd/MM/yyyy"))
                    .append("\n");
        });
    }

    private boolean intervalsIntersect(Interval activityInterval, Interval delegateInterval) {
        Interval overlap = activityInterval.overlap(delegateInterval);
        return overlap != null;
    }

    private Interval getIntervalForActivity(Interval interval) {
        return new Interval(new DateTime(interval.getStart().getYear(), interval.getStart().getMonthOfYear(), 01, 01, 00),
                new DateTime(interval.getEnd().getYear(), interval.getEnd().getMonthOfYear(), 01, 01, 00));
    }

    private ExtraCurricularActivityType getActivityType(Delegate d) {
        ExtraCurricularActivityType activityType = null;
        if (d instanceof YearDelegate) {
            activityType = YEAR_DELEGATE_ACTIVITY;
        } else if (d instanceof DegreeDelegate) {
            activityType = DEGREE_DELEGATE_ACTIVITY;
        } else { //CycleDelegate
            activityType = DEGREE_DELEGATE_ACTIVITY;
        }
        return activityType;
    }

    private ExtraCurricularActivityType getDelegateExtraActivityType(String activityName) {
        return Bennu.getInstance().getExtraCurricularActivityTypeSet().stream()
                .filter(eca -> eca.getNamePt().equalsIgnoreCase(activityName)).findAny().orElse(null);
    }

    private boolean hasMinimumFunctionTime(Delegate d) {
        return d.getInterval().toPeriod(PeriodType.days()).getDays() >= MINIMUM_FUNCTION_DAYS;
    }
}
