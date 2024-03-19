package org.profinef.controller;

import com.monitorjbl.xlsx.StreamingReader;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.*;
import org.profinef.entity.*;
import org.profinef.entity.Currency;
import org.profinef.payload.request.NewClientRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.*;

/**
 * Данный класс реализует функциональность для загрузки на сервер файла в формате office open xml и извлечением из него
 * информации о записях с последующим обновлением базы данных
 */
@Controller
@RequestMapping(path = "/upload")
public class Uploader {

    @Autowired
    private final ClientController clientController;
    @Autowired
    private final TransController transController;
    @Autowired
    private final CurrencyController currencyController;
    @Autowired
    private final AccountController accountController;
    @Autowired
    private final RoleController roleController;

    private Client client;
    private Date date = null;
    private List<Integer> currencies = new ArrayList<>();
    private double amount = 0;
    private final String amountColor = "";
    private double commission = 0;
    private double rate = 1;
    private double transportation = 0;
    private boolean isTransaction = false;
    private String comment = "";
    private final int columnPerCurrency = 8;

    private static final Logger logger = LoggerFactory.getLogger(Uploader.class);

    public Uploader(ClientController clientController, TransController transController, CurrencyController currencyController, AccountController accountController, RoleController roleController) {
        this.clientController = clientController;
        this.transController = transController;
        this.currencyController = currencyController;
        this.accountController = accountController;
        this.roleController = roleController;
    }

    /**
     * Основной метод, реализующий обработку файла и обновление базы данных на его основе
     * @param file импортированный файл для обработки
     * @param dateAfter дата с которой включительно будут удалены записи из базы данных, а вместо них записаны те, что
     *                  будут обнаружены в файле. Если дата не задана используется значение по умолчанию: 01-01-0001,
     *                  т.е. база будет очищена полностью
     * @param session
     * @return
     * @throws Exception может быть выброшено при проблемах считывания файла
     */

