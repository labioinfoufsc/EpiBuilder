package ufsc.br.epibuilder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ufsc.br.epibuilder.model.EpitopeTaskData;

@Repository
public interface EpitopeTaskDataRepository extends JpaRepository<EpitopeTaskData, String> {
}