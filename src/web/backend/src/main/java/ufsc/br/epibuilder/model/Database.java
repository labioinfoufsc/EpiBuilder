package ufsc.br.epibuilder.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "db_files")
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Database {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String alias;
    @Column(nullable = false)
    private String fileName;
    @Column(nullable = false)
    private String absolutePath;
    @Column(nullable = false)
    private LocalDateTime date;
    @Transient
    private String sourceType;

    @Override
    public String toString() {
        return this.alias + "=" + this.absolutePath;
    }
}
