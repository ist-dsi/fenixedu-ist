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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.fenixedu.academic.domain.CompetenceCourse;

public class CompetenceCourseResultsResume implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CurricularCourseResumeResult> curricularCourseResumeResults;
    private CompetenceCourse competenceCourse;

    public CompetenceCourseResultsResume(CompetenceCourse competenceCourse) {
        setCompetenceCourse(competenceCourse);
    }

    @Override
    public int hashCode() {
        return getCompetenceCourse().hashCode();
    }

    public void addCurricularCourseResumeResult(CurricularCourseResumeResult curricularCourseResumeResult) {
        if (getCurricularCourseResumeResults() == null) {
            setCurricularCourseResumeResults(new ArrayList<CurricularCourseResumeResult>());
        }
        getCurricularCourseResumeResults().add(curricularCourseResumeResult);
    }

    public List<CurricularCourseResumeResult> getOrderedCurricularCourseResumes() {
        Collections.sort(getCurricularCourseResumeResults(),
                Comparator.comparing(CurricularCourseResumeResult::getFirstPresentationName));
        return getCurricularCourseResumeResults();
    }

    public void setCurricularCourseResumeResults(List<CurricularCourseResumeResult> curricularCourseResumeResults) {
        this.curricularCourseResumeResults = curricularCourseResumeResults;
    }

    public List<CurricularCourseResumeResult> getCurricularCourseResumeResults() {
        return curricularCourseResumeResults;
    }

    public void setCompetenceCourse(CompetenceCourse competenceCourse) {
        this.competenceCourse = competenceCourse;
    }

    public CompetenceCourse getCompetenceCourse() {
        return competenceCourse;
    }
}
