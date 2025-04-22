package ufsc.br.epibuilder.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.util.List;

/**
 * Represents an epitope entity with its biochemical properties and
 * characteristics.
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

    @Column
    private String epitopeId;

    @Column
    private Long N;

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
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_data_id", nullable = false)
    @JsonBackReference
    private EpitopeTaskData epitopeTaskData;

    /**
     * The topological information associated with this epitope.
     * Cascades all operations and removes orphaned topology.
     */
    @OneToMany(mappedBy = "epitope", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<EpitopeTopology> epitopeTopologies;

    /**
     * The epitope sequence in string format.
     */
    @Column
    private String epitope;

    /**
     * The starting position of the epitope in the parent protein sequence.
     */
    @Column
    private Integer start;

    /**
     * The ending position of the epitope in the parent protein sequence.
     */
    @Column
    private Integer end;

    /**
     * The length of the epitope sequence.
     */
    @Column
    private Integer length;

    /**
     * The hydropathy index of the epitope.
     */
    @Column
    private Double hydropathy;

    /**
     * The isoelectric point (pI) of the epitope.
     */
    @Column
    private Double isoelectricPoint;

    /**
     * The molecular weight of the epitope in Daltons.
     */
    @Column
    private Double molecularWeight;

    /**
     * The Parker hydrophilicity prediction score.
     */
    @Column
    private Double parker;

    /**
     * The N-glycosylation potential of the epitope.
     */
    @Column
    private Integer nGlyc;

    /**
     * The count of potential N-glycosylation sites.
     */
    @Column
    private Integer nGlycCount;

    /**
     * The Karplus-Schulz flexibility prediction score.
     */
    @Column
    private Double karplusSchulz;

    /**
     * The Kolaskar-Tongaonkar antigenicity prediction score.
     */
    private Double kolaskar;

    /**
     * The Chou-Fasman secondary structure prediction score.
     */
    @Column
    private Double chouFosman;

    /**
     * The Emini surface accessibility prediction score.
     */
    @Column
    private Double emini;

    /**
     * The BepiPred 3.0 linear epitope prediction score.
     */
    @Column
    private Double bepiPred3;
}