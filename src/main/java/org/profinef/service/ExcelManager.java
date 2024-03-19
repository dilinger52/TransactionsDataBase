package org.profinef.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.profinef.controller.AccountController;
import org.profinef.controller.ClientController;
import org.profinef.controller.CurrencyController;
import org.profinef.controller.TransController;
import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.entity.Transaction;
import org.profinef.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Класс предназначен для создания эксель файла отражающего данные в БД с заданными параметрами
 */
@Service
public class ExcelManager {
    @Autowired
    private final ClientRepository clientRepository;
    @Autowired
    private final CurrencyController currencyController;
    @Autowired
    private final TransController transController;
    @Autowired
    private final AccountController accountController;
    private final int columnPerCurrency = 8;

    private static final Logger logger = LoggerFactory.getLogger(ExcelManager.class);

    public ExcelManager(ClientRepository clientRepository, CurrencyController currencyController, TransController transController, AccountController accountController) {
        this.clientRepository = clientRepository;
        this.currencyController = currencyController;
        this.transController = transController;
        this.accountController = accountController;
    }

    /**
     * Класс создает Excel файл содержащий информацию обо всех записях в БД
     * @throws IOException выбрасывается при невозможности записать данные в файл
     */
    public void createFull() throws IOException {
        //собираем информацию
        List<Client> clients = clientRepository.findAll();
        List<Currency> currencies = currencyController.getAllCurrencies();
        List<Transaction> transactions = transController.getAllTransactions();
        //создаем книгу
        Workbook book = createWorkbook(clients, currencies, transactions);
        // создаем итоговый лист
        Sheet total = null;
        if (book != null) {
            total = book.createSheet("Итоговый");
        }
        logger.info("Created total sheet");
        // создаем ряд стилей
        CellStyle boldStyle = book.createCellStyle();
        Font bold = book.createFont();
        bold.setBold(true);
        boldStyle.setFont(bold);

        CellStyle redBoldStyle = book.createCellStyle();
        Font redBold = book.createFont();
        redBold.setBold(true);
        redBold.setColor(Font.COLOR_RED);
        redBoldStyle.setFont(redBold);
        redBoldStyle.setBorderTop(BorderStyle.MEDIUM);
        logger.debug("Styles set");

        //создаем первый ряд
        Row firstRow = total.createRow(0);
        Cell firstCell = firstRow.createCell(2);
        firstCell.setCellStyle(boldStyle);
        firstCell.setCellValue("Итоговый лист");
        logger.trace("Created first cell");

        CellStyle dateStyle = book.createCellStyle();
        DataFormat format = book.createDataFormat();
        dateStyle.setDataFormat(format.getFormat("dd.mm.yyyy"));

        // создаем второй ряд
        Row secondRow = total.createRow(1);
        Cell secondCell = secondRow.createCell(2);
        secondCell.setCellStyle(dateStyle);
        secondCell.setCellValue(LocalDate.now());
        total.autoSizeColumn(2);
        logger.trace("Created second cell");

        // создаем третий ряд
        Row thirdRow = total.createRow(2);
        logger.debug("Created third row");

        //делаем заголовки с названиями валют
        int headerCellNum = 0;
        for (Currency currency : currencies) {
            Cell headerCell = thirdRow.createCell(++headerCellNum);
            headerCell.setCellStyle(boldStyle);
            headerCell.setCellValue(currency.getName());
            logger.trace("Created currency names cell with value = " + currency.getName());
        }
        // счетчик ряда начинается с третьего, т.к. увеличение происходит в начале цикла
        int rowNum = 2;
        for (Client client : clients) {
            // создаем ряд для клиента
            Row row = total.createRow(++rowNum);
            // записываем имя
            Cell nameCell = row.createCell(0);
            nameCell.setCellStyle(boldStyle);
            nameCell.setCellValue(client.getPib());
            logger.trace("Created client cell with value = " + client.getPib());
            int cellNum = 0;
            List<Account> accounts = accountController.getAccountsExactly(client.getPib());
            // записываем балансы по каждой из валют
            for (Account account : accounts) {
                Cell cell = row.createCell(++cellNum);
                cell.setCellStyle(boldStyle);
                cell.setCellValue(account.getBalance());
                logger.trace("Created currency cell with value = " + account.getBalance());
            }
        }

        // создаем итоговый ряд
        Row totalRow = total.createRow(++rowNum);
        logger.debug("Created row total");
        // записывае имя
        Cell nameTotalCell = totalRow.createCell(0);
        nameTotalCell.setCellStyle(redBoldStyle);
        nameTotalCell.setCellValue("Итог");
        logger.trace("Created cell total names");

        // записываем суммы балансов по всем клиентам для каждой из валют
        List<Account> accounts = accountController.getAllAccounts();
        List<Account> totalAccounts = new ArrayList<>();
        for (Currency currency : currencies) {
            Account totalAc = new Account(new Client("Итого"), currency);
            double sum = 0;
            for (Account account : accounts) {
                if (account.getCurrency().equals(currency)) {
                    sum += account.getBalance();
                }
            }
            totalAc.setBalance(sum);
            totalAccounts.add(totalAc);
        }

        int totalCellNum = 0;
        for (Account totalAc : totalAccounts) {
            Cell totalCell = totalRow.createCell(++totalCellNum);
            totalCell.setCellStyle(redBoldStyle);
            totalCell.setCellValue(totalAc.getBalance());
            logger.trace("Created cell total with value = " + totalAc.getBalance());
        }
        // Записываем всё в файл
        book.write(new FileOutputStream("excel/info.xlsx"));
        book.close();
        logger.info("File created");
    }

