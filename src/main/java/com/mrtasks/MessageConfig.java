package com.mrtasks;

import com.mrtasks.repository.UserProfileRepository;
import com.mrtasks.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Configuration
public class MessageConfig implements WebMvcConfigurer {

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(Locale.ENGLISH); // Default for when no cookie is available
        return slr;
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            // Define supported languages
            private final Set<String> supportedLanguages = new HashSet<>(Arrays.asList("en", "el", "es", "fr", "de", "it"));

            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String language = "en"; // Default to English

                // Check for userLanguage cookie
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    language = Arrays.stream(cookies)
                            .filter(cookie -> "userLanguage".equals(cookie.getName()))
                            .map(Cookie::getValue)
                            .findFirst()
                            .orElse("en");
                }

                // Validate the language; fall back to English if invalid
                if (!supportedLanguages.contains(language)) {
                    language = "en";
                    // Optionally, update the cookie to the default language
                    Cookie languageCookie = new Cookie("userLanguage", language);
                    languageCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
                    languageCookie.setPath("/");
                    response.addCookie(languageCookie);
                    System.out.println("Invalid language in cookie; reset to default: " + language);
                }


                // Set the locale
                Locale locale = Locale.forLanguageTag(language);
                LocaleContextHolder.setLocale(locale);
                request.getSession().setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, locale);
                return true;
            }
        });
    }
}