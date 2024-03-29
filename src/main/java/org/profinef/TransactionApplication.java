package org.profinef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Главный класс приложения, который инициирует запуск приложения, создание всех необходимых объектов, запуск сервера,
 * системы логирования, подключением к базе данных и проводит стартовую конфигурацию этих и других необходимых систем.
 */
@SpringBootApplication
@EnableScheduling
public class TransactionApplication {
    private static final Logger logger = LoggerFactory.getLogger(TransactionApplication.class);
    public static void main(String[] args) {
        logger.info("Application starts...");
        //SpringApplication.run(TransactionApplication.class, args);
        try {
            final SpringApplication application = new SpringApplication(TransactionApplication.class);
            application.setBannerMode(Banner.Mode.OFF);
            application.setWebApplicationType(WebApplicationType.SERVLET);
            application.run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("Application started");
    }
}