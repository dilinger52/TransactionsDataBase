package org.profinef.repository;

import org.profinef.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByLogin(String login);

    Boolean existsByLogin(String login);

}
