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
    @Autowired
    private final DatabaseUtil databaseUtil;
    private Logger logger = LoggerFactory.getLogger(Scheduler.class);
    public Scheduler(NBUManager nbuManager, DatabaseUtil databaseUtil) {
        this.nbuManager = nbuManager;
        this.databaseUtil = databaseUtil;
    }

    @Scheduled(cron = "0 0 16 * * *")
    public void updateCurrencyExchange() throws Exception {
        logger.info("Updating exchange rates");
        nbuManager.getTodayCurrencyExchange();
    }
    @PostConstruct
    public void makeBackUp() throws Exception {
        logger.info("Making backup");
        String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        databaseUtil.backup("root", "2223334456", "transactions", "backup\\" + date + ".sql");
    }
}
