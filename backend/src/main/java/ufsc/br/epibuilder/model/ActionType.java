package ufsc.br.epibuilder.model;

/**
 * Enum representing the different types of actions that can be performed.
 */
public enum ActionType {

    /**
     * Action for prediction only.
     */
    PREDICT("predict"),
    
    /**
     * Action for analysis only.
     */
    ANALYZE("analyze");

    private final String action;

    /**
     * Constructs an ActionType enum constant.
     * 
     * @param action The string representation of the action.
     */
    ActionType(String action) {
        this.action = action;
    }

    /**
     * Gets the string representation of the action.
     * 
     * @return The action as a string.
     */
    public String getAction() {
        return action;
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
            if (type.action.equalsIgnoreCase(action)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No constant with action " + action + " found");
    }
}