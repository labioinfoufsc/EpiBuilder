package ufsc.br.epibuilder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ufsc.br.epibuilder.model.EpitopeTaskData;
import ufsc.br.epibuilder.model.Status;
import ufsc.br.epibuilder.model.TaskStatus;

import java.util.List;

@Repository
public interface EpitopeTaskDataRepository extends JpaRepository<EpitopeTaskData, String> {

    public List<EpitopeTaskData> findTasksByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM EpitopeTopology et WHERE et.epitope IN (SELECT e FROM Epitope e WHERE e.epitopeTaskData.id = :taskId)")
    void deleteTopologiesByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("DELETE FROM Blast b WHERE b.epitope IN (SELECT e FROM Epitope e WHERE e.epitopeTaskData.id = :taskId)")
    void deleteBlastsByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("DELETE FROM Epitope e WHERE e.epitopeTaskData.id = :taskId")
    void deleteEpitopesByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("DELETE FROM EpitopeTaskData etd WHERE etd.id = :taskId")
    void deleteTaskById(@Param("taskId") Long taskId);

    public EpitopeTaskData findById(Long id);

    public List<EpitopeTaskData> findTasksByTaskStatusStatus(Status status);

}