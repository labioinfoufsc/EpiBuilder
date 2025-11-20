package ufsc.br.epibuilder.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ufsc.br.epibuilder.model.EpitopeTaskData;
import ufsc.br.epibuilder.model.Status;
import ufsc.br.epibuilder.model.TaskStatus;

import java.util.Optional;

import java.util.List;

@Repository
public interface EpitopeTaskDataRepository extends JpaRepository<EpitopeTaskData, Long> {

    List<EpitopeTaskData> findTasksByUserId(Long userId);

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

    @EntityGraph(attributePaths = { "epitopes" })
    Optional<EpitopeTaskData> findById(Long id);

    List<EpitopeTaskData> findByUserIdAndTaskStatusStatus(Long userId, Status status);

    @Query("SELECT t FROM EpitopeTaskData t JOIN FETCH t.taskStatus WHERE t.taskStatus.status = :status")
    List<EpitopeTaskData> findTasksByTaskStatusStatus(@Param("status") Status status);

}