/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Credits.
 *
 * FenixEdu IST Teacher Credits is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Credits is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Credits.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.teacher.evaluation.domain.credits.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.Professorship;

import pt.ist.fenixedu.teacher.evaluation.domain.teacher.DegreeProjectTutorialService;

public class ProjectTutorialServiceBean implements Serializable {

    protected Professorship professorship;
    protected Attends attend;
    protected Integer percentage;
    protected DegreeProjectTutorialService degreeProjectTutorialService;
    protected List<DegreeProjectTutorialService> othersDegreeProjectTutorialService =
            new ArrayList<DegreeProjectTutorialService>();

    public ProjectTutorialServiceBean(Professorship professorship, Attends attend) {
        this.professorship = professorship;
        this.attend = attend;
        for (DegreeProjectTutorialService degreeProjectTutorialService : attend.getDegreeProjectTutorialServicesSet()) {
            if (degreeProjectTutorialService.getProfessorship().equals(professorship)) {
                this.percentage = degreeProjectTutorialService.getPercentageValue();
                this.degreeProjectTutorialService = degreeProjectTutorialService;
            } else {
                othersDegreeProjectTutorialService.add(degreeProjectTutorialService);
            }
        }
    }

    public Professorship getProfessorship() {
        return professorship;
    }

    public void setProfessorship(Professorship professorship) {
        this.professorship = professorship;
    }

    public Attends getAttend() {
        return attend;
    }

    public void setAttend(Attends attend) {
        this.attend = attend;
    }

    public Integer getPercentage() {
        return percentage;
    }

    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }

    public List<DegreeProjectTutorialService> getOthersDegreeProjectTutorialService() {
        return othersDegreeProjectTutorialService;
    }

    public void setOthersDegreeProjectTutorialService(List<DegreeProjectTutorialService> othersDegreeProjectTutorialService) {
        this.othersDegreeProjectTutorialService = othersDegreeProjectTutorialService;
    }

    public DegreeProjectTutorialService getDegreeProjectTutorialService() {
        return degreeProjectTutorialService;
    }

    public void setDegreeProjectTutorialService(DegreeProjectTutorialService degreeProjectTutorialService) {
        this.degreeProjectTutorialService = degreeProjectTutorialService;
    }

    public String getOthersDegreeProjectTutorialServiceString() {
        List<String> result = new ArrayList<String>();
        for (DegreeProjectTutorialService degreeProjectTutorialService : getOthersDegreeProjectTutorialService()) {
            result.add(degreeProjectTutorialService.getProfessorship().getTeacher().getPerson().getPresentationName() + " - "
                    + degreeProjectTutorialService.getPercentageValue() + "%");
        }
        return result.stream().collect(Collectors.joining("<br/>"));
    }
}