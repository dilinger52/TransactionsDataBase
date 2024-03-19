package org.profinef.repository;

import org.profinef.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CurrencyRepository extends JpaRepository<Currency, Integer> {
    Currency findByName(String name);
}
