package com.victadore.webmafia.mafia_web_of_lies.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configure static resource handling for sounds directory
        registry.addResourceHandler("/sounds/**")
                .addResourceLocations("classpath:/static/sounds/")
                .setCachePeriod(3600); // Cache for 1 hour
    }
} 