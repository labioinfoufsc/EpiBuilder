package ufsc.br.epibuilder.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BiologicalClassification {

    private CellType cellType;
    private Organism organism;
    private BacterialType bacterialType;

    public String toLocParam() {
        if (cellType == null)
            return null;

        switch (cellType) {
            case EUKARYOTE:
                return organism != null ? organism.toString().toLowerCase() : null;
            case BACTERIA:
                if (bacterialType == BacterialType.GRAM_POSITIVE)
                    return "gram_pos";
                if (bacterialType == BacterialType.GRAM_NEGATIVE)
                    return "gram_neg";
                break;
            case ARCHAEA:
                return "arch";
        }

        return null;
    }

}