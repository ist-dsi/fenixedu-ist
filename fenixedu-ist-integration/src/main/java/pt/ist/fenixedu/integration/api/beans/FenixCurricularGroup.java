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
package pt.ist.fenixedu.integration.api.beans;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;

public class FenixCurricularGroup {
    String id;
    String name;
    Set<String> parentGroupsId;
    List<String> childCoursesId;

    public FenixCurricularGroup(final CourseGroup courseGroup, final ExecutionYear executionYear) {
        setId(courseGroup.getExternalId());
        setName(courseGroup.getName());

        parentGroupsId = new HashSet<String>();
        courseGroup.getParentCourseGroups().stream().flatMap(pcg -> pcg.getValidChildContexts(executionYear).stream())
                .forEach(context -> parentGroupsId.add(context.getParentCourseGroup().getExternalId()));

        childCoursesId = new ArrayList<String>();
        courseGroup.getChildDegreeModulesValidOn(executionYear).stream().filter(DegreeModule::isCurricularCourse)
                .forEach(dm -> childCoursesId.add(dm.getExternalId()));
    }

    public FenixCurricularGroup(String id, String name) {
        setId(id);
        setName(name);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getParentGroupsId() {
        return parentGroupsId;
    }

    public List<String> getChildCoursesId() {
        return childCoursesId;
    }
}