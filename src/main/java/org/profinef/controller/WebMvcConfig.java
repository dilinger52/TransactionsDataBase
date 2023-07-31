package org.profinef.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.profinef.entity.Role;
import org.profinef.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.io.IOException;


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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        HandlerInterceptor authorizationInterceptor = new HandlerInterceptor(){
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
                Role[] authorizedRoles = Role.values();
                HttpSession session = request.getSession();
                User user = (User) session.getAttribute("user");
                for (Role role : authorizedRoles) {
                    if (user != null && user.getRole() == role) {
                        return true;
                    }
                }
                response.sendRedirect(request.getContextPath() + "/log_in");
                return false;
            }
        };
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/*").excludePathPatterns("/log_in", "/check_user", "/registration");
    }
}
