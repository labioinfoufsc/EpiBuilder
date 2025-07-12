package ufsc.br.epibuilder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ufsc.br.epibuilder.model.EpitopeTopology;

@Repository
public interface EpitopeTopologyRepository extends JpaRepository<EpitopeTopology, String> {
}