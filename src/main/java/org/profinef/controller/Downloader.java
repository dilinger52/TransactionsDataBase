package org.profinef.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.entity.Transaction;
import org.profinef.entity.User;
import org.profinef.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.*;
import java.net.URLConnection;
import java.time.ZoneId;
import java.util.*;

/**
 * Этот класс используется для скачивания базы данных в формате office open xml excel-файла
 */
@Controller
@RequestMapping(path = "/download")
public class Downloader {

    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final CurrencyManager currencyManager;
    @Autowired
    private final TransManager transManager;
    @Autowired
    private final AccountManager accountManager;
    @Autowired
    private final ExcelManager excelManager;
    private final int columnPerCurrency = 8;

    private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

    public Downloader(ClientManager clientManager, CurrencyManager currencyManager, TransManager transManager,
                      AccountManager accountManager, ExcelManager excelManager) {
        this.clientManager = clientManager;
        this.currencyManager = currencyManager;
        this.transManager = transManager;
        this.accountManager = accountManager;
        this.excelManager = excelManager;
    }

    /**
     * Метод реализует скачивание файла Excel с теми данными, которые были выведены на странице клиента. Таким образом
     * фильтрация объема информации, необходимой для наличия в данном файле осуществляется теми же системами и средствами,
     * что и для фильтрации этого контента на самой странице
     * @param response объект ответа сервера для отправки соответсвующего файла
     * @param session объект сессии, содержащий информацию о примененнных фильтрах
     */
    @RequestMapping(path = "/client_info")
    public void downloadClientInfo(HttpServletResponse response, HttpSession session) {
        User user = (User) session.getAttribute("user");
        logger.info(user + " Downloading excel workbook...");
        try {
            // получение списка клиентов, для которых будет отобрана информация
            List<Client> clients = new ArrayList<>();
            clients.add((Client) session.getAttribute("client"));
            // получение списка валют, для которых будет отобрана информация
            List<Currency> currencies = (List<Currency>) session.getAttribute("currencies");
            // получение списка записей, для которых будет отобрана информация
            List<Transaction> transactions = ((List<Transaction>) session.getAttribute("transactions"));

            // создаем объект книги с указанными параметрами
            Workbook book = createWorkbook(clients, currencies, transactions);

            // Записываем всё в файл
            book.write(new FileOutputStream("excel/info.xlsx"));
            book.close();
            logger.info(user + " File created");
            // отправляем файл пользователю
            sendFile(response, "excel/info.xlsx");
            logger.info(user + " File sent");
        } catch (Exception e) {
            logger.error(Arrays.toString(new String[]{e.getMessage() + Arrays.toString(e.getStackTrace())}));
        }
    }

    /**
     * Метод созданий объекта книги Excel с информацией отобранной по заданным параметрам
     * @param clients список клиентов, для которых будет отобрана информация
     * @param currencies список валют, для которых будет отобрана информация
     * @param trans список записей, для которых будет отобрана информация
     * @return
     */
    private Workbook createWorkbook(List<Client> clients, List<Currency> currencies, List<Transaction> trans) {

            //создание книги
            Workbook book = new XSSFWorkbook();
            logger.info("Work book created");

            //создание стиля строк заголовков
            CellStyle headerStyle = book.createCellStyle();
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);
            headerStyle.setBorderLeft(BorderStyle.MEDIUM);
            headerStyle.setBorderTop(BorderStyle.MEDIUM);
            headerStyle.setBorderRight(BorderStyle.MEDIUM);
            Font headerFont = book.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            //создание стиля для ячеек с датами
            CellStyle dateStyle = book.createCellStyle();
            DataFormat format = book.createDataFormat();
            dateStyle.setDataFormat(format.getFormat("dd.mm.yyyy"));

            //создание стиля для ячеек с именем клиента
            CellStyle nameStyle = book.createCellStyle();
            Font font = book.createFont();
            font.setItalic(true);
            nameStyle.setFont(font);
            nameStyle.setBorderBottom(BorderStyle.MEDIUM);
            nameStyle.setBorderLeft(BorderStyle.MEDIUM);
            nameStyle.setBorderTop(BorderStyle.MEDIUM);
            nameStyle.setBorderRight(BorderStyle.MEDIUM);

