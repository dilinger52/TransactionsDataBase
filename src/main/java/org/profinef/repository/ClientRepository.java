package org.profinef.repository;

import org.profinef.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Integer> {
    @Override
    <S extends Client> S save(S entity);
    @Override
    void deleteById(Integer integer);

    Optional<Client> findByIdOrderByPib(Integer integer);
    Client findByPibIgnoreCaseOrderByPib(String name);

    List<Client> findAllByPibContainsIgnoreCaseOrderByPib(String name);


    List<Client> findAllByPhoneContainsOrderByPib(String phone);

    List<Client> findAllByTelegramContainsOrderByPib(String telegram);

    List<Client> findAllByOrderByPib();
}
