package org.profinef.repository;

import org.profinef.dto.TransactionDto;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.sql.Timestamp;
import java.util.List;

public interface TransactionRepository extends CrudRepository<TransactionDto, Integer> {
    @Override
    <S extends TransactionDto> S save(S entity);

    List<TransactionDto> findAllByIdOrderByDate(Integer integer);

    @Override
    void deleteById(Integer integer);

    @Query(nativeQuery = true, value = "SELECT * FROM transaction WHERE client_id=?1 AND currency_id in ?2 AND date BETWEEN ?3 AND ?4")
    List<TransactionDto> findAllByClientIdAndCurrencyIdsAndDateBetweenOrderByDate(int clientId, List<Integer> currencyIds, Timestamp dateStart, Timestamp dateEnd);


    TransactionDto findByIdAndClientIdOrderByDate(int id, int clientId);

    List<TransactionDto> findAllByOrderByDateDesc();
}
