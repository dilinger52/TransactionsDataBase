package org.profinef;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
public class TransactionApplication {
    public static void main(String[] args) {
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