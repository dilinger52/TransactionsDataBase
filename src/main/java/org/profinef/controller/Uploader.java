package org.profinef.controller;

import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.profinef.entity.Client;
import org.profinef.service.ClientManager;
import org.profinef.service.TransManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.IntStream;

@Controller
@RequestMapping(path = "/upload")
public class Uploader {

    @Autowired
    private final ClientManager clientManager;
    @Autowired
    private final TransManager transManager;

    private Client client;
    private Date date = null;
    private double amount = 0;
    private String amountColor = "";
    private double commission = 0;
    private double rate = 1;
    private double transportation = 0;
    private boolean isTransaction = false;

    public Uploader(ClientManager clientManager, TransManager transManager) {
        this.clientManager = clientManager;
        this.transManager = transManager;
    }


    @Transactional
    @RequestMapping("/full")
    public String uploadDataFromExcel(@RequestParam("file") MultipartFile file) throws IOException {
        try {
            clientManager.deleteAll();
            XSSFWorkbook myExcelBook = new XSSFWorkbook(file.getInputStream());
            for (int sheetNum = 0; sheetNum < myExcelBook.getNumberOfSheets() - 1; sheetNum++) {
                XSSFSheet myExcelSheet = myExcelBook.getSheetAt(sheetNum);
                client = new Client(myExcelSheet.getSheetName());
                clientManager.addClient(client);
                System.out.println("Client: " + client);
                for (int rowNum = 2; rowNum < myExcelSheet.getLastRowNum() - 1; rowNum++) {
                    XSSFRow row = myExcelSheet.getRow(rowNum);
                    for (int cellNum = 0; cellNum < row.getLastCellNum(); cellNum++) {
                        //System.out.println("cell: " + cellNum + "; row: " + rowNum + "; sheet: " + sheetNum + "; value: " + row.getCell(cellNum).toString());
                        if (row.getCell(cellNum).getCellType() == CellType.NUMERIC) {
                            if (cellNum == 0) {
                                date = row.getCell(cellNum).getDateCellValue();
                                System.out.println("date: " + date);
                                continue;
                            }
                            if (cellNum == 1) break;
                            double value = row.getCell(cellNum).getNumericCellValue();
                            if (cellNum == 2 || cellNum == 3 || cellNum == 10 || cellNum == 11 || cellNum == 18 || cellNum == 19
                                    || cellNum == 26 || cellNum == 27 || cellNum == 34 || cellNum == 35) {
                                amount = value;
                                XSSFColor c = row.getCell(cellNum).getCellStyle().getFont().getXSSFColor();
                                if (c != null) {
                                    amountColor = "rgb(" + Byte.toUnsignedInt(c.getRGB()[0]) + "," + Byte.toUnsignedInt(c.getRGB()[1]) + ","  + Byte.toUnsignedInt(c.getRGB()[2]) + ")";
                                }
                                System.out.println("amount: " + amount);
                                System.out.println("amountColor: " + amountColor);
                                isTransaction = true;
                                continue;
                            }
                            if (cellNum == 4 || cellNum == 12 || cellNum == 20 || cellNum == 28 || cellNum == 36) {
                                commission = value;
                                System.out.println("commission: " + commission);
                                continue;
                            }
                            if (cellNum == 6 || cellNum == 14 || cellNum == 22 || cellNum == 30 || cellNum == 38) {
                                rate = value;
                                System.out.println("rate: " + rate);
                                continue;
                            }
                            if (cellNum == 7 || cellNum == 15 || cellNum == 23 || cellNum == 31 || cellNum == 39) {
                                transportation = value;
                                continue;
                            }
                        }
                        if (cellNum == 8 && isTransaction) {
                            insertTransaction(row, cellNum, 980);
                            continue;
                        }
                        if (cellNum == 16 && isTransaction) {
                            insertTransaction(row, cellNum, 840);
                            continue;
                        }
                        if (cellNum == 24 && isTransaction) {
                            insertTransaction(row, cellNum, 978);
                            continue;
                        }
                        /* for rubles if it needed
                        if (cellNum == 32 && isTransaction) {
                            insertTransaction(row, cellNum, 643);
                            continue;

                        }*/
                        if (cellNum == 40 && isTransaction) {
                            insertTransaction(row, cellNum, 985);
                        }

                    }
                }
            }
            myExcelBook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/client";
    }

    private void insertTransaction(XSSFRow row, int cellNum, int currencyId) {
        XSSFColor c = row.getCell(cellNum).getCellStyle().getFont().getXSSFColor();
        String balanceColor = null;
        if (c != null) {
            balanceColor = "rgb(" + Byte.toUnsignedInt(c.getRGB()[0]) + "," + Byte.toUnsignedInt(c.getRGB()[1]) + ","  + Byte.toUnsignedInt(c.getRGB()[2]) + ")";
        }
        int transactionId = transManager.getAllTransactions()
                .stream()
                .flatMapToInt(t -> IntStream.of(t.getId()))
                .max()
                .orElse(0) + 1;
        transManager.remittance(transactionId, new Timestamp(date.getTime()), client.getPib(), currencyId, rate, commission, amount, transportation, null, amountColor, balanceColor);
        amount = 0;
        commission = 0;
        rate = 1;
        transportation = 0;
        isTransaction = false;

    }
    private String currencyFormatter(String name) {
        switch (name) {
            case "Гривна" -> {return "UAH";}
            case "Доллар" -> {return "USD";}
            case "Евро" -> {return "EUR";}
            case "Рубль" -> {return "RUB";}
            case "Злотый" -> {return "PLN";}
        }
        return "";
    }
}
