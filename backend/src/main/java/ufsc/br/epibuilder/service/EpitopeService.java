package ufsc.br.epibuilder.service;

import org.springframework.stereotype.Service;
import ufsc.br.epibuilder.repository.EpitopeRepository;
import ufsc.br.epibuilder.model.Epitope;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for handling operations related to Epitope.
 */
@Service
public class EpitopeService {
    
    private final EpitopeRepository epitopeRepository;

    public EpitopeService(EpitopeRepository epitopeRepository) {
        this.epitopeRepository = epitopeRepository;
    }

    /**
     * Saves an epitope.
     *
     * @param epitope the epitope to save
     * @return the saved epitope
     */
    public Epitope save(Epitope epitope) {
        return epitopeRepository.save(epitope);
    }

    public Epitope update(Epitope epitope) {
        return epitopeRepository.save(epitope);
    }

    /** 
     * Retrieves an epitope by its ID.
     *
     * @param id the ID of the epitope to retrieve
     * @return an Optional containing the found epitope, or empty if not found
     */
    public Optional<Epitope> findById(String id) {
        return epitopeRepository.findById(id);  
    }

    /**
     * Retrieves all epitopes.
     *
     * @return a list of all epitopes
     */
    public List<Epitope> findAll() {
        return epitopeRepository.findAll();
    }
}
