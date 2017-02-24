package pt.ist.fenixedu.integration.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import org.fenixedu.academic.domain.LessonInstance;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.Summary;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import pt.ist.fenixedu.integration.api.beans.publico.FenixSpace;

import static com.fasterxml.jackson.annotation.JsonInclude.*;

public class FenixLessonSummary {
    private String shift;
    private String shiftName;
    private String shiftType;
    private String lessonDate;
    private FenixSpace room;
    private FenixSummary summary;

    public FenixLessonSummary(Shift shift, DateTime lessonDate, Space space, LessonInstance lessonInstance) {
        this.shift = shift.getExternalId();
        this.shiftName = shift.getPresentationName();
        this.shiftType = !shift.getSortedTypes().isEmpty() ? shift.getSortedTypes().first().getFullNameTipoAula() : null;
        this.lessonDate = lessonDate.toString("yyyy-MM-dd HH:mm:ss");
        this.room = space != null ? new FenixSpace.Room(space) : null;
        if(lessonInstance != null && lessonInstance.getSummary() != null) {
            summary = new FenixSummary(lessonInstance.getSummary());
        }
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public String getLessonDate() {
        return lessonDate;
    }

    public void setLessonDate(String lessonDate) {
        this.lessonDate = lessonDate;
    }

    public FenixSpace getRoom() {
        return room;
    }

    public void setRoom(FenixSpace room) {
        this.room = room;
    }

    @JsonInclude(Include.NON_NULL)
    public FenixSummary getSummary() {
        return summary;
    }

    public void setSummary(FenixSummary summary) {
        this.summary = summary;
    }

    public static class FenixSummary {
        private String teacher;
        private String title;
        private String content;
        private int attendanceCount;
        private Boolean taught;
        private String reason;

        public FenixSummary(Summary summary) {
            this.teacher = !Strings.isNullOrEmpty(summary.getTeacherName()) ?
                    summary.getTeacherName() :
                    summary.getProfessorship().getPerson().getName();
            this.title = summary.getTitle().exportAsString();
            this.attendanceCount = summary.getStudentsNumber() != null ? summary.getStudentsNumber() : 0;
            this.taught = summary.getTaught();
            if(summary.getTaught()) {
                this.content = summary.getSummaryText().exportAsString();
            } else {
                this.reason = summary.getSummaryText().exportAsString();
            }
        }

        public String getTeacher() {
            return teacher;
        }

        public void setTeacher(String teacher) {
            this.teacher = teacher;
        }

        @JsonRawValue
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @JsonRawValue
        @JsonInclude(Include.NON_NULL)
        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public int getAttendanceCount() {
            return attendanceCount;
        }

        public void setAttendanceCount(int attendanceCount) {
            this.attendanceCount = attendanceCount;
        }

        public Boolean getTaught() {
            return taught;
        }

        public void setTaught(Boolean taught) {
            this.taught = taught;
        }

        @JsonRawValue
        @JsonInclude(Include.NON_NULL)
        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
