package org.profinef.service;

import org.profinef.dto.UserDto;
import org.profinef.entity.Role;
import org.profinef.entity.User;
import org.profinef.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс отвечает за обработку данных пользователя полученных из базы данных перед передачей их в контроллер
 */
@Service
public class UserManager {

    @Autowired
    private final UserRepository userRepository;

    public UserManager(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Метод осуществляет поиск пользователя в БД по логину
     * @param login логин пользователя, которого нужно найти
     * @return найденного пользователя
     */
    public User getUser(String login) {
        UserDto userDto = userRepository.findByLogin(login);
        if (userDto == null) throw new RuntimeException("Пользователя с таким логином не существует");
        return formatFromDto(userDto);
    }

    /**
     * Метод осуществляет поиск пользователя по ИД
     * @param id ИД пользователя, которого нужно найти
     * @return найденного пользователя
     */
    public User getUser(int id) {
        UserDto userDto = userRepository.findById(id).orElse(null);
        if (userDto == null) throw new RuntimeException("Пользователя не существует");
        return formatFromDto(userDto);
    }

    /**
     * Метод преобразует объект пользователя с ИД связанных объектов в объект пользователя со ссылками на них
     * @param userDto объект для преобразования
     * @return объект после преобразования
     */
    private User formatFromDto(UserDto userDto) {
        User user = new User();
        user.setId(userDto.getId());
        user.setLogin(userDto.getLogin());
        user.setPassword(userDto.getPassword());
        user.setRole(Role.values()[userDto.getRoleId()]);
        return user;
    }

    /**
     * Метод осуществляет поиск всех пользователей в БД
     * @return список всех пользователей
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        for (UserDto userDto : userRepository.findAll()) {
            users.add(formatFromDto(userDto));
        }
        return users;
    }

    /**
     * Метод сохраняет пользователя в БД
     * @param user пользователь, которого необходимо сохранить
     */
    public void save(User user) {
        userRepository.save(formatToDto(user));
    }

    /**
     * Метод преобразует объект пользователя содержащего ссылки на связанные объекты в объект содержащий их ИД
     * @param user объект для преобразования
     * @return объект после преобразования
     */
    private UserDto formatToDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setLogin(user.getLogin());
        userDto.setPassword(user.getPassword());
        userDto.setRoleId(user.getRole().ordinal());
        return userDto;
    }

    /**
     * Метод удаляет пользователя из БД по его ИД
     * @param id ИД пользователя, которого необходимо удалить
     */
    public void delete(int id) {
        userRepository.deleteById(id);
    }
}
