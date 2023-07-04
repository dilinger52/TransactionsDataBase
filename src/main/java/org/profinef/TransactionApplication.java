package org.profinef;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class TransactionApplication {
    public static void main(String[] args) {
        //SpringApplication.run(TransactionApplication.class, args);
        try {
            final SpringApplication application = new SpringApplication(TransactionApplication.class);
            application.setBannerMode(Banner.Mode.OFF);
            application.setWebApplicationType(WebApplicationType.SERVLET);
            application.run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}