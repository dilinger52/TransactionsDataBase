package org.profinef.repository;

import org.profinef.dto.ClientDto;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ClientRepository extends CrudRepository<ClientDto, Integer> {
    @Override
    <S extends ClientDto> S save(S entity);
    @Override
    void deleteById(Integer integer);
    @Override
    Optional<ClientDto> findById(Integer integer);

    ClientDto findByPibIgnoreCase(String name);

    @Override
    Iterable<ClientDto> findAll();

    ClientDto findByPhone(String phone);

    ClientDto findByTelegram(String telegram);
}
