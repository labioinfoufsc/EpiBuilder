package ufsc.br.epibuilder.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import ufsc.br.epibuilder.model.Method;
import ufsc.br.epibuilder.model.Epitope;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Represents the topological characteristics of an epitope, including
 * structural and spatial information predicted by various bioinformatics methods.
 * This entity is mapped to the "epitope_topologies" database table.
 */
@Entity
@Table(name = "epitope_topologies")
@Getter
@Setter
public class EpitopeTopology {

    /**
     * Unique identifier for the topology record
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Raw topology prediction data in string format.
     * May contain structural annotations or prediction details.
     */
    @Column(name = "topology_data")
    private String topologyData;

    /**
     * Associated epitope to which this topology belongs.
     * Uses LAZY loading to optimize performance.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epitope_id", nullable = false)
    @JsonBackReference
    private Epitope epitope;

    /**
     * Bioinformatics method used for topology prediction.
     * Examples might include "TMHMM" for transmembrane helices
     * or "SignalP" for signal peptide prediction.
     */
    @Column(nullable = false)
    private Method method;

    /**
     * Threshold value used for topology classification.
     * Represents the cutoff score for considering a feature as present.
     */
    @Column(nullable = false)
    private Double threshold;

    /**
     * Average prediction score across the entire epitope sequence.
     * Indicates the confidence level of the topology prediction.
     */
    @Column(nullable = false)
    private Double avgScore;

    /**
     * Percentage of the epitope sequence covered by the predicted topology feature.
     * Expressed as a decimal value between 0 and 1.
     */
    @Column(nullable = false)
    private Double cover;
}