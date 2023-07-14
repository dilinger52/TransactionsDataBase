package org.profinef;

import org.profinef.controller.AppController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class TransactionApplication {
    private static Logger logger = LoggerFactory.getLogger(AppController.class);
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