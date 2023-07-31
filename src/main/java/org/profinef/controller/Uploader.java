package org.profinef.controller;

import com.monitorjbl.xlsx.StreamingReader;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.profinef.entity.Account;
import org.profinef.entity.Client;
import org.profinef.entity.Currency;
import org.profinef.service.AccountManager;
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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.*;
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
    @Autowired
    private final AccountManager accountManager;

    private Client client;
    private Date date = null;
    private List<Integer> currencies = new ArrayList<>();
    private double amount = 0;
    private String amountColor = "";
    private double commission = 0;
    private double rate = 1;
    private double transportation = 0;
    private boolean isTransaction = false;
    private String comment = "";
    private final int columnPerCurrency = 8;

    private static Logger logger = LoggerFactory.getLogger(Uploader.class);

    public Uploader(ClientManager clientManager, TransManager transManager, CurrencyManager currencyManager, AccountManager accountManager) {
        this.clientManager = clientManager;
        this.transManager = transManager;
        this.currencyManager = currencyManager;
        this.accountManager = accountManager;
    }

    /**
     * Method for getting file from user and updating the database
     *
     * @param file file with information to update
     */
    @RequestMapping("/full")
    public String uploadDataFromExcel(@RequestParam("file") MultipartFile file,
                                      @RequestParam(name = "date", required = false, defaultValue = "0") java.sql.Date dateAfter,
                                      HttpSession session) throws Exception {
        logger.info("Getting file...");
        System.out.println(dateAfter);
        //clean database before inserting new data. it needed to avoid doubles. method marked transactional, so
        //old information will be restored in case of exception, I hope
        if (dateAfter == null) {
            session.setAttribute("error", "date is null");
            clientManager.deleteAll();
            return "error";
        } else {
            transManager.deleteAfter(new Timestamp(dateAfter.getTime()));
        }

        logger.info("Data base cleared");
        //creating workbook from file
        //XSSFWorkbook myExcelBook = new XSSFWorkbook(file.getInputStream());
        InputStream stream = file.getInputStream();
        //org.apache.poi.util.IOUtils.setByteArrayMaxOverride(1000000000);

        Workbook myExcelBook = StreamingReader.builder()
                .rowCacheSize(1000000000)
                .bufferSize(1000000000)
                .open(stream);
        logger.info("Created workbook");
        for (Sheet sheet : myExcelBook) {
            if (sheet.getSheetName().equals("Итоговый лист")) {
                break;
            }
            if (sheet.getSheetName().equals("Итоговый")) {
                break;
            }
            logger.info("Created sheet: " + sheet.getSheetName());

            client = new Client(sheet.getSheetName());
            try {
                accountManager.addClient(client);
            } catch (RuntimeException ignored) {}

            //first row contains information about currencies
            for (Row row : sheet) {
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
                if (row.getRowNum() == 1) continue;
                logger.debug("Created row" + row.getRowNum());

                amount = 0;
                commission = 0;
                rate = 1;
                transportation = 0;
                isTransaction = false;
                comment = "";

                for (Cell cell : row) {
                    if (cell == null) continue;
                    double value = 0.0;
                    if (cell.getCellType() == CellType.STRING) {
                        if (cell.getColumnIndex() % columnPerCurrency == 1) {
                            comment = cell
                                    .getStringCellValue();
                            logger.trace("Found comment: " + comment);
                            continue;
                        } else {
                            String valueS = cell.getStringCellValue().replace("'", "");
                            try {
                                value = Double.parseDouble(valueS);
                            } catch (NumberFormatException e) {
                                logger.debug("Неверный формат ячейки. Лист: " + sheet.getSheetName() + ",   Ряд: " + row.getRowNum());
                            }
                        }
                    }
                    if (cell.getCellType() == CellType.FORMULA) {
                        if (Objects.requireNonNull(cell.getCachedFormulaResultType()) == CellType.NUMERIC) {
                            value = cell.getNumericCellValue();
                        }
                    }
                    if (cell.getCellType() == CellType.NUMERIC) {
                        //getting date
                        if (cell.getColumnIndex() == 0) {
                            date = cell.getDateCellValue();
                            logger.trace("Found date: " + date);
                            continue;
                        }

                        //cell can contain sum of other cells that is not usefully
                        if (cell.getColumnIndex() % columnPerCurrency == 1) {
                            comment = String.valueOf(cell
                                    .getNumericCellValue());
                            logger.trace("Found comment: " + comment);
                            continue;
                        }

                        value = cell.getNumericCellValue();
                    }
                    if (date.before(dateAfter)) break;
                    if (value != 0) {
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
                            //if (date.after(new Date(123, Calendar.JULY, 19))) isTransaction = true; //TODO change date to variable
                            continue;
                        }
                        //getting commission
                        if (cell.getColumnIndex() % columnPerCurrency == 4) {

                            commission = value;
                            logger.trace("Found commission: " + commission);
                            continue;

                        }
                        //getting rate
                        if (cell.getColumnIndex() % columnPerCurrency == 6) {
                            rate = value;
                            logger.trace("Found rate: " + rate);
                            continue;
                        }
                        //getting transportation
                        if (cell.getColumnIndex() % columnPerCurrency == 7) {
                            transportation = value;
                            logger.trace("Found transportation: " + transportation);
                            continue;
                        }
                    }
                    //last cell for transaction. Insert transaction with previous info
                    if ((cell.getColumnIndex() % columnPerCurrency == 0) && isTransaction) {
                        System.out.println("currencyId=" + (currencies.get((cell.getColumnIndex() / columnPerCurrency) - 1)));
                        logger.info("Inserting transaction on sheet: " + sheet.getSheetName());
                        insertTransaction(row, cell.getColumnIndex(), currencies.get((cell.getColumnIndex() / columnPerCurrency) - 1));
                        amount = 0;
                        commission = 0;
                        rate = 1;
                        transportation = 0;
                        isTransaction = false;
                        comment = "";
                    }
                }


                //second row doesn't contain usefully cells
                //next rows contains info about transactions
            }
            currencies = new ArrayList<>();
        }
            //for each sheet except last creating user
            myExcelBook.close();
            logger.info("File decoded and uploaded");
            return "redirect:/client";
    }


    /**
     * Insert transaction to database. Uses information from class fields and some additional params
     *
     * @param row        contain information about color of balance cell
     * @param cellNum    number of cell that contains balance value
     * @param currencyId id of currency for which transaction will be created
     * @throws Exception potentially can be sent but actually it not possible
     */
    private void insertTransaction(Row row, int cellNum, int currencyId) throws Exception {

        //Getting balance color if it exists
        /*XSSFColor c = cell
        .getCellStyle().getFont().getXSSFColor();
        String balanceColor = null;
        if (c != null) {
            balanceColor = "rgb(" + Byte.toUnsignedInt(c.getRGB()[0]) + "," + Byte.toUnsignedInt(c.getRGB()[1]) +
                    ","  + Byte.toUnsignedInt(c.getRGB()[2]) + ")";
            logger.trace("Found balance color: " + balanceColor);
        }*/
        //Getting transaction id based on max id of transaction in database
        int transactionId;
        try {
            transactionId = transManager.getMaxId() + 1;
            /*transactionId = transManager.getAllTransactions()
                    .stream()
                    .flatMapToInt(t -> IntStream.of(t.getId()))
                    .max()
                    .orElse(0) + 1;*/
        } catch (RuntimeException e) {
            //if zero transactions exist in base set id to 1
            transactionId = 1;
        }
        try {
            //writing transaction to database
            transManager.remittance(transactionId, new Timestamp(date.getTime()), client.getPib(), comment,
                    currencyId, rate, commission, amount, transportation, null, amountColor, null);
        } catch (RuntimeException e) {
            e.printStackTrace();
            //if currency did not exist create it and try again
            Currency currency = new Currency(currencyId, currencyBackFormatter(currencyId));
            try {
                currencyManager.addCurrency(currency);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
            transManager.remittance(transactionId, new Timestamp(date.getTime()), client.getPib(), comment,
                    currencyId, rate, commission, amount, transportation, null, amountColor, null);
        }
        //setting parameters to default
        amount = 0;
        commission = 0;
        rate = 1;
        transportation = 0;
        isTransaction = false;
        comment = "";

    }


    /**
     * Formatter that contain information about currencies, their names and codes/ids. Can be updated by adding more
     * currencies
     * @param name names of currency. Prefer short format of three uppercase letters
     * @return code of currency that type is int. If there is no information about currency with such names return -1
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
     * Formatter that contain information about currencies, their names and codes/ids. Can be updated by adding more
     * currencies
     * @param id code of currency. Must contain three digit
     * @return names of currency in ISO format. If there is no information about currency with such id return empty
     * string
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
