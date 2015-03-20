/**
 * Copyright © 2011 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Exam Vigilancies.
 *
 * FenixEdu Exam Vigilancies is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Exam Vigilancies is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Exam Vigilancies.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.vigilancies.ui.struts.action.vigilancy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fenixedu.academic.domain.WrittenEvaluation;
import org.fenixedu.academic.domain.organizationalStructure.Unit;

import pt.ist.fenixedu.vigilancies.domain.VigilantGroup;
import pt.ist.fenixedu.vigilancies.domain.VigilantWrapper;

public class VigilancyConvokeBean implements Serializable {

    private Unit unit;

    private WrittenEvaluation writtenEvaluation;

    private VigilantGroup vigilantGroup;

    private Collection<VigilantWrapper> vigilantsSugestion;

    private Boolean convokeIsFinal;

    private Integer numberOfVigilantsToSelect;

    public Unit getUnit() {
        return this.unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public WrittenEvaluation getWrittenEvaluation() {
        return this.writtenEvaluation;
    }

    public void setWrittenEvaluation(WrittenEvaluation writtenEvaluation) {
        this.writtenEvaluation = writtenEvaluation;
    }

    public VigilantGroup getVigilantGroup() {
        return this.vigilantGroup;
    }

    public void setVigilantGroup(VigilantGroup vigilantGroup) {
        this.vigilantGroup = vigilantGroup;
    }

    public Boolean getConvokeIsFinal() {
        return convokeIsFinal;
    }

    public void setConvokeIsFinal(Boolean convokeIsFinal) {
        this.convokeIsFinal = convokeIsFinal;
    }

    public Integer getNumberOfVigilantsToSelect() {
        return numberOfVigilantsToSelect;
    }

    public void setNumberOfVigilantsToSelect(Integer numberOfVigilantsToSelect) {
        this.numberOfVigilantsToSelect = numberOfVigilantsToSelect;
    }

    public List<VigilantWrapper> getVigilantsSugestion() {
        List vigilants = new ArrayList<VigilantWrapper>();
        for (VigilantWrapper vigilant : this.vigilantsSugestion) {
            if (vigilant != null) {
                vigilants.add(vigilant);
            }
        }
        return vigilants;
    }

    public void setVigilantsSugestion(List<VigilantWrapper> vigilantsList) {
        this.vigilantsSugestion = new ArrayList<VigilantWrapper>();
        for (VigilantWrapper vigilant : vigilantsList) {
            if (vigilant != null) {
                this.vigilantsSugestion.add(vigilant);
            }
        }
    }

}
