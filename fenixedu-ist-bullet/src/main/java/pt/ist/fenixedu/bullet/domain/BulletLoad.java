package pt.ist.fenixedu.bullet.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CourseLoad;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.academic.domain.ShiftType;

public class BulletLoad extends BulletObject {
    private ExecutionCourse course;
    private CourseLoad load;

    private BulletLoad(ExecutionCourse course, CourseLoad load) {
        this.course = course;
        this.load = load;
    }

    public static Stream<BulletLoad> all(final DumpContext context) {
        return context.executions.stream()
                .flatMap(ec -> ec.getShiftTypes().stream()
                        .map(ec::getCourseLoadByShiftType).filter(l -> l.getTotalQuantity().compareTo(BigDecimal.ZERO) != 0)
                        .map(l -> new BulletLoad(ec, l)));
    }

    @Override
    public void populateSlots(final DumpContext context, final LinkedHashMap<String, String> slots) {
        ShiftType type = load.getType();
        BigDecimal totalQuantity = load.getTotalQuantity(), unitQuantity = load.getUnitQuantity();
        if (unitQuantity == null || unitQuantity.compareTo(BigDecimal.ZERO) == 0 || unitQuantity.compareTo(totalQuantity) > 0) { // Unit quantity not defined or erroneous, look into base semester's lessons
            Set<BigDecimal> quantities = load.getShiftsSet().stream()
                    .flatMap(s -> s.getAssociatedLessonsSet().stream())
                    .map(Lesson::getUnitHours).collect(Collectors.toSet());
            if (quantities.size() == 1) { // All lessons agree in duration, use that value
                unitQuantity = quantities.iterator().next();
            } else { // Varying lesson hours, estimate a uniform duration and frequency that fills weekly hours estimate with minimal overhead [if total quantity is erroneous will at least account for minimal lesson time]
                BigDecimal minClassSlots = quantities.stream().map(q -> q.multiply(context.slotsPerHour).setScale(0, RoundingMode.CEILING)).min(Comparator.naturalOrder()).orElse(context.minClassSlots);
                BigDecimal maxClassSlots = quantities.stream().map(q -> q.multiply(context.slotsPerHour).setScale(0, RoundingMode.CEILING)).max(Comparator.naturalOrder()).orElse(context.maxClassSlots);

                BigDecimal slotsPerWeek = minClassSlots.max(load.getWeeklyHours().multiply(context.slotsPerHour));

                Set<BigDecimal> repeatCandidates = Stream.iterate(minClassSlots, s -> s.add(BigDecimal.ONE))
                        .limit(maxClassSlots.subtract(minClassSlots).intValue() + 1)
                        .map(n -> slotsPerWeek.divide(n, RoundingMode.UP))
                        .collect(Collectors.toSet());
                Optional<BigDecimal> bestFitRepeat = repeatCandidates.stream()
                        .filter(n -> n.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0)
                        .min(Comparator.naturalOrder());
                if (!bestFitRepeat.isPresent()) {
                    bestFitRepeat = repeatCandidates.stream()
                            .min(Comparator.comparing((BigDecimal n) -> n.remainder(BigDecimal.ONE)).reversed().thenComparing(Comparator.naturalOrder()));
                }
                unitQuantity = load.getWeeklyHours().divide(bestFitRepeat.get().setScale(0, RoundingMode.UP), RoundingMode.UP);
            }
        }
        String weeks = "";
        /* XXX currently since we attempt to find a uniform solution for all weekly loads so weeks can be left empty
         * the following snippet finds the weeks where there were classes for this load in the base semester:
                load.getShiftsSet().stream().flatMap(s->s.getAssociatedLessonsSet().stream())
                .flatMap(l->l.getAllLessonInstanceDates().stream())
                .map(BulletLoad::week).distinct().sorted()
                .map(String::valueOf).collect(Collectors.joining(","));
         * this is not a good estimate though, considering lessons may have different durations and some dates may
         * have been on holidays, it might not match with unit/total quantities.
         * To be thorough weeks should either be planned beforehand with different (slots,repeats) pairs or
         * load should be copied exactly on a lesson by lesson basis and week based groupings calculated from those.
         */

        int slotNumber, repeats, shifts;
        slotNumber = unitQuantity.multiply(context.slotsPerHour).setScale(0, RoundingMode.CEILING).intValue();
        repeats = load.getWeeklyHours().divide(unitQuantity, RoundingMode.UP).setScale(0, RoundingMode.CEILING).intValue();
        //XXX At least one shift when no shifts are defined, it's probably more correct to calculate this
        // from an estimate of the number of students enrolled. Bullet Class has similar issues, as it's based on shifts as well.
        shifts = Math.max(1, course.getNumberOfShifts(type));

        slots.put(BulletObjectTag.COURSE_CODE.unit(), course.getSigla());
        /* XXX For testing and checking different solutions
        slots.put("TOTAL", load.getTotalQuantity().toString());
        slots.put("WEEKLY", load.getWeeklyHours().toString());
        slots.put("UNIT", originalUnitQuantity != null ? originalUnitQuantity.toString() : "N/A");
        slots.put("UNIT EMPIRICAL", empiricalUnitQuantity != null ? empiricalUnitQuantity.toString() : "N/A");
        slots.put("UNIT ESTIMATE", estimatedUnitQuantity != null ? estimatedUnitQuantity.toString() : "N/A");
         */
        slots.put(BulletObjectTag.LOAD_NAME.unit(), type.getFullNameTipoAula());
        slots.put(BulletObjectTag.TYPE_NAME.unit(), type.getSiglaTipoAula());
        slots.put(BulletObjectTag.SLOTS.unit(), String.valueOf(slotNumber));
        slots.put(BulletObjectTag.REPEATS.unit(), String.valueOf(repeats));
        slots.put(BulletObjectTag.SHIFTS.unit(), String.valueOf(shifts));
        slots.put(BulletObjectTag.WEEKS.unit(), weeks);
    }
}
