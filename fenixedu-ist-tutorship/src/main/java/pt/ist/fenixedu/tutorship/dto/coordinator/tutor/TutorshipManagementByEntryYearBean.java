/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Tutorship.
 *
 * FenixEdu IST Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.dto.coordinator.tutor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Teacher;

import pt.ist.fenixedu.tutorship.domain.Tutorship;

public class TutorshipManagementByEntryYearBean implements Serializable {
    private List<Tutorship> studentsList;

    private ExecutionYear executionYear;

    private Teacher teacher;

    public TutorshipManagementByEntryYearBean(ExecutionYear executionYear, Teacher teacher) {
        this.studentsList = new ArrayList<Tutorship>();
        this.executionYear = executionYear;
        this.teacher = teacher;
    }

    public List<Tutorship> getStudentsList() {
        List<Tutorship> students = new ArrayList<Tutorship>();
        for (Tutorship tutor : this.studentsList) {
            students.add(tutor);
        }
        return students;
    }

    public void setStudentsList(List<Tutorship> students) {
        this.studentsList = new ArrayList<Tutorship>();
        for (Tutorship tutor : students) {
            this.studentsList.add(tutor);
        }
    }

    public ExecutionYear getExecutionYear() {
        return (executionYear);
    }

    public void setExecutionYear(ExecutionYear executionYear) {
        this.executionYear = executionYear;
    }

    public Teacher getTeacher() {
        return (teacher);
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

}
