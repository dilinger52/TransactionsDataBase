package org.profinef.repository;

import org.profinef.dto.UserDto;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserDto, Integer> {
    UserDto findByLogin(String login);
}
