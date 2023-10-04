package org.profinef.controller;

import com.google.common.hash.Hashing;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.profinef.config.HttpSessionConfig;
import org.profinef.dto.TransactionDto;
import org.profinef.entity.*;
import org.profinef.entity.Currency;
import org.profinef.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

@Controller
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    @Autowired
    private final UserManager userManager;
    @Autowired
    private final CurrencyManager currencyManager;
    @Autowired
    private final TransManager transManager;
    @Autowired
    private final Scheduler scheduler;
    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final AccountManager accountManager;

    public AdminController(UserManager userManager, CurrencyManager currencyManager, TransManager transManager, Scheduler scheduler, ClientManager clientManager, AccountManager accountManager) {
        this.userManager = userManager;
        this.currencyManager = currencyManager;
        this.transManager = transManager;
        this.scheduler = scheduler;
        this.clientManager = clientManager;
        this.accountManager = accountManager;
    }

    @GetMapping("/users")
    public String getUsersPage(HttpSession session) {
        logger.info("Loading users page...");
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        List<User> users = userManager.getAllUsers();
        Map<User, Integer> active = new HashMap<>();
        for (User user : users) {
            active.put(user, 0);
        }
        List<HttpSession> sessions = new HttpSessionConfig().getActiveSessions();
        for (HttpSession ses : sessions) {
            if (ses != null) {
                User user = (User) ses.getAttribute("user");
                if (user != null) {
                    active.put(user, active.get(user) + 1);
                }
            }
        }
        session.setAttribute("active", active);
        session.setAttribute("users", users);
        logger.info("Users page loaded");
        return "users";
    }

    @PostMapping("/create_user")
    public String createUser(@RequestParam(name = "login") String login, HttpSession session) {
        logger.info("Creating new user...");
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        try {
            userManager.getUser(login);
            logger.info("Redirecting to error page with error: Логин занят. Придумайте другой");
            session.setAttribute("error", "Логин занят. Придумайте другой");
            return "error";
        } catch (RuntimeException e) {
            User user = new User();
            user.setLogin(login);
            user.setPassword("d17f25ecfbcc7857f7bebea469308be0b2580943e96d13a3ad98a13675c4bfc2");
            user.setRole(Role.Manager);
            userManager.save(user);
        }
        logger.info("User created");
        return "redirect:/users";
    }

    @PostMapping("/restore_pass")
    public String restorePass(@RequestParam(name = "id") int id, HttpSession session) {
        logger.info("Restoring password...");
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        User user = userManager.getUser(id);

        String sha256hex = Hashing.sha256()
                .hashString("11111", StandardCharsets.UTF_8)
                .toString();

        user.setPassword(sha256hex);

        userManager.save(user);
        logger.info("Password restored");
        return "redirect:/users";
    }

    @PostMapping("/delete_user")
    public String deleteUser(@RequestParam(name = "id") int id, HttpSession session) {
        logger.info("Deleting user");
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        User user = userManager.getUser(id);
        if (user.getRole() == Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        userManager.delete(id);
        logger.info("User deleted");
        return "redirect:/users";
    }



    @GetMapping("/manager_history")
    public String managerHistory(@RequestParam(name = "manager_id", required = false) Integer managerId,
                                 @RequestParam(name = "strDate", required = false) Date strDate,
                                 @RequestParam(name = "edDate", required = false) Date edDate,
                                 @RequestParam(name = "client_name", required = false) String clientName,
                                 @RequestParam(name = "currencyId", required = false) List<Integer> currencyId,
                                 HttpSession session) {
        logger.info("Loading manger history page...");
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        if (managerId == null) {
            managerId = ((User) session.getAttribute("manager")).getId();
        }
        logger.trace("managerId: " + managerId);
        if (strDate == null) strDate = Date.valueOf(LocalDate.now());
        logger.trace("strDate: " + strDate);
        if (edDate == null) edDate = new Date(strDate.getTime() + 86400000);
        logger.trace("endDate: " + edDate);
        List<Currency> currencies = currencyManager.getAllCurrencies();
        if (currencyId == null) {
            logger.debug("currencyId = null");
            currencyId = new ArrayList<>();

            for (Currency currency : currencies) {
                currencyId.add(currency.getId());
            }
            logger.trace("currencyId = " + currencyId);
        }
        List<String> currencyName = new ArrayList<>();
        for (Integer curId : currencyId) {
            currencyName.add(currencyManager.getCurrency(curId).getName());
        }
        List<Transaction> transactions = transManager.findByUser(managerId, new Timestamp(strDate.getTime()), new Timestamp(edDate.getTime() - 1), clientName, currencyId);
        User manager = userManager.getUser(managerId);
        session.setAttribute("strDate", new Timestamp(strDate.getTime()));
        session.setAttribute("edDate", new Timestamp(edDate.getTime()));
        session.setAttribute("manager", manager);
        session.setAttribute("transactions", transactions);
        session.setAttribute("currencies", currencies);
        session.setAttribute("currency_name", currencyName);
        logger.info("Manager history page loaded");
        return "managerHistory";
    }

    @GetMapping("/database")
    public String makeBackUp(HttpSession session, HttpServletResponse response) throws Exception {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        scheduler.makeBackUp();
        return "redirect:/users";
    }

    @PostMapping("/database")
    public String restoreBackUp(HttpSession session, HttpServletResponse response) throws Exception {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser.getRole() != Role.Admin) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        scheduler.restoreFromBackUp();
        return "redirect:/users";
    }


}