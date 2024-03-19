package org.profinef.repository;

import org.profinef.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    @Override
    <S extends Transaction> S save(S entity);

    @Override
    void deleteById(Integer integer);

    @Query(nativeQuery = true, value = "SELECT * FROM transaction WHERE account_id in ?1 AND date>?2 AND date<?3 ORDER BY date ASC")
    List<Transaction> findAllByAccountIdInAndDateBetweenOrderByDateAscIdAsc
    (List<Integer> accountIds, Timestamp dateStart, Timestamp dateEnd);

    List<Transaction> findAllByOrderByDateAsc();
    Optional<Transaction> findTopByAccountIdOrderByDateDesc(int id);

    @Query(nativeQuery = true, value = "SELECT comment FROM transaction")
    List<String> findAllComments();
    @Query(nativeQuery = true, value = "SELECT operation_id FROM transaction ORDER BY operation_id DESC LIMIT 0, 1")
    Integer getMaxId();

    void deleteByDateBetween(Timestamp startDate, Timestamp endDate);

    List<Transaction> findAllByOperationId(Integer id);
    @Query(nativeQuery = true, value = "SELECT date FROM transactions.transaction WHERE date>=?1 AND date<?2 ORDER BY date DESC LIMIT 0, 1")
    Timestamp getMaxDateForDay(Timestamp date, Timestamp nextDate);

    List<Transaction> findAllByOperationIdOrderByDate(int operationId);

    Transaction findByAccountIdAndOperationIdOrderByDate(int id, int operationId);

    List<Transaction> findAllByAccountIdOrderByAccountCurrencyIdAscDateAscIdAsc(int id);
    @Query(nativeQuery = true, value = "SELECT * FROM transactions.transaction WHERE account_id=?1 AND date<?2 ORDER BY date DESC LIMIT 0, 1")
    Transaction findAllByAccountIdAndDateBetweenLimit1(int id, Timestamp afterDate);
    @Query(nativeQuery = true, value = "SELECT * FROM transactions.transaction WHERE user_id=?1 AND account_id in ?2 AND date>=?3 AND date<?4 ORDER BY date ASC")
    List<Transaction> findAllByUserIdAndAccountIdInAndDateBetweenOrderByDateAsc(Integer managerId, List<Integer> accountIds, Timestamp startDate, Timestamp endDate);

    List<Transaction> findAllByAccountIdAndDateBetweenOrderByDateAscIdAsc(int id, Timestamp timestamp, Timestamp endDate);

    List<Transaction> findAllByAccountClientIdAndDateBetweenOrderByDateAsc(int id, Timestamp startDate, Timestamp endDate);
}
