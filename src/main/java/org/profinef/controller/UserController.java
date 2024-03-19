package org.profinef.controller;

import org.profinef.entity.ERole;
import org.profinef.entity.Role;
import org.profinef.entity.User;
import org.profinef.payload.request.ChangeLoginRequest;
import org.profinef.payload.request.ChangePasswordRequest;
import org.profinef.payload.request.NewUserRequest;
import org.profinef.payload.response.JwtResponse;
import org.profinef.payload.response.MessageResponse;
import org.profinef.repository.RoleRepository;
import org.profinef.repository.UserRepository;
import org.profinef.security.jwt.JwtUtils;
import org.profinef.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Класс отвечает за обработку данных пользователя полученных из базы данных перед передачей их в контроллер
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final AuthenticationManager authenticationManager;
    @Autowired
    private final JwtUtils jwtUtils;
    @Autowired
    private final PasswordEncoder encoder;
    @Autowired
    private final RoleRepository roleRepository;
    @Autowired
    private final UserDetailsService userDetailsService;

    public UserController(UserRepository userRepository, AuthenticationManager authenticationManager, JwtUtils jwtUtils, PasswordEncoder encoder, RoleRepository roleRepository, UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.encoder = encoder;
        this.roleRepository = roleRepository;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Метод осуществляет поиск пользователя в БД по логину
     * @param login логин пользователя, которого нужно найти
     * @return найденного пользователя
     */
    @GetMapping("/login/{login}")
    public User getUser(@PathVariable String login) {
        return userRepository.findByLogin(login).orElseThrow(() -> new RuntimeException("Пользователя с таким логином не существует"));
    }

    /**
     * Метод осуществляет поиск пользователя по ИД
     * @param id ИД пользователя, которого нужно найти
     * @return найденного пользователя
     */
    @GetMapping("/{id}")
    public User getUser(@PathVariable int id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) throw new RuntimeException("Пользователя не существует");
        return user;
    }

    /**
     * Метод осуществляет поиск всех пользователей в БД
     * @return список всех пользователей
     */
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Метод сохраняет пользователя в БД
     * @param user пользователь, которого необходимо сохранить
     */
    @PostMapping
    public void save(@RequestBody User user) {
        userRepository.save(user);
    }

    /**
     * Метод удаляет пользователя из БД по его ИД
     * @param id ИД пользователя, которого необходимо удалить
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public void delete(@PathVariable int id) {
        userRepository.deleteById(id);
    }

    @PostMapping("/login")
    public ResponseEntity<?> changeLogin(@RequestBody ChangeLoginRequest request) {
        User user = userRepository.findById(request.getUser().getId()).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (userRepository.existsByLogin(request.getUsername())) return ResponseEntity
                .badRequest()
                .body(new MessageResponse("Ошибка: Логин занят!"));
        user.setLogin(request.getUsername());
        user = userRepository.save(user);


        UserDetails userDetails2 = userDetailsService.loadUserByUsername(request.getUsername());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails2,
                        null,
                        userDetails2.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        AuthController.updateLoggedCounter((UserDetailsImpl) userDetails2);

        return ResponseEntity.ok(new JwtResponse(jwt,
                user.getId(),
                user.getLogin(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList())));
    }

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        User user = userRepository.findById(request.getUser().getId()).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        if (Objects.equals(user.getPassword(), encoder.encode(request.getOldPassword()))) return ResponseEntity
                .badRequest()
                .body(new MessageResponse("Ошибка: Неверный пароль!"));
        user.setPassword(encoder.encode(request.getNewPassword()));
        user = userRepository.save(user);

        String username = jwtUtils.getUserNameFromJwtToken(request.getUser().getAccessToken());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.getNewPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);



        return ResponseEntity.ok(new JwtResponse(jwt,
                user.getId(),
                user.getLogin(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList())));
    }

    @PostMapping("/restore/{id}")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public User restorePassword(@PathVariable int id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.setPassword(encoder.encode("11111"));
        user = userRepository.save(user);
        return user;
    }

    @PostMapping("/{id}/role/{roleId}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public User changeRole(@PathVariable int id, @PathVariable int roleId) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Роль не найдена")));
        user.setRoles(roles);
        user = userRepository.save(user);
        return user;
    }

    @PostMapping("/new")
    @PreAuthorize("hasRole('SUPERADMIN') or hasRole('ADMIN')")
    public ResponseEntity<?> newUser(@RequestBody NewUserRequest request) {
        if (userRepository.existsByLogin(request.getUsername())) return ResponseEntity.badRequest()
                .body(new MessageResponse("Ошибка: Логин занят!"));
        User user = new User();
        user.setLogin(request.getUsername());
        user.setPassword(encoder.encode("11111"));
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName(ERole.valueOf(request.getRole())).orElseThrow(() -> new RuntimeException("Роль не найдена")));
        user.setRoles(roles);
        return ResponseEntity.ok(userRepository.save(user));

    }
}
