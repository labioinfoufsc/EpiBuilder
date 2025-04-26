package ufsc.br.epibuilder.model;

public enum PredictionModelType {
    AAC("AAC based RF"), HYBRID("Hybrid");

    private final String description;

    PredictionModelType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
