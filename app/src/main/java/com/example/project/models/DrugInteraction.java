package com.example.project.models;

public class DrugInteraction {
    private String id;
    private String genericA;
    private String genericB;
    private String severity;
    private String message;

    public DrugInteraction() {
    }

    public DrugInteraction(String id, String genericA, String genericB, String severity, String message) {
        this.id = id;
        this.genericA = genericA;
        this.genericB = genericB;
        this.severity = severity;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGenericA() {
        return genericA;
    }

    public void setGenericA(String genericA) {
        this.genericA = genericA;
    }

    public String getGenericB() {
        return genericB;
    }

    public void setGenericB(String genericB) {
        this.genericB = genericB;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
