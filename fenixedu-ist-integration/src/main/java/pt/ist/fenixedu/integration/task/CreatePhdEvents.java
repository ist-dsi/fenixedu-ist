package pt.ist.fenixedu.integration.task;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.DueDateAmountMap;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Event_Base;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeEvent;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcessState;
import org.fenixedu.academic.domain.phd.PhdProgramProcessState;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@Task(englishTitle = "CreatePhdEvents", readOnly = true)
public class CreatePhdEvents extends CronTask {

    private static final Comparator<? super PhdProgramProcessState> STATE_COMPARATOR = (s1, s2) -> {
        final int c = s1.getWhenCreated().compareTo(s2.getWhenCreated());
        return c == 0 ? s1.getExternalId().compareTo(s2.getExternalId()) : c;
    };

    @Override
    public void runTask() throws Exception {
        final LocalDate today = new LocalDate();
        for (int i = 0; i < 8; i++) {
            final LocalDate day = today.minusDays(i);
            Bennu.getInstance().getPartysSet().stream()
                .parallel()
                .filter(p -> p instanceof Person)
                .map(p -> (Person) p)
                .forEach(p -> process(p, day));
        }
    }

    private void process(final Person person, final LocalDate today) {
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                person.getPhdIndividualProgramProcessesSet().forEach(p -> process(p, today));
                return null;
            }, new AtomicInstance(TxMode.WRITE, false));
        } catch (final Exception e) {
            throw new Error(e);
        }
    }

    private void process(final PhdIndividualProgramProcess phdIndividualProgramProcess, final LocalDate today) {
        final LocalDate whenStartedStudies = phdIndividualProgramProcess.getWhenStartedStudies();
        if (whenStartedStudies != null) {
            final DateTime dt = today.toDateTimeAtStartOfDay().plusDays(1);
            final LocalDate initialDateForEventCreation = initialDateForEventCreation(phdIndividualProgramProcess, dt);
            if (initialDateForEventCreation != null && initialDateForEventCreation.getDayOfMonth() == today.getDayOfMonth() && initialDateForEventCreation.getMonthOfYear() == today.getMonthOfYear()) {
                PhdProgramProcessState stateForToday = findStateForDate(phdIndividualProgramProcess, dt);
                final PhdIndividualProgramProcessState type = stateForToday.getType();
                if (type == PhdIndividualProgramProcessState.WORK_DEVELOPMENT || type == PhdIndividualProgramProcessState.SUSPENDED) {
                    final Person person = phdIndividualProgramProcess.getPerson();
                    final PhdGratuityEvent existingPhd = findPhdEvent(person, today);
                    final InsuranceEvent existingInsurance = findInsurance(person, today);
                    if (existingPhd == null) {
                        fixDueDate(new PhdGratuityEvent(phdIndividualProgramProcess, today.getYear(), dt));
                    }
                    if (existingInsurance == null) {
                        fixDueDate(new InsuranceEvent(person, ExecutionYear.readCurrentExecutionYear()));
                        fixDueDate(new AdministrativeOfficeFeeEvent(phdIndividualProgramProcess.getAdministrativeOffice(), person, ExecutionYear.readCurrentExecutionYear()));
                    }
                }
            }
        }
    }

    private InsuranceEvent findInsurance(Person person, LocalDate today) {
        return person.getEventsSet().stream()
                .filter(e -> !e.isCancelled() && e instanceof InsuranceEvent)
                .map(e -> (InsuranceEvent) e)
                .filter(e -> e.getWhenOccured().getYear() == e.getEventStartDate().getYear() || e.getWhenOccured().getYear() == e.getDueDate().getYear())
                .filter(e -> e.getWhenOccured().getYear() == today.getYear())
                .findAny().orElse(null);
    }

    private PhdGratuityEvent findPhdEvent(final Person person, final LocalDate today) {
        return person.getEventsSet().stream()
                .filter(e -> !e.isCancelled() && e instanceof PhdGratuityEvent)
                .map(e -> (PhdGratuityEvent) e)
                .filter(e -> e.getYear().intValue() == today.getYear())
                .findAny().orElse(null);
    }

    private PhdProgramProcessState findStateForDate(final PhdIndividualProgramProcess phdIndividualProgramProcess, final DateTime tomorrow) {
        PhdProgramProcessState result = null;
        for (final PhdProgramProcessState state : phdIndividualProgramProcess.getStatesSet().stream().sorted(STATE_COMPARATOR).collect(Collectors.toList())) {
            if (!state.getStateDate().isAfter(tomorrow)) {
                result = state;
            }
        }
        return result;
    }

    private LocalDate initialDateForEventCreation(PhdIndividualProgramProcess phdIndividualProgramProcess, final DateTime today) {
        return phdIndividualProgramProcess.getStatesSet().stream().sorted(STATE_COMPARATOR)
                .filter(s -> s.getType() == PhdIndividualProgramProcessState.WORK_DEVELOPMENT || s.getType() == PhdIndividualProgramProcessState.SUSPENDED)
                .filter(s -> !s.getStateDate().isAfter(today))
                .map(s -> s.getStateDate().toLocalDate())
                .max((d1, d2) -> d1.compareTo(d2)).orElse(null);
    }

    private void fixDueDate(final Event event) {
        final LocalDate dueDate;
        if (event instanceof InsuranceEvent) {
            dueDate = new LocalDate().plusMonths(1);
            ((InsuranceEvent) event).setDueDate(dueDate.toDateTimeAtStartOfDay());
        } else if (event instanceof AdministrativeOfficeFeeEvent) {
            dueDate = new LocalDate().plusMonths(1);
            ((AdministrativeOfficeFeeEvent) event).setPaymentEndDate(toYmd(dueDate));
        } else if (event instanceof PhdGratuityEvent) {
            dueDate = event.getWhenOccured().plusYears(1).minusDays(1).toLocalDate();
        } else {
            throw new Error();
        }

        final Map<LocalDate,Money> map = new HashMap<>();
        final Entry<LocalDate, Money> e = event.getDueDateAmountMap().entrySet().iterator().next();
        map.put(dueDate, e.getValue());
        try {
            Method setDueDateAmountMapMethod = Event_Base.class.getDeclaredMethod("setDueDateAmountMap", DueDateAmountMap.class);
            setDueDateAmountMapMethod.setAccessible(true);
            setDueDateAmountMapMethod.invoke(event, new DueDateAmountMap(map));
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
            throw new Error(e1);
        }
    }

    private YearMonthDay toYmd(LocalDate d) {
        return new YearMonthDay(d.getYear(), d.getMonthOfYear(), d.getDayOfMonth());
    }

}
