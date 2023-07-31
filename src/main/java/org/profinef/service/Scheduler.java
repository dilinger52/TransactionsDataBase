package org.profinef.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
}
