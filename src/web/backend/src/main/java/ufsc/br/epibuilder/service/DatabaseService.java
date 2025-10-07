package ufsc.br.epibuilder.service;

import ufsc.br.epibuilder.repository.DatabaseRepository;
import ufsc.br.epibuilder.model.Database;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DatabaseService {

    private final DatabaseRepository dbRepository;

    public Database save(Database database) {
        return dbRepository.save(database);
    }

    public List<Database> getAll() {
        return dbRepository.findAll();
    }

    public Database getById(Long id) {
        return dbRepository.getById(id);
    }

    public Database getByAlias(String alias) {
        return dbRepository.getByAlias(alias);
    }

    @Transactional
    public void deleteById(Long id) {
        dbRepository.deleteById(id);
    }

}
