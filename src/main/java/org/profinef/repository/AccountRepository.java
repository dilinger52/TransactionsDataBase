package org.profinef.repository;

import org.profinef.dto.AccountDto;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AccountRepository extends CrudRepository<AccountDto, Integer> {
    @Override
    <S extends AccountDto> S save(S entity);
    List<AccountDto> findByClientId(int clientId);
    AccountDto findByClientIdAndCurrencyId(int clientId, int currencyId);
    @Override
    void deleteById(Integer integer);

     AccountDto findFirstByCurrencyId(int currencyId);
}
