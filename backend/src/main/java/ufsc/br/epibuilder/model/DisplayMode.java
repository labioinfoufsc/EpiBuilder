package ufsc.br.epibuilder.model;

public enum DisplayMode {
    ALLERGEN_ONLY("Allergen Peptides Only"), ALL("All Peptides");

    private final String description;

    private DisplayMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
