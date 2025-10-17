package ufsc.br.epibuilder.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "proteins")
@Getter
@Setter
public class Protein {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String proteinId;

    @Column
    private String description;

    @Column
    private String localization;

    @OneToOne(mappedBy = "protein")
    @JsonBackReference
    private Epitope epitope;
    
}
