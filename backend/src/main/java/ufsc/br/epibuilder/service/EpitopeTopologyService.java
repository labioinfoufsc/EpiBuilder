package ufsc.br.epibuilder.service;

import org.springframework.stereotype.Service;
import ufsc.br.epibuilder.repository.EpitopeTopologyRepository;
import ufsc.br.epibuilder.model.EpitopeTopology;
import java.util.List;
import java.util.Optional;


/**
 * Service layer for handling operations related to EpitopeTopology.
 */
@Service
public class EpitopeTopologyService {

    private final EpitopeTopologyRepository epitopeTopologyRepository;

    public EpitopeTopologyService(EpitopeTopologyRepository epitopeTopologyRepository) {
        this.epitopeTopologyRepository = epitopeTopologyRepository;
    }

    /**
     * Saves an epitope topology.
     *
     * @param epitopeTopology the epitope topology to save
     * @return the saved epitope topology
     */
    public EpitopeTopology save(EpitopeTopology epitopeTopology) {
        return epitopeTopologyRepository.save(epitopeTopology);
    }

    public EpitopeTopology update(EpitopeTopology epitopeTopology) {
        return epitopeTopologyRepository.save(epitopeTopology);
    }

    /**
     * Retrieves all epitope topologies.
     *
     * @return a list of all epitope topologies
     */
    public List<EpitopeTopology> findAll() {
        return epitopeTopologyRepository.findAll();
    }
}


