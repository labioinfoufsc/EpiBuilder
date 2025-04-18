package ufsc.br.epibuilder.model;

public enum Method {
    BEPIPRED("BepiPred"),
    EMINI("Emini"),
    CHOU_FASMAN("Chou Fasman"),
    KOLASKAR("Kolaskar"),
    KARPLUS_SCHULZ("Karplus Schulz"),
    PARKER("Parker"),
    ALL("All matches"),
    NGLYC("N-Glyc"),
    HYDROPATHY("Hidropathy");

    private String description;

    Method(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
