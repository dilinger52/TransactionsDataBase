package org.profinef.controller;

import jakarta.servlet.http.HttpSession;
import org.profinef.dto.TransactionDto;
import org.profinef.entity.*;
import org.profinef.entity.Currency;
import org.profinef.service.AccountManager;
import org.profinef.service.ClientManager;
import org.profinef.service.CurrencyManager;
import org.profinef.service.TransManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.ZoneId.systemDefault;
import static java.util.Collections.reverse;

/**
 * Класс используется для обработки запросов связанных с внесением изменения в записи, их добавление и удаление
 */
@SuppressWarnings({"SameReturnValue", "UnusedReturnValue"})
@Controller
public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
    @Autowired
    private final AccountManager accountManager;
    @Autowired
    private final CurrencyManager currencyManager;
    @Autowired
    private final TransManager transManager;
    @Autowired
    private final ClientManager clientManager;

    public TransactionController(AccountManager accountManager, CurrencyManager currencyManager, TransManager transManager, ClientManager clientManager) {
        this.accountManager = accountManager;
        this.currencyManager = currencyManager;
        this.transManager = transManager;
        this.clientManager = clientManager;
    }

    /**
     * Метод обрабатывающий HTML запрос, связанный отображеним страницы для добавления новой записи (в насоящее время не
     * используется)
     * @param session
     * @return
     */
    @GetMapping("/add_transaction")
    public String addTransaction(HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading add transaction page...");
        // удаляем лишнюю информацию из сессии
        session.removeAttribute("transaction");
        // получаем список клиентов
        List<Account> clients = accountManager.getAllAccounts();
        // получаем список валют
        List<Currency> currencies = currencyManager.getAllCurrencies();
        // создаем транзакцию с дефолтными параметрами
        List<Transaction> transaction = new ArrayList<>();
        Transaction tr = new Transaction();
        tr.setClient(new Client(""));
        tr.setCurrency(new Currency());
        tr.setRate(1.0);
        tr.setCommission(0.0);
        tr.setTransportation(0.0);
        transaction.add(tr);
        // ложим необходимую информацию в сессию
        session.setAttribute("clients", clients);
        session.setAttribute("transaction", transaction);
        session.setAttribute("path", "/add_transaction");
        session.setAttribute("currencies", currencies);
        logger.info(user + " Add transaction page loaded");
        return "addTransaction";
    }

    /**
     * Метод обрабатывающий HTML запрос, связанный отображеним страницы для редактирования записи (в насоящее время не
     * используется)
     *
     * @param transactionId id of transaction for editing
     * @param session       collect parameters for view
     */
    @GetMapping(path = "/edit")
    public String editTransactionPage(@RequestParam(name = "transaction_id", required = false, defaultValue = "0") int transactionId,
                                      HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading edit transaction page...");
        // если транзакция не задана в запросе
        if (transactionId == 0) {
            logger.debug("transactionId == 0");
            // если транзакция не задана в сессии перенаправляем на главную страницу
            if (session.getAttribute("transaction") == null) {
                logger.debug("Transaction is no set");
                logger.info(user + " Redirecting to the main page");
                return "redirect:/client";
            }
            // берем ИД транзакции из сессии
            transactionId = ((List<Transaction>) session.getAttribute("transaction")).get(0).getId();
            logger.debug("clientId had get from session");
            logger.trace("transactionId = " + transactionId);
        }
        // получаем список клиентов
        List<Account> clients = accountManager.getAllAccounts();
        // получаем список связанных записей
        List<Transaction> transaction = transManager.getTransaction(transactionId);
        // получаем список валют
        List<Currency> currencies = currencyManager.getAllCurrencies();
        // складываем всю информацию  всессию
        session.setAttribute("clients", clients);
        session.setAttribute("currencies", currencies);
        session.setAttribute("transaction", transaction);
        session.setAttribute("path", "/edit");
        logger.info(user + " Edit transaction page loaded");
        return "addTransaction";
    }

    /**
     * Метод обрабатывающий HTML запросы, связанные с редактированием записей
     * @param transactionId     список ИД записей, которые необходимо отредактировать
     * @param clientName        имена клиентов, чьи записи необходимо отредактировать
     * @param comment           список новых комментариев для записей
     * @param currencyName      список названий валют, для которых необходимо отредактировать записи
     * @param rateS             список курсов записей в виде строки
     * @param commissionS       список тарифов записей в виде строки
     * @param positiveAmountS   список приемов записей в виде строки
     * @param negativeAmountS   список выдач записей в виде строки
     * @param transportationS   список инкасаций записей в виде строки
     * @param pointer           ИД ячейки в которой был установлен курсо на момент отправки запроса
     * @param session
     * @return
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PostMapping(path = "/edit_transaction")
    public String editTransaction(@RequestParam(name = "transaction_id") List<Integer> transactionId,
                                  @RequestParam(name = "client_name") List<String> clientName,
                                  @RequestParam(name = "comment") List<String> comment,
                                  @RequestParam(name = "currency_name") List<String> currencyName,
                                  @RequestParam(name = "rate") List<String> rateS,
                                  @RequestParam(name = "commission") List<String> commissionS,
                                  @RequestParam(name = "positiveAmount") List<String> positiveAmountS,
                                  @RequestParam(name = "negativeAmount") List<String> negativeAmountS,
                                  @RequestParam(name = "transportation") List<String> transportationS,
                                  @RequestParam(name = "pointer", required = false, defaultValue = "UAHtr0_pAmount") String pointer,
                                  HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Saving transaction after editing...");
        // ряд преобразований переменных из строчных к численным предварительно форматируя к необходимому виду
        List<Double> rate = new ArrayList<>();
        for (String s : rateS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                rate.add(Double.valueOf(s));
            } else {
                rate.add(null);
            }
        }
        List<Double> commission = new ArrayList<>();
        for (String s : commissionS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                commission.add(Double.valueOf(s));
            } else {
                commission.add(null);
            }
        }
        List<Double> positiveAmount = new ArrayList<>();
        for (String s : positiveAmountS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                positiveAmount.add(Double.valueOf(s));
            } else {
                positiveAmount.add(null);
            }
        }
        List<Double> negativeAmount = new ArrayList<>();
        for (String s : negativeAmountS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                negativeAmount.add(Double.valueOf(s));
            } else {
                negativeAmount.add(null);
            }
        }
        List<Double> transportation = new ArrayList<>();
        for (String s : transportationS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                transportation.add(Double.valueOf(s));
            } else {
                transportation.add(null);
            }
        }

        // проверка на наличие объема записи. необходима для предотвращения некорректного отображения и дальнейших расчетов
        List<Double> amount = new ArrayList<>();
        int size = positiveAmount.size();
        if (size < negativeAmount.size()) size = negativeAmount.size();
        if (size < 1) {
            logger.info(user + " Redirecting to error page with error: Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
            session.setAttribute("error", "Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
            return "error";
        }

        // проверка на наличие нескольких записей с совпадающими ИД, клиентом и валютой. Эти три параметра определяют
        // уникальность и идентичность записи, и потому запись в базу нескольких записей с полным их сопадением невозмодно
        List<String> clientDouble = new ArrayList<>();
        List<String> currencyDouble = new ArrayList<>();
        for (int i = 0; i < clientName.size(); i++) {
            if (!clientDouble.contains(clientName.get(i))) {
                clientDouble.add(clientName.get(i));
                currencyDouble.add(currencyName.get(i));
            } else if (currencyDouble.contains(currencyName.get(i))) {
                session.setAttribute("error", "Обнаружено наличие как минимум двух позиций с совпадением " +
                        "значений клиента и валюты. Подобная операция не допустима");
                logger.info(user + " Redirecting to error page with error: Обнаружено наличие как минимум двух позиций " +
                        "с совпадением значений клиента и валюты. Подобная операция не допустима");
                return "error";
            }
        }
        for (int i = 0; i < size; i++) {
            // дополняем списки приема и выдачи до одного размера, заполняя пропуски нулями
            if (positiveAmount.size() < size) {
                positiveAmount.add(0.0);
            }
            if (positiveAmount.get(i) == null) {
                positiveAmount.set(i, 0.0);
            }

            if (negativeAmount.size() < size) {
                negativeAmount.add(0.0);
            }
            if (negativeAmount.get(i) == null) {
                negativeAmount.set(i, 0.0);
            }
            // выполняем проверку на 0 в сумме приема и выдачи
            if (positiveAmount.isEmpty() || (!Objects.equals(positiveAmount.get(i), 0.0) && positiveAmount.get(i) + negativeAmount.get(i) == 0)) {
                logger.info(user + " Redirecting to error page with error: Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
                session.setAttribute("error", "Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
                return "error.html";
            }
            // устанавливаем объем записи равный сумме приема и выдачи
            amount.add(positiveAmount.get(i) + negativeAmount.get(i));


        }
        // для пустого списка с комментариями меняем его на список пустых строк
        if (comment.isEmpty()) {
            logger.debug("comment is empty");
            comment = new ArrayList<>();
            for (String ignored : clientName) {
                comment.add("");
            }
        }

        try {
            List<Integer> currencyId = new ArrayList<>();
            List<Integer> clientId = new ArrayList<>();
            // получаем список ИД валют
            for (String name : currencyName) {
                currencyId.add(currencyManager.getCurrency(name).getId());
            }
            // получаем список ИД клиентов
            for (String name : clientName) {
                clientId.add(clientManager.getClientExactly(name).getId());
            }
            // берем из сессии историю действий для отмены
            Stack<Action> undos = (Stack<Action>) session.getAttribute("undos" + clientId.get(0));
            if (undos == null) {
                undos = new Stack<>();
            }
            // берем из сессии историю отмененных действий для их повторения
            Stack<Action> redos = (Stack<Action>) session.getAttribute("redos" + clientId.get(0));
            if (redos == null) {
                redos = new Stack<>();
            }
            // меняем порядок списков на противоположный (шаблонизатор не поддерживает списки типа последний добавлен -
            // первый взят)
            reverse(undos);
            reverse(redos);

            // получаем список записей по ИД и сортируем так, чтобы запись клиента, с которым ведется работа, была первой
            List<Transaction> transactionList = transManager.getTransaction(transactionId.get(0)).stream().sorted(new Comparator<>() {
                final Client client = (Client) session.getAttribute("client");

                @Override
                public int compare(Transaction o1, Transaction o2) {
                    if (Objects.equals(o1.getClient().getId(), client.getId())) return -1;
                    if (Objects.equals(o2.getClient().getId(), client.getId())) return 1;
                    return 0;
                }
            }).toList();

            // создаем списки старых и новых балансов по каждому клиенту и каждой валюте
            Map<Client, Map<Currency, Double>> oldBalances = new TreeMap<>();
            Map<Client, Map<Currency, Double>> newBalances = new TreeMap<>();
            Transaction tr = transactionList.get(0);
            // удаляем все записи из списка с откатом балансов и пересчетов последующих записей
            transManager.deleteTransaction(transactionList);

            for (int i = 0; i < clientName.size(); i++) {
                // дополняем списки тарифа, курса и инкасации до размера списка объема, заполняя пропуски нулями
                if (commission.size() < clientName.size()) {
                    commission.add(0.0);
                }
                if (commission.get(i) == null) {
                    commission.set(i, 0.0);
                }

                if (rate.size() < clientName.size()) {
                    rate.add(1.0);
                }
                if (rate.get(i) == null) {
                    rate.set(i, 1.0);
                }

                if (transportation.size() < clientName.size()) {
                    transportation.add(0.0);
                }
                if (transportation.get(i) == null) {
                    transportation.set(i, 0.0);
                }
            }

            // формируем списки старых балансов
            for (int i = 0; i < clientName.size(); i++) {
                // создаем список баланосв клиента по каждой валюте
                Map<Currency, Double> oldBalance = new TreeMap<>();
                // если для текущего клиента список балансов уже есть -  берем его
                if (oldBalances.containsKey(clientManager.getClientExactly(clientName.get(i)))) {
                    oldBalance = oldBalances.get(clientManager.getClientExactly(clientName.get(i)));
                }
                // находим предыдущую запись
                TransactionDto previous = transManager.findPrevious(clientId.get(i), currencyId.get(i), tr.getDate(), transactionId.get(0));
                // задаем баланс равный нулю
                Double b = 0.0;
                // если предыдущая запись существует - устанавливаем баланс равным ее балансу
                if (previous != null) b = previous.getBalance();
                // добавляем баланс в соответсвующие списки
                oldBalance.put(currencyManager.getCurrency(currencyId.get(i)), b);
                oldBalances.put(clientManager.getClientExactly(clientName.get(i)), oldBalance);
            }
            for (int i = 0; i < clientName.size(); i++) {
                // находим предыдущую запись
                TransactionDto previous = transManager.findPrevious(clientId.get(i), currencyId.get(i), tr.getDate(), transactionId.get(0));
                // устанавливаем баланс равным балансу предыдущей записи или нулую, если такая запись не существует
                Double balance = 0.0;
                if (previous != null) balance = previous.getBalance();
                int finalI = i;
                // из списка записей выбераем ту, у которой ИД валюты и ИД клиента совпадают с ИД валюты и ИД клиента
                // из списков, с порядковыми номерами соответсвующих номеру итерации
                Transaction tran = transactionList.stream()
                        .filter(t -> Objects.equals(t.getCurrency().getId(), currencyId.get(finalI))
                                && Objects.equals(t.getClient().getId(), clientId.get(finalI))).findFirst().orElse(new Transaction());
                // создаем новую запись (в замен удаленной) с новыми параметрами, получая новый баланс
                Double b = transManager.update(tr.getId(), tr.getDate(), clientName.get(i), comment.get(i),
                        currencyId.get(i), rate.get(i), commission.get(i), amount.get(i),
                        transportation.get(i), tran.getCommentColor(), tran.getAmountColor(), tr.getUser().getId(), balance,
                        tran.getInputColor(), tran.getOutputColor(), tran.getTarifColor(), tran.getCommissionColor(), tran.getRateColor(),
                        tran.getTransportationColor(), tran.getBalanceColor());
                // складываем новый баланс в соответсвующий список
                Map<Currency, Double> newBalance = new TreeMap<>();
                if (newBalances.containsKey(clientManager.getClientExactly(clientName.get(i)))) {
                    newBalance = newBalances.get(clientManager.getClientExactly(clientName.get(i)));
                }
                newBalance.put(currencyManager.getCurrency(currencyId.get(i)), b);
                newBalances.put(clientManager.getClientExactly(clientName.get(i)), newBalance);
            }
            logger.trace("oldBalances: " + oldBalances);
            logger.trace("newBalances: " + newBalances);
            // для каждого значения из списка старых балансов, обновляем баласы последующих записей, увеличивая их
            // значения на разницу соответсвующих значений нового и старого балансов
            Set<Map.Entry<Client, Map<Currency, Double>>> set = oldBalances.entrySet();
            for (Map.Entry<Client, Map<Currency, Double>> entry : set) {
                Client key = entry.getKey();
                Map<Currency, Double> value = entry.getValue();
                Set<Map.Entry<Currency, Double>> valueSet = value.entrySet();
                for (Map.Entry<Currency, Double> e : valueSet) {
                    Currency k = e.getKey();
                    Double v = e.getValue();
                    List<Integer> o = new ArrayList<>();
                    o.add(k.getId());
                    transManager.updateNext(key.getPib(), o, newBalances.get(key).get(k) - v, tr.getDate());
                }
            }
            // выполняем пополнение истории изменений
            // получаем список новых записей отсортированных, так чтобы запись клиента, с которым ведется работа, была первой
            List<Transaction> newTransactionList = transManager.getTransaction(transactionId.get(0)).stream().sorted(new Comparator<>() {
                final Client client = (Client) session.getAttribute("client");

                @Override
                public int compare(Transaction o1, Transaction o2) {
                    if (Objects.equals(o1.getClient().getId(), client.getId())) return -1;
                    if (Objects.equals(o2.getClient().getId(), client.getId())) return 1;
                    return 0;
                }
            }).toList();
            // определяем изменение
            String change = findChanges(newTransactionList, transactionList, (Client) session.getAttribute("client"));
            if (!change.isEmpty()) {
                logger.debug("found changes");
                // если  это новое действие очищаем историю отмен действий
                if (session.getAttribute("do") != "undo" && session.getAttribute("do") != "redo") {
                    logger.debug("new action of editing");
                    redos.clear();
                }
                // если это не отмена действия добавляем действие в список для отмены
                if (session.getAttribute("do") != "undo" /*&& session.getAttribute("do") != "redo"*/) {
                    logger.debug("redoing of editing");
                    Action action = new Action();
                    action.setName("Редактирование " + change);
                    //action.setName("Редактирование");
                    action.setChanges(transactionList);
                    logger.trace(String.valueOf(action));
                    if (!undos.contains(action)) {
                        logger.debug("new action");
                        undos.push(action);
                    }
                    session.removeAttribute("do");
                }
                // если это отмена действия добавляем действие в список для повторения
                if (session.getAttribute("do") == "undo") {
                    logger.debug("undoing of editing");
                    Action action = getAction(change, transactionList);
                    logger.trace(String.valueOf(action));
                    if (!redos.contains(action)) {
                        logger.debug("new action");
                        redos.push(action);
                    }
                    session.removeAttribute("do");
                }
            }
            // переворачиваем списки
            reverse(undos);
            reverse(redos);
            session.setAttribute("undos" + clientId.get(0), undos);
            session.setAttribute("redos" + clientId.get(0), redos);

            // если курсор стоял в ячейке новой записи - меняем параметр pointer на реальный ИД новой записи
            String[] pp = pointer.split("_");
            if (pp.length < 3) {
                if (pp[0].substring(0, 3).matches("[0-9]{3,}.*")) {
                    pointer = transactionId.get(0) + "_" + pp[0].substring(0, 3) + "_" + clientId.get(clientId.size() - 1) + "_" + pp[1];
                }
            }
        } catch (Exception e) {
            logger.info(user + " Redirecting to error page with error: " + e.getMessage() + Arrays.toString(e.getStackTrace()));

            session.setAttribute("error", e.getMessage());
            return "error";
        }

        logger.info(user + " Transaction saved");
        session.setAttribute("pointer", pointer);
        return "redirect:/client_info";
    }

    /**
     * Вспомогательный метод для определения вида действия
     * @param change
     * @param transactionList
     * @return
     */
    private static Action getAction(String change, List<Transaction> transactionList) {
        Action action = new Action();
        action.setName("Редактирование " + change);
        //action.setName("Редактирование");
                /*List<Transaction> changes = new ArrayList<>();
                for (int i = 0; i < clientName.size(); i++) {
                   Transaction change = new Transaction();
                   change.setId(tr.getId());
                   change.setClient(clientManager.getClientExactly(clientName.get(i)));
                   change.setCurrency(currencyManager.getCurrency(currencyName.get(i)));
                   change.setDate(tr.getDate());
                   change.setComment(comment.get(i));
                   change.setAmount(amount.get(i));
                   change.setCommission(commission.get(i));
                   change.setRate(rate.get(i));
                   change.setTransportation(transportation.get(i));
                   changes.add(change);
                }
                action.setChanges(changes);*/
        action.setChanges(transactionList);
        return action;
    }


    /**
     * Вспомогательный метод, который определяет наличие изменения, его вид, параметры, и записывает это  в виде строки
     * @param newTransactionList список записей после изменений
     * @param transactionList список записей д о изменений
     * @param client клиент с записями которого проводились действия
     * @return
     */
    private String findChanges(List<Transaction> newTransactionList, List<Transaction> transactionList, Client client) {
        // создаем строку для результата
        StringBuilder result = new StringBuilder();
        // параметр длины принимаем как максимальный размер нового и старого списков записей
        int length = Math.max(newTransactionList.size(), transactionList.size());
        // если новый список меньше
        if (newTransactionList.size() < length) {
            // если была удалена запись текущего клиента - фиксируем изменение записи, с указанием объема этой записи
            for (int i = 0; i < length; i++) {
                if (!newTransactionList.contains(transactionList.get(i))) {
                    if (transactionList.get(i).getClient().equals(client)) {
                        result.append("записи ").append("\"").append(transactionList.get(i).getAmount()).append("\"");
                        return result.toString();
                    }
                }
            }
            // если была удалена связанная запись - фиксируем удаление контрагента, с указанием его имени
            for (int i = 0; i < length; i++) {
                if (!newTransactionList.contains(transactionList.get(i))) {
                    result.append("удаление контрагента ").append("\"").append(transactionList.get(i).getClient().getPib()).append("\"");
                    return result.toString();
                }
            }
        }
        // если старый список записей меньше
        if (transactionList.size() < length) {
            // если была добавлена запись текущего клиента - фиксируем добавление записи, с указанием объема этой записи
            for (int i = 0; i < length; i++) {
                if (!transactionList.contains(newTransactionList.get(i))) {
                    if (newTransactionList.get(i).getClient().equals(client)) {
                        result.append("записи ").append("\"").append(newTransactionList.get(i).getAmount()).append("\"");
                        return result.toString();
                    }
                }
            }
            // если была добавлена связанная запись - фиксируем добавление контрагента, с указанием его имени
            for (int i = 0; i < length; i++) {
                if (!transactionList.contains(newTransactionList.get(i))) {
                    result.append("добавление контрагента ").append("\"").append(newTransactionList.get(i).getClient().getPib()).append("\"");
                    return result.toString();
                }
            }
        }
        for (int i = 0; i < length; i++) {
            // если был изменен комментарий - фиксируем с указанием нового комментария
            if (!Objects.equals(transactionList.get(i).getComment(), newTransactionList.get(i).getComment())) {
                result.append("изменение комментария ").append("\"").append(newTransactionList.get(i).getComment()).append("\"");
                break;
            }
            // если был изменен объем - фиксируем с указанием нового объема
            if (!Objects.equals(transactionList.get(i).getAmount(), newTransactionList.get(i).getAmount())) {
                result.append("изменение объема ").append("\"").append(newTransactionList.get(i).getAmount()).append("\"");
                break;
            }
            // если был изменена коммиссия - фиксируем с указанием новой коммиссии
            if (!Objects.equals(transactionList.get(i).getCommission(), newTransactionList.get(i).getCommission())) {
                result.append("изменение тарифа ").append("\"").append(newTransactionList.get(i).getCommission()).append("\"");
                break;
            }
            // если был изменен курс - фиксируем с указанием нового курса
            if (!Objects.equals(transactionList.get(i).getRate(), newTransactionList.get(i).getRate())) {
                result.append("изменение курса ").append("\"").append(newTransactionList.get(i).getRate()).append("\"");
                break;
            }
            // если был изменена инкасация - фиксируем с указанием новой инкасации
            if (!Objects.equals(transactionList.get(i).getTransportation(), newTransactionList.get(i).getTransportation())) {
                result.append("изменение инкасации ").append("\"").append(newTransactionList.get(i).getTransportation()).append("\"");
                break;
            }
        }
        return result.toString();
    }

    /**
     * Метод обрабатывающий HTML запросы, связанные с удалением записей
     *
     * @param transactionId ИД записи, которую необходимо удалить
     * @param session
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PostMapping(path = "delete_transaction")
    public String deleteTransaction(@RequestParam(name = "transaction_id") int transactionId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Deleting transaction...");
        // получаем ИД клиента из сессии
        int clientId = ((Client) session.getAttribute("client")).getId();
        // получаем списки истории действий и изменений, и переворачиваем их
        Stack<Action> undos = (Stack<Action>) session.getAttribute("undos" + clientId);
        if (undos == null) {
            undos = new Stack<>();
        }
        Stack<Action> redos = (Stack<Action>) session.getAttribute("redos" + clientId);
        if (redos == null) {
            redos = new Stack<>();
        }
        reverse(undos);
        reverse(redos);
        // получаем список записей по ИД из базы данных
        List<Transaction> transactionList = transManager.getTransaction(transactionId);
        // удаляем все записи из списка с обновлением балансов и последующих записей
        transManager.deleteTransaction(transactionList);
        // получаем новый список записей (должен быть пустым)
        List<Transaction> newTransactionList = transManager.getTransaction(transactionId);
        // находим изменения
        String change = findChanges(newTransactionList, transactionList, (Client) session.getAttribute("client"));
        if (!change.isEmpty()) {
            logger.debug("found changes");
            // если это новое действие - очищаем список истории отмен изменений
            if (session.getAttribute("do") != "undo" && session.getAttribute("do") != "redo") {
                redos.clear();
                logger.debug("new action of deleting");
            }
            // если это не отмена действия - добавляем в список отмен действий
            if (session.getAttribute("do") != "undo" /*&& session.getAttribute("do") != "redo"*/) {
                logger.debug("redoing deleting");
                Action action = new Action();
                action.setName("Удаление " + change);
                //action.setName("Удаление");
                action.setChanges(transactionList);
                logger.trace(String.valueOf(action));
                if (!undos.contains(action)) {
                    logger.debug("new action");
                    undos.push(action);
                }
                session.removeAttribute("do");

            }
            // если это отмена действия - добавляем в список повторения действий
            if (session.getAttribute("do") == "undo") {
                logger.debug("undoing adding");
                Action action = new Action();
                action.setName("Добавление " + change);
                //action.setName("Добавление");
                action.setChanges(transactionList);
                logger.trace(String.valueOf(action));
                if (!redos.contains(action)) {
                    logger.debug("new action");
                    redos.push(action);
                }
                session.removeAttribute("do");

            }
        }
        // переворачиваем списки и ложим в сессию
        reverse(undos);
        reverse(redos);
        session.setAttribute("undos" + clientId, undos);
        session.setAttribute("redos" + clientId, redos);
        logger.info(user + " Transaction deleted");
        return "redirect:/client_info";
    }

    /**
     * Метод обрабатывает HTML запросы, связанные с добавлением новых записей
     * @param clientName            имена клиентов, участвующих в транзакции
     * @param currencyName          названия валют, которые менялись в данной транзакции
     * @param comment               список комментариев к записям транзакции
     * @param rateS                 список курсов записей в виде строк
     * @param commissionS           список коммиссий записей в виде строк
     * @param positiveAmountS       список приемов записей в виде строк
     * @param negativeAmountS       список выдач записей в виде строк
     * @param transportationS       список инкасаций записей в виде строк
     * @param date                  дата проведения транзакции
     * @param pointer               параметр, указывающий ИД ячейки в которой был курсор на момент обращения к серверу
     * @param session
     * @param commentColor
     * @param inputColor
     * @param outputColor
     * @param tarifColor
     * @param commissionColor
     * @param rateColor
     * @param transportationColor
     * @param amountColor
     * @param balanceColor
     * @return
     */

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PostMapping(path = "/transaction")
    public String doTransaction(@RequestParam(name = "client_name") List<String> clientName,
                                @RequestParam(name = "currency_name") List<String> currencyName,
                                @RequestParam(name = "comment", required = false) List<String> comment,
                                @RequestParam(name = "rate") List<String> rateS,
                                @RequestParam(name = "commission") List<String> commissionS,
                                @RequestParam(name = "positiveAmount") List<String> positiveAmountS,
                                @RequestParam(name = "negativeAmount") List<String> negativeAmountS,
                                @RequestParam(name = "transportation") List<String> transportationS,
                                @RequestParam(name = "date", required = false) java.sql.Date date,
                                @RequestParam(name = "pointer", required = false, defaultValue = "UAHtr0_pAmount") String pointer,
                                HttpSession session,
                                @RequestParam(required = false) List<String> commentColor,
                                @RequestParam(required = false) List<String> inputColor,
                                @RequestParam(required = false) List<String> outputColor,
                                @RequestParam(required = false) List<String> tarifColor,
                                @RequestParam(required = false) List<String> commissionColor,
                                @RequestParam(required = false) List<String> rateColor,
                                @RequestParam(required = false) List<String> transportationColor,
                                @RequestParam(required = false) List<String> amountColor,
                                @RequestParam(required = false) List<String> balanceColor) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Creating new transaction...");
        // преобразование величин из строчных в численные, предварительно выполнив необходимое форматирование
        List<Double> rate = new ArrayList<>();
        for (String s : rateS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                rate.add(Double.valueOf(s));
            } else {
                rate.add(null);
            }
        }
        List<Double> commission = new ArrayList<>();
        for (String s : commissionS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                commission.add(Double.valueOf(s));
            } else {
                commission.add(null);
            }
        }
        List<Double> positiveAmount = new ArrayList<>();
        for (String s : positiveAmountS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                positiveAmount.add(Double.valueOf(s));
            } else {
                positiveAmount.add(null);
            }
        }
        List<Double> negativeAmount = new ArrayList<>();
        for (String s : negativeAmountS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                negativeAmount.add(Double.valueOf(s));
            } else {
                negativeAmount.add(null);
            }
        }
        List<Double> transportation = new ArrayList<>();
        for (String s : transportationS) {
            s = s.replaceAll("\\s+", "").replaceAll(",", ".");
            if (!s.isEmpty()) {
                transportation.add(Double.valueOf(s));
            } else {
                transportation.add(null);
            }
        }

        List<Double> amount = new ArrayList<>();
        // параметр size определяется как размер большего списка среди списков приема и выдачи
        int size = positiveAmount.size();
        if (size < negativeAmount.size()) size = negativeAmount.size();
        // приводим списки приема и выдачи к одному размеру, заполняя пропуски нулями
        for (int i = 0; i < size; i++) {
            if (positiveAmount.size() < size) {
                positiveAmount.add(0.0);
            }
            if (positiveAmount.get(i) == null) {
                positiveAmount.set(i, 0.0);
            }
            if (negativeAmount.size() < size) {
                negativeAmount.add(0.0);
            }
            if (negativeAmount.get(i) == null) {
                negativeAmount.set(i, 0.0);
            }
            // проверка на зануление записи (детальнее в редактировании записей)
            if (positiveAmount.isEmpty() || (positiveAmount.get(i) != 0 && positiveAmount.get(i) + negativeAmount.get(i) == 0)) {
                logger.info(user + " Redirecting to error page with error: Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
                session.setAttribute("error", "Недопустимое зануление транзакции. Пожалуйста воспользуйтесь кнопкой удалить на против соответсвующей транзакции");
                return "error";
            }
            // определяем объем записи как сумму приема и выдачи
            amount.add(positiveAmount.get(i) + negativeAmount.get(i));
        }
        // если запись была выполнена с помощью страницы "Обмен валют" меняем знак у объема второй записи
        if (session.getAttribute("path") == "/transfer") {
            amount.set(0, (-1) * amount.get(0));
        }
        try {
            // если список комментариев пуст, заполняем его пустыми строками
            if (comment.isEmpty()) {
                comment = new ArrayList<>();
                for (String ignored : clientName) {
                    comment.add("");
                }
            }
            // проверка на совпадение ИД, клиента и валюты у двух и более записей (подробней в редактировании записей)
            List<String> clientDouble = new ArrayList<>();
            List<String> currencyDouble = new ArrayList<>();
            for (int i = 0; i < clientName.size(); i++) {
                if (!clientDouble.contains(clientName.get(i))) {
                    clientDouble.add(clientName.get(i));
                    currencyDouble.add(currencyName.get(i));
                } else if (currencyDouble.contains(currencyName.get(i))) {
                    session.setAttribute("error", "Обнаружено наличие как минимум двух позиций с совпадением " +
                            "значений клиента и валюты. Подобная операция не допустима");
                    logger.info(user + " Redirecting to error page with error: Обнаружено наличие как минимум двух позиций " +
                            "с совпадением значений клиента и валюты. Подобная операция не допустима");
                    return "error";
                }
            }
            // ИД новой записи, как максимальный ИД имеющейся записи, увеличенный на единицу
            int transactionId = transManager.getMaxId() + 1;
            logger.trace("transactionId: " + transactionId);
            // старый список записей (пустой)
            List<Transaction> oldTransactionList = transManager.getTransaction(transactionId);
            // получаем список ИД валют
            List<Integer> currencyId = new ArrayList<>();
            for (String name : currencyName) {
                currencyId.add(currencyManager.getCurrency(name).getId());
            }
            // получаем список ИД клиентов
            List<Integer> clientId = new ArrayList<>();
            for (String name : clientName) {
                clientId.add(clientManager.getClientExactly(name).getId());
            }
            // если цвета не заданы заменяем их списком со значениями null
            List<String> arr = new ArrayList<>();
            for (int i = 0; i < clientName.size(); i++) {
                arr.add(null);
            }
            if (commentColor == null) commentColor = new ArrayList<>(arr);
            if (inputColor == null) inputColor = new ArrayList<>(arr);
            if (outputColor == null) outputColor = new ArrayList<>(arr);
            if (tarifColor == null) tarifColor = new ArrayList<>(arr);
            if (commissionColor == null) commissionColor = new ArrayList<>(arr);
            if (rateColor == null) rateColor = new ArrayList<>(arr);
            if (transportationColor == null) transportationColor = new ArrayList<>(arr);
            if (amountColor == null) amountColor = new ArrayList<>(arr);
            if (balanceColor == null) balanceColor = new ArrayList<>(arr);

            for (int i = 0; i < clientName.size(); i++) {
                // дополняем списки тарифа курса и инкасации, заполняя пропуски стандартными значениями
                if (commission.size() < clientName.size()) {
                    commission.add(0.0);
                }
                if (commission.get(i) == null) {
                    commission.set(i, 0.0);
                }

                if (rate.size() < clientName.size()) {
                    rate.add(1.0);
                }
                if (rate.get(i) == null) {
                    rate.set(i, 1.0);
                }

                if (transportation.size() < clientName.size()) {
                    transportation.add(0.0);
                }
                if (transportation.get(i) == null) {
                    transportation.set(i, 0.0);
                }
                // если объем записи равен нулю - она игнорируется
                if (amount.get(i) == 0) continue;

                try {
                    // если дата записи раньше текущей даты
                    if (date != null && date.before(new java.sql.Date(LocalDate.now().atStartOfDay(systemDefault()).toInstant().toEpochMilli()))) {
                        logger.debug("Date before today date");
                        // находим доступное в эту дату значение времени
                        Timestamp dt = transManager.getAvailableDate(new Timestamp(date.getTime()));
                        // находим предыдущую запись
                        TransactionDto previousTransaction = transManager.findPrevious(clientId.get(i), currencyId.get(i), dt, 0);
                        // если она существует берем ее баланс, если нет - то null
                        Double trBalance = null;
                        if (previousTransaction != null) {
                            trBalance = previousTransaction.getBalance();
                        }
                        logger.trace("trBalance: " + trBalance);
                        // старый баланс - баланс аккаунта в соответсвующей валюте
                        Double oldBalance = accountManager.getAccount(clientName.get(i)).getCurrencies().get(currencyManager.getCurrency(currencyId.get(i))).getAmount();
                        logger.trace("oldBalance: " + oldBalance);
                        // сохраняем новую запись, обновляя и получая новый баланс аккаунта
                        Double newBalance = transManager.remittance(transactionId, dt, clientName.get(i), comment.get(i),
                                currencyId.get(i), rate.get(i), commission.get(i), amount.get(i), transportation.get(i),
                                commentColor.get(i), amountColor.get(i), user.getId(), trBalance, inputColor.get(i),
                                outputColor.get(i), tarifColor.get(i), commissionColor.get(i),
                                rateColor.get(i), transportationColor.get(i), balanceColor.get(i));
                        logger.trace("newBalance: " + newBalance);
                        List<Integer> list = new ArrayList<>();
                        list.add(currencyId.get(i));
                        logger.trace("list: " + list);
                        // увеличиваем балансы последующих записей на величину разницы нового и старого балансов
                        transManager.updateNext(clientName.get(i), list, newBalance - oldBalance, dt);
                    } else {
                        // иначе сохраняем текущую запись
                        logger.debug("Date is today");
                        transManager.remittance(transactionId, null, clientName.get(i), comment.get(i),
                                currencyId.get(i), rate.get(i), commission.get(i), amount.get(i), transportation.get(i),
                                commentColor.get(i), amountColor.get(i), user.getId(), 0.0, inputColor.get(i),
                                outputColor.get(i), tarifColor.get(i), commissionColor.get(i),
                                rateColor.get(i), transportationColor.get(i), balanceColor.get(i));
                    }
                } catch (Exception e) {
                    logger.info(user + " Redirecting to error page with error: " + e.getMessage() + Arrays.toString(e.getStackTrace()));
                    session.setAttribute("error", e.getMessage());
                    return "error";
                }
            }

            // система отслеживания изменений (описывалась ранее)
            Stack<Action> undos = (Stack<Action>) session.getAttribute("undos" + clientId.get(0));
            if (undos == null) {
                undos = new Stack<>();
            }
            Stack<Action> redos = (Stack<Action>) session.getAttribute("redos" + clientId.get(0));
            if (redos == null) {
                redos = new Stack<>();
            }
            reverse(undos);
            reverse(redos);
            List<Transaction> transactionList = transManager.getTransaction(transactionId);
            String change = findChanges(transactionList, oldTransactionList, (Client) session.getAttribute("client"));
            if (!change.isEmpty()) {
                logger.debug("found changes");
                if (session.getAttribute("do") != "undo" && session.getAttribute("do") != "redo") {
                    logger.debug("new action of adding");
                    redos.clear();
                }
                if (session.getAttribute("do") != "undo" /*&& session.getAttribute("do") != "redo"*/) {
                    logger.debug("redo adding");
                    Action action = new Action();
                    action.setName("Добавление " + change);
                    //action.setName("Добавление");
                    action.setChanges(transactionList);
                    logger.trace(String.valueOf(action));
                    if (!undos.contains(action)) {
                        logger.debug("new action");
                        undos.push(action);
                    }
                    session.removeAttribute("do");

                }
                if (session.getAttribute("do") == "undo") {
                    logger.debug("undo deleting");
                    Action action = new Action();
                    action.setName("Удаление " + change);
                    //action.setName("Удаление");
                    action.setChanges(transactionList);
                    logger.trace(String.valueOf(action));
                    if (!redos.contains(action)) {
                        logger.debug("new action");
                        redos.push(action);
                    }
                    session.removeAttribute("do");

                }
            }
            reverse(undos);
            reverse(redos);
            session.setAttribute("undos" + clientId.get(0), undos);
            session.setAttribute("redos" + clientId.get(0), redos);
            // система корректировки параметра pointer (описывалась ранее)
            clientId = clientId.stream().distinct().collect(Collectors.toList());
            System.out.println(pointer);
            String[] pp = pointer.split("_");
            pointer = transactionId + "_" + currencyManager.getCurrency(pp[0].substring(0, 3)).getId() + "_" + clientId.get(Integer.parseInt(pp[0].substring(5))) + "_" + pp[1];
            System.out.println(pointer);
            // если дата новой записи вне диапазона дат ранее выбранного для отображения на странице клиента, то
            // следующий алгоритм сместит нужную границу до даты новой записи
            java.sql.Date startDate = new Date(((Timestamp) session.getAttribute("startDate" + clientId.get(0))).getTime());
            if (startDate.after(date)) {
                startDate = date;
            }
            assert startDate != null;
            session.setAttribute("startDate" + clientId.get(0), new Timestamp(startDate.getTime()));
            java.sql.Date endDate = new Date(((Timestamp) session.getAttribute("endDate" + clientId.get(0))).getTime());
            if (endDate.before(date)) {
                endDate = date;
            }
            assert endDate != null;
            session.setAttribute("endDate" + clientId.get(0), new Timestamp(endDate.getTime()));
        } catch (Exception e) {
            logger.info(user + " Redirecting to error page with error: " + e.getMessage() + Arrays.toString(e.getStackTrace()));
            if (Objects.equals(e.getMessage(), "Index 0 out of bounds for length 0")) {
                session.setAttribute("error", "Недопустимое зануление транзакции");
            } else {
                session.setAttribute("error", e.getMessage());
            }
            return "error";
        }

        logger.info(user + " Transaction created");
        session.setAttribute("pointer", pointer);
        return "redirect:/client_info";
    }

    /**
     * Метод обрабатывающий HTML запросы связанные с отменой последнего или нескольких последних действий
     * @param id        ИД действия до которого включительно необходимо выполнить отмену
     * @param session
     * @return
     */
    @PostMapping("/undo")
    public String undo(@RequestParam(name = "id", required = false, defaultValue = "-1") int id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Starting undoing...");
        logger.trace("Action id=" + id);
        int clientId = ((Client) session.getAttribute("client")).getId();

        // проверка на наличие в запросе ИД действия
        if (id != -1) {

            for (int i = 0; i <= id; i++) {
                // указываем что была/будет производиться отмена
                session.setAttribute("do", "undo");
                // получаем списки для отмен и повторов действий
                Stack<Action> undos = (Stack<Action>) session.getAttribute("undos" + clientId);
                if (undos == null) {
                    undos = new Stack<>();
                }
                Stack<Action> redos = (Stack<Action>) session.getAttribute("redos" + clientId);
                if (redos == null) {
                    redos = new Stack<>();
                }
                // переворачиваем их
                reverse(undos);
                reverse(redos);
                logger.trace("undos: " + undos);
                logger.trace("redos: " + redos);
                /*Action action = undos.get(undos.size() - i - 1);
                undos.pop();*/
                // берем крайнее действие из списка для отмены действий
                Action action = undos.pop();
                // сортируем список изменений в действии таким образом, чтобы первым шло изменение связанное с
                // относящееся к текущему клиенту
                action.setChanges(action.getChanges().stream().sorted((o1, o2) -> {
                    if (o1.getClient().getId() == clientId) return -1;
                    if (o2.getClient().getId() == clientId) return 1;
                    return 0;
                }).toList());
                // возвращем списки обратно в сессию
                reverse(undos);
                reverse(redos);
                session.setAttribute("redos" + clientId, redos);
                session.setAttribute("undos" + clientId, undos);

                // если название дейтвия начинается на Редактирование - производим изменение записи с параметрами
                // записанными в изменениях
                if (action.getName().matches("Редактирование.*")) {
                    logger.debug("found edit");
                    List<Transaction> transactions = action.getChanges();
                    List<Integer> transactionId = new ArrayList<>();
                    List<String> clientName = new ArrayList<>();
                    List<String> comment = new ArrayList<>();
                    List<String> currencyName = new ArrayList<>();
                    List<String> rate = new ArrayList<>();
                    List<String> commission = new ArrayList<>();
                    List<String> positiveAmount = new ArrayList<>();
                    List<String> negativeAmount = new ArrayList<>();
                    List<String> transportation = new ArrayList<>();
                    for (Transaction transaction : transactions) {
                        transactionId.add(transaction.getId());
                        clientName.add(transaction.getClient().getPib());
                        comment.add(transaction.getComment());
                        currencyName.add((transaction.getCurrency().getName()));
                        rate.add(String.valueOf(transaction.getRate()));
                        commission.add(String.valueOf(transaction.getCommission()));
                        positiveAmount.add(String.valueOf(transaction.getAmount()));
                        negativeAmount.add(String.valueOf(0.0));
                        transportation.add(String.valueOf(transaction.getTransportation()));
                    }
                    editTransaction(transactionId, clientName, comment, currencyName, rate, commission, positiveAmount, negativeAmount, transportation, "UAHtr0_pAmount", session);
                    //return "redirect:/client_info";
                    // если название действия начинается на Удаление - производим создание записи
                } else if (action.getName().matches("Удаление.*")) {
                    logger.debug("found delete");
                    List<Transaction> transactions = action.getChanges();
                    List<Integer> transactionId = new ArrayList<>();
                    Timestamp date = transactions.get(0).getDate();
                    List<String> clientName = new ArrayList<>();
                    List<String> comment = new ArrayList<>();
                    List<String> currencyName = new ArrayList<>();
                    List<String> rate = new ArrayList<>();
                    List<String> commission = new ArrayList<>();
                    List<String> positiveAmount = new ArrayList<>();
                    List<String> negativeAmount = new ArrayList<>();
                    List<String> transportation = new ArrayList<>();
                    List<String> commentColor = new ArrayList<>();
                    List<String> inputColor = new ArrayList<>();
                    List<String> outputColor = new ArrayList<>();
                    List<String> tarifColor = new ArrayList<>();
                    List<String> commissionColor = new ArrayList<>();
                    List<String> rateColor = new ArrayList<>();
                    List<String> transportationColor = new ArrayList<>();
                    List<String> amountColor = new ArrayList<>();
                    List<String> balanceColor = new ArrayList<>();
                    for (Transaction transaction : transactions) {
                        transactionId.add(transaction.getId());
                        clientName.add(transaction.getClient().getPib());
                        comment.add(transaction.getComment());
                        currencyName.add((transaction.getCurrency().getName()));
                        rate.add(String.valueOf(transaction.getRate()));
                        commission.add(String.valueOf(transaction.getCommission()));
                        positiveAmount.add(String.valueOf(transaction.getAmount()));
                        negativeAmount.add(String.valueOf(0.0));
                        transportation.add(String.valueOf(transaction.getTransportation()));
                        commentColor.add(transaction.getCommentColor());
                        inputColor.add(transaction.getInputColor());
                        outputColor.add(transaction.getOutputColor());
                        tarifColor.add(transaction.getTarifColor());
                        commissionColor.add(transaction.getCommissionColor());
                        rateColor.add(transaction.getRateColor());
                        transportationColor.add(transaction.getTransportationColor());
                        amountColor.add(transaction.getAmountColor());
                        balanceColor.add(transaction.getBalanceColor());
                    }
                    doTransaction(clientName, currencyName, comment, rate, commission, positiveAmount, negativeAmount,
                            transportation, new Date(date.getTime()), "UAHtr0_pAmount", session, commentColor,
                            inputColor, outputColor, tarifColor, commissionColor, rateColor, transportationColor,
                            amountColor, balanceColor);
                    //return "redirect:/client_info";
                    // если название действия начинается на Добавление - производим удаление записи
                } else if (action.getName().matches("Добавление.*")) {
                    logger.debug("found add");
                    List<Transaction> transactions = action.getChanges();
                    deleteTransaction(transactions.get(0).getId(), session);
                    //return "redirect:/client_info";
                }
            }

        }
        return "redirect:/client_info";
    }

    /**
     * Метод обрабатывает HTML запросы, связанные с повторным выполнением отмененных действий
     * @param id ИД действия до которого включительно необходимо повторить все ранее отмененные действия
     * @param session
     * @return
     */
    @PostMapping("/redo")
    public String redo(@RequestParam(name = "id", required = false, defaultValue = "-1") int id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Starting redoing...");
        logger.trace("Action id=" + id);
        int clientId = ((Client) session.getAttribute("client")).getId();
        // проверка на наличия ИД действия в запросе
        if (id != -1) {

            for (int i = 0; i <= id; i++) {
                // обозначаем что было/будет производиться повторное выполнение действия
                session.setAttribute("do", "redo");
                // берем списки действий для отмены и повторного выполнения действий
                Stack<Action> undos = (Stack<Action>) session.getAttribute("undos" + clientId);
                if (undos == null) {
                    undos = new Stack<>();
                }
                Stack<Action> redos = (Stack<Action>) session.getAttribute("redos" + clientId);
                if (redos == null) {
                    redos = new Stack<>();
                }
                reverse(undos);
                reverse(redos);
                logger.trace("undos: " + undos);
                logger.trace("redos: " + redos);
                /*Action action = redos.get(redos.size() - i - 1);
                redos.pop();*/
                // берем следующее действие из списка действий для повторного выполнения
                Action action = redos.pop();

            /*if (!undos.contains(action)) {
                undos.push(action);
            }*/
                // складываем списки обратно в сессию
                reverse(undos);
                reverse(redos);
                session.setAttribute("undos" + clientId, undos);
                session.setAttribute("redos" + clientId, redos);
                // сортируем список изменений в действии так, чтобы первым шло изменение касающееся текущего клиента
                action.setChanges(action.getChanges().stream().sorted((o1, o2) -> {
                    if (o1.getClient().getId() == clientId) return -1;
                    if (o2.getClient().getId() == clientId) return 1;
                    return 0;
                }).toList());
                // если название действия начинается на Редактирование - запускаем редактирование записи
                if (action.getName().matches("Редактирование.*")) {
                    logger.debug("found edit");
                    List<Transaction> transactions = action.getChanges();
                    List<Integer> transactionId = new ArrayList<>();
                    List<String> clientName = new ArrayList<>();
                    List<String> comment = new ArrayList<>();
                    List<String> currencyName = new ArrayList<>();
                    List<String> rate = new ArrayList<>();
                    List<String> commission = new ArrayList<>();
                    List<String> positiveAmount = new ArrayList<>();
                    List<String> negativeAmount = new ArrayList<>();
                    List<String> transportation = new ArrayList<>();

                    for (Transaction transaction : transactions) {
                        transactionId.add(transaction.getId());
                        clientName.add(transaction.getClient().getPib());
                        comment.add(transaction.getComment());
                        currencyName.add((transaction.getCurrency().getName()));
                        rate.add(String.valueOf(transaction.getRate()));
                        commission.add(String.valueOf(transaction.getCommission()));
                        positiveAmount.add(String.valueOf(transaction.getAmount()));
                        negativeAmount.add(String.valueOf(0.0));
                        transportation.add(String.valueOf(transaction.getTransportation()));
                    }
                    editTransaction(transactionId, clientName, comment, currencyName, rate, commission, positiveAmount, negativeAmount, transportation, "UAHtr0_pAmount", session);
                    //return "redirect:/client_info";
                    // если название действия начинается на Удаление - запускаем удаление действия
                } else if (action.getName().matches("Удаление.*")) {
                    logger.debug("found delete");
                    List<Transaction> transactions = action.getChanges();
                    deleteTransaction(transactions.get(0).getId(), session);
                    //return "redirect:/client_info";
                    // если название действия начинается на Добавление - запускаем создание новой записи
                } else if (action.getName().matches("Добавление.*")) {
                    logger.debug("found add");
                    List<Transaction> transactions = action.getChanges();
                    List<Integer> transactionId = new ArrayList<>();
                    Timestamp date = transactions.get(0).getDate();
                    List<String> clientName = new ArrayList<>();
                    List<String> comment = new ArrayList<>();
                    List<String> currencyName = new ArrayList<>();
                    List<String> rate = new ArrayList<>();
                    List<String> commission = new ArrayList<>();
                    List<String> positiveAmount = new ArrayList<>();
                    List<String> negativeAmount = new ArrayList<>();
                    List<String> transportation = new ArrayList<>();
                    List<String> commentColor = new ArrayList<>();
                    List<String> inputColor = new ArrayList<>();
                    List<String> outputColor = new ArrayList<>();
                    List<String> tarifColor = new ArrayList<>();
                    List<String> commissionColor = new ArrayList<>();
                    List<String> rateColor = new ArrayList<>();
                    List<String> transportationColor = new ArrayList<>();
                    List<String> amountColor = new ArrayList<>();
                    List<String> balanceColor = new ArrayList<>();
                    for (Transaction transaction : transactions) {
                        transactionId.add(transaction.getId());
                        clientName.add(transaction.getClient().getPib());
                        comment.add(transaction.getComment());
                        currencyName.add((transaction.getCurrency().getName()));
                        rate.add(String.valueOf(transaction.getRate()));
                        commission.add(String.valueOf(transaction.getCommission()));
                        positiveAmount.add(String.valueOf(transaction.getAmount()));
                        negativeAmount.add(String.valueOf(0.0));
                        transportation.add(String.valueOf(transaction.getTransportation()));
                        commentColor.add(transaction.getCommentColor());
                        inputColor.add(transaction.getInputColor());
                        outputColor.add(transaction.getOutputColor());
                        tarifColor.add(transaction.getTarifColor());
                        commissionColor.add(transaction.getCommissionColor());
                        rateColor.add(transaction.getRateColor());
                        transportationColor.add(transaction.getTransportationColor());
                        amountColor.add(transaction.getAmountColor());
                        balanceColor.add(transaction.getBalanceColor());
                    }
                    doTransaction(clientName, currencyName, comment, rate, commission, positiveAmount, negativeAmount,
                            transportation, new Date(date.getTime()), "UAHtr0_pAmount", session, commentColor,
                            inputColor, outputColor, tarifColor, commissionColor, rateColor, transportationColor,
                            amountColor, balanceColor);
                    //return "redirect:/client_info";
                }
            }

        }
        return "redirect:/client_info";
    }

    /**
     * Метод для обработки HTML запроса, связанного с отображением странички "Обмен валют"
     * @param session
     * @return
     */
    @GetMapping("/convertation")
    public String convertation(HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading convertation page");
        session.setAttribute("path", "/convertation");
        return "convertation";
    }

    /**
     * Метод для обработки HTML запросов, связанных с отображением странички "Перевод" (в настоящее время не используется)
     * @param session
     * @return
     */
    @GetMapping("/transfer")
    public String transfer(HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Loading transfer page");
        session.setAttribute("path", "/transfer");
        return "transfer";
    }

}