    @RequestMapping("/full")
    public String uploadDataFromExcel(@RequestParam("file") MultipartFile file,
                                      @RequestParam(name = "date", required = false, defaultValue = "0001-01-01") java.sql.Date dateAfter,
                                      HttpSession session) throws Exception {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Getting file...");
        // проверка наличия необходимых прав у пользователя. Ввиду серьезных изменений в базе данных, данное действие
        // запрещено к выполнению не администраторам
        if (!user.getRoles().contains(roleController.get(ERole.ROLE_ADMIN)) && !user.getRoles().contains(roleController.get(ERole.ROLE_ADMIN))) {
            logger.info(user + " Redirecting to error page with error: Отказано в доступе");
            session.setAttribute("error", "Отказано в доступе");
            return "error";
        }
        // выполняем удаление записей из базы данных
        transController.deleteAfter(new Timestamp(dateAfter.getTime()));

        logger.info(user + " Data base cleared");
        //создаем книгу из файла
        InputStream stream = file.getInputStream();

        Workbook myExcelBook = StreamingReader.builder()
                .rowCacheSize(1000000000)
                .bufferSize(1000000000)
                .open(stream);
        logger.info(user + " Created workbook");
        // для каждого листа
        for (Sheet sheet : myExcelBook) {
            // игнорируем итоговый лист
            if (sheet.getSheetName().equals("Итоговый лист")) {
                break;
            }
            if (sheet.getSheetName().equals("Итоговый")) {
                break;
            }
            logger.info(user + " Created sheet: " + sheet.getSheetName());
            // создаем клиента с именем взятым из названия листа
            client = new Client(sheet.getSheetName());
            NewClientRequest request = new NewClientRequest();
            request.setPib(sheet.getSheetName());
            // создаем новый аккаунт этого клиента, если его не существует
            try {
                accountController.addClient(request);
            } catch (RuntimeException ignored) {}
            //для каждого ряда
            for (Row row : sheet) {
                // первый ряд содержит информацию о валютах
                if (row.getRowNum() == 0) {
                    logger.debug("Created first row");
                    for (Cell cell : row) {
                        if (cell.getColumnIndex() < 2) {
                            continue;
                        }
                        if (cell.getCellType() == CellType.STRING) {
                            int curId = currencyFormatter(cell.getStringCellValue());
                            if (curId > 0) {
                                currencies.add(currencyFormatter(cell.getStringCellValue()));
                                logger.debug("Found currencies: " + currencies);
                            }
                        }
                    }
                    continue;
                }
                // второй ряд игнорируем
                if (row.getRowNum() == 1) continue;
                logger.debug("Created row" + row.getRowNum());
                //обнуляем параметры записи
                amount = 0;
                commission = 0;
                rate = 1;
                transportation = 0;
                isTransaction = false;
                comment = "";
                // для каждой ячейки в ряду
                for (Cell cell : row) {
                    // пустые ячейки игнорируются
                    if (cell == null) continue;
                    // дефолтное значение числовых данных, позже может быть изменено и использовано в качестве значения
                    // одного из параметров записи. Сбрасывается каждую итерацию
                    double value = 0.0;
                    // если тип данных строчный
                    if (cell.getCellType() == CellType.STRING) {
                        // если номер столбца 1-й для одной из валют - это комментарий
                        if (cell.getColumnIndex() % columnPerCurrency == 1) {
                            comment = cell
                                    .getStringCellValue();
                            logger.trace("Found comment: " + comment);
                            continue;
                        } else {
                            //иначе это число со спецсимволом. удаляем его и получаем числовое значение
                            String valueS = cell.getStringCellValue().replace("'", "");
                            try {
                                value = Double.parseDouble(valueS);
                            } catch (NumberFormatException e) {
                                logger.debug("Неверный формат ячейки. Лист: " + sheet.getSheetName() + ",   Ряд: " + row.getRowNum());
                            }
                        }
                    }
                    // если тип данных - формула, получаем кэшированное значение в качестве числа
                    if (cell.getCellType() == CellType.FORMULA) {
                        if (Objects.requireNonNull(cell.getCachedFormulaResultType()) == CellType.NUMERIC) {
                            value = cell.getNumericCellValue();
                        }
                    }
                    // если тип данных числовой
                    if (cell.getCellType() == CellType.NUMERIC) {
                        // если в 1-ом столбце - это дата
                        if (cell.getColumnIndex() == 0) {
                            date = cell.getDateCellValue();
                            logger.trace("Found date: " + date);
                            continue;
                        }

                        // если 1-ом столбце для одной из валют - это числовой комментарий. преобразуем его в строку
                        if (cell.getColumnIndex() % columnPerCurrency == 1) {
                            comment = String.valueOf(cell
                                    .getNumericCellValue());
                            logger.trace("Found comment: " + comment);
                            continue;
                        }
                        // иначе записываем значение этой ячейки
                        value = cell.getNumericCellValue();
                    }
                    // если дата до необходимой - игнорируем строку
                    if (date.before(dateAfter)) break;
                    // если ранее полученное значение не 0
                    if (value != 0) {
                        // если во 2-ом или 3-ем столбце - это объем. также наличие значения в этом столбце говорит о
                        // наличии записи, в принципе
                        if (cell.getColumnIndex() % columnPerCurrency == 2 || cell.getColumnIndex() % columnPerCurrency == 3) {
                            amount = value;
                            logger.trace("Found amount: " + amount);
                                /*XSSFColor c = cell
                                .getCellStyle().getFont().getXSSFColor();
                                if (c != null) {
                                    amountColor = "rgb(" + Byte.toUnsignedInt(c.getRGB()[0]) + "," +
                                            Byte.toUnsignedInt(c.getRGB()[1]) + ","  +
                                            Byte.toUnsignedInt(c.getRGB()[2]) + ")";
                                }
                            logger.trace("Found amountColor: " + amountColor);*/
                            isTransaction = true;
                            continue;
                        }
                        //если в 4-ом столбце - это тариф
                        if (cell.getColumnIndex() % columnPerCurrency == 4) {

                            commission = value;
                            logger.trace("Found commission: " + commission);
                            continue;

                        }
                        // если в 6 столбце - это курс
                        if (cell.getColumnIndex() % columnPerCurrency == 6) {
                            rate = value;
                            logger.trace("Found rate: " + rate);
                            continue;
                        }
                        // если в 7 столбце - это инкасация
                        if (cell.getColumnIndex() % columnPerCurrency == 7) {
                            transportation = value;
                            logger.trace("Found transportation: " + transportation);
                            continue;
                        }
                    }
                    // если это крайний столбец и наличие записи подтверждено - добавляем в базу новую запись и обнуляю
                    // параметры
                    if ((cell.getColumnIndex() % columnPerCurrency == 0) && isTransaction) {
                        logger.info(user + " Inserting transaction on sheet: " + sheet.getSheetName());
                        insertTransaction(row, cell.getColumnIndex(), currencies.get((cell.getColumnIndex() / columnPerCurrency) - 1), user);
                        amount = 0;
                        commission = 0;
                        rate = 1;
                        transportation = 0;
                        isTransaction = false;
                        comment = "";
                    }
                }
            }
            // обнуляем список валют перед переходом на новый лист
            currencies = new ArrayList<>();
        }
            myExcelBook.close();
            logger.info(user + " File decoded and uploaded");
            return "redirect:/client";
    }


