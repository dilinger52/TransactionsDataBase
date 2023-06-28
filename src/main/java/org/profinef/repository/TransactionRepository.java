package org.profinef.repository;

import org.profinef.dto.TransactionDto;
import org.springframework.data.repository.CrudRepository;

import java.sql.Timestamp;
import java.util.List;

public interface TransactionRepository extends CrudRepository<TransactionDto, Integer> {
    @Override
    <S extends TransactionDto> S save(S entity);

    List<TransactionDto> findAllById(Integer integer);

    @Override
    void deleteById(Integer integer);

    List<TransactionDto> findAllByClientIdAndCurrencyIdAndDateBetween(int clientId, int currencyId, Timestamp dateStart, Timestamp dateEnd);


    TransactionDto findByIdAndClientId(int id, int clientId);
}
