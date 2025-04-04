package ufsc.br.epibuilder.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents an epitope entity with its biochemical properties and characteristics.
 * This class is mapped to the "epitopes" table in the database.
 */
@Entity
@Table(name = "epitopes")
@Getter
@Setter
public class Epitope {
    
    /**
     * The unique identifier for the epitope.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The amino acid sequence of the epitope.
     */
    @Column(name = "sequence")
    private String sequence;

    /**
     * The prediction score assigned to the epitope.
     */
    @Column(name = "score")
    private Double score;

    /**
     * The associated task data that produced this epitope.
     * Uses lazy fetching to improve performance.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_data_id", nullable = false)
    private EpitopeTaskData epitopeTaskData;

    /**
     * The topological information associated with this epitope.
     * Cascades all operations and removes orphaned topology.
     */
    @OneToOne(mappedBy = "epitope", cascade = CascadeType.ALL, orphanRemoval = true)
    private EpitopeTopology epitopeTopology;

    /**
     * The epitope sequence in string format.
     */
    private String epitope;

    /**
     * The starting position of the epitope in the parent protein sequence.
     */
    private Integer start;

    /**
     * The ending position of the epitope in the parent protein sequence.
     */
    private Integer end;

    /**
     * The length of the epitope sequence.
     */
    private Integer length;

    /**
     * The hydropathy index of the epitope.
     */
    private Double hydropathy;

    /**
     * The isoelectric point (pI) of the epitope.
     */
    private Double isoelectricPoint;

    /**
     * The molecular weight of the epitope in Daltons.
     */
    private Double molecularWeight;

    /**
     * The Parker hydrophilicity prediction score.
     */
    private Double parker;

    /**
     * The N-glycosylation potential of the epitope.
     */
    private Integer nGlyc;

    /**
     * The count of potential N-glycosylation sites.
     */
    private Integer nGlycCount;

    /**
     * The Karplus-Schulz flexibility prediction score.
     */
    private Double karplusSchulz;

    /**
     * The Kolaskar-Tongaonkar antigenicity prediction score.
     */
    private Double kolaskar;

    /**
     * The Chou-Fasman secondary structure prediction score.
     */
    private Double chouFosman;

    /**
     * The Emini surface accessibility prediction score.
     */
    private Double emini;

    /**
     * The BepiPred 3.0 linear epitope prediction score.
     */
    private Double bepiPred3;
}