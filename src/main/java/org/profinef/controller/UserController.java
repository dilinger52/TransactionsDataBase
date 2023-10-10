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

@SuppressWarnings("SameReturnValue")
@Controller
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private final UserManager userManager;

    public UserController(UserManager userManager) {
        this.userManager = userManager;
    }

    @GetMapping("/log_in")
    public String getLoginPage() {
        logger.info("Loading log in page");
        return "login";
    }

    @PostMapping("/log_in")
    public String login(HttpSession session, @RequestParam String login, @RequestParam String password) {
        User currentUser = (User) session.getAttribute("user");
        logger.info(currentUser + " Checking user authorisation information...");
        User user;
        try {
            user = userManager.getUser(login);
        } catch (RuntimeException e) {
            session.setAttribute("error", e.getMessage());
            logger.info(currentUser + " Redirecting to error page with error: " + e.getMessage());
            return "error";
        }

        String sha256hex = Hashing.sha256()
                .hashString(password, StandardCharsets.UTF_8)
                .toString();
        if (!sha256hex.equals(user.getPassword())) {
            session.setAttribute("error", "Пароль не верный");
            logger.info(currentUser + " Redirecting to error page with error: Пароль не верный");
            return "error";
        }

        session.setAttribute("user", user);
        logger.info(currentUser + " User accepted");
        return "redirect:/client";
    }

    @PostMapping("/log_out")
    public String logout(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        logger.info(currentUser + " Logging out");
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/user_settings")
    public String getUserSettingsPage() {
        logger.info("Loading user settings page");
        return "userSettings";
    }

    @PostMapping("/change_login")
    public String changeLogin(@RequestParam(name = "login") String login, HttpSession session) {

        User user = (User) session.getAttribute("user");
        logger.info(user + " Changing user login...");
        user = userManager.getUser(user.getId());
        try {
            userManager.getUser(login);
            logger.info(user + " Redirecting to error page with error: Логин занят. Придумайте другой");
            session.setAttribute("error", "Логин занят. Придумайте другой");
            return "error";
        } catch (RuntimeException e) {
            user.setLogin(login);
            userManager.save(user);
        }

        session.setAttribute("user", user);
        logger.info(user + " Login changed");
        return "redirect:/user_settings";
    }

    @PostMapping("/change_password")
    public String changePassword(@RequestParam(name = "old_password") String oldPassword,
                                 @RequestParam(name = "new_password") String newPassword,
                                 @RequestParam(name = "check_password") String checkPassword,
                                 HttpSession session) {

        User user = (User) session.getAttribute("user");
        logger.info(user + " Changing password...");
        user = userManager.getUser(user.getId());
        String oldPassSha = Hashing.sha256()
                .hashString(oldPassword, StandardCharsets.UTF_8)
                .toString();
        if (!Objects.equals(user.getPassword(), oldPassSha)) {
            logger.info(user + " Redirecting to error page with error: Неверный пароль");
            session.setAttribute("error", "Неверный пароль");
            return "error";
        }

        if (!Objects.equals(newPassword, checkPassword)) {
            logger.info(user + " Redirecting to error page with error: Пароли не совпадают");
            session.setAttribute("error", "Пароли не совпадают");
            return "error";
        }

        String newPassSha = Hashing.sha256()
                .hashString(newPassword, StandardCharsets.UTF_8)
                .toString();
        user.setPassword(newPassSha);
        userManager.save(user);
        session.setAttribute("user", user);
        logger.info(user + " Password changed");
        return "redirect:/user_settings";
    }
}
