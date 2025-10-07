package ufsc.br.epibuilder.model;

public enum Method {
    BEPIPRED("BepiPred"),
    EMINI("Emini"),
    CHOU_FASMAN("Chou Fosman"),
    KOLASKAR("Kolaskar"),
    KARPLUS_SCHULZ("Karplus Schulz"),
    PARKER("Parker"),
    ALL_MATCHES("All matches"),
    N_GLYC("N-Glyc"),
    HYDROPATHY("Hydropathy");

    private final String description;

    Method(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static Method fromDescription(String description) {
        for (Method method : values()) {
            if (method.getDescription().equalsIgnoreCase(description)) {
                return method;
            }
        }
        throw new IllegalArgumentException("No method found with description: " + description);
    }
}
