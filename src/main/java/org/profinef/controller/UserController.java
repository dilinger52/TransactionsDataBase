package org.profinef.controller;

import com.google.common.hash.Hashing;
import jakarta.servlet.http.HttpSession;
import org.profinef.entity.User;
import org.profinef.service.UserManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.slf4j.Logger;

/**
 * Класс предназначен для обработки HTML запросов, связанных с действиями с аккаунтами пользователей
 */
@SuppressWarnings("SameReturnValue")
@Controller
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private final UserManager userManager;

    public UserController(UserManager userManager) {
        this.userManager = userManager;
    }

    /**
     * Метод обрабатывает HTML запросы, связанные с отображением странички авторизации
     * @return
     */
    @GetMapping("/log_in")
    public String getLoginPage() {
        logger.info("Loading log in page");
        return "login";
    }

    /**
     * Метод обрабатывает HTML запросы, связанные с авторизацией пользователей
     * @param session
     * @param login введенный логин пользователя
     * @param password введенный пароль пользователя
     * @return
     */
    @PostMapping("/log_in")
    public String login(HttpSession session, @RequestParam String login, @RequestParam String password) {
        User currentUser = (User) session.getAttribute("user");
        logger.info(currentUser + " Checking user authorisation information...");
        User user;
        // ищем в базе пользователя с таким логином, иначе переводим на страницу ошибки
        try {
            user = userManager.getUser(login);
        } catch (RuntimeException e) {
            session.setAttribute("error", e.getMessage());
            logger.info(currentUser + " Redirecting to error page with error: " + e.getMessage());
            return "error";
        }

        //хэшируем введенный пароль и сравниваем с хэшем пароля в базе. если они не совпадают переводим на страницу ошибки
        String sha256hex = Hashing.sha256()
                .hashString(password, StandardCharsets.UTF_8)
                .toString();
        if (!sha256hex.equals(user.getPassword())) {
            session.setAttribute("error", "Пароль не верный");
            logger.info(currentUser + " Redirecting to error page with error: Пароль не верный");
            return "error";
        }
        // добавляем данные пользователя в сессию и переводим клиента на главную
        session.setAttribute("user", user);
        logger.info(currentUser + " User accepted");
        return "redirect:/client";
    }

    /**
     * Метод обрабатывает HTML запросы, связанные с выходом пользователя из аккаунта. Удаляет данные текущего
     * пользователя из сессии и уничтожает сессию
     * @param session
     * @return
     */
    @PostMapping("/log_out")
    public String logout(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        logger.info(currentUser + " Logging out");
        session.invalidate();
        return "redirect:/";
    }

    /**
     * Метод обрабатывает HTML запросы, связанные с отображением страницы настроек аккаунта пользователя
     * @return
     */
    @GetMapping("/user_settings")
    public String getUserSettingsPage() {
        logger.info("Loading user settings page");
        return "userSettings";
    }

    /**
     * Метод обрабатывает HTML запросы, связанные со сменой логина пользователя
     * @param login новый логин пользователя
     * @param session
     * @return
     */
    @PostMapping("/change_login")
    public String changeLogin(@RequestParam(name = "login") String login, HttpSession session) {

        User user = (User) session.getAttribute("user");
        logger.info(user + " Changing user login...");
        // проверяем наличие пользователя в базе
        user = userManager.getUser(user.getId());
        try {
            // проверяем наличие пользователя с таким же логином. если существует перенаправляем на страницу ошибки
            userManager.getUser(login);
            logger.info(user + " Redirecting to error page with error: Логин занят. Придумайте другой");
            session.setAttribute("error", "Логин занят. Придумайте другой");
            return "error";
        } catch (RuntimeException e) {
            // иначе меняем логин пользователя и сохраняем в базу
            user.setLogin(login);
            userManager.save(user);
        }
        // обновляем данные пользователя в сессии
        session.setAttribute("user", user);
        logger.info(user + " Login changed");
        return "redirect:/user_settings";
    }

    /**
     * Метод обрабатывает HTML запросы, связанные со сменой пароля пользователя
     * @param oldPassword старый пароль, для улучшения безопасности аккаунта
     * @param newPassword новый пароль
     * @param checkPassword повторно новый пароль, обеспечит отсутствие опечаток в новом пароле
     * @param session
     * @return
     */
    @PostMapping("/change_password")
    public String changePassword(@RequestParam(name = "old_password") String oldPassword,
                                 @RequestParam(name = "new_password") String newPassword,
                                 @RequestParam(name = "check_password") String checkPassword,
                                 HttpSession session) {

        User user = (User) session.getAttribute("user");
        logger.info(user + " Changing password...");
        // проверка наличия пользователя в базе
        user = userManager.getUser(user.getId());
        // проверка соответвия хэша введенного старого пароля с хэшем пароля записанного в базе
        String oldPassSha = Hashing.sha256()
                .hashString(oldPassword, StandardCharsets.UTF_8)
                .toString();
        if (!Objects.equals(user.getPassword(), oldPassSha)) {
            logger.info(user + " Redirecting to error page with error: Неверный пароль");
            session.setAttribute("error", "Неверный пароль");
            return "error";
        }
        // проверка совпадения нового пароля и проверочного пароля
        if (!Objects.equals(newPassword, checkPassword)) {
            logger.info(user + " Redirecting to error page with error: Пароли не совпадают");
            session.setAttribute("error", "Пароли не совпадают");
            return "error";
        }
        // хэшируем новый пароль и записываем в базу
        String newPassSha = Hashing.sha256()
                .hashString(newPassword, StandardCharsets.UTF_8)
                .toString();
        user.setPassword(newPassSha);
        userManager.save(user);
        // обновляем данные пользователя в сессии
        session.setAttribute("user", user);
        logger.info(user + " Password changed");
        return "redirect:/user_settings";
    }
}
