package org.profinef.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.entity.Transaction;
import org.profinef.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.*;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * This class uses to download database as office open xml excel-file
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

    private static Logger logger = LoggerFactory.getLogger(Downloader.class);

    public Downloader(ClientManager clientManager, CurrencyManager currencyManager, TransManager transManager,
                      AccountManager accountManager, ExcelManager excelManager) {
        this.clientManager = clientManager;
        this.currencyManager = currencyManager;
        this.transManager = transManager;
        this.accountManager = accountManager;
        this.excelManager = excelManager;
    }

    @RequestMapping(path = "/client_info")
    public void downloadClientInfo(HttpServletResponse response, HttpSession session) throws IOException {
        logger.info("Downloading excel workbook...");
        try {
            List<Client> clients = new ArrayList<>();
            clients.add((Client) session.getAttribute("client"));
            List<Currency> currencies = (List<Currency>) session.getAttribute("currencies");
            List<Transaction> transactions = ((List<Transaction>) session.getAttribute("transactions"));
            Workbook book = createWorkbook(clients, currencies, transactions);

            DataFormat format = book.createDataFormat();
            Sheet total = book.createSheet("Итоговый");
            logger.info("Created total sheet");
            CellStyle boldStyle = book.createCellStyle();
            Font bold = book.createFont();
            bold.setBold(true);
            boldStyle.setDataFormat(format.getFormat("# ### ### ### ### ### ### ### ### ### ###"));
            boldStyle.setFont(bold);

            CellStyle redBoldStyle = book.createCellStyle();
            Font redBold = book.createFont();
            redBold.setBold(true);
            redBold.setColor(Font.COLOR_RED);
            redBoldStyle.setFont(redBold);
            redBoldStyle.setBorderTop(BorderStyle.MEDIUM);
            redBoldStyle.setDataFormat(format.getFormat("# ### ### ### ### ### ### ### ### ###"));
            logger.debug("Styles set");

            Row firstRow = total.createRow(0);
            Cell firstCell = firstRow.createCell(2);
            firstCell.setCellStyle(boldStyle);
            firstCell.setCellValue("Итоговый лист");
            logger.trace("Created first cell");

            //creating style for date cells
            CellStyle dateStyle = book.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd.mm.yyyy"));

            Row secondRow = total.createRow(1);
            Cell secondCell = secondRow.createCell(2);
            secondCell.setCellStyle(dateStyle);
            secondCell.setCellValue(LocalDate.now());
            total.autoSizeColumn(2);
            logger.trace("Created second cell");

            Row thirdRow = total.createRow(2);
            logger.debug("Created third row");
            int headerCellNum = 0;
            for (Currency currency : currencies){
                Cell headerCell = thirdRow.createCell(++headerCellNum);
                headerCell.setCellStyle(boldStyle);
                headerCell.setCellValue(currency.getName());
                logger.trace("Created currency names cell with value = " + currency.getName());
            }
            int rowNum = 2;
            for (Client client : clients) {
                Row row = total.createRow(++rowNum);
                Cell nameCell = row.createCell(0);
                nameCell.setCellStyle(boldStyle);
                nameCell.setCellValue(client.getPib());
                logger.trace("Created client cell with value = " + client.getPib());
                int cellNum = 0;
                Set<Map.Entry<Currency, Account.Properties>> currencySet = accountManager.getAccounts(client.getPib()).get(0)
                        .getCurrencies().entrySet();
                for (Map.Entry<Currency, Account.Properties> currency : currencySet) {
                    Cell cell = row.createCell(++cellNum);
                    cell.setCellStyle(boldStyle);
                    cell.setCellValue(currency.getValue().getAmount());
                    logger.trace("Created currency cell with value = " + currency.getValue());
                }
            }

            Row totalRow = total.createRow(++rowNum);
            logger.debug("Created row total");
            Cell nameTotalCell = totalRow.createCell(0);
            nameTotalCell.setCellStyle(redBoldStyle);
            nameTotalCell.setCellValue("Итог");
            logger.trace("Created cell total names");

            List<Map.Entry<Currency, Account.Properties>> currencyList = accountManager.getAllAccounts()
                    .stream()
                    .map(account -> account.getCurrencies().entrySet())
                    .flatMap(Collection::stream)
                    .toList();
            Map<Integer, Double> totalMap = new HashMap<>();
            for (Map.Entry<Currency, Account.Properties> entry : currencyList) {
                if (totalMap.containsKey(entry.getKey().getId())) {
                    totalMap.put(entry.getKey().getId(), totalMap.get(entry.getKey().getId()) + entry.getValue().getAmount());
                } else {
                    totalMap.put(entry.getKey().getId(), entry.getValue().getAmount());
                }
            }
            int totalCellNum = 0;
            for (Map.Entry<Integer, Double> entry : totalMap.entrySet()) {
                Cell totalCell = totalRow.createCell(++totalCellNum);
                totalCell.setCellStyle(redBoldStyle);
                totalCell.setCellValue(entry.getValue());
                total.autoSizeColumn(totalCellNum);
                logger.trace("Created cell total with value = " + entry.getValue());
            }

            // Записываем всё в файл
            book.write(new FileOutputStream("excel/info.xlsx"));
            book.close();
            logger.info("File created");

            sendFile(response, "excel/info.xlsx");
            logger.info("File sent");
        } catch (Exception e) {
            logger.error(Arrays.toString(new String[]{e.getMessage() + Arrays.toString(e.getStackTrace())}));
        }
    }

    private Workbook createWorkbook(List<Client> clients, List<Currency> currencies, List<Transaction> trans) {

            //creating workbook
            Workbook book = new XSSFWorkbook();
            logger.info("Work book created");

            //creating style for header rows
            CellStyle headerStyle = book.createCellStyle();
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);
            headerStyle.setBorderLeft(BorderStyle.MEDIUM);
            headerStyle.setBorderTop(BorderStyle.MEDIUM);
            headerStyle.setBorderRight(BorderStyle.MEDIUM);
            Font headerFont = book.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            //creating style for date cells
            CellStyle dateStyle = book.createCellStyle();
            DataFormat format = book.createDataFormat();
            dateStyle.setDataFormat(format.getFormat("dd.mm.yyyy"));

            //creating style for names cell
            CellStyle nameStyle = book.createCellStyle();
            Font font = book.createFont();
            font.setItalic(true);
            nameStyle.setFont(font);
            nameStyle.setBorderBottom(BorderStyle.MEDIUM);
            nameStyle.setBorderLeft(BorderStyle.MEDIUM);
            nameStyle.setBorderTop(BorderStyle.MEDIUM);
            nameStyle.setBorderRight(BorderStyle.MEDIUM);

            //creating style for last row for each date
            CellStyle lastForDateStyle = book.createCellStyle();
            lastForDateStyle.setBorderBottom(BorderStyle.MEDIUM);

            //creating style for date in last row for each date
            CellStyle dateLastForDateStyle = book.createCellStyle();
            dateLastForDateStyle.setBorderBottom(BorderStyle.MEDIUM);
            dateLastForDateStyle.setDataFormat(format.getFormat("dd.mm.yyyy"));


            for (Client client : clients) {
                //for each client create a sheet. names of sheet is client's names
                Sheet sheet = book.createSheet(client.getPib());
                logger.info("Created sheet: " + sheet.getSheetName());
                Row fistRow = sheet.createRow(0);
                fistRow.setRowStyle(headerStyle);
                logger.info("Created first row");
                Row secondRow = sheet.createRow(1);
                secondRow.setRowStyle(headerStyle);
                logger.info("Created second row");
                Cell nameCell = fistRow.createCell(0);
                nameCell.setCellValue(client.getPib());
                nameCell.setCellStyle(nameStyle);
                logger.trace("Created names cell");
                secondRow.createCell(0).setCellValue("Дата");
                logger.trace("Created date cell");
                int initial = -1;
                for (Currency currency : currencies) {
                    fistRow.createCell(2 + columnPerCurrency * ++initial).setCellValue(currency.getName());
                    for (int i = 2; i < fistRow.getLastCellNum() - 1; i += columnPerCurrency) {
                        fistRow.getCell(i).setCellStyle(headerStyle);
                    }

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
                    List<Transaction> transactions = trans.stream().filter(t -> t.getClient().equals(client)).filter(t -> t.getCurrency().equals(currency)).toList();
                    int rowNum = 1;
                    for (Transaction transaction : transactions) {
                        Row row = sheet.getRow(++rowNum);
                        if (row == null) {
                            row = sheet.createRow(rowNum);

                        }

                        if (row.getCell(0) != null) {
                            while (row.getCell(0) != null && row.getCell(0).getDateCellValue().toInstant()
                                    .atZone(ZoneId.of("Europe/Kyiv")).toLocalDate().atStartOfDay()
                                    .isBefore(transaction.getDate().toInstant().atZone(ZoneId.of("Europe/Kyiv"))
                                            .toLocalDate().atStartOfDay())) {
                                row = sheet.getRow(++rowNum);
                                if (row == null) {
                                    row = sheet.createRow(rowNum);
                                }

                            }
                            if (row.getCell(0).getDateCellValue().toInstant()
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

                        if (row.getCell(0) == null) {
                            Cell cell = row.createCell(0);
                            cell.setCellValue(transaction.getDate());
                            cell.setCellStyle(dateStyle);
                            sheet.autoSizeColumn(0);
                            logger.trace("Created date cell with value = " + transaction.getDate());
                        }
                        List<Transaction> tl = transManager.getTransaction(transaction.getId());
                        StringBuilder anotherClients = new StringBuilder();
                        for (Transaction t : tl) {
                            if (!t.getClient().getId().equals(transaction.getClient().getId())) {
                                anotherClients.append(" ");
                                anotherClients.append(t.getClient().getPib());
                            }
                        }
                        anotherClients.append(" ").append(transaction.getComment());
                        Cell clientsCell = row.createCell(1 + columnPerCurrency * initial);
                        clientsCell.setCellValue(anotherClients.toString());
                        CellStyle clientsCellStyle = book.createCellStyle();
                        clientsCellStyle.cloneStyleFrom(clientsCell.getCellStyle());
                        clientsCellStyle.setBorderLeft(BorderStyle.MEDIUM);
                        clientsCell.setCellStyle(clientsCellStyle);
                        sheet.autoSizeColumn(1 + columnPerCurrency * initial);
                        logger.trace("Created clients cell with value = " + anotherClients);

                        if (transaction.getCommentColor() != null && !transaction.getCommentColor().isEmpty()) {
                            CellStyle clientsStyle = book.createCellStyle();
                            XSSFFont clientsFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getCommentColor().split(";");
                            for (String sample : styleSample) {
                                System.out.println(sample);
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
                                System.out.println(sample);
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

                        Cell commissionCell = row.createCell(4 + columnPerCurrency * initial);
                        commissionCell.setCellValue(transaction.getCommission());
                        CellStyle commissionCellStyle = book.createCellStyle();
                        commissionCellStyle.cloneStyleFrom(commissionCell.getCellStyle());
                        commissionCellStyle.setDataFormat(format.getFormat("# ##0.##"));
                        commissionCell.setCellStyle(commissionCellStyle);
                        sheet.autoSizeColumn(4 + columnPerCurrency * initial);
                        logger.trace("Created tariff cell with value = " + transaction.getCommission());

                        if (transaction.getTarifColor() != null && !transaction.getTarifColor().isEmpty()) {
                            CellStyle commissionStyle = book.createCellStyle();
                            XSSFFont commissionFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getTarifColor().split(";");
                            for (String sample : styleSample) {
                                System.out.println(sample);
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

                        Cell comCell = row.createCell(5 + columnPerCurrency * initial);
                        comCell.setCellValue(Math.abs(transaction.getCommission() / 100 * transaction.getAmount()));
                        CellStyle comCellStyle = book.createCellStyle();
                        comCellStyle.cloneStyleFrom(comCell.getCellStyle());
                        comCellStyle.setDataFormat(format.getFormat("# ##0"));
                        comCell.setCellStyle(comCellStyle);
                        sheet.autoSizeColumn(5 + columnPerCurrency * initial);
                        logger.trace("Created commission cell with value = " +
                                Math.abs(transaction.getCommission() / 100 * transaction.getAmount()));

                        if (transaction.getCommissionColor() != null && !transaction.getCommissionColor().isEmpty()) {
                            CellStyle comStyle = book.createCellStyle();
                            XSSFFont comFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getCommissionColor().split(";");
                            for (String sample : styleSample) {
                                System.out.println(sample);
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

                        Cell rateCell = row.createCell(6 + columnPerCurrency * initial);
                        rateCell.setCellValue(transaction.getRate());
                        CellStyle rateCellStyle = book.createCellStyle();
                        rateCellStyle.cloneStyleFrom(rateCell.getCellStyle());
                        rateCellStyle.setDataFormat(format.getFormat("# ##0.##"));
                        rateCell.setCellStyle(rateCellStyle);
                        sheet.autoSizeColumn(6 + columnPerCurrency * initial);
                        logger.trace("Created rate cell with value = " + transaction.getRate());

                        if (transaction.getRateColor() != null && !transaction.getRateColor().isEmpty()) {
                            CellStyle rateStyle = book.createCellStyle();
                            XSSFFont rateFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getRateColor().split(";");
                            for (String sample : styleSample) {
                                System.out.println(sample);
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

                        Cell transportationCell = row.createCell(7 + columnPerCurrency * initial);
                        transportationCell.setCellValue(transaction.getTransportation());
                        CellStyle transportationCellStyle = book.createCellStyle();
                        transportationCellStyle.cloneStyleFrom(transportationCell.getCellStyle());
                        transportationCellStyle.setDataFormat(format.getFormat("# ##0"));
                        transportationCell.setCellStyle(transportationCellStyle);
                        sheet.autoSizeColumn(7 + columnPerCurrency * initial);
                        logger.trace("Created transportation cell with value = " + transaction.getTransportation());

                        if (transaction.getTransportationColor() != null && !transaction.getTransportationColor().isEmpty()) {
                            CellStyle transportationStyle = book.createCellStyle();
                            XSSFFont transportationFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getTransportationColor().split(";");
                            for (String sample : styleSample) {
                                System.out.println(sample);
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

                        Cell balanceCell = row.createCell(8 + columnPerCurrency * initial);
                        balanceCell.setCellValue(transaction.getAmount() * (1 + transaction.getCommission() / 100) + transaction.getTransportation());
                        CellStyle balanceCellStyle = book.createCellStyle();
                        balanceCellStyle.cloneStyleFrom(balanceCell.getCellStyle());
                        balanceCellStyle.setDataFormat(format.getFormat("# ##0"));
                        balanceCell.setCellStyle(balanceCellStyle);
                        sheet.autoSizeColumn(8 + columnPerCurrency * initial);
                        logger.trace("Created balance cell with value = " + transaction.getBalance());

                        if (transaction.getAmountColor() != null && !transaction.getAmountColor().isEmpty()) {
                            CellStyle balanceStyle = book.createCellStyle();
                            XSSFFont balanceFont = (XSSFFont) book.createFont();
                            String[] styleSample = transaction.getAmountColor().split(";");
                            for (String sample : styleSample) {
                                System.out.println(sample);
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
                //sheet.shiftRows(0,0,0); //magick string that do nothing, but needed to program work
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
     * Create a file that represents database and send it to user for download
     * @param response used to send a file
     */
    @Transactional
    @RequestMapping(path = "/all")
    public void writeIntoExcel(HttpServletResponse response) {
        logger.info("Downloading excel workbook...");
        try {
            excelManager.createFull();
            sendFile(response, "excel/info.xlsx");
            logger.info("File sent");
        } catch (Exception e) {
            logger.error(Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * Util method to send a file
     * @param response used to send a file
     * @throws IOException thrown in case if file to send does not exist
     */
    private void sendFile(HttpServletResponse response, String source) throws IOException {
        File file = new File(source);
        if (file.exists()) {
            logger.debug("File exist");
            //get the mimetype
            String mimeType = URLConnection.guessContentTypeFromName(file.getName());
            if (mimeType == null) {
                //unknown mimetype so set the mimetype to application/octet-stream
                mimeType = "application/octet-stream";
            }

            response.setContentType(mimeType);
            logger.debug("Content type set");


            response.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() + "\""));

            //Here we have mentioned it to show as attachment
            //response.setHeader("Content-Disposition", String.format("attachment; filename=\"" + file.getName() + "\""));

            response.setContentLength((int) file.length());

            InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

            FileCopyUtils.copy(inputStream, response.getOutputStream());

        }
    }
}
