package ufsc.br.epibuilder.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ufsc.br.epibuilder.model.Status;

@Entity
@Table(name = "task_status")
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TaskStatus {

    /**
     * Unique identifier for the task status
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @OneToOne(mappedBy = "taskStatus", fetch = FetchType.LAZY)
    @JsonBackReference
    private EpitopeTaskData epitopeTaskData;

    /**
     * Unique identifier for the task process (UUID)
     * This is used to track the task in the background processing system
     */
    @Column
    private String taskUUID;

    /**
     * Status of the task (e.g., PENDING, RUNNING, COMPLETED, FAILED)
     */
    @Column
    @Enumerated(EnumType.STRING)
    private Status status;

    
}
