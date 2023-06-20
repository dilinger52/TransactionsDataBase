package org.profinef.repository;

import org.profinef.dbo.ClientsCurrencyDbo;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ClientsCurrencyRepository extends CrudRepository<ClientsCurrencyDbo, Integer> {
    @Override
    <S extends ClientsCurrencyDbo> S save(S entity);
    List<ClientsCurrencyDbo> findByClientId(int clientId);
    ClientsCurrencyDbo findByClientIdAndCurrencyId(int clientId, int currencyId);
    @Override
    void deleteById(Integer integer);
}
