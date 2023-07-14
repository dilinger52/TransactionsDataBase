package org.profinef.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private static Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);
    @Bean
    public ThymeleafViewResolver thymeleafViewResolver(@Autowired SpringTemplateEngine templateEngine ) {
        logger.info("Configuring ThymeleafViewResolver");
        ThymeleafViewResolver thymeleafViewResolver = new ThymeleafViewResolver();
        thymeleafViewResolver.setTemplateEngine( templateEngine );
        thymeleafViewResolver.setCharacterEncoding( "UTF-8" );
        return thymeleafViewResolver;
    }
}
