package ufsc.br.epibuilder.service;

import org.springframework.stereotype.Service;
import ufsc.br.epibuilder.repository.EpitopeTaskDataRepository;
import ufsc.br.epibuilder.model.EpitopeTaskData;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for handling operations related to EpitopeTaskData.
 */
@Service
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

    /**
     * Retrieves all epitopes.
     *
     * @return a list of all epitopes
     */
    public List<EpitopeTaskData> findAll() {
        return epitopeTaskDataRepository.findAll();
    }

}