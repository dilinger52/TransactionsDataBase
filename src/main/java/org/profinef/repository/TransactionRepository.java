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

    @Query(nativeQuery = true, value = "SELECT * FROM transaction WHERE client_id=?1 AND currency_id in ?2 AND date>?3 AND date<?4 ORDER BY date ASC")
    List<TransactionDto> findAllByClientIdAndCurrencyIdInAndDateBetweenOrderByDateAscIdAsc
    (int clientId, List<Integer> currencyIds, Timestamp dateStart, Timestamp dateEnd);


    TransactionDto findByIdAndClientIdOrderByDate(int id, int clientId);

    List<TransactionDto> findAllByOrderByDateDesc();
    @Query(nativeQuery = true, value = "SELECT * FROM transactions.transaction WHERE id=?1 AND client_id=?2 AND currency_id=?3")
    TransactionDto findByIdAndClientIdAndCurrencyIdOrderByDate(int id, int clientId, int currencyId);

    List<TransactionDto> findAllByClientIdAndCurrencyIdOrderByCurrencyIdAscDateAscIdAsc(Integer id, Integer id1);
    @Query(nativeQuery = true, value = "SELECT comment FROM transaction")
    List<String> findAllComments();
    @Query(nativeQuery = true, value = "SELECT id FROM transaction ORDER BY id DESC LIMIT 0, 1")
    Integer getMaxId();
    @Query(nativeQuery = true, value = "SELECT * FROM transaction WHERE client_id=?1 AND currency_id=?2 AND id!=?4 AND date < ?3  ORDER BY date DESC LIMIT 0, 1")
    TransactionDto findAllByClientIdAndCurrencyIdAndDateBetweenLimit1(int clientId, int currencyId, Timestamp startDate, int id);

    void deleteByDateBetween(Timestamp startDate, Timestamp endDate);

    List<TransactionDto> findAllByUserIdAndClientIdAndCurrencyIdInAndDateBetweenOrderByDateAsc(Integer managerId, Integer id, List<Integer> currencyId, Timestamp startDate, Timestamp endDate);

    List<TransactionDto> findAllByUserIdAndCurrencyIdInAndDateBetweenOrderByDateAsc(Integer managerId, List<Integer> currencyId, Timestamp startDate, Timestamp endDate);

    List<TransactionDto> findAllById(Integer id);
    @Query(nativeQuery = true, value = "SELECT date FROM transactions.transaction WHERE date>=?1 AND date<?2 ORDER BY date DESC LIMIT 0, 1")
    Timestamp getMaxDateForDay(Timestamp date, Timestamp nextDate);
}
