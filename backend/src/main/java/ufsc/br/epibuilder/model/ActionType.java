package ufsc.br.epibuilder.model;

/**
 * Enum representing the different types of actions that can be performed.
 */
public enum ActionType {

    /**
     * Action for prediction only.
     */
    PREDICT(0, "predict"),

    /**
     * Action for analysis only.
     */
    ANALYSIS(1, "analysis");

    private final String desc;

    /**
     * Constructor for ActionType enum.
     * 
     * @param id     The ID of the action type.
     * @param action The string representation of the action type.
     */
    ActionType(int id, String desc) {
        this.desc = desc;
    }

    /**
     * Gets the string representation of the action.
     * 
     * @return The action as a string.
     */
    public String getDesc() {
        return desc;
    }

    /**
     * Converts a string to the corresponding ActionType enum constant.
     * 
     * @param action The string to convert (case-insensitive).
     * @return The matching ActionType enum constant.
     * @throws IllegalArgumentException if no matching constant is found.
     */
    public static ActionType fromString(String action) {
        for (ActionType type : ActionType.values()) {
            if (type.desc.equalsIgnoreCase(action)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No constant with action " + action + " found");
    }
}