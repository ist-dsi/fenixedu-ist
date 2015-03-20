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
/**
 * 
 */
package pt.ist.fenixedu.quc.dto;

import org.apache.struts.action.ActionForm;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;

import pt.ist.fenixframework.FenixFramework;

public class SearchInquiriesResultPageDTO extends ActionForm {

    private String method;

    private String executionSemesterID;

    private ExecutionSemester executionSemester;

    private String executionDegreeID;

    private ExecutionDegree executionDegree;

    private String executionCourseID;

    private ExecutionCourse executionCourse;

    public String getExecutionSemesterID() {
        return executionSemesterID;
    }

    public boolean isEmptyExecutionSemesterID() {
        return isNullOrZero(executionSemesterID);
    }

    public void setExecutionSemesterID(String executionSemesterID) {
        this.executionSemesterID = executionSemesterID;
        this.executionSemester =
                isNullOrZero(executionSemesterID) ? null : (ExecutionSemester) FenixFramework
                        .getDomainObject(executionSemesterID);
    }

    private boolean isNullOrZero(String id) {
        return id == null || id.length() == 0;
    }

    public String getExecutionDegreeID() {
        return executionDegreeID;
    }

    public void setExecutionDegreeID(String executionDegreeID) {
        this.executionDegreeID = executionDegreeID;
        this.executionDegree =
                isNullOrZero(executionDegreeID) ? null : (ExecutionDegree) FenixFramework.getDomainObject(executionDegreeID);
    }

    public String getExecutionCourseID() {
        return executionCourseID;
    }

    public void setExecutionCourseID(String executionCourseID) {
        this.executionCourseID = executionCourseID;
        this.executionCourse =
                isNullOrZero(executionCourseID) ? null : (ExecutionCourse) FenixFramework.getDomainObject(executionCourseID);
    }

    public ExecutionSemester getExecutionSemester() {
        return executionSemester;
    }

    public void setExecutionSemester(ExecutionSemester executionSemester) {
        this.executionSemester = executionSemester;
    }

    public ExecutionDegree getExecutionDegree() {
        return executionDegree;
    }

    public ExecutionCourse getExecutionCourse() {
        return executionCourse;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

}
