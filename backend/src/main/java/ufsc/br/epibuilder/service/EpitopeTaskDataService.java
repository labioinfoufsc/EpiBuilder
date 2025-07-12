package ufsc.br.epibuilder.service;

import org.springframework.stereotype.Service;
import ufsc.br.epibuilder.repository.EpitopeTaskDataRepository;
import ufsc.br.epibuilder.model.EpitopeTaskData;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import ufsc.br.epibuilder.model.Status;

/**
 * Service layer for handling operations related to EpitopeTaskData.
 */
@Service
@Slf4j
@Transactional
public class EpitopeTaskDataService {

    private final EpitopeTaskDataRepository epitopeTaskDataRepository;

    public EpitopeTaskDataService(EpitopeTaskDataRepository epitopeTaskDataRepository) {
        this.epitopeTaskDataRepository = epitopeTaskDataRepository;
    }

    /**
     * Saves an epitope.
     *
     * @param epitope the epitope to save
     * @return the saved epitope
     */
    public EpitopeTaskData save(EpitopeTaskData epitopeTaskData) {
        return epitopeTaskDataRepository.save(epitopeTaskData);
    }

    public EpitopeTaskData update(EpitopeTaskData epitopeTaskData) {
        return epitopeTaskDataRepository.save(epitopeTaskData);
    }

    public EpitopeTaskData findById(Long id) {
        return epitopeTaskDataRepository.findById(id);
    }

    /**
     * Retrieves all epitopes.
     *
     * @return a list of all epitopes
     */
    public List<EpitopeTaskData> findAll() {
        return epitopeTaskDataRepository.findAll();
    }

    public List<EpitopeTaskData> findTasksByTaskStatusStatus(Status status) {
        return epitopeTaskDataRepository.findTasksByTaskStatusStatus(status);
    }

    @Transactional
    public void deleteEpitopeTaskDataWithAssociations(Long id) {
        epitopeTaskDataRepository.deleteTopologiesByTaskId(id);
        epitopeTaskDataRepository.deleteBlastsByTaskId(id);
        epitopeTaskDataRepository.deleteEpitopesByTaskId(id);
        epitopeTaskDataRepository.deleteTaskById(id);
    }

    public List<EpitopeTaskData> findTasksByUserId(Long userId) {

        log.info("Fetching tasks for user ID: {}", userId);
        List<EpitopeTaskData> tasks = epitopeTaskDataRepository.findTasksByUserId(userId);

        log.info("Number of tasks found: {}", tasks.size());
        tasks.forEach(task -> log.info("Task ID: {}, User ID: {}",
                task.getId(),
                task.getUser() != null ? task.getUser().getId() : "null"));

        return tasks;
    }

}