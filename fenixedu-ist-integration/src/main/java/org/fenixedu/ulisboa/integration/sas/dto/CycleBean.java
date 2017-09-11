package org.fenixedu.ulisboa.integration.sas.dto;

import java.io.Serializable;

public class CycleBean implements Serializable {

    String id;
    int duration;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public CycleBean() {
    }

    public CycleBean(String id, int duration) {
        setDuration(duration);
        setId(id);
    }
}