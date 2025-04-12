package ufsc.br.epibuilder.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ufsc.br.epibuilder.model.Database;
import java.util.Optional;

@Repository
public interface DatabaseRepository extends JpaRepository<Database, String> {
    void deleteById(Long id);

    Database getById(String id);
}
