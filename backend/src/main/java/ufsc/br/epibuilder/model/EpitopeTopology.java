package ufsc.br.epibuilder.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the topological characteristics of an epitope, including
 * structural and spatial information predicted by various bioinformatics
 * methods.
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
    @ManyToOne
    @JoinColumn(name = "epitope_id")
    @JsonBackReference
    private Epitope epitope;

    /**
     * Method used for topology prediction.
     * Enumerated type to restrict values to predefined methods.
     * 
     */
    @Column
    @Enumerated(EnumType.STRING)
    private Method method;

    /**
     * Threshold value used for topology classification.
     * Represents the cutoff score for considering a feature as present.
     */
    @Column(nullable = true)
    private Double threshold;

    /**
     * Average prediction score across the entire epitope sequence.
     * Indicates the confidence level of the topology prediction.
     */
    @Column
    private Double avgScore;

    /**
     * Percentage of the epitope sequence covered by the predicted topology feature.
     * Expressed as a decimal value between 0 and 1.
     */
    @Column
    private Double cover;
}