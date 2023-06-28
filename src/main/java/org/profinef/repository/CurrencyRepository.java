package org.profinef.repository;

import org.profinef.dto.CurrencyDto;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CurrencyRepository extends CrudRepository<CurrencyDto, Integer> {
    @Override
    <S extends CurrencyDto> S save(S entity);
    List<CurrencyDto> findByClientId(int clientId);
    CurrencyDto findByClientIdAndCurrencyId(int clientId, int currencyId);
    @Override
    void deleteById(Integer integer);

     CurrencyDto findFirstByCurrencyId(int currencyId);
}
