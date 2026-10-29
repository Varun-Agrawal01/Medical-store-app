package com.example.project.models;

public class Symptom {
    private String id;
    private String name;
    private String redFlagAdvice;
    private boolean selected;

    public Symptom() {
    }

    public Symptom(String id, String name, String redFlagAdvice) {
        this.id = id;
        this.name = name;
        this.redFlagAdvice = redFlagAdvice;
        this.selected = false;
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

    public String getRedFlagAdvice() {
        return redFlagAdvice;
    }

    public void setRedFlagAdvice(String redFlagAdvice) {
        this.redFlagAdvice = redFlagAdvice;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
