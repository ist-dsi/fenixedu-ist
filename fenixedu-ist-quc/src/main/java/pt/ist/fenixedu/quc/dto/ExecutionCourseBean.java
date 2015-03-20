/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST QUC.
 *
 * FenixEdu IST QUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST QUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST QUC.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.quc.dto;

import java.io.Serializable;
import java.util.Set;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionCourse;

import pt.ist.fenixedu.quc.domain.ExecutionCourseAudit;
import pt.ist.fenixedu.quc.domain.InquiriesRoot;
import pt.ist.fenixframework.Atomic;

public class ExecutionCourseBean implements Serializable {

    private static final long serialVersionUID = 1L;
    ExecutionCourse executionCourse;

    public ExecutionCourseBean(ExecutionCourse executionCourse) {
        super();
        this.executionCourse = executionCourse;
    }

    public Set<Department> getDepartments() {
        return executionCourse.getDepartments();
    }

    public String getNome() {
        return executionCourse.getNome();
    }

    public String getDegreePresentationString() {
        return executionCourse.getDegreePresentationString();
    }

    public boolean isExecutionCourseUnderAudition() {
        return executionCourse.getExecutionCourseAudit() != null;
    }

    public String getExternalId() {
        return executionCourse.getExternalId();
    }

    public ExecutionCourseAudit getExecutionCourseAudit() {
        return executionCourse.getExecutionCourseAudit();
    }

    public boolean isAvailableForInquiries() {
        return executionCourse.getAvailableForInquiries() != null;
    }

    @Atomic
    public void setAvailableForInquiries(boolean availableForInquiries) {
        if (!availableForInquiries) {
            executionCourse.setAvailableForInquiries(null);
        } else {
            executionCourse.setAvailableForInquiries(InquiriesRoot.getInstance());
        }
    }

}
