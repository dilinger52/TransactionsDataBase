package org.profinef.repository;

import org.profinef.dbo.ClientDbo;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ClientRepository extends CrudRepository<ClientDbo, Integer> {
    @Override
    <S extends ClientDbo> S save(S entity);
    @Override
    void deleteById(Integer integer);
    @Override
    Optional<ClientDbo> findById(Integer integer);

    @Override
    Iterable<ClientDbo> findAll();
}
