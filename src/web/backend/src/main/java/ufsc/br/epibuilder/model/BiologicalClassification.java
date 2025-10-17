package ufsc.br.epibuilder.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BiologicalClassification {

    private CellType cellType;
    private Organism organism;
    private BacterialType bacterialType;

}