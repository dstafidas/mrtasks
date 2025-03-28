package com.mrtasks;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class MessageConfig {

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("messages"); // Base name without locale suffix
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false); // Use messages_en as default if no exact match
        return source;
    }
}