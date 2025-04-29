package ufsc.br.epibuilder.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ufsc.br.epibuilder.model.Epitope;

@Repository
public interface EpitopeRepository extends JpaRepository<Epitope, String> {

}
