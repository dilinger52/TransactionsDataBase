package org.profinef.repository;

import org.profinef.dto.ClientDto;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends CrudRepository<ClientDto, Integer> {
    @Override
    <S extends ClientDto> S save(S entity);
    @Override
    void deleteById(Integer integer);

    Optional<ClientDto> findByIdOrderByPib(Integer integer);

    List<ClientDto> findAllByPibContainsIgnoreCaseOrderByPib(String name);


    List<ClientDto> findAllByPhoneContainsOrderByPib(String phone);

    List<ClientDto> findAllByTelegramContainsOrderByPib(String telegram);

}
