package org.profinef.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public final class DatabaseUtil {

    private static final Logger logger =  LoggerFactory.getLogger(DatabaseUtil.class);
    public static boolean backup(String dbUsername, String dbPassword, String dbName, String outputFile)
            throws InterruptedException, IOException {
        try {
            String command = String.format("C:\\Program Files\\MySQL\\MySQL Workbench 8.0\\mysqldump -u%s -p%s --add-drop-table --databases %s -r %s",
                    dbUsername, dbPassword, dbName, outputFile);
            Process process = Runtime.getRuntime().exec(command);
            int processComplete = process.waitFor();
            return processComplete == 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
