package org.profinef.repository;

import org.profinef.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    @Override
    <S extends Account> S save(S entity);
    List<Account> findByClientId(int clientId);
    Account findByClientIdAndCurrencyId(int clientId, int currencyId);
    @Override
    void deleteById(Integer integer);


    List<Account> findAllByClientIdIn(List<Integer> clientIds);
    List<Account> findAllByOrderByClientPib();

    List<Account> findAllByCurrencyIdIn(List<Integer> currencyId);

    List<Account> findAllByClientIdAndCurrencyIdIn(Integer id, List<Integer> currencyId);
}
