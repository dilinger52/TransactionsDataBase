package org.profinef.repository;

import org.profinef.dbo.TransactionDbo;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TransactionRepository extends CrudRepository<TransactionDbo, Integer> {
    @Override
    <S extends TransactionDbo> S save(S entity);

    @Override
    Optional<TransactionDbo> findById(Integer integer);

    @Override
    void deleteById(Integer integer);
}
