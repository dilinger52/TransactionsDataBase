package org.profinef.repository;

import org.profinef.dbo.CurrencyDbo;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CurrencyRepository extends CrudRepository<CurrencyDbo, Integer> {
    @Override
    <S extends CurrencyDbo> S save(S entity);

    @Override
    Optional<CurrencyDbo> findById(Integer integer);

    @Override
    Iterable<CurrencyDbo> findAll();

    @Override
    void deleteById(Integer integer);
}
