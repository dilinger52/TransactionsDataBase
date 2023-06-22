package org.profinef.repository;

import org.profinef.dto.TransactionDto;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TransactionRepository extends CrudRepository<TransactionDto, Integer> {
    @Override
    <S extends TransactionDto> S save(S entity);

    @Override
    Optional<TransactionDto> findById(Integer integer);

    @Override
    void deleteById(Integer integer);
}
