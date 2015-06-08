/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST CMS Components.
 *
 * FenixEdu IST CMS Components is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST CMS Components is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST CMS Components.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.cmscomponents.domain.unit.components;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.TeacherAuthorization;
import org.fenixedu.academic.domain.TeacherCategory;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.component.ComponentType;
import org.fenixedu.cms.rendering.TemplateContext;
import pt.ist.fenixedu.contracts.domain.Employee;

import java.util.*;
import java.util.function.Supplier;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;

@ComponentType(name = "Unit Teachers", description = "Teachers information for a Department")
public class UnitTeachersComponent extends UnitSiteComponent {

    private static final Supplier<TreeMap<TeacherCategory, TreeSet<Teacher>>> categoryFactory =
            () -> new TreeMap<>(Comparator.reverseOrder());

    private static final Comparator<Teacher> TEACHER_CATEGORY_COMPARATOR = (t1, t2) ->
            t1.getCategory() != null && t2.getCategory() != null ? t1.getCategory().compareTo(t2.getCategory()) : 0;

    private static final Comparator<Teacher> TEACHER_COMPARATOR = (t1, t2) -> {
        int result = TEACHER_CATEGORY_COMPARATOR.reversed().compare(t1, t2);
        return result != 0 ? result : Teacher.TEACHER_COMPARATOR_BY_CATEGORY_AND_NUMBER.compare(t1, t2);
    };

    @Override
    public void handle(Page page, TemplateContext componentContext, TemplateContext globalContext) {
        Unit unit = unit(page);
        globalContext.put("teachersByCategory", teachersByCategory(unit));
        setupTeachersAreas(unit, globalContext);
        globalContext.put("currentSemester", ExecutionSemester.readActualExecutionSemester());
    }

    private SortedMap<TeacherCategory, TreeSet<Teacher>> teachersByCategory(Unit unit) {
        Supplier<TreeSet<Teacher>> createSortedTreeSet = () -> new TreeSet<>(TEACHER_COMPARATOR);
        return unit.getDepartmentUnit().getDepartment().getAllCurrentTeachers().stream()
                .filter(teacher -> teacher.getCategory() != null)
                .collect(groupingBy(Teacher::getCategory, categoryFactory, toCollection(createSortedTreeSet)));
    }

    private void setupTeachersAreas(Unit unit, TemplateContext globalContext) {
        SortedMap<Unit, TreeSet<Teacher>> teachersByAreas = new TreeMap<>(Unit.COMPARATOR_BY_NAME_AND_ID);
        SortedSet<Teacher> teachersNoArea = new TreeSet<>(TEACHER_COMPARATOR);

        for (Teacher teacher : unit.getDepartmentUnit().getDepartment().getAllCurrentTeachers()) {
            Unit area = getCurrentSectionOrScientificArea(teacher);

            if (area != null) {
                teachersByAreas.putIfAbsent(area, new TreeSet<>(TEACHER_COMPARATOR));
                teachersByAreas.get(area).add(teacher);
            } else {
                teachersNoArea.add(teacher);
            }
        }
        globalContext.put("teachersByArea", teachersByAreas);
        globalContext.put("teachersWithoutArea", teachersNoArea);
    }

    public Unit getCurrentSectionOrScientificArea(Teacher teacher) {
        final Employee employee = teacher.getPerson().getEmployee();
        if (employee != null) {
            final Unit unit = getCurrentSectionOrScientificArea(employee);
            if (unit != null) {
                return unit;
            }
        }

        Optional<TeacherAuthorization> teacherAuthorization = teacher.getTeacherAuthorization();
        if (teacherAuthorization.isPresent() && !teacherAuthorization.get().isContracted()) {
            return teacherAuthorization.get().getDepartment().getDepartmentUnit();
        }
        return null;
    }

    private Unit getCurrentSectionOrScientificArea(Employee employee) {
        return getSectionOrScientificArea(employee.getCurrentWorkingPlace());
    }

    private Unit getSectionOrScientificArea(Unit unit) {
        if (unit == null || unit.isScientificAreaUnit() || unit.isSectionUnit()) {
            return unit;
        }

        for (Unit parent : unit.getParentUnits()) {
            Unit parentUnit = getSectionOrScientificArea(parent);

            if (parentUnit != null) {
                return parentUnit;
            }
        }

        return null;
    }

}