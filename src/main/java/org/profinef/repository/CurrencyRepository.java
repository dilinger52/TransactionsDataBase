package org.profinef.repository;

import org.profinef.dto.CurrencyDto;
import org.springframework.data.repository.CrudRepository;

public interface CurrencyRepository extends CrudRepository<CurrencyDto, Integer> {
    CurrencyDto findByName(String name);
}
