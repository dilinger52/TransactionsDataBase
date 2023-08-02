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

    //@Query(nativeQuery = true, value = "SELECT * FROM transaction WHERE client_id=?1 AND currency_id in ?2 AND date BETWEEN ?3 AND ?4")
    List<TransactionDto> findAllByClientIdAndCurrencyIdInAndDateBetweenOrderByCurrencyIdAscDateAsc
    (int clientId, List<Integer> currencyIds, Timestamp dateStart, Timestamp dateEnd);


    TransactionDto findByIdAndClientIdOrderByDate(int id, int clientId);

    List<TransactionDto> findAllByOrderByDateDesc();

    TransactionDto findByIdAndClientIdAndCurrencyIdOrderByDate(int id, int clientId, int currencyId);

    List<TransactionDto> findAllByClientIdAndCurrencyIdOrderByCurrencyIdAscDateAsc(Integer id, Integer id1);
    @Query(nativeQuery = true, value = "SELECT comment FROM transaction")
    List<String> findAllComments();
    @Query(nativeQuery = true, value = "SELECT id FROM transaction ORDER BY id DESC LIMIT 0, 1")
    Integer getMaxId();
    @Query(nativeQuery = true, value = "SELECT * FROM transaction WHERE client_id=?1 AND currency_id=?2 AND date < ?3  ORDER BY id DESC LIMIT 0, 1")
    TransactionDto findAllByClientIdAndCurrencyIdAndDateBetweenLimit1(int clientId, int currencyId, Timestamp startDate);

    void deleteByDateBetween(Timestamp startDate, Timestamp endDate);

    List<TransactionDto> findAllByUserIdAndClientIdAndCurrencyIdInAndDateBetween(Integer managerId, Integer id, List<Integer> currencyId, Timestamp startDate, Timestamp endDate);
}
