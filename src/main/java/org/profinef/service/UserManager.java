package org.profinef.service;

import org.profinef.dto.UserDto;
import org.profinef.entity.Role;
import org.profinef.entity.User;
import org.profinef.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserManager {

    @Autowired
    private final UserRepository userRepository;

    public UserManager(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(String login) {
        UserDto userDto = userRepository.findByLogin(login);
        System.out.println(userDto);
        if (userDto == null) throw new RuntimeException("Пользователя с таким логином не существует");
        return formatFromDto(userDto);
    }

    public User getUser(int id) {
        UserDto userDto = userRepository.findById(id).orElse(null);
        if (userDto == null) throw new RuntimeException("Пользователя не существует");
        return formatFromDto(userDto);
    }

    private User formatFromDto(UserDto userDto) {
        User user = new User();
        user.setId(userDto.getId());
        user.setLogin(userDto.getLogin());
        user.setPassword(userDto.getPassword());
        user.setRole(Role.values()[userDto.getRoleId()]);
        return user;
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        for (UserDto userDto : userRepository.findAll()) {
            users.add(formatFromDto(userDto));
        }
        return users;
    }

    public void save(User user) {
        userRepository.save(formatToDto(user));
    }

    private UserDto formatToDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setLogin(user.getLogin());
        userDto.setPassword(user.getPassword());
        userDto.setRoleId(user.getRole().ordinal());
        return userDto;
    }

    public void delete(int id) {
        userRepository.deleteById(id);
    }
}