    /**
     * Метод создания книги с заданной информацией
     * @param clients список клиентов
     * @param currencies список валют
     * @param trans список записей
     * @return созданную книгу Excel
     */
    private Workbook createWorkbook(List<Client> clients, List<Currency> currencies, List<Transaction> trans) {
        try {
            // создаем книгу
            Workbook book = new XSSFWorkbook();
            logger.info("Work book created");

            // создаем стиль для строки заголовка
            CellStyle headerStyle = book.createCellStyle();
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);
            headerStyle.setBorderLeft(BorderStyle.MEDIUM);
            headerStyle.setBorderTop(BorderStyle.MEDIUM);
            headerStyle.setBorderRight(BorderStyle.MEDIUM);
            Font headerFont = book.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // создаем стиль для ячеек с датой
            CellStyle dateStyle = book.createCellStyle();
            DataFormat format = book.createDataFormat();
            dateStyle.setDataFormat(format.getFormat("dd.mm.yyyy"));

            // создаем стиль для ячеек имени клиента
            CellStyle nameStyle = book.createCellStyle();
            Font font = book.createFont();
            font.setItalic(true);
            nameStyle.setFont(font);
            nameStyle.setBorderBottom(BorderStyle.MEDIUM);
            nameStyle.setBorderLeft(BorderStyle.MEDIUM);
            nameStyle.setBorderTop(BorderStyle.MEDIUM);
            nameStyle.setBorderRight(BorderStyle.MEDIUM);

            //создаем стиль для последнего ряда для каждой даты
            CellStyle lastForDateStyle = book.createCellStyle();
            lastForDateStyle.setBorderBottom(BorderStyle.MEDIUM);

            // создаем стиль для ячеек даты в последнем ряду для каждой даты
            CellStyle dateLastForDateStyle = book.createCellStyle();
            dateLastForDateStyle.setBorderBottom(BorderStyle.MEDIUM);
            dateLastForDateStyle.setDataFormat(format.getFormat("dd.mm.yyyy"));

            // стиль ячеек комментария
            CellStyle commentStyle = book.createCellStyle();
            commentStyle.setBorderLeft(BorderStyle.THICK);

            // стиль ячеек комментария в последнем ряду даты
            CellStyle commentLastForDateStyle = book.createCellStyle();
            commentLastForDateStyle.setBorderLeft(BorderStyle.THICK);
            commentLastForDateStyle.setBorderBottom(BorderStyle.MEDIUM);

            // стиль для больших чисел
            CellStyle bigNumberStyle = book.createCellStyle();
            bigNumberStyle.setDataFormat(format.getFormat("# ### ### ##0"));

            // стиль для больших чисел в последнем ряду даты
            CellStyle bigNumberLastForDateStyle = book.createCellStyle();
            bigNumberLastForDateStyle.setDataFormat(format.getFormat("# ### ### ##0"));
            bigNumberLastForDateStyle.setBorderBottom(BorderStyle.MEDIUM);

            // стиль для малых чисел
            CellStyle smallNumberStyle = book.createCellStyle();
            smallNumberStyle.setDataFormat(format.getFormat("# ##0.##"));

            // стиль для малых чисел в последнем ряду даты
            CellStyle smallNumberLastForDateStyle = book.createCellStyle();
            smallNumberLastForDateStyle.setDataFormat(format.getFormat("# ##0.##"));
            smallNumberLastForDateStyle.setBorderBottom(BorderStyle.MEDIUM);


            for (Client client : clients) {
                // для каждого клиента создаем лист. название листа - это имя клиента
                Sheet sheet = book.createSheet(client.getPib());
                logger.info("Created sheet: " + sheet.getSheetName());
                // создаем первый ряд
                Row fistRow = sheet.createRow(0);
                fistRow.setRowStyle(headerStyle);
                logger.info("Created first row");
                // создаем второй ряд
                Row secondRow = sheet.createRow(1);
                secondRow.setRowStyle(headerStyle);
                logger.info("Created second row");
                // записываем имя клиента в соответствующую ячейку
                Cell nameCell = fistRow.createCell(0);
                nameCell.setCellValue(client.getPib());
                nameCell.setCellStyle(nameStyle);
                logger.trace("Created names cell");
                // записываем заголовок "Дата"
                secondRow.createCell(0).setCellValue("Дата");
                logger.trace("Created date cell");
                // начальный номер валюты. равен -1, т.к. увеличение происходит в начале цикла
                int initial = -1;
                for (Currency currency : currencies) {
                    // записываем название валюты
                    fistRow.createCell(2 + columnPerCurrency * ++initial).setCellValue(currency.getName());
                    for (int i = 2; i < fistRow.getLastCellNum() - 1; i += columnPerCurrency) {
                        fistRow.getCell(i).setCellStyle(headerStyle);
                    }

                    // записываем заголовки для записей данной валюты
                    secondRow.createCell(1 + columnPerCurrency * initial).setCellValue("Комментарий");
                    secondRow.createCell(2 + columnPerCurrency * initial).setCellValue("Прием");
                    secondRow.createCell(3 + columnPerCurrency * initial).setCellValue("Выдача");
                    secondRow.createCell(4 + columnPerCurrency * initial).setCellValue("Тариф");
                    secondRow.createCell(5 + columnPerCurrency * initial).setCellValue("Комис");
                    secondRow.createCell(6 + columnPerCurrency * initial).setCellValue("Курс");
                    secondRow.createCell(7 + columnPerCurrency * initial).setCellValue("Инкас");
                    secondRow.createCell(8 + columnPerCurrency * initial).setCellValue("Итого");

                    for (int i = 0; i < secondRow.getLastCellNum() - 1; i++) {
                        secondRow.getCell(i).setCellStyle(headerStyle);
                    }
                    logger.trace("Created cells for currency names: " + currency.getName());
                    // выбираем записи, относящиеся к текущему клиенту и валюте
                    List<Transaction> transactions = trans.stream().filter(t -> t.getAccount().getClient().equals(client)).filter(t -> t.getAccount().getCurrency().equals(currency)).toList();
                    // начальный номер ряда, соответствует второму, т.к. увеличение происходит в начале цикла
                    int rowNum = 1;
                    for (Transaction transaction : transactions) {
                        // переходим на новый ряд, если его нет - создаем
                        Row row = sheet.getRow(++rowNum);
                        if (row == null) {
                            row = sheet.createRow(rowNum);

                        }

                        if (row.getCell(0) != null) {
                            // если дата в ряду раньше даты записи - переходим на ряд ниже, при необходимости создаем его
                            while (row.getCell(0) != null && row.getCell(0).getDateCellValue().toInstant()
                                    .atZone(ZoneId.of("Europe/Kyiv")).toLocalDate().atStartOfDay()
                                    .isBefore(transaction.getDate().toInstant().atZone(ZoneId.of("Europe/Kyiv"))
                                            .toLocalDate().atStartOfDay())) {
                                row = sheet.getRow(++rowNum);
                                if (row == null) {
                                    row = sheet.createRow(rowNum);
                                }

                            }
                            // если дата в ряду больше даты текущей записи - встраиваем новый ряд выше
                            if (row.getCell(0) != null && row.getCell(0).getDateCellValue().toInstant()
                                    .atZone(ZoneId.of("Europe/Kyiv")).toLocalDate().atStartOfDay()
                                    .isAfter(transaction.getDate().toInstant()
                                            .atZone(ZoneId.of("Europe/Kyiv")).toLocalDate().atStartOfDay())) {
                                sheet.shiftRows(rowNum, sheet.getLastRowNum(), 1);
                                row = sheet.getRow(rowNum);
                                if (row == null) {
                                    row = sheet.createRow(rowNum);
                                }
                            }
                        }
                        // если в ряду нет записи даты - вписываем дату текущей запсиси
                        if (row.getCell(0) == null) {
                            Cell cell = row.createCell(0);
                            cell.setCellValue(transaction.getDate());
                            cell.setCellStyle(dateStyle);
                            sheet.autoSizeColumn(0);
                            logger.trace("Created date cell with value = " + transaction.getDate());
                        }

                        // формируем строку комментария, выписывая имена контрагентов и комментарий и добавляем в
                        // соответствующую ячейку
                        List<Transaction> tl = transController.getTransaction(transaction.getId());
                        StringBuilder anotherClients = new StringBuilder();
                        for (Transaction t : tl) {
                            if (!t.getAccount().getClient().getId().equals(transaction.getAccount().getClient().getId())) {
                                anotherClients.append(" ");
                                anotherClients.append(t.getAccount().getClient().getPib());
                            }
                        }
                        anotherClients.append(" ").append(transaction.getComment());
                        Cell clientsCell = row.createCell(1 + columnPerCurrency * initial);
                        clientsCell.setCellValue(anotherClients.toString());
                        clientsCell.setCellStyle(commentStyle);
                        sheet.autoSizeColumn(1 + columnPerCurrency * initial);
                        logger.trace("Created clients cell with value = " + anotherClients);

                        /*if (transaction.getCommentColor() != null && !transaction.getCommentColor().isEmpty()) {
                            CellStyle clientsStyle = book.createCellStyle();
                            XSSFFont clientsFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getCommentColor().split(";");
                            for (String sample : styleSample) {
                                System.out.println(sample);
                                if (sample.matches("^font-weight: bold")) clientsFont.setBold(true);
                                if (sample.matches("^font-style: italic")) clientsFont.setItalic(true);
                                if (sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    clientsFont.setColor(new XSSFColor(color, null));
                                }
                            }
                            clientsCell.setCellStyle(clientsStyle);
                            sheet.autoSizeColumn(1);
                        }*/
                        // вписываем объем записи в соответствующую ячейку
                        Cell amountCell;
                        if (transaction.getAmount() >= 0) {
                            amountCell = row.createCell(2 + columnPerCurrency * initial);
                        } else {
                            amountCell = row.createCell(3 + columnPerCurrency * initial);
                        }
                        amountCell.setCellValue(transaction.getAmount());
                        amountCell.setCellStyle(bigNumberStyle);
                        sheet.autoSizeColumn(2 + columnPerCurrency * initial);
                        logger.trace("Created amount cell with value = " + transaction.getAmount());
                        /*if (transaction.getAmountColor() != null && !transaction.getAmountColor().isEmpty()) {
                            CellStyle amountStyle = book.createCellStyle();
                            XSSFFont amountFont = (XSSFFont) book.createFont();
                            String[] styleSample;
                            if (transaction.getAmount() >= 0) {
                                styleSample = transaction.getInputColor().split(";");
                            } else {
                                styleSample = transaction.getOutputColor().split(";");
                            }
                            for (String sample : styleSample) {
                                System.out.println(sample);
                                if (sample.matches("^font-weight: bold")) amountFont.setBold(true);
                                if (sample.matches("^font-style: italic")) amountFont.setItalic(true);
                                if (sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    amountFont.setColor(new XSSFColor(color, null));
                                }
                            }
                            amountStyle.setFont(amountFont);
                            amountCell.setCellStyle(amountStyle);
                            sheet.autoSizeColumn(1);
                        }*/
                        // вписываем тариф в соответствующую ячейку
                        Cell commissionCell = row.createCell(4 + columnPerCurrency * initial);
                        commissionCell.setCellValue(transaction.getCommission());
                        commissionCell.setCellStyle(smallNumberStyle);
                        sheet.autoSizeColumn(4 + columnPerCurrency * initial);
                        logger.trace("Created tariff cell with value = " + transaction.getCommission());

                        /*if (transaction.getTarifColor() != null && !transaction.getTarifColor().isEmpty()) {
                            CellStyle commissionStyle = book.createCellStyle();
                            XSSFFont commissionFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getTarifColor().split(";");
                            for (String sample : styleSample) {
                                System.out.println(sample);
                                if (sample.matches("^font-weight: bold")) commissionFont.setBold(true);
                                if (sample.matches("^font-style: italic")) commissionFont.setItalic(true);
                                if (sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    commissionFont.setColor(new XSSFColor(color, null));
                                }
                            }
                            commissionStyle.setFont(commissionFont);
                            clientsCell.setCellStyle(commissionStyle);
                            sheet.autoSizeColumn(1);
                        }*/
                        // вписываем комиссию в соответствующую ячейку
                        Cell comCell = row.createCell(5 + columnPerCurrency * initial);
                        comCell.setCellValue(Math.abs(transaction.getCommission() / 100 * transaction.getAmount()));
                        comCell.setCellStyle(bigNumberStyle);
                        sheet.autoSizeColumn(5 + columnPerCurrency * initial);
                        logger.trace("Created commission cell with value = " +
                                Math.abs(transaction.getCommission() / 100 * transaction.getAmount()));

                        /*if (transaction.getCommissionColor() != null && !transaction.getCommissionColor().isEmpty()) {
                            CellStyle comStyle = book.createCellStyle();
                            XSSFFont comFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getCommissionColor().split(";");
                            for (String sample : styleSample) {
                                System.out.println(sample);
                                if (sample.matches("^font-weight: bold")) comFont.setBold(true);
                                if (sample.matches("^font-style: italic")) comFont.setItalic(true);
                                if (sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    comFont.setColor(new XSSFColor(color, null));
                                }
                            }
                            comStyle.setFont(comFont);
                            comCell.setCellStyle(comStyle);
                            sheet.autoSizeColumn(1);
                        }*/
                        // вписываем курс в соответствующую ячейку
                        Cell rateCell = row.createCell(6 + columnPerCurrency * initial);
                        rateCell.setCellValue(transaction.getRate());
                        rateCell.setCellStyle(smallNumberStyle);
                        sheet.autoSizeColumn(6 + columnPerCurrency * initial);
                        logger.trace("Created rate cell with value = " + transaction.getRate());

                        /*if (transaction.getRateColor() != null && !transaction.getRateColor().isEmpty()) {
                            CellStyle rateStyle = book.createCellStyle();
                            XSSFFont rateFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getRateColor().split(";");
                            for (String sample : styleSample) {
                                System.out.println(sample);
                                if (sample.matches("^font-weight: bold")) rateFont.setBold(true);
                                if (sample.matches("^font-style: italic")) rateFont.setItalic(true);
                                if (sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    rateFont.setColor(new XSSFColor(color, null));
                                }
                            }
                            rateStyle.setFont(rateFont);
                            rateCell.setCellStyle(rateStyle);
                            sheet.autoSizeColumn(1);
                        }*/
                        // вписываем инкасацию в соответствующую ячейку
                        Cell transportationCell = row.createCell(7 + columnPerCurrency * initial);
                        transportationCell.setCellValue(transaction.getTransportation());
                        transportationCell.setCellStyle(bigNumberStyle);
                        sheet.autoSizeColumn(7 + columnPerCurrency * initial);
                        logger.trace("Created transportation cell with value = " + transaction.getTransportation());

                        /*if (transaction.getTransportationColor() != null && !transaction.getTransportationColor().isEmpty()) {
                            CellStyle transportationStyle = book.createCellStyle();
                            XSSFFont transportationFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getTransportationColor().split(";");
                            for (String sample : styleSample) {
                                System.out.println(sample);
                                if (sample.matches("^font-weight: bold")) transportationFont.setBold(true);
                                if (sample.matches("^font-style: italic")) transportationFont.setItalic(true);
                                if (sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    transportationFont.setColor(new XSSFColor(color, null));
                                }
                            }

                            transportationStyle.setFont(transportationFont);
                            transportationCell.setCellStyle(transportationStyle);
                            sheet.autoSizeColumn(1);
                        }*/
                        // вписываем баланс в соответствующую ячейку
                        Cell balanceCell = row.createCell(8 + columnPerCurrency * initial);
                        balanceCell.setCellValue(transaction.getAmount() * (1 + transaction.getCommission() / 100) + transaction.getTransportation());
                        balanceCell.setCellStyle(bigNumberStyle);
                        sheet.autoSizeColumn(8 + columnPerCurrency * initial);
                        logger.trace("Created balance cell with value = " + transaction.getBalance());

                        /*if (transaction.getAmountColor() != null && !transaction.getAmountColor().isEmpty()) {
                            CellStyle balanceStyle = book.createCellStyle();
                            XSSFFont balanceFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getAmountColor().split(";");
                            for (String sample : styleSample) {
                                System.out.println(sample);
                                if (sample.matches("^font-weight: bold")) balanceFont.setBold(true);
                                if (sample.matches("^font-style: italic")) balanceFont.setItalic(true);
                                if (sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    balanceFont.setColor(new XSSFColor(color, null));
                                }
                            }
                            balanceStyle.setFont(balanceFont);
                            balanceCell.setCellStyle(balanceStyle);
                            sheet.autoSizeColumn(1);
                        }*/
                        logger.info("Created row" + rowNum + " on sheet " + sheet.getSheetName() + ", currency " + currency.getName() + ", amount " + transaction.getAmount());
                    }
                }
                // применяем стили к последним строка каждой даты. это необходимо выполнять в конце, т.к. последняя
                // строка для даты может измениться в процессе формирования листа
                for (Row row : sheet) {

                    if (row.getRowNum() < 2) continue;
                    for (int i = 0; i < columnPerCurrency * 5; i++) {
                        Cell cell = row.getCell(i);
                        if (cell == null) {
                            cell = row.createCell(i);
                        }
                        if (cell.getColumnIndex() % columnPerCurrency == 1) {
                            CellStyle style = cell.getCellStyle();
                            style.setBorderLeft(BorderStyle.MEDIUM);
                            cell.setCellStyle(style);
                        }
                    }
                    if (row.getRowNum() < 3) continue;
                    if (row.getCell(0).getDateCellValue().toInstant().atZone(ZoneId.of("Europe/Kyiv"))
                            .toLocalDate().atStartOfDay().isAfter(
                                    sheet.getRow(row.getRowNum() - 1).getCell(0).getDateCellValue().toInstant()
                                            .atZone(ZoneId.of("Europe/Kyiv")).toLocalDate().atStartOfDay())) {
                        Row row1 = sheet.getRow(row.getRowNum() - 1);
                        row1.setRowStyle(lastForDateStyle);
                        for (Cell cell : row1) {
                            if (cell.getColumnIndex() == 0) {
                                cell.setCellStyle(dateLastForDateStyle);
                            } else if (cell.getColumnIndex() % columnPerCurrency == 1){
                                cell.setCellStyle(commentLastForDateStyle);
                            } else if (cell.getColumnIndex() % columnPerCurrency == 2
                            || cell.getColumnIndex() % columnPerCurrency == 3
                            || cell.getColumnIndex() % columnPerCurrency == 5
                            || cell.getColumnIndex() % columnPerCurrency == 7
                            || cell.getColumnIndex() % columnPerCurrency == 0) {
                                cell.setCellStyle(bigNumberLastForDateStyle);
                            } else if (cell.getColumnIndex() % columnPerCurrency == 4
                            || cell.getColumnIndex() % columnPerCurrency == 6) {
                                cell.setCellStyle(smallNumberLastForDateStyle);
                            }

                        }
                    }

                }

            }
            return book;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
