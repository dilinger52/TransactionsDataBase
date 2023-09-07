package org.profinef.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class Scheduler {
    @Autowired
    private final NBUManager nbuManager;
    private Logger logger = LoggerFactory.getLogger(Scheduler.class);
    public Scheduler(NBUManager nbuManager) {
        this.nbuManager = nbuManager;
    }

    @Scheduled(cron = "0 0 16 * * *")
    public void updateCurrencyExchange() throws Exception {
        logger.info("Updating exchange rates");
        nbuManager.getTodayCurrencyExchange();
    }
    @PostConstruct
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
        /*boolean doneNetwork = databaseUtil.backup("root", "2223334456", "transactions", "\\\\192.168.2.169\\D:\\backup\\" + date + ".sql");
        if (doneNetwork) {
            logger.info("Network backup made");
        } else {
            logger.info("some errors with network backup");
        }*/
    }
}
