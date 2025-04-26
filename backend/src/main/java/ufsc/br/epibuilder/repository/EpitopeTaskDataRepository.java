package ufsc.br.epibuilder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ufsc.br.epibuilder.model.EpitopeTaskData;
import ufsc.br.epibuilder.model.Status;
import ufsc.br.epibuilder.model.TaskStatus;

import java.util.List;

@Repository
public interface EpitopeTaskDataRepository extends JpaRepository<EpitopeTaskData, String> {

    public List<EpitopeTaskData> findTasksByUserId(Long userId);

    public Long deleteById(Long id);

    public EpitopeTaskData findById(Long id);

    public List<EpitopeTaskData> findTasksByTaskStatusStatus(Status status);

}