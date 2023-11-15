package org.profinef.config;

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
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Основной класс конфигурации
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);

    /**
     * Конфигурация шаблонизатора, создающего страницы по шаблонам
     * @param templateEngine
     * @return
     */
    @Bean
    public ThymeleafViewResolver thymeleafViewResolver(@Autowired SpringTemplateEngine templateEngine ) {
        logger.info("Configuring ThymeleafViewResolver");
        ThymeleafViewResolver thymeleafViewResolver = new ThymeleafViewResolver();
        thymeleafViewResolver.setTemplateEngine( templateEngine );
        thymeleafViewResolver.setCharacterEncoding( "UTF-8" );
        return thymeleafViewResolver;
    }

    /**
     * Добавляет кастомную настройку делиметера в общую конфиграцию
     * @param registry
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new CustomStringToArrayConverter());
    }

    /**
     * Настройка перехватчика HTML запросов для проверки авторизации и прочего
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //создаем перехватчик проверяющий авторизацию пользователя
        HandlerInterceptor authorizationInterceptor = new HandlerInterceptor(){
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
                //получаем список ролей
                Role[] authorizedRoles = Role.values();
                //получаем сессию для которой проводим проверки
                HttpSession session = request.getSession();
                //удаляем имеющиеся сообщения об ошибках
                session.removeAttribute("error");
                //session.removeAttribute("pointer");
                //получаем авторизованного пользователя
                User user = (User) session.getAttribute("user");
                for (Role role : authorizedRoles) {
                    //проверка авторизации пользователя и его роли
                    if (user != null && user.getRole() == role) {

                        String mes = "";
                        //проверка пароля на безопасность
                        if (user.getPassword().equals("d17f25ecfbcc7857f7bebea469308be0b2580943e96d13a3ad98a13675c4bfc2")) {
                            mes += "Смените пароль. Использование стандартного пароля не безопасно \n";
                        }
                        //проверка имеющихся авторизаций данного пользователя
                        List<HttpSession> sessions = new HttpSessionConfig().getActiveSessions();
                        for (HttpSession ses : sessions) {
                            if (ses != null && !Objects.equals(ses.getId(), session.getId()) && Objects.equals(user, ses.getAttribute("user"))) {
                                mes += "Обнаружен вход с другого устройства. За детальной информацией обратитесь к администратору \n";
                                break;
                            }
                        }
                        //добавляем предупреждающее сообщение в сессию
                        session.setAttribute("mes", mes);
                        return true;
                    }
                }
                //в случае если пользоваетль не авторизован перенаправляем его на страницу авторизации
                response.sendRedirect(request.getContextPath() + "/log_in");
                return false;
            }
        };
        //настраиваем URL которые будут отслеживаться и игнорироваться перехватчиком
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/*").excludePathPatterns("/log_in", "/check_user", "/registration", "/error");

    }
}
