package ufsc.br.epibuilder.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum Organism {
    ANIMAL, PLANT, FUNGI;

    @JsonCreator
    public static Organism fromString(String value) {
        return Arrays.stream(Organism.values())
                .filter(e -> e.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid Organism: " + value));
    }
}