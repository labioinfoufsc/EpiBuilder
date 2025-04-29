package ufsc.br.epibuilder.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

import ufsc.br.epibuilder.model.Status;
import ufsc.br.epibuilder.model.TaskStatus;
import lombok.ToString;
import java.time.LocalDateTime;
import ufsc.br.epibuilder.model.ActionType;
import ufsc.br.epibuilder.model.DisplayMode;
import ufsc.br.epibuilder.model.PredictionModelType;

@Entity
@Table(name = "blasts")
@Getter
@Setter
@ToString
public class Blast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long N;
    private String sacc;
    private Double pident;
    private Double qcovs;
    private String qseq;
    private String sseq;
    private String database;

    @ManyToOne
    @JoinColumn(name = "epitope_id")
    @JsonBackReference
    private Epitope epitope;

}