    /**
     * Вспомогательный метод, добавляющий запись в базу данных. Использует величины из полей класса и некоторую
     * дополнительную информацию
     *
     * @param row        ряд, содержит информацию о стилях (в данный момент не используется)
     * @param cellNum    номер ячейки содержащей баланс (в данный момент не используется)
     * @param currencyId ИД валюты к которой относится запись
     */
    private void insertTransaction(Row row, int cellNum, int currencyId, User user) {

        //Getting balance color if it exists
        /*XSSFColor c = cell
        .getCellStyle().getFont().getXSSFColor();
        String balanceColor = null;
        if (c != null) {
            balanceColor = "rgb(" + Byte.toUnsignedInt(c.getRGB()[0]) + "," + Byte.toUnsignedInt(c.getRGB()[1]) +
                    ","  + Byte.toUnsignedInt(c.getRGB()[2]) + ")";
            logger.trace("Found balance color: " + balanceColor);
        }*/
        //получаем ИД записи
        int transactionId = getTransactionId();
        try {
            // находим доступное время в эту дату
            date = transController.getAvailableDate(new Timestamp(date.getTime()));
            // добавляем в базу новую запись с заданными параметрами
            Account account = accountController.getAccount(client.getId(), currencyId);
            transController.remittance(transactionId, new Timestamp(date.getTime()), account, comment,
                    rate, commission, amount, transportation, null, amountColor, user.getId(),
                    0.0, null, null, null, null, null,
                    null, null);
        } catch (RuntimeException e) {
            e.printStackTrace();
            //если валюты с указанным ИД не существует - создаем ее и повторяем добавление
            Currency currency = new Currency(currencyId, currencyBackFormatter(currencyId));
            try {
                currencyController.addCurrency(currency);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
            Account account = accountController.getAccount(client.getId(), currencyId);
            transController.remittance(transactionId, new Timestamp(date.getTime()), account, comment,
                    rate, commission, amount, transportation, null, amountColor,
                    user.getId(), 0.0, null, null, null, null, null,
                    null, null);
        }
        //сбрасываем переменные
        amount = 0;
        commission = 0;
        rate = 1;
        transportation = 0;
        isTransaction = false;
        comment = "";

    }

    /**
     * Вспомогательный метод, определяющий доступный ИД записи. Находит существующий максимальный ИД и увеличивает его на 1
     * @return ИД для новой записи
     */
    private int getTransactionId() {
        int transactionId;
        try {
            transactionId = transController.getMaxId() + 1;
            /*transactionId = transController.getAllTransactions()
                    .stream()
                    .flatMapToInt(t -> IntStream.of(t.getId()))
                    .max()
                    .orElse(0) + 1;*/
        } catch (RuntimeException e) {
            //if zero transactions exist in base set id to 1
            transactionId = 1;
        }
        return transactionId;
    }


    /**
     * Вспомогательный метод, преобразующий текстовое название валюты в ее код/ИД
     * @param name название валюты или общепринятая аббревиатура
     * @return общепринятый код или ИД валюты
     */
    private int currencyFormatter(String name) {
        logger.debug("Currency formatting from names to id");
        switch (name) {
            case "Гривна","UAH" -> {return 980;}
            case "Доллар","USD" -> {return 840;}
            case "Евро","EUR" -> {return 978;}
            case "Рубль","RUB" -> {return 643;}
            case "Злотый","PLN" -> {return 985;}
        }
        return -1;
    }

    /**
     * Вспомогательный метод, преобразующий код/ИД валюты в общепринятую аббревиатуру
     * @param id общепринятый код или ИД валюты
     * @return общепринятую аббревиатуру
     */
    private String currencyBackFormatter(int id) {
        logger.debug("Currency formatting from id to names");
        switch (id) {
            case 980 -> {return "UAH";}
            case 840 -> {return "USD";}
            case 978 -> {return "EUR";}
            case 643 -> {return "RUB";}
            case 985 -> {return "PLN";}
        }
        return "";
    }
}
