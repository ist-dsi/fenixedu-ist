/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.api.beans.publico;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExportGrouping;
import org.fenixedu.academic.domain.Grouping;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentGroup;
import org.fenixedu.academic.util.EnrolmentGroupPolicyType;
import org.joda.time.DateTime;

public class FenixCourseGroup {

    public static class GroupedCourse {

        String name;
        List<FenixDegree> degrees;
        String id;

        public GroupedCourse(final ExecutionCourse executionCourse) {
            setName(executionCourse.getName());
            setId(executionCourse.getExternalId());
            setDegrees(executionCourse);
        }

        private void setDegrees(ExecutionCourse executionCourse) {
            this.degrees = new ArrayList<>();
            for (Degree degree : executionCourse.getDegreesSortedByDegreeName()) {
                degrees.add(new FenixDegree(degree));
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<FenixDegree> getDegrees() {
            return degrees;
        }

        public void setDegrees(List<FenixDegree> degrees) {
            this.degrees = degrees;
        }

    }

    public static class StudentGroupMembers {
        int groupNumber;
        String shift;
        List<StudentGroupMember> members;

        public static class StudentGroupMember {
            String name;
            String username;

            public StudentGroupMember(Person person) {
                this.name = person.getName();
                this.username = person.getUsername();
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }
        }

        public StudentGroupMembers(StudentGroup studentGroup) {
            this.groupNumber = studentGroup.getGroupNumber();
            this.shift = studentGroup.getShift() != null ? studentGroup.getShift().getPresentationName() : null;
            this.members = studentGroup.getAttendsSet().stream()
                    .map(a -> a.getRegistration().getPerson())
                    .map(StudentGroupMember::new)
                    .collect(Collectors.toList());
        }

        public int getGroupNumber() {
            return groupNumber;
        }

        public void setGroupNumber(int groupNumber) {
            this.groupNumber = groupNumber;
        }

        public String getShift() {
            return shift;
        }

        public void setShift(String shift) {
            this.shift = shift;
        }

        public List<StudentGroupMember> getMembers() {
            return members;
        }

        public void setMembers(List<StudentGroupMember> members) {
            this.members = members;
        }
    }

    String name;
    String description;
    FenixInterval enrolmentPeriod;
    String enrolmentPolicy;
    Integer minimumCapacity;
    Integer maximumCapacity;
    Integer idealCapacity;
    List<GroupedCourse> associatedCourses = new ArrayList<>();
    List<StudentGroupMembers> associatedGroups = new ArrayList<>();

    public FenixCourseGroup(final Grouping grouping) {
        this.name = grouping.getName();
        this.description = grouping.getProjectDescription();

        final DateTime start = grouping.getEnrolmentBeginDayDateDateTime();
        final DateTime end = grouping.getEnrolmentEndDayDateDateTime();
        this.enrolmentPeriod = new FenixInterval(start, end);

        final EnrolmentGroupPolicyType enrolmentPolicy = grouping.getEnrolmentPolicy();
        this.enrolmentPolicy = enrolmentPolicy == null ? null : enrolmentPolicy.getTypeFullName();

        this.minimumCapacity = grouping.getMinimumCapacity();
        this.maximumCapacity = grouping.getMaximumCapacity();
        this.idealCapacity = grouping.getIdealCapacity();

        for (final ExportGrouping exportGrouping : grouping.getExportGroupingsSet()) {
            final ExecutionCourse executionCourse = exportGrouping.getExecutionCourse();
            associatedCourses.add(new GroupedCourse(executionCourse));
        }

        this.associatedGroups = grouping.getStudentGroupsSet().stream().map(StudentGroupMembers::new).collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public FenixInterval getEnrolmentPeriod() {
        return enrolmentPeriod;
    }

    public void setEnrolmentPeriod(final FenixInterval enrolmentPeriod) {
        this.enrolmentPeriod = enrolmentPeriod;
    }

    public String getEnrolmentPolicy() {
        return enrolmentPolicy;
    }

    public void setEnrolmentPolicy(final String enrolmentPolicy) {
        this.enrolmentPolicy = enrolmentPolicy;
    }

    public Integer getMinimumCapacity() {
        return minimumCapacity;
    }

    public void setMinimumCapacity(final Integer minimumCapacity) {
        this.minimumCapacity = minimumCapacity;
    }

    public Integer getMaximumCapacity() {
        return maximumCapacity;
    }

    public void setMaximumCapacity(final Integer maximumCapacity) {
        this.maximumCapacity = maximumCapacity;
    }

    public Integer getIdealCapacity() {
        return idealCapacity;
    }

    public void setIdealCapacity(final Integer idealCapacity) {
        this.idealCapacity = idealCapacity;
    }

    public List<GroupedCourse> getAssociatedCourses() {
        return associatedCourses;
    }

    public void setAssociatedCourses(List<GroupedCourse> associatedCourses) {
        this.associatedCourses = associatedCourses;
    }

    public List<StudentGroupMembers> getAssociatedGroups() {
        return associatedGroups;
    }

    public void setAssociatedGroups(List<StudentGroupMembers> groups) {
        this.associatedGroups = groups;
    }

}
