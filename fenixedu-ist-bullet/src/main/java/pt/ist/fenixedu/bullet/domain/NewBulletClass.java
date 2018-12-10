package pt.ist.fenixedu.bullet.domain;

import java.util.LinkedHashMap;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.academic.domain.SchoolClass;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTimeConstants;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class NewBulletClass extends BulletObject {
    private SchoolClass schoolClass;

    public NewBulletClass(SchoolClass schoolClass) {
        this.schoolClass = schoolClass;
    }

    @Override
    public void populateSlots(final DumpContext context, LinkedHashMap<String, String> slots) {
        //
    }

    public static Stream<NewBulletClass> all(final DumpContext context) {
        return Bennu.getInstance().getSchoolClasssSet().stream().map(NewBulletClass::new);
    }

    @Override
    public JsonObject toJson(final DumpContext context){
        return getSchoolClassJsonObject(context, this.schoolClass);
    }

    private JsonObject getSchoolClassJsonObject(final DumpContext context, final SchoolClass schoolClass) {
        final JsonObject schoolClassObject = new JsonObject();
        schoolClassObject.addProperty("className", schoolClass.getNome());

        final JsonArray shifts = new JsonArray();
        schoolClass.getAssociatedShiftsSet().stream().filter(shift -> shift.getExecutionCourse().getAcademicInterval().equals
                (context.baseSemester.getAcademicInterval())).map(this::getShiftJsonObject).forEach(shifts::add);
        schoolClassObject.add("shifts", shifts);
        return schoolClassObject;
    }

    private JsonObject getShiftJsonObject(Shift shift) {
        final JsonObject shiftsObject = new JsonObject();
        shiftsObject.addProperty("shiftName", shift.getNome());

        final JsonArray lessons = new JsonArray();
        shift.getLessonsOrderedByWeekDayAndStartTime().stream().map(this::getLessonJsonObject).forEach(lessons::add);
        shiftsObject.add("lessons", lessons);
        return shiftsObject;
    }

    private JsonObject getLessonJsonObject(Lesson lesson) {
        final JsonObject lessonObject = new JsonObject();

        final JsonArray semanas = new JsonArray();
        lesson.getAllLessonIntervals().stream()
                .map(interval -> interval.getStart().toLocalDate().withDayOfWeek(DateTimeConstants.MONDAY).toString())
                .sorted()
                .forEachOrdered(semanas::add);
        lessonObject.add("semanas", semanas);

        lessonObject.addProperty("diaSemana", lesson.getDiaSemana().getDiaSemanaString());
        lessonObject.addProperty("inicio", lesson.getBeginHourMinuteSecond().toString("HH:mm"));
        lessonObject.addProperty("fim", lesson.getEndHourMinuteSecond().toString("HH:mm"));
        
        if (lesson.getSala() != null) {
            lessonObject.addProperty("sala", lesson.getSala().getName());
            lessonObject.addProperty("lotacao", lesson.getSala().getAllocatableCapacity().toString());
        }
        return lessonObject;
    }
}
