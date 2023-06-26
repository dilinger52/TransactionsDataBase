package org.profinef.repository;

import org.profinef.dto.TransactionDto;
import org.springframework.data.repository.CrudRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends CrudRepository<TransactionDto, Integer> {
    @Override
    <S extends TransactionDto> S save(S entity);

    List<TransactionDto> findAllById(Integer integer);

    @Override
    void deleteById(Integer integer);

    List<TransactionDto> findAllByClientIdAndCurrencyIdAndDateAfter(int clientId, int currencyId, Timestamp date);

    TransactionDto findByIdAndClientId(int id, int clientId);
}
