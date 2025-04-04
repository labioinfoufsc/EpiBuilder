package ufsc.br.epibuilder.model;

public enum Method {
    BepiPred("BepiPred"),
    Emini("Emini"),
    ChouFasman("Chou Fasman"),
    Kolaskar("Kolaskar"),
    KarplusSchulz("Karplus Schulz"),
    Parker("Parker"),
    All("All matches"),
    NGlyc("N-Glyc"),
    Hydropathy("Hidropathy");

    private String description;

    Method(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
