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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс для обработки HTML запросов, связанных с действиями админа и суперадмина
 */
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

    /**
     * Метод для отрисовки страницы админпанели
     * @param session
     * @return
     */
    @GetMapping("/users")
    public String getUsersPage(HttpSession session) {
        //получение авторизованного пользователя из сесии
        User currentUser = (User) session.getAttribute("user");
        logger.info(currentUser + " Loading users page...");

        //проверка прав пользователя для доступа к данному контенту, в противном случае перенаправляет на страницу
        // ошибки с соответсвующим сообщением
        if (currentUser.getRole() != Role.Admin && currentUser.getRole() != Role.Superadmin) {
            logger.info(currentUser + " Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        //счетчик активных сессий для каждого из аккаунтов
        //из базы данных подтягиваем всех зарегистрированных пользователей
        List<User> users = userManager.getAllUsers();
        //для каждого пользователя устанавливаем счетчик на 0
        Map<User, Integer> active = new HashMap<>();
        for (User user : users) {
            active.put(user, 0);
        }
        //получаем список активных сессий
        List<HttpSession> sessions = new HttpSessionConfig().getActiveSessions();
        //для каждой сессии
        for (HttpSession ses : sessions) {
            if (ses != null) {
                //извлекаем авторизованного пользователя
                User user = (User) ses.getAttribute("user");
                if (user != null) {
                    //и увеличиваем значение соответсвующего счетчика на 1
                    active.put(user, active.get(user) + 1);
                }
            }
        }

        //получаем список имеющихся бекапов для отката
        //задаем путь к директории с бекапами
        File directory = new File("backup\\");
        //выбираем все файлы из директории
        List<File> files = new ArrayList<>(Arrays.stream(directory.listFiles(File::isFile))
                //сортируем их по дате начиная с новых
                .sorted((o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified())).toList());
        //оставляем 30 первых и берем только их названия
        List<String> fs = files.stream().limit(30).map(File::getName).toList();

        //складываем всю инфу в сессию
        session.setAttribute("files", fs);
        session.setAttribute("active", active);
        session.setAttribute("users", users);
        logger.info(currentUser + " Users page loaded");
        //запускаем отрисовку страницы шаблонизатором
        return "users";
    }

    /**
     * Метод создания нового пользователя
     * @param login логин нового пользователя
     * @param role роль нового пользователя (менеджер или админ)
     * @param session
     * @return
     */
    @PostMapping("/create_user")
    public String createUser(@RequestParam(name = "login") String login,
                             @RequestParam(name = "role") String role,
                             HttpSession session) {
        //достаем авторизованного пользователя из сессии
        User currentUser = (User) session.getAttribute("user");
        logger.info(currentUser + " Creating new user...");

        //проверка прав пользователя для доступа к данному контенту, в противном случае перенаправляет на страницу
        // ошибки с соответсвующим сообщением
        if (currentUser.getRole() != Role.Admin && currentUser.getRole() != Role.Superadmin) {
            logger.info(currentUser + " Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        //проверка на наличие пользователя с таким же логином, в противном случае перенаправляет на страницу
        // ошибки с соответсвующим сообщением
        try {
            userManager.getUser(login);
            logger.info(currentUser + " Redirecting to error page with error: Логин занят. Придумайте другой");
            session.setAttribute("error", "Логин занят. Придумайте другой");
            return "error";
        } catch (RuntimeException e) {
            //если все нормально создаем нового пользователя с указанными логином, ролью и стандартным паролем
            User user = new User();
            user.setLogin(login);
            user.setPassword("d17f25ecfbcc7857f7bebea469308be0b2580943e96d13a3ad98a13675c4bfc2");//11111
            user.setRole(Role.valueOf(role));
            userManager.save(user);
        }
        logger.info(currentUser + " User created");
        //перенаправляем запрос на метод отрисовки страницы админпанели
        return "redirect:/users";
    }

    /**
     * Метод для смены роли пользователя для уменьшения или увеличения его прав
     * @param id ИД пользователя, котрому необходимо сменить роль
     * @param role новая роль пользователя
     * @param session
     * @return
     */
    @PostMapping("/change_role")
    public String changeRole(@RequestParam(name = "id") int id,
                             @RequestParam(name = "role") String role,
                             HttpSession session) {
        //достаем из сессии автризованого пользователя
        User currentUser = (User) session.getAttribute("user");
        logger.info(currentUser + " Changing role...");

        //проверка прав пользователя для доступа к данному контенту, в противном случае перенаправляет на страницу
        // ошибки с соответсвующим сообщением
        if (currentUser.getRole() != Role.Superadmin) {
            logger.info(currentUser + " Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        //устанавливаем новую роль пользователю
        User user = userManager.getUser(id);
        user.setRole(Role.valueOf(role));
        userManager.save(user);
        logger.info(currentUser + " Role changed");
        //перенаправляем запрос на метод отрисовки страницы админпанели
        return "redirect:/users";
    }

    /**
     * Метод для сброса пароля пользователя. Ввиду отсутвия запасного метода коммуникации и подтверждения личности
     * пользователя, самостоятельный сброс пароля не возможен, и доступен только другим пользователям с необходимым
     * уровнем доступа
     * @param id ИД пользователя, которому необходимо сбросить пароль
     * @param session
     * @return
     */
    @PostMapping("/restore_pass")
    public String restorePass(@RequestParam(name = "id") int id, HttpSession session) {
        //достаем из сессии автризованого пользователя
        User currentUser = (User) session.getAttribute("user");
        logger.info(currentUser + " Restoring password...");

        //проверка прав пользователя для доступа к данному контенту, в противном случае перенаправляет на страницу
        // ошибки с соответсвующим сообщением
        if (currentUser.getRole() != Role.Admin && currentUser.getRole() != Role.Superadmin) {
            logger.info(currentUser + " Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        //устанавливаем стандартный пароль пользователю
        User user = userManager.getUser(id);

        String sha256hex = Hashing.sha256()
                .hashString("11111", StandardCharsets.UTF_8)
                .toString();
        user.setPassword(sha256hex);

        userManager.save(user);

        logger.info(currentUser + " Password restored");

        //перенаправляем запрос на метод отрисовки страницы админпанели
        return "redirect:/users";
    }

    /**
     * Метод для удаления пользователя
     * @param id ИД пользователя, которого необходимо удалить
     * @param session
     * @return
     */
    @PostMapping("/delete_user")
    public String deleteUser(@RequestParam(name = "id") int id, HttpSession session) {
        //достаем из сессии автризованого пользователя
        User currentUser = (User) session.getAttribute("user");
        logger.info(currentUser + " Deleting user");

        //проверка прав пользователя для доступа к данному контенту, в противном случае перенаправляет на страницу
        // ошибки с соответсвующим сообщением
        if (currentUser.getRole() != Role.Admin && currentUser.getRole() != Role.Superadmin) {
            logger.info(currentUser + " Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        //удаление аккаунтов суперадминов или админов другими админами также не допускается
        User user = userManager.getUser(id);
        if (user.getRole() == Role.Superadmin || (user.getRole() == Role.Admin && currentUser.getRole() != Role.Superadmin)) {
            logger.info("Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        //удаление аккаунта пользователя
        userManager.delete(id);
        logger.info(currentUser + " User deleted");
        //перенаправляем запрос на метод отрисовки страницы админпанели
        return "redirect:/users";
    }


    /**
     * Метод отрисовки страницы содержащей все записи сделанные менеджером за определенный период
     * @param managerId ИД менеджера для которого необходимо отобразить список сделанных записей
     * @param strDate начальная дата пероида для отображения
     * @param edDate конечная дата периода для отображения
     * @param clientName имя клиента по которому возможно отфильтровать записи. если пустое, то будут отображены записи
     *                   для всех пользователей
     * @param currencyId список ИД валют по которой возможно отфильтровать записи. если пустой, то будут отображены записи
     *                   для всех пользователей
     * @param session
     * @return
     */
    @GetMapping("/manager_history")
    public String managerHistory(@RequestParam(name = "manager_id", required = false) Integer managerId,
                                 @RequestParam(name = "strDate", required = false) Date strDate,
                                 @RequestParam(name = "edDate", required = false) Date edDate,
                                 @RequestParam(name = "client_name", required = false) String clientName,
                                 @RequestParam(name = "currency_id", required = false) List<Integer> currencyId,
                                 HttpSession session) {
        //достаем из сессии автризованого пользователя
        User currentUser = (User) session.getAttribute("user");
        logger.info(currentUser + " Loading manger history page...");

        //проверка прав пользователя для доступа к данному контенту, в противном случае перенаправляет на страницу
        // ошибки с соответсвующим сообщением
        if (currentUser.getRole() != Role.Admin && currentUser.getRole() != Role.Superadmin) {
            logger.info(currentUser + " Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }

        //если ИД менеджера нет в запросе, то берем его из сессии
        if (managerId == null) {
            managerId = ((User) session.getAttribute("manager")).getId();
        }
        logger.trace("managerId: " + managerId);

        //если начальной даты нет в запросе - устанавливаем сегодняшнюю
        if (strDate == null) strDate = Date.valueOf(LocalDate.now());
        logger.trace("strDate: " + strDate);
        //если конечной даты нет в запросе - устанавливаем через сутки от начальной
        if (edDate == null) edDate = new Date(strDate.getTime());
        edDate = new Date(edDate.getTime() + 86400000); // 24ч
        logger.trace("endDate: " + edDate);


        List<Currency> currencies = currencyManager.getAllCurrencies();

        //если ИД валюты не задано - используем ИД всех валют имеющихся в базе
        if (currencyId == null) {
            logger.debug("currencyId = null");
            currencyId = new ArrayList<>();

            for (Currency currency : currencies) {
                currencyId.add(currency.getId());
            }
            logger.trace("currencyId = " + currencyId);
        }
        //получаем список названий валют
        List<String> currencyName = new ArrayList<>();
        for (Integer curId : currencyId) {
            currencyName.add(currencyManager.getCurrency(curId).getName());
        }
        List<Client> clients = clientManager.findAll();
        List<Transaction> transactions = transManager.findByUser(managerId, new Timestamp(strDate.getTime()), new Timestamp(edDate.getTime() - 1), clientName, currencyId);
        User manager = userManager.getUser(managerId);

        //складываем всю информацию в сессию
        session.setAttribute("strDate", new Timestamp(strDate.getTime()));
        session.setAttribute("edDate", new Timestamp(edDate.getTime() - 86400000));
        session.setAttribute("manager", manager);
        session.setAttribute("transactions", transactions);
        session.setAttribute("currencies", currencies);
        session.setAttribute("currency_name", currencyName);
        session.setAttribute("clients", clients);
        logger.info(currentUser + " Manager history page loaded");
        //запускаем отрисовку страницы шаблонизатором
        return "managerHistory";
    }

    /**
     * Метод создания бэкапа существующей базы данных на сервере
     * @param session
     * @param response
     * @return
     * @throws Exception
     */
    @GetMapping("/database")
    public String makeBackUp(HttpSession session, HttpServletResponse response) throws Exception {
        //достаем из сессии автризованого пользователя
        User currentUser = (User) session.getAttribute("user");
        logger.info(currentUser + " Making backup...");

        //проверка прав пользователя для доступа к данному контенту, в противном случае перенаправляет на страницу
        // ошибки с соответсвующим сообщением
        if (currentUser.getRole() != Role.Admin && currentUser.getRole() != Role.Superadmin) {
            logger.info(currentUser + " Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        //запускаем процесс создания бэкапа с стандартными настройками
        scheduler.makeBackUp();
        logger.info(currentUser + " Backup made");
        //перенаправляем на страницу админпанели
        return "redirect:/users";
    }

    /**
     * Метод для восстановления базы данных на основании выбранного файла бэкапа
     * @param session
     * @param file название файла бэкапа, на основании которого необходимо провести восстановление
     * @return
     * @throws Exception
     */
    @PostMapping("/database")
    public String restoreBackUp(HttpSession session, @RequestParam(name = "file") String file) throws Exception {
        //достаем из сессии автризованого пользователя
        User currentUser = (User) session.getAttribute("user");
        logger.info(currentUser + " Restoring from backup...");
        //проверка прав пользователя для доступа к данному контенту, в противном случае перенаправляет на страницу
        // ошибки с соответсвующим сообщением
        if (currentUser.getRole() != Role.Admin && currentUser.getRole() != Role.Superadmin) {
            logger.info(currentUser + " Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        //запуск восстановления из бэкапа
        scheduler.restoreFromBackUp(file);
        logger.info(currentUser + " Backup restored");
        //перенаправляем на страницу админпанели
        return "redirect:/users";
    }


}
