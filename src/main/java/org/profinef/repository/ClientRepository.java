package org.profinef.repository;

import org.profinef.dto.ClientDto;
import org.springframework.data.repository.CrudRepository;

import java.sql.Date;
import java.util.Optional;

public interface ClientRepository extends CrudRepository<ClientDto, Integer> {
    @Override
    <S extends ClientDto> S save(S entity);
    @Override
    void deleteById(Integer integer);

    Optional<ClientDto> findByIdOrderByPib(Integer integer);

    ClientDto findByPibIgnoreCaseOrderByPib(String name);


    ClientDto findByPhoneOrderByPib(String phone);

    ClientDto findByTelegramOrderByPib(String telegram);

}
