/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Vigilancies.
 *
 * FenixEdu IST Vigilancies is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Vigilancies is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Vigilancies.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.vigilancies.service;

import java.util.ArrayList;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.exceptions.DomainException;

import pt.ist.fenixedu.vigilancies.domain.VigilantGroup;
import pt.ist.fenixframework.Atomic;

public class AddExecutionCourseToGroup {

    public static List<ExecutionCourse> run(VigilantGroup group, List<ExecutionCourse> executionCourses) {

        List<ExecutionCourse> executionCoursesUnableToAdd = new ArrayList<ExecutionCourse>();
        for (ExecutionCourse course : executionCourses) {
            try {
                runAtomic(group, course);
            } catch (DomainException e) {
                executionCoursesUnableToAdd.add(course);
            }
        }
        return executionCoursesUnableToAdd;
    }

    @Atomic
    private static void runAtomic(VigilantGroup group, ExecutionCourse course) {
        group.addExecutionCourses(course);
    }

}