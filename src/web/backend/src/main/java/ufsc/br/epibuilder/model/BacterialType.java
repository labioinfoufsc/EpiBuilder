package ufsc.br.epibuilder.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum BacterialType {
    GRAM_POSITIVE, GRAM_NEGATIVE;

    @JsonCreator
    public static BacterialType fromString(String value) {
        return Arrays.stream(BacterialType.values())
                .filter(e -> e.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid BacterialType: " + value));
    }
}