            //создание стиля для последней строки для каждой даты
            CellStyle lastForDateStyle = book.createCellStyle();
            lastForDateStyle.setBorderBottom(BorderStyle.MEDIUM);

            //создание стиля для ячейки даты последней для каждой даты
            CellStyle dateLastForDateStyle = book.createCellStyle();
            dateLastForDateStyle.setBorderBottom(BorderStyle.MEDIUM);
            dateLastForDateStyle.setDataFormat(format.getFormat("dd.mm.yyyy"));


            for (Client client : clients) {
                //для каждого клиента создаем лист. имя листа является именем клиента
                Sheet sheet = book.createSheet(client.getPib());
                logger.info("Created sheet: " + sheet.getSheetName());
                //создаем первую строку заголовка
                Row fistRow = sheet.createRow(0);
                fistRow.setRowStyle(headerStyle);
                logger.info("Created first row");
                //создаем вторую строку заголовка
                Row secondRow = sheet.createRow(1);
                secondRow.setRowStyle(headerStyle);
                logger.info("Created second row");
                //записываем имя клиента в ячейку имени
                Cell nameCell = fistRow.createCell(0);
                nameCell.setCellValue(client.getPib());
                nameCell.setCellStyle(nameStyle);
                logger.trace("Created names cell");
                //создаем заголовок столбца даты
                secondRow.createCell(0).setCellValue("Дата");
                logger.trace("Created date cell");
                // счетчик валют, для записи информациии о каждой новой валюте в более дальние столбцы. изначально
                // отрицательный, т.к. увеличение происходит в начале цикла
                int initial = -1;
                // для каждой валюты
                for (Currency currency : currencies) {
                    // создаем заголовок с названием валюты
                    fistRow.createCell(2 + columnPerCurrency * ++initial).setCellValue(currency.getName());
                    for (int i = 2; i < fistRow.getLastCellNum() - 1; i += columnPerCurrency) {
                        fistRow.getCell(i).setCellStyle(headerStyle);
                    }
                    // создаем загловки соответсвующих столбцов
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

                    // из списка записей отбираем те, что относятся к текущей валюте
                    List<Transaction> transactions = trans.stream().filter(t -> t.getClient().equals(client)).filter(t -> t.getCurrency().equals(currency)).toList();
                    // начальный номер столбца (соответсвует второму столбцу, т.к. увеличение происходит в начале цикла)
                    int rowNum = 1;
                    // для каждой записи
                    for (Transaction transaction : transactions) {
                        // устананавливаем в качестве текущего ряда ряд с необходимым порядковым номером, либо создаем
                        // новый, если он не существует
                        Row row = sheet.getRow(++rowNum);
                        if (row == null) {
                            row = sheet.createRow(rowNum);

                        }

                        if (row.getCell(0) != null) {
                            // если имеющаяся дата в ряду раньше чем дата записи - опускаемся на ряд ниже и повторяем проверку
                            while (row.getCell(0) != null && row.getCell(0).getDateCellValue().toInstant()
                                    .atZone(ZoneId.of("Europe/Kyiv")).toLocalDate().atStartOfDay()
                                    .isBefore(transaction.getDate().toInstant().atZone(ZoneId.of("Europe/Kyiv"))
                                            .toLocalDate().atStartOfDay())) {
                                row = sheet.getRow(++rowNum);
                                if (row == null) {
                                    row = sheet.createRow(rowNum);
                                }

                            }
                            // если имеющаяся дата в ряду позже чем дата записи - встраиваем новый ряд выше
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
                        // если в ячейке даты нет значения - вписываем дату текущей записи
                        if (row.getCell(0) == null) {
                            Cell cell = row.createCell(0);
                            cell.setCellValue(transaction.getDate());
                            cell.setCellStyle(dateStyle);
                            sheet.autoSizeColumn(0);
                            logger.trace("Created date cell with value = " + transaction.getDate());
                        }

                        // находим все связанные записи
                        List<Transaction> tl = transManager.getTransaction(transaction.getId());
                        // получаем имена контрагентов, и записываем в строку
                        StringBuilder anotherClients = new StringBuilder();
                        for (Transaction t : tl) {
                            if (!t.getClient().getId().equals(transaction.getClient().getId())) {
                                anotherClients.append(" ");
                                anotherClients.append(t.getClient().getPib());
                            }
                        }
                        // добавляем комментарий
                        anotherClients.append(" ").append(transaction.getComment());
                        // создаем ячейку комментария содержащую собранную строку
                        Cell clientsCell = row.createCell(1 + columnPerCurrency * initial);
                        clientsCell.setCellValue(anotherClients.toString());
                        CellStyle clientsCellStyle = book.createCellStyle();
                        clientsCellStyle.cloneStyleFrom(clientsCell.getCellStyle());
                        clientsCellStyle.setBorderLeft(BorderStyle.MEDIUM);
                        clientsCell.setCellStyle(clientsCellStyle);
                        sheet.autoSizeColumn(1 + columnPerCurrency * initial);
                        logger.trace("Created clients cell with value = " + anotherClients);

                        // если в базе имеются стили для данной ячейки - применяем их
                        if (transaction.getCommentColor() != null && !transaction.getCommentColor().isEmpty()) {
                            CellStyle clientsStyle = book.createCellStyle();
                            XSSFFont clientsFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getCommentColor().split(";");
                            for (String sample : styleSample) {
                                if(sample.matches("^font-weight: bold")) clientsFont.setBold(true);
                                if(sample.matches("^font-style: italic")) clientsFont.setItalic(true);
                                if(sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    clientsFont.setColor(new XSSFColor(color, null));
                                }
                            }
                            clientsCell.setCellStyle(clientsStyle);
                            sheet.autoSizeColumn(1);
                        }
                        // объем записи вписываем в ячейку "приема" или "выдачи" в зависимости от знака
                        Cell amountCell;
                        if (transaction.getAmount() >= 0) {
                            amountCell = row.createCell(2 + columnPerCurrency * initial);
                        } else {
                            amountCell = row.createCell(3 + columnPerCurrency * initial);
                        }
                        amountCell.setCellValue(transaction.getAmount());
                        CellStyle amountCellStyle = book.createCellStyle();
                        amountCellStyle.cloneStyleFrom(amountCell.getCellStyle());
                        amountCellStyle.setDataFormat(format.getFormat("# ##0"));
                        amountCell.setCellStyle(amountCellStyle);
                        sheet.autoSizeColumn(2 + columnPerCurrency * initial);
                        logger.trace("Created amount cell with value = " + transaction.getAmount());
                        // если в базе имеются стили для данной ячейки - применяем их
                        if (transaction.getAmountColor() != null && !transaction.getAmountColor().isEmpty()) {
                            CellStyle amountStyle = book.createCellStyle();
                            XSSFFont amountFont = (XSSFFont) book.createFont();
                            String[] styleSample;
                            if (transaction.getAmount() >= 0) {
                                styleSample = transaction.getInputColor().split(";");
                            } else {
                                styleSample = transaction.getOutputColor().split(";");
                            }
                            for (String sample : styleSample) {
                                if(sample.matches("^font-weight: bold")) amountFont.setBold(true);
                                if(sample.matches("^font-style: italic")) amountFont.setItalic(true);
                                if(sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    amountFont.setColor(new XSSFColor(color, null));
                                }
                            }
                            amountStyle.setFont(amountFont);
                            amountCell.setCellStyle(amountStyle);
                            sheet.autoSizeColumn(1);
                        }

                        // создаем ячейку тарифа
                        Cell commissionCell = row.createCell(4 + columnPerCurrency * initial);
                        commissionCell.setCellValue(transaction.getCommission());
                        CellStyle commissionCellStyle = book.createCellStyle();
                        commissionCellStyle.cloneStyleFrom(commissionCell.getCellStyle());
                        commissionCellStyle.setDataFormat(format.getFormat("# ##0.##"));
                        commissionCell.setCellStyle(commissionCellStyle);
                        sheet.autoSizeColumn(4 + columnPerCurrency * initial);
                        logger.trace("Created tariff cell with value = " + transaction.getCommission());
                        // если в базе имеются стили для данной ячейки - применяем их
                        if (transaction.getTarifColor() != null && !transaction.getTarifColor().isEmpty()) {
                            CellStyle commissionStyle = book.createCellStyle();
                            XSSFFont commissionFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getTarifColor().split(";");
                            for (String sample : styleSample) {
                                if(sample.matches("^font-weight: bold")) commissionFont.setBold(true);
                                if(sample.matches("^font-style: italic")) commissionFont.setItalic(true);
                                if(sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    commissionFont.setColor(new XSSFColor(color, null));
                                }
                            }
                            commissionStyle.setFont(commissionFont);
                            clientsCell.setCellStyle(commissionStyle);
                            sheet.autoSizeColumn(1);
                        }
                        // создаем ячейку коммиссии
                        Cell comCell = row.createCell(5 + columnPerCurrency * initial);
                        comCell.setCellValue(Math.abs(transaction.getCommission() / 100 * transaction.getAmount()));
                        CellStyle comCellStyle = book.createCellStyle();
                        comCellStyle.cloneStyleFrom(comCell.getCellStyle());
                        comCellStyle.setDataFormat(format.getFormat("# ##0"));
                        comCell.setCellStyle(comCellStyle);
                        sheet.autoSizeColumn(5 + columnPerCurrency * initial);
                        logger.trace("Created commission cell with value = " +
                                Math.abs(transaction.getCommission() / 100 * transaction.getAmount()));
                        // если в базе имеются стили для данной ячейки - применяем их
                        if (transaction.getCommissionColor() != null && !transaction.getCommissionColor().isEmpty()) {
                            CellStyle comStyle = book.createCellStyle();
                            XSSFFont comFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getCommissionColor().split(";");
                            for (String sample : styleSample) {
                                if(sample.matches("^font-weight: bold")) comFont.setBold(true);
                                if(sample.matches("^font-style: italic")) comFont.setItalic(true);
                                if(sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    comFont.setColor(new XSSFColor(color, null));
                                }
                            }
                            comStyle.setFont(comFont);
                            comCell.setCellStyle(comStyle);
                            sheet.autoSizeColumn(1);
                        }
                        // создаем ячейку курса
                        Cell rateCell = row.createCell(6 + columnPerCurrency * initial);
                        rateCell.setCellValue(transaction.getRate());
                        CellStyle rateCellStyle = book.createCellStyle();
                        rateCellStyle.cloneStyleFrom(rateCell.getCellStyle());
                        rateCellStyle.setDataFormat(format.getFormat("# ##0.##"));
                        rateCell.setCellStyle(rateCellStyle);
                        sheet.autoSizeColumn(6 + columnPerCurrency * initial);
                        logger.trace("Created rate cell with value = " + transaction.getRate());
                        // если в базе имеются стили для данной ячейки - применяем их
                        if (transaction.getRateColor() != null && !transaction.getRateColor().isEmpty()) {
                            CellStyle rateStyle = book.createCellStyle();
                            XSSFFont rateFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getRateColor().split(";");
                            for (String sample : styleSample) {
                                if(sample.matches("^font-weight: bold")) rateFont.setBold(true);
                                if(sample.matches("^font-style: italic")) rateFont.setItalic(true);
                                if(sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    rateFont.setColor(new XSSFColor(color, null));
                                }
                            }
                            rateStyle.setFont(rateFont);
                            rateCell.setCellStyle(rateStyle);
                            sheet.autoSizeColumn(1);
                        }
                        // создаем ячейку инкасации
                        Cell transportationCell = row.createCell(7 + columnPerCurrency * initial);
                        transportationCell.setCellValue(transaction.getTransportation());
                        CellStyle transportationCellStyle = book.createCellStyle();
                        transportationCellStyle.cloneStyleFrom(transportationCell.getCellStyle());
                        transportationCellStyle.setDataFormat(format.getFormat("# ##0"));
                        transportationCell.setCellStyle(transportationCellStyle);
                        sheet.autoSizeColumn(7 + columnPerCurrency * initial);
                        logger.trace("Created transportation cell with value = " + transaction.getTransportation());
                        // если в базе имеются стили для данной ячейки - применяем их
                        if (transaction.getTransportationColor() != null && !transaction.getTransportationColor().isEmpty()) {
                            CellStyle transportationStyle = book.createCellStyle();
                            XSSFFont transportationFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getTransportationColor().split(";");
                            for (String sample : styleSample) {
                                if(sample.matches("^font-weight: bold")) transportationFont.setBold(true);
                                if(sample.matches("^font-style: italic")) transportationFont.setItalic(true);
                                if(sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    transportationFont.setColor(new XSSFColor(color, null));
                                }
                            }

                            transportationStyle.setFont(transportationFont);
                            transportationCell.setCellStyle(transportationStyle);
                            sheet.autoSizeColumn(1);
                        }
                        // создаем ячейку баланса
                        Cell balanceCell = row.createCell(8 + columnPerCurrency * initial);
                        balanceCell.setCellValue(transaction.getAmount() * (1 + transaction.getCommission() / 100) + transaction.getTransportation());
                        CellStyle balanceCellStyle = book.createCellStyle();
                        balanceCellStyle.cloneStyleFrom(balanceCell.getCellStyle());
                        balanceCellStyle.setDataFormat(format.getFormat("# ##0"));
                        balanceCell.setCellStyle(balanceCellStyle);
                        sheet.autoSizeColumn(8 + columnPerCurrency * initial);
                        logger.trace("Created balance cell with value = " + transaction.getBalance());
                        // если в базе имеются стили для данной ячейки - применяем их
                        if (transaction.getAmountColor() != null && !transaction.getAmountColor().isEmpty()) {
                            CellStyle balanceStyle = book.createCellStyle();
                            XSSFFont balanceFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getAmountColor().split(";");
                            for (String sample : styleSample) {
                                if(sample.matches("^font-weight: bold")) balanceFont.setBold(true);
                                if(sample.matches("^font-style: italic")) balanceFont.setItalic(true);
                                if(sample.matches("^color:.+")) {
                                    String[] s = sample.substring(11).split(",");
                                    byte[] color = {(byte) Integer.parseInt(s[0].trim()), (byte) Integer.parseInt(s[1].trim()),
                                            (byte) Integer.parseInt(s[2].substring(0, s[2].length() - 1).trim())};
                                    balanceFont.setColor(new XSSFColor(color, null));
                                }
                            }
                            balanceStyle.setFont(balanceFont);
                            balanceCell.setCellStyle(balanceStyle);
                            sheet.autoSizeColumn(1);
                        }
                        logger.info("Created row" + rowNum + " on sheet " + sheet.getSheetName() + ", currency " + currency.getName() + ", amount " + transaction.getAmount());
                    }
                }
                // применение стилей для последних строк каждой даты, необходимо выполнять в конце, т.к. последняя строка
                // может меняться в процессе заполнения листа
                for (Row row : sheet) {
                    for (int i = 0; i < columnPerCurrency * 5; i++) {
                        Cell cell = row.getCell(i);
                        if (cell == null) {
                            cell = row.createCell(i);
                        }
                        if (cell.getColumnIndex() % columnPerCurrency == 1) {
                            CellStyle style = book.createCellStyle();
                            style.cloneStyleFrom(cell.getCellStyle());
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
                            CellStyle style = book.createCellStyle();
                            style.cloneStyleFrom(cell.getCellStyle());
                            style.setBorderBottom(BorderStyle.MEDIUM);
                            cell.setCellStyle(style);
                        }
                    }

                }
            }
            return book;
        }
    /**
     * Метод создает Excel файл содержащий информацию о всех записях имеющихся в базе данных
     * @param response объект ответа сервера, который используется для отправки файла
     */
    @RequestMapping(path = "/all")
    public void writeIntoExcel(HttpServletResponse response) {
        logger.info("Downloading excel workbook...");
        try {
            // создаем файл
            excelManager.createFull();
            // отправляем файл
            sendFile(response, "excel/info.xlsx");
            logger.info("File sent");
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * Вспомогательный метод, который отправляет файл
     * @param response объект ответа сервера, используемый для отправки файла
     * @throws IOException исключение выбрасывается если файл для отправки не существует (перед отправкой файл должен
     * располагаться в корне приложения)
     */
    private void sendFile(HttpServletResponse response, String source) throws IOException {
        File file = new File(source);
        if (file.exists()) {
            logger.debug("File exist");
            //получаем mimetype
            String mimeType = URLConnection.guessContentTypeFromName(file.getName());
            if (mimeType == null) {
                //если mimetype не известен устанавливаем его application/octet-stream
                mimeType = "application/octet-stream";
            }
            // устанавливаем тип контента для ответа
            response.setContentType(mimeType);
            logger.debug("Content type set");

            // устанавливаем пара метры хэдера ответа
            response.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() + "\""));

            response.setContentLength((int) file.length());
            // преобразуем файл в поток информации
            InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            // отправляем этот поток информации вместе с ответом сервера
            FileCopyUtils.copy(inputStream, response.getOutputStream());

        }
    }
}
