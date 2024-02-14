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

/**
 * Класс предназначен для настройки выполнения автоматических, периодически повторяемых действий
 */
@Service
public class Scheduler {
    @Autowired
    private final NBUManager nbuManager;
    @Autowired
    private final ExcelManager excelManager;
    private final Logger logger = LoggerFactory.getLogger(Scheduler.class);
    public Scheduler(NBUManager nbuManager, ExcelManager excelManager) {
        this.nbuManager = nbuManager;
        this.excelManager = excelManager;
    }

    /**
     * Метод запускает обновление курсов валют каждый день в 16:00
     */
    @Scheduled(cron = "0 0 16 * * *")
    public void updateCurrencyExchange() {
        logger.info("Updating exchange rates");
        nbuManager.getTodayCurrencyExchange();
    }

    /**
     * Метод запускает создание бэкапа при запуске и остановке приложения, а также каждый день в 8:00, 14:00 и 22:00
     */
    @PostConstruct
    @PreDestroy
    @Scheduled(cron = "0 0 8,14,22 * * *")
    public void makeBackUp() {
        logger.info("Making backup");
        String date = new SimpleDateFormat("dd-MM-yyyy_HH-mm").format(new Date());
        boolean doneLocal = DatabaseUtil.backupLocal("root", "2223334456", "transactions", "backup\\" + date + ".sql");
        if (doneLocal) {
            logger.info("Local backup made");
        } else {
            logger.info("some errors with local backup");
        }
        //DatabaseUtil.copyBackup("Desktop-7bo0mfq", "oper", "oper01", "Desktop-7bo0mfq", "qaz", "backup\\" + date + ".sql");
        logger.info("Network backup made");
    }

    /**
     * Метод автоматического восстановления БД из самого свежего бэкапа. Автоматическое выполнение настраивается для
     * удаленного хранилища каждый день в 22:30
     */
    /*@Scheduled(cron = "0 30 22 * * *")*/
    public void restoreFromBackUp() {
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
        restoreFromBackUp(chosenFile.getName());
    }

    /**
     * Метод восстановления базы данных из выбранного файла бэкапа
     * @param chosenFile файл бэкапа дял восстановления
     */
    public void restoreFromBackUp(String chosenFile) {
        assert chosenFile != null;
        boolean doneLocal = DatabaseUtil.restore("root", "2223334456", "transactions", "backup\\" + chosenFile);
        if (doneLocal) {
            logger.info("Restored from backup");
        } else {
            logger.info("some errors with restoring from backup");
        }
    }

    /**
     * Метод выполняет создание файла Excel который отражает всю информацию из БД. Автоматическое выполнение
     * настраивается на удаленном хранилище каждый день в 23:00
     * @throws Exception выбрасывается при невозможности создания файла
     */
    /*@Scheduled(cron = "0 0 23 * * *")*/
    public void makeExcel() throws Exception {
        excelManager.createFull();
    }


}
