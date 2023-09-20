package org.profinef.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class Scheduler {
    @Autowired
    private final NBUManager nbuManager;
    @Autowired
    private final ExcelManager excelManager;
    private Logger logger = LoggerFactory.getLogger(Scheduler.class);
    public Scheduler(NBUManager nbuManager, ExcelManager excelManager) {
        this.nbuManager = nbuManager;
        this.excelManager = excelManager;
    }

    @Scheduled(cron = "0 0 16 * * *")
    public void updateCurrencyExchange() throws Exception {
        logger.info("Updating exchange rates");
        nbuManager.getTodayCurrencyExchange();
    }
    @PostConstruct
    @PreDestroy
    @Scheduled(cron = "0 0 8,14 * * *")
    public void makeBackUp() throws Exception {
        logger.info("Making backup");
        String date = new SimpleDateFormat("dd-MM-yyyy_HH-mm").format(new Date());
        boolean doneLocal = DatabaseUtil.backup("root", "2223334456", "transactions", "backup\\" + date + ".sql");
        if (doneLocal) {
            logger.info("Local backup made");
        } else {
            logger.info("some errors with local backup");
        }
        /*boolean doneNetwork = DatabaseUtil.backup("root", "2223334456", "transactions", "S:\\backup\\" + date + ".sql");
        if (doneNetwork) {
            logger.info("Network backup made");
        } else {
            logger.info("some errors with network backup");
        }*/
    }
/*
    @Scheduled(cron = "0 0 22 * * *")
    public void restoreFromBackUp() throws Exception {
        File directory = new File("backup\\");
        File[] files = directory.listFiles(File::isFile);
        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;
        if (files != null)
        {
            for (File file : files)
            {
                if (file.lastModified() > lastModifiedTime)
                {
                    chosenFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }
        assert chosenFile != null;
        boolean doneLocal = DatabaseUtil.restore("root", "2223334456", "transactions", chosenFile.getPath());
        if (doneLocal) {
            logger.info("Restored from backup");
        } else {
            logger.info("some errors with restoring from backup");
        }
    }

    @Scheduled(cron = "0 30 22 * * *")
    public void makeExcel() throws Exception {
        excelManager.createFull();
    }

 */
}
