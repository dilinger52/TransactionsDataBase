package org.profinef.controller;

import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.service.ClientManager;
import org.profinef.service.CurrencyManager;
import org.profinef.service.TransManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

/**
 * This class realised functionality to get file in office open xml format and use it for update the database
 */
@Controller
@RequestMapping(path = "/upload")
public class Uploader {

    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final TransManager transManager;
    @Autowired
    private final CurrencyManager currencyManager;

    private Client client;
    private Date date = null;
    private List<Integer> currencies = new ArrayList<>();
    private double amount = 0;
    private String amountColor = "";
    private double commission = 0;
    private double rate = 1;
    private double transportation = 0;
    private boolean isTransaction = false;
    private final  int columnPerCurrency = 8;

    private static Logger logger = LoggerFactory.getLogger(Uploader.class);

    public Uploader(ClientManager clientManager, TransManager transManager, CurrencyManager currencyManager) {
        this.clientManager = clientManager;
        this.transManager = transManager;
        this.currencyManager = currencyManager;
    }

    /**
     * Method for getting file from user and updating the database
     * @param file file with information to update
     */
    @Transactional
    @RequestMapping("/full")
    public String uploadDataFromExcel(@RequestParam("file") MultipartFile file) {
        logger.info("Getting file...");
        try {
            //clean database before inserting new data. it needed to avoid doubles. method marked transactional, so
            //old information will be restored in case of exception, I hope
            clientManager.deleteAll();
            logger.info("Data base cleared");
            //creating workbook from file
            XSSFWorkbook myExcelBook = new XSSFWorkbook(file.getInputStream());
            logger.debug("Created workbook");
            //for each sheet except last creating user
            for (int sheetNum = 0; sheetNum < myExcelBook.getNumberOfSheets() - 1; sheetNum++) {
                XSSFSheet myExcelSheet = myExcelBook.getSheetAt(sheetNum);
                logger.debug("Created sheet" + sheetNum);
                client = new Client(myExcelSheet.getSheetName());
                clientManager.addClient(client);
                //first row contains information about currencies
                Row firstRow = myExcelSheet.getRow(0);
                logger.debug("Created first row");
                for (int cellNum = 2; cellNum < firstRow.getLastCellNum(); cellNum++) {
                    if (firstRow.getCell(cellNum) == null) continue;
                    if (firstRow.getCell(cellNum).getCellType() == CellType.STRING) {
                        currencies.add(currencyFormatter(firstRow.getCell(cellNum).getStringCellValue()));
                    }
                }
                logger.debug("Found currencies: " + currencies);
                //second row doesn't contain usefully cells
                //next rows contains info about transactions
                for (int rowNum = 2; rowNum <= myExcelSheet.getLastRowNum(); rowNum++) {
                    XSSFRow row = myExcelSheet.getRow(rowNum);
                    logger.debug("Created row" + rowNum);
                    for (int cellNum = 0; cellNum <= row.getLastCellNum(); cellNum++) {
                        if (row.getCell(cellNum) == null) continue;
                        if (row.getCell(cellNum).getCellType() == CellType.NUMERIC) {
                            //getting date
                            if (cellNum == 0) {
                                date = row.getCell(cellNum).getDateCellValue();
                                logger.trace("Found date: " + date);
                                continue;
                            }
                            //cell can contain sum of other cells that is not usefully
                            if (cellNum == 1) break;
                            double value = row.getCell(cellNum).getNumericCellValue();
                            //getting amount
                            if (cellNum % columnPerCurrency == 2 || cellNum % columnPerCurrency == 3) {
                                amount = value;
                                logger.trace("Found amount: " + amount);
                                XSSFColor c = row.getCell(cellNum).getCellStyle().getFont().getXSSFColor();
                                if (c != null) {
                                    amountColor = "rgb(" + Byte.toUnsignedInt(c.getRGB()[0]) + "," +
                                            Byte.toUnsignedInt(c.getRGB()[1]) + ","  +
                                            Byte.toUnsignedInt(c.getRGB()[2]) + ")";
                                }
                                logger.trace("Found amountColor: " + amountColor);
                                isTransaction = true;
                                continue;
                            }
                            //getting commission
                            if (cellNum % columnPerCurrency == 4) {
                                commission = value;
                                logger.trace("Found commission: " + commission);
                                continue;
                            }
                            //getting rate
                            if (cellNum % columnPerCurrency == 6) {
                                rate = value;
                                logger.trace("Found rate: " + rate);
                                continue;
                            }
                            //getting transportation
                            if (cellNum % columnPerCurrency == 7) {
                                transportation = value;
                                logger.trace("Found transportation: " + transportation);
                                continue;
                            }
                        }
                        //last cell for transaction. Insert transaction with previous info
                        if ((cellNum % columnPerCurrency == 0 ) && isTransaction) {
                            insertTransaction(row, cellNum, currencies.get((cellNum / columnPerCurrency) - 1));
                        }
                    }
                }
            }
            myExcelBook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("File decoded and uploaded");
        return "redirect:/client";
    }

    /**
     * Insert transaction to database. Uses information from class fields and some additional params
     * @param row contain information about color of balance cell
     * @param cellNum number of cell that contains balance value
     * @param currencyId id of currency for which transaction will be created
     * @throws Exception potentially can be sent but actually it not possible
     */
    private void insertTransaction(XSSFRow row, int cellNum, int currencyId) throws Exception {
        logger.info("Inserting transaction");
        //Getting balance color if it exists
        XSSFColor c = row.getCell(cellNum).getCellStyle().getFont().getXSSFColor();
        String balanceColor = null;
        if (c != null) {
            balanceColor = "rgb(" + Byte.toUnsignedInt(c.getRGB()[0]) + "," + Byte.toUnsignedInt(c.getRGB()[1]) +
                    ","  + Byte.toUnsignedInt(c.getRGB()[2]) + ")";
            logger.trace("Found balance color: " + balanceColor);
        }
        //Getting transaction id based on max id of transaction in database
        int transactionId;
        try {
            transactionId = transManager.getAllTransactions()
                    .stream()
                    .flatMapToInt(t -> IntStream.of(t.getId()))
                    .max()
                    .orElse(0) + 1;
        } catch (RuntimeException e) {
            //if zero transactions exist in base set id to 1
            transactionId = 1;
        }
        try{
            //writing transaction to database
            transManager.remittance(transactionId, new Timestamp(date.getTime()), client.getPib(), null,
                    currencyId, rate, commission, amount, transportation, null, amountColor, balanceColor);
        } catch (RuntimeException e) {
            //if currency did not exist create it and try again
            Currency currency = new Currency(currencyId, currencyBackFormatter(currencyId));
            currencyManager.addCurrency(currency);
            transManager.remittance(transactionId, new Timestamp(date.getTime()), client.getPib(), null,
                    currencyId, rate, commission, amount, transportation, null, amountColor, balanceColor);
        }
        //setting parameters to default
        amount = 0;
        commission = 0;
        rate = 1;
        transportation = 0;
        isTransaction = false;

    }

    /**
     * Formatter that contain information about currencies, their names and codes/ids. Can be updated by adding more
     * currencies
     * @param name name of currency. Prefer short format of three uppercase letters
     * @return code of currency that type is int. If there is no information about currency with such name return -1
     */
    private int currencyFormatter(String name) {
        logger.debug("Currency formatting from name to id");
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
     * Formatter that contain information about currencies, their names and codes/ids. Can be updated by adding more
     * currencies
     * @param id code of currency. Must contain three digit
     * @return name of currency in ISO format. If there is no information about currency with such id return empty
     * string
     */
    private String currencyBackFormatter(int id) {
        logger.debug("Currency formatting from id to name");
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
