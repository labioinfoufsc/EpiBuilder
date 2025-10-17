package ufsc.br.epibuilder.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum CellType {
    EUKARYOTE, BACTERIA, ARCHAEA;

     @JsonCreator
    public static CellType fromString(String value) {
        return Arrays.stream(CellType.values())
                .filter(e -> e.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid CellType: " + value));
    }
}
