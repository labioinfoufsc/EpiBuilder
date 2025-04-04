package ufsc.br.epibuilder.model;

import ufsc.br.epibuilder.model.Status;

public class TaskStatus {

    private Status status;
    private String output;
    private String error;

    public TaskStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}