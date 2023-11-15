package org.profinef.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.profinef.config.HttpSessionConfig;
import org.profinef.dto.TransactionDto;
import org.profinef.entity.*;
import org.profinef.entity.Currency;
import org.profinef.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Основной класс для обработки HTML запросов
 */
@SuppressWarnings("SameReturnValue")
@Controller
public class AppController {
    private static final Logger logger = LoggerFactory.getLogger(AppController.class);
    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final AccountManager accountManager;
    @Autowired
    private final TransManager transManager;
    @Autowired
    private final CurrencyManager currencyManager;

    public AppController(ClientManager clientManager, AccountManager accountManager, TransManager transManager,
                         CurrencyManager currencyManager) {
        this.clientManager = clientManager;
        this.accountManager = accountManager;
        this.transManager = transManager;
        this.currencyManager = currencyManager;
    }

    /**
     * Метод нужен только для перенаправления на главную страницу
     */
    @GetMapping("/")
    public String viewHomePage() {
        logger.info("Redirecting to main page");
        return "redirect:/client";
    }

    /**
     * Метод нужен для генерации странички выбора эксель файла для импорта информации
     *
     * @param session
     */
    @GetMapping("/excel")
    public String loadPage(HttpSession session) {
        logger.info("Loading load files page");
        //получение авторизованного пользователя из сесии
        User user = (User) session.getAttribute("user");
        //проверка прав пользователя для доступа к данному контенту, в противном случае перенаправляет на страницу
        //ошибки с соответсвующим сообщением
        if (user.getRole() != Role.Admin && user.getRole() != Role.Superadmin) {
            logger.info(user + " Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        //параметр path используется кнопкой назад на странице ошибки
        session.setAttribute("path", "/excel");
        //запускаем отрисовку страницы шаблонизатором
        return "uploadFiles";
    }

    /**
     * Генерирует страничку клиента со списком транзакций и балансом по каждой из валют. Также на этой страничке
     * возможно добавить новую транзакцию.
     *
     * @param clientId   ИД клиента информация которого будет отображена
     * @param currencyId список ИД валют по которым будут отфильтрованы записи. Если список пуст - будут отображены записи
     *                   для всех валют
     * @param startDate  начало периода для которого необходимо отобразить информацию
     * @param endDate    конец периода для которого необходимо отобразить информацию
     * @param session
     */
    @GetMapping("/client_info")
    public String viewClientTransactions(@RequestParam(name = "client_id", required = false, defaultValue = "0")
                                         int clientId,
                                         @RequestParam(name = "client_name", required = false)
                                         String clientName,
                                         @RequestParam(name = "currency_id", required = false) List<Integer> currencyId,
                                         @RequestParam(name = "startDate", required = false) Date startDate,
                                         @RequestParam(name = "endDate", required = false) Date endDate,
                                         HttpSession session) {
        //получаем авторизованного пользователя из сессии
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading client info page...");
        //удаление лишней информации из сессии
        session.removeAttribute("transaction");
        session.removeAttribute("client_alert");
        //если ИД клиента не задано
        if (clientId == 0) {
            logger.debug("clientId = 0");
            //если есть имя клиента - находим в базе этого клиента и берем его ИД
            if (clientName != null) {
                clientId = clientManager.getClientExactly(clientName).getId();
                //если в сессии клиент не записан перенаправляем на главную
            } else if (session.getAttribute("client") == null) {
                logger.debug("client = null");
                logger.info(user + " Redirecting to main page");
                return "redirect:/client";
                //иначе берем ИД клиента записанного в сессии
            } else {
                clientId = ((Client) session.getAttribute("client")).getId();
            }
            logger.debug("clientId had get from session");
            logger.trace("clientId = " + clientId);
        }

        //получаем список названий валют
        List<String> currencyName = new ArrayList<>();
        //если ИД валют не заданы - берем из базы все валюты и формируем из них список ИД валют и список названий валют
        if (currencyId == null) {
            logger.debug("currencyId = null");

            currencyId = new ArrayList<>();
            List<Currency> currencies = currencyManager.getAllCurrencies();
            for (Currency currency : currencies) {
                currencyName.add(currency.getName());
                currencyId.add(currency.getId());
            }
            logger.trace("currencyId = " + currencyId);
            logger.trace("currencyName = " + currencyName);
            //иначе для каждого из ИД находим соответсвующее название валюты и собираем в список
        } else {
            logger.debug("currencyId != null");
            for (int id : currencyId) {
                currencyName.add(currencyManager.getCurrency(id).getName());
            }
            logger.trace("currencyName = " + currencyName);
        }

        //если начальная дата не задана запросом
        if (startDate == null) {
            logger.debug("requested start date is null");
            //если стартовая дата не записана в сессию принимаем ее сегодняшней датой
            if (session.getAttribute("startDate" + clientId) == null) {
                logger.debug("session start date is null");
                startDate = Date.valueOf(LocalDate.now());
                //иначе берем дату из сессии
            } else {
                startDate = new Date(((Timestamp) session.getAttribute("startDate" + clientId)).getTime());
            }
            //если начальная дата в запросе не совпадает с начальной датой сохраненной в сессии (была изменена)
        } else if (startDate.getTime() != ((Timestamp) session.getAttribute("startDate" + clientId)).getTime()) {
            //и начальная дата в запросе позднее конечной даты
            if (startDate.after(new Date(endDate.getTime() - 86400000))) {
                //меняем конечную дату на ту же что и начальная
                endDate = new Date(startDate.getTime());

            }
        }

        // фактическая конечная дата используемая в запросах к базе данных и при прочих манипуляциях с ней на сутки
        // позднеее отображаемой визуально, т.к. время в таких запросах задается равным 00:00. Т.е. задав начальную и
        // конечную даты одинаковыми, например 19.10 и 19.10, в запросе даты будут соответсвенно 19.10 и 20.10

        //если конечная дата не задана запросом
        if (endDate == null) {
            logger.debug("requested end date is null");
            //если конечная дата не сохранена в сессии - устанавливаем конечную дату на сутки позднее начальной
            if (session.getAttribute("endDate" + clientId) == null) {
                logger.debug("requested end date is null");
                endDate = new Date(startDate.getTime() + 86400000);// 24ч
                //иначе берем конечную дату из сессии
            } else {
                endDate = new Date(((Timestamp) session.getAttribute("endDate" + clientId)).getTime() + 86400000);//24ч
            }
        } else {
            endDate = new Date(endDate.getTime() +  86400000);//24ч
            //если начальная дата позднее конечной - начальная дата принимается равной конечной
            if (startDate.after(new Date(endDate.getTime() - 86400000))) {//24ч
                startDate = new Date(endDate.getTime() - 86400000);//24ч
            }
        }
        logger.trace("startDate = " + startDate);
        logger.trace("endDate = " + endDate);

        //получаем из базы данных список транзакци для ыбранного клиента, валют, начальной и конечной даты
        Client client = clientManager.getClient(clientId);
        List<Transaction> transactions = transManager.findByClientForDate(clientId, currencyId,
                new Timestamp(startDate.getTime()), new Timestamp(endDate.getTime()));

        //получаем из базы данных список всех использованных ранее комментариев (без повторений)
        Set<String> comments = transManager.getAllComments();
        //получаем из базы данных список всех валют
        List<Currency> currencies = currencyManager.getAllCurrencies();

        Map<String, Double> total = new HashMap<>();
        Map<Integer, List<Integer>> transactionIds = new HashMap<>();
        for (Currency currency : currencies) {
            //разделяем весь список транзакций на отдельные по каждой из валют и берем ИД транзакций
            List<Integer> list = transactions.stream()
                    .filter(transaction -> transaction.getCurrency().getId().equals(currency.getId()))
                    .map(Transaction::getId).collect(Collectors.toList());
            transactionIds.put(currency.getId(), list);
            //формируем список значений начального и конечного балансов

            //находим запись для данного клиента, данной валюты, которая была последней перед выбранным диапазоном дат
            TransactionDto transactionDto = transManager.findPrevious(clientId, currency.getId(), new Timestamp(startDate.getTime()), 0);
            //если такой записи не существует принимаем начальный баланс равный 0
            if (transactionDto == null) {
                total.put("amount" + currency.getId(), 0.0);
                //иначе берем баланс из этой записи
            } else {
                total.put("amount" + currency.getId(), transactionDto.getBalance());
            }
            //разделяем весь список транзакций на отдельные по каждой из валют
            List<Transaction> transactionList = transactions.stream().filter(transaction -> transaction.getCurrency()
                    .getId().equals(currency.getId())).toList();
            //если он пуст - принимаем конечный баланс равный начальному
            if (transactionList.isEmpty()) {
                total.put("balance" + currency.getId(), total.get("amount" + currency.getId()));
            } else {
                //иначе берем баланс последней записи из этого списка
                total.put("balance" + currency.getId(), transactionList.get(transactionList.size() - 1).getBalance());
            }
        }
        logger.trace("total: " + total);

        List<Transaction> list = new ArrayList<>();
        //находим все связанные записи с записями данного клиента и формируем общий список
        for (Transaction tr : transactions) {
            List<Transaction> l = transManager.findById(tr.getId());
            for (Transaction t : l) {
                if (!list.contains(t)) {
                    list.add(t);
                }
            }

        }
        //сортируем этот список так, чтобы после каждой записи данного клиента шли связанные с этой записью записи
        //контрагентов
        list.sort((o1, o2) -> {
            if (Objects.equals(o1.getId(), o2.getId())) {
                if (Objects.equals(o1.getClient().getId(), client.getId())) return -1;
                if (Objects.equals(o2.getClient().getId(), client.getId())) return 1;
            }
            return 0;
        });
        transactions = list;
        logger.trace("transactions: " + transactions);

        //выполняем проверку работы с данным клиентом другими пользователями, и добавляем собщение-предупреждение,
        // если необходимо
        List<HttpSession> sessions = new HttpSessionConfig().getActiveSessions();
        for (HttpSession ses : sessions) {
            if (ses != null) {
                if (ses.getAttribute("path") == "/client_info" && ses.getAttribute("client").equals(client) && ses.getAttribute("user") != session.getAttribute("user")) {
                    session.setAttribute("client_alert", "Другой пользователь просматривает и/или редактирует данного клиента. Для получения актуальных данных дождитесь окончания работы данного пользователя");
                }
            }
        }
        //формируем список клиентов, исключая выбранного, для быстрого перехода
        List<Client> clients = clientManager.findAll();
        clients.remove(client);
        clients.sort(Comparator.comparing(Client::getPib));

        //складываем все необходимые данные в сессию
        session.setAttribute("clients", clients);
        session.setAttribute("comments", comments);
        session.setAttribute("path", "/client_info");
        session.setAttribute("currencies", currencies);
        session.setAttribute("total", total);
        session.setAttribute("startDate" + clientId, new Timestamp(startDate.getTime()));
        session.setAttribute("endDate" + clientId, new Timestamp(endDate.getTime() - 86400000));
        session.setAttribute("client", client);
        session.setAttribute("transactions", transactions);
        session.setAttribute("transactionIds", transactionIds);
        session.setAttribute("currency_name", currencyName);
        logger.info(user + " Client info page loaded");

        //запускаем отрисовку странички информации о клиенте
        return "clientInfo2";
    }

    /**
     * Метод запускает отрисовку главной страницы, которая содержит список всех пользователей и их балансов по каждой
     * из валют, а также суммарные балансы по всем клиентам
     *
     * @param clientName     параметр для фильтрации отображаемых пользователей по имени. Если не задано фильтр
     *                       не применяется
     * @param clientPhone    параметр для фильтрации отображаемых пользователей по номеру телефона. Если не задано
     *                       фильтр не применяется
     * @param clientTelegram параметр для фильтрации отображаемых пользователей по тегу Telegram. Если не задано
     *                       фильтр не применяется
     * @param searchExactly  если true необходимо точное будет найдено точное соответсвие имени, если false результаты
     *                       поиска также будут включать все варианты для которых введенное значение является частью
     *                       имени, вне зависимоти находится эта часть в начале, в середине или конце имени
     * @param session
     */
    @GetMapping(path = "/client")
    public String getClientsInfo(@RequestParam(name = "client_name", required = false) String clientName,
                                 @RequestParam(name = "client_phone", required = false) String clientPhone,
                                 @RequestParam(name = "client_telegram", required = false) String clientTelegram,
                                 @RequestParam(name = "search_exactly", required = false) boolean searchExactly,
                                 HttpSession session) {
        //получаем авторизованного пользователя из сессии
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading main page...");
        //удаление лишней информации из сессии
        session.removeAttribute("error");
        session.removeAttribute("client");

        //формируем список клиентов
        List<Account> clients;
        //если имя клиента задано фильтруем клиентов по имени
        if (clientName != null && !clientName.isEmpty()) {
            logger.debug("Filtering clients by names");
            clients = new ArrayList<>();
            try {
                //если нужно точное соответсвие
                if (searchExactly) {
                    clients.add(accountManager.getAccount(clientName));
                    //если ищем по части имени
                } else {
                    clients.addAll(accountManager.getAccounts(clientName));
                }
            } catch (Exception e) {
                session.setAttribute("error", e.getMessage());
                logger.info(user + " Redirected to error page with error: " + e.getMessage());
                return "error";
            }
            //если телефон задан фильтруем клиентов по телефону
        } else if (clientPhone != null && !clientPhone.isEmpty()) {
            logger.debug("Filtering clients by phone");
            clients = new ArrayList<>();
            try {
                clients.addAll(accountManager.getAccountsByPhone(clientPhone));
            } catch (Exception e) {
                session.setAttribute("error", e.getMessage());
                logger.info(user + " Redirected to error page with error: " + e.getMessage());
                return "error";
            }
            //если телеграм тег задан фильтруем клиентов по тегу
        } else if (clientTelegram != null && !clientTelegram.isEmpty()) {
            logger.debug("Filtering clients by telegram");
            clients = new ArrayList<>();
            try {
                clients.addAll(accountManager.getAccountsByTelegram(clientTelegram));
            } catch (Exception e) {
                session.setAttribute("error", e.getMessage());
                logger.info(user + " Redirected to error page with error: " + e.getMessage());
                return "error";
            }
            //иначе фильтры не применяются
        } else {
            logger.debug("Filters are not present");
            clients = new ArrayList<>();
            List<Account> c = accountManager.getAllAccounts();
            clients.add(transManager.addTotal(c));
            clients.addAll(c);
        }
        //складываем всю информацию в сессию
        List<Currency> currencies = currencyManager.getAllCurrencies();
        session.setAttribute("path", "/client");
        session.setAttribute("currencies", currencies);
        session.setAttribute("clients", clients);
        logger.info(user + " Main page loaded");
        //запускаем отрисовку главной страницы
        return "example";
    }




    /**
     * Метод запускает генерацию странички для добавления нового клиента
     *
     * @param session
     */
    @GetMapping(path = "/new_client")
    public String newClient(HttpSession session) {
        logger.info("Loading new client page...");
        session.setAttribute("path", "/new_client");
        logger.info("New client page loaded");
        return "addClient";
    }

    /**
     * Метод запускает генерацию странички для редактирования информации о клиенте
     *
     * @param session
     * @param clientId ИД клиента которого необходимо редактировать
     */
    @GetMapping(path = "/edit_client")
    public String editClient(HttpSession session, @RequestParam(name = "client_id") int clientId) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading edit client page...");
        Client client = clientManager.getClient(clientId);
        session.setAttribute("client", client);
        session.setAttribute("path", "/edit_client");
        logger.info(user + " Edit client page loaded");
        return "addClient";
    }

    /**
     * Метод сохраняет информацию о новом или существующем клиенте
     *
     * @param clientName новое имя клиента (обязательный параметр)
     * @param clientId   ИД клиента которого необходимо отредактировать (не задается для нового клиента)
     * @param phone      новый номер телефона клиента
     * @param telegram   новый телеграм тег клиента
     * @param session
     */
    @PostMapping(path = "/add_client")
    public String saveNewClient(@RequestParam(name = "client_name") String clientName,
                                @RequestParam(name = "client_id", required = false) int clientId,
                                @RequestParam(name = "client_phone", required = false) String phone,
                                @RequestParam(name = "client_telegram", required = false) String telegram,
                                HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Saving client...");

        // задаем имя клиенту
        Client client;
        //если ИД не задано - создаем нового клиента с нужным именем
        if (clientId <= 0) {
            logger.debug("Client is new");
            client = new Client(clientName);
        } else {
            //иначе меняем имя существующего клиента
            logger.debug("Client already present");
            client = clientManager.getClient(clientId);
            client.setPib(clientName);
        }

        // если задан номер телефона
        if (!phone.isEmpty()) {
            // проверка на корректность введенного номера и перенаправление на страницу ошибки при несоответсвии паттерну
            logger.debug("Validating phone...");
            if (!phone.matches("\\+38 \\(0[0-9]{2}\\) [0-9]{3}-[0-9]{2}-[0-9]{2}")) {
                logger.info(user + " Redirecting to error page with error: Номер телефона должен соответсвовать паттерну: " +
                        "+38 (011) 111-11-11");
                session.setAttribute("error", "Номер телефона должен соответсвовать паттерну: " +
                        "+38 (011) 111-11-11");
                return "error";
            }
            // назначение клиенту нового номера телефона
            client.setPhone(phone);
        }
        // если телеграм тег задан
        if (!telegram.isEmpty()) {
            //проверка на корректность введенного тега и перенаправление на страницу ошибки при несоответсвии паттерну
            logger.debug("Validating telegram...");
            if (!telegram.matches("@[a-z._0-9]+")) {
                logger.info(user + " Redirecting to error page with error: Телеграм тег должен начинаться с @ содержать " +
                        "латинские буквы нижнего регистра, цифры, \".\" или \"_\"");
                session.setAttribute("error", "Телеграм тег должен начинаться с @ содержать латинские буквы " +
                        "нижнего регистра, цифры, \".\" или \"_\"");
                return "error";
            }
            //назначение клиенту нового тега телеграм
            client.setTelegram(telegram);
        }
        try {
            //если клиент существующий - обновляем его информацию
            if (session.getAttribute("path") == "/edit_client") {
                logger.debug("Updating client");
                clientManager.updateClient(client);
            }
            //если клиент новый - добавляем его в базу
            if (session.getAttribute("path") == "/new_client") {
                logger.debug("Adding client");
                accountManager.addClient(client);
            }
        } catch (Exception e) {
            logger.info(user + " Redirecting to error page with error: " + e.getMessage());
            session.setAttribute("error", e.getMessage());
            return "error";
        }
        logger.info(user + " Client saved");
        // перенаправляем на главную страницу
        return "redirect:/client";
    }

    /**
     * Метод для удаления клиента
     *
     * @param clientId ИД клиента, которого необходимо удалить
     * @param session
     */
    @PostMapping(path = "delete_client")
    public String deleteClient(@RequestParam(name = "id") int clientId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Deleting client...");
        Client client = clientManager.getClient(clientId);
        clientManager.deleteClient(client);
        logger.info(user + " Client deleted");
        return "redirect:/client";
    }

    /**
     * Метод запускает генерацию странички для добавления новой валюты в базу данных
     *
     * @param session collect parameters for view
     */
    @GetMapping(path = "/new_currency")
    public String newCurrency(HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading new currency page...");
        session.setAttribute("path", "/new_currency");
        logger.info(user + " New currency page loaded");
        return "addCurrency";
    }

    /**
     * Метод добавляющий новую валюту в базу данных
     *
     * @param currencyName название новой валюты
     * @param currencyId   код/ИД новой валюты
     * @param session
     */
    @PostMapping(path = "/new_currency")
    public String saveNewCurrency(@RequestParam(name = "currency_name") String currencyName,
                                  @RequestParam(name = "currency_id") int currencyId,
                                  HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Saving new currency...");

        //проверка соответсвия названия паттерну, и перенаправление на страницу ошибки в противном случае
        if (!currencyName.matches("[A-Z]{3}")) {
            logger.info(user + " Redirecting to error page with error: В названии должны быть три заглавные латинские буквы");
            session.setAttribute("error", "В названии должны быть три заглавные латинские буквы");
            return "error";
        }
        //проверка соответствия кода/ИД принятому диапазону, и перенаправление на страницу ошибки в противном случае
        if (currencyId < 100 || currencyId > 999) {
            logger.info(user + " Redirecting to error page with error: Код должен состоять из трех цифр от 0 до 9");
            session.setAttribute("error", "Код должен состоять из трех цифр от 0 до 9");
            return "error";
        }
        // создание новой валюты с заданными параметрами и добавление ее в базу
        Currency newCurrency = new Currency();
        newCurrency.setName(currencyName);
        newCurrency.setId(currencyId);
        try {
            currencyManager.addCurrency(newCurrency);
        } catch (Exception e) {
            logger.info(user + " Redirecting to error page with error: " + e.getMessage());
            session.setAttribute("error", e.getMessage());
            return "error";
        }
        logger.info(user + " Currency saved");

        //перенаправление на главную страницу
        return "redirect:/client";
    }

    /**
     * Метод сохраняет стили определенных значений записи. Информация для сохранения передается из JavaScript по
     * технологии AJAX в формате JSON. Сами значения при этом остаются неизменными
     *
     * @param colors  Строка в формате JSON, содержащая информацию про параметры записи и их цвета, которые необходимо
     *                сохранить
     * @param session
     * @throws JsonProcessingException выбрасывается исключение при невозможности интерпритировать строку colors
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PostMapping(path = "/save_colors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String saveColors(@RequestBody String colors, HttpSession session) throws JsonProcessingException {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Saving colors...");

        //преобразуем исходную строку в список параметров
        List<List<String>> entryList = new ObjectMapper().readValue(colors, new TypeReference<>() {});

        //для каждого элемента списка выбираем
        for (List<String> list : entryList) {
            String[] arr = list.get(0).split("_");
            // ИД записи
            int id = Integer.parseInt(arr[0]);
            // ИД клиента
            int clientId = Integer.parseInt(arr[2]);
            // ИД валюты
            int currencyId = Integer.parseInt(arr[1]);
            // находим соответсвующую запись в базе
            Transaction transaction = transManager.getTransactionByClientAndByIdAndByCurrencyId(id, clientId, currencyId);
            // применяем стиль к соответвующему параметру данной записи
            switch (arr[3]) {
                case "comment" -> transaction.setCommentColor(list.get(1));
                case "pAmount" -> transaction.setInputColor(list.get(1));
                case "nAmount" -> transaction.setOutputColor(list.get(1));
                case "commission" -> transaction.setTarifColor(list.get(1));
                case "com" -> transaction.setCommissionColor(list.get(1));
                case "rate" -> transaction.setRateColor(list.get(1));
                case "transportation" -> transaction.setTransportationColor(list.get(1));
                case "total" -> transaction.setAmountColor(list.get(1));
                case "balance" -> transaction.setBalanceColor(list.get(1));
            }
            transManager.save(transaction);
        }
        logger.info(user + " Colors saved");
        return "redirect:/client_info";
    }

    /**
     * Метод сохраняет стили определенных значений каждого клиента. Информация для сохранения передается из JavaScript по
     * технологии AJAX в формате JSON. Сами значения при этом остаются неизменными
     *
     * @param colors  Строка в формате JSON, содержащая информацию про параметры записи и их цвета, которые необходимо
     *                сохранить
     * @param session
     * @throws JsonProcessingException выбрасывается исключение при невозможности интерпритировать строку colors
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PostMapping(path = "/save_main_colors", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String saveMainColors(@RequestBody String colors, HttpSession session) throws JsonProcessingException {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Saving colors...");

        //преобразуем исходную строку в список параметров
        List<List<String>> entryList = new ObjectMapper().readValue(colors, new TypeReference<>() {});
        //для каждого элемента списка выбираем
        for (List<String> list : entryList) {
            String[] arr = list.get(0).split("_");
            // ИД клиента
            int clientId = Integer.parseInt(arr[0]);
            Client client = clientManager.getClient(clientId);
            int currencyId = -1;
            // сохраняем стиль соответствующей валюты
            if (arr[1].matches("[0-9]{3}")) {
                currencyId = Integer.parseInt(arr[1]);
                Account account = accountManager.getAccount(client.getPib());
                Currency currency = currencyManager.getCurrency(currencyId);
                Map<Currency, Account.Properties> currencies = account.getCurrencies();
                Account.Properties prop = currencies.get(currency);
                prop.setColor(list.get(1));
                currencies.put(currency, prop);
                account.setCurrencies(currencies);
                accountManager.save(account);
            }
            // сохраняем стиль имени клиента
            if (currencyId == -1) {
                client.setColor(list.get(1));
                clientManager.save(client);
            }


        }
        logger.info(user + " Colors saved");
        return "redirect:/client";
    }

    /**
     * Метод для генерации странички с выбором параметров пересчета кжшированных значений
     * @param session
     * @return
     */
    @GetMapping("/recashe")
    public String getRefreshPage(HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading refresh cashe page...");
        User currentUser = (User) session.getAttribute("user");
        session.setAttribute("clients", clientManager.findAll());
        logger.info(user + " Refresh page loaded");
        return "recashe";
    }

    /**
     * Метод для выполнения пересчета кэшированных значений
     * @param date дата с которой необходимо начать пересчет
     * @param clientName имя клиента для которого необходимо выполнить пересчет. Если не задано - выполняется пересчет
     *                   по всем клиентам
     * @param session
     * @return
     */
    @PostMapping("/recashe")
    public String refreshCashe(@RequestParam(name = "date", required = false, defaultValue = "0001-01-01") Date date,
                               @RequestParam(name = "client", required = false) String clientName,
                               HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Updating balance values");

        //получаем список аккаунтов для которых необходимо выполнить пересчет
        List<Account> accounts = new ArrayList<>();
        if (clientName.isEmpty()) {
            logger.debug("client name is empty");
            accounts = accountManager.getAllAccounts();
        } else {
            logger.debug("client name found");
            accounts.add(accountManager.getAccount(clientName));
        }
        logger.info(user + " Found " + accounts.size() + " accounts");
        //для каждого аккаунта
        for (Account account : accounts) {
            logger.info(user + " Updating " + account.getClient().getPib());
            //выбираем информацию о валютах и их балансах
            Map<Currency, Account.Properties> currencies = account.getCurrencies();
            // формируем список ИД валют
            List<Integer> currenciesId = currencies.keySet().stream().map(Currency::getId).toList();
            // находим список записей для этого аккаунта и валют
            List<Transaction> transactions = transManager.findByClientForDate(account.getClient().getId(), currenciesId, new Timestamp(date.getTime()), new Timestamp(System.currentTimeMillis()));
            logger.trace("transactions: " + transactions);
            // для каждой валюты
            for (Currency currency : currencies.keySet()) {
                // находим первую запись из списка
                Optional<Transaction> trOpt = transactions.stream().filter(t -> Objects.equals(t.getCurrency().getId(), currency.getId())).findFirst();
                if (trOpt.isEmpty()) continue;
                Transaction first = trOpt.get();
                logger.trace("first: " + first);

                // находим начальное значение баланса из предыдущей записи (если ее нет, то принимаем 0)
                TransactionDto previous = transManager.findPrevious(account.getClient().getId(), currency.getId(), new Timestamp(date.getTime() - 1), first.getId());
                logger.trace("previous: " + previous);
                Double previousBalance = 0.0;
                if (previous != null) {
                    previousBalance = previous.getBalance();
                }
                logger.trace("previousBalance: " + previousBalance);
                // устанавливаем данное значение баланса валюты для аккаунта
                currencies.put(currency, new Account.Properties(previousBalance, null));
                account.setCurrencies(currencies);
                accountManager.save(account);
            }
            // для каждой записи выполняем расчет с обновлением баланса записи и общего баланса
            for (Transaction t : transactions) {
                transManager.remittance(t.getId(), t.getDate(), t.getClient().getPib(), t.getComment(), t.getCurrency().getId(), t.getRate(), t.getCommission(),
                        t.getAmount(), t.getTransportation(), t.getCommentColor(), t.getAmountColor(), t.getUser().getId(), 0.0,
                        t.getInputColor(), t.getOutputColor(), t.getTarifColor(), t.getCommissionColor(), t.getRateColor(), t.getTransportationColor(),
                        t.getBalanceColor());
            }

        }
        logger.info(user + " Data updated");
        // выполняется перенаправление на страницу информации о клиенте
        return "redirect:/client_info";
    }






}
