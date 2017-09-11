package org.fenixedu.ulisboa.integration.sas.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ActiveDegreeBean implements Serializable {
    String degreeCode;
    String designation;
    String schoolLevel;
    List<CycleBean> cycles;
    String duration;
    String oficialCode;

    public String getDegreeCode() {
        return degreeCode;
    }

    public void setDegreeCode(String degreeCode) {
        this.degreeCode = degreeCode;
    }

    public String getDesignation() {
        return designation != null ? designation : "";
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getSchoolLevel() {
        return schoolLevel != null ? schoolLevel : "";
    }

    public void setSchoolLevel(String schoolLevel) {
        this.schoolLevel = schoolLevel;
    }

    public List<CycleBean> getCycles() {
        return cycles != null ? cycles : new ArrayList<CycleBean>();
    }

    public void setCycles(List<CycleBean> cycles) {
        this.cycles = cycles;
    }

    public String getDuration() {
        return duration != null ? duration : "";
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getOficialCode() {
        return oficialCode != null ? oficialCode : "";
    }

    public void setOficialCode(String oficialCode) {
        this.oficialCode = oficialCode;
    }

}