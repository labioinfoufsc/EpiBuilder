package ufsc.br.epibuilder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ufsc.br.epibuilder.model.EpitopeTaskData;

import java.util.List;

@Repository
public interface EpitopeTaskDataRepository extends JpaRepository<EpitopeTaskData, String> {
   
    public List<EpitopeTaskData> findTasksByUserId(Long userId);

    public boolean deleteById(Long id);

}